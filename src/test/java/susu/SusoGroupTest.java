package susu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SusuGroupTest {
    private SusuGroup group;

    /** GHS 50.00 weekly, members Ama, Kofi, Esi. */
    @BeforeEach
    void setUp() {
        group = new SusuGroup(Money.parse("50.00"));
        group.addMember("Ama");
        group.addMember("Kofi");
        group.addMember("Esi");
    }

    private void pay(String name, int week, String amount) {
        group.recordContribution(name, week, Money.parse(amount));
    }

    @Test
    void briefExample_fullWalkThrough() {
        pay("Ama", 1, "50");
        pay("Kofi", 1, "50");
        pay("Esi", 1, "30");                                   // partial

        CloseResult first = group.closeWeek();
        assertFalse(first.closed());
        assertEquals(2000L, first.owing().get("Esi"));         // "Esi owes 20.00"
        assertEquals(1, group.currentWeek());
        assertTrue(group.history().isEmpty());

        pay("Esi", 1, "20");
        CloseResult second = group.closeWeek();
        assertTrue(second.closed());
        assertEquals("Ama", second.payout().member());
        assertEquals(15000L, second.payout().amount());        // Ama receives 150.00

        assertEquals(2, group.currentWeek());                  // week 2 begins
        assertEquals("Kofi", group.currentRecipient());        // Kofi is next
        assertEquals(5000L, group.owed("Ama"));                // week 2 starts with nothing paid
    }

    @Test
    void partialPayment_reducesWhatIsOwed() {
        pay("Esi", 1, "30");
        assertEquals(2000L, group.owed("Esi"));
        pay("Esi", 1, "5.50");
        assertEquals(1450L, group.owed("Esi"));
        assertFalse(group.paidInFull().contains("Esi"));
    }

    @Test
    void weekCannotClose_whenAnyoneOwes_andNothingIsPaidOut() {
        pay("Ama", 1, "50");
        CloseResult r = group.closeWeek();
        assertFalse(r.closed());
        assertNull(r.payout());
        assertEquals(List.of("Kofi", "Esi"), List.copyOf(r.owing().keySet()));
        assertEquals(5000L, r.owing().get("Kofi"));
        assertEquals(1, group.currentWeek());
        assertTrue(group.history().isEmpty());
    }

    @Test
    void rejectsUnknownMember() {
        assertThrows(SusuException.class, () -> pay("Yaw", 1, "10"));
    }

    @Test
    void rejectsDuplicateMember_ignoringCaseAndSpaces() {
        assertThrows(SusuException.class, () -> group.addMember("Ama"));
        assertThrows(SusuException.class, () -> group.addMember("  ama "));
        assertEquals(3, group.members().size());
    }

    @Test
    void rejectsZeroAndNegativeAmounts() {
        assertThrows(SusuException.class, () -> Money.parse("0"));
        assertThrows(SusuException.class, () -> Money.parse("-5"));
        assertThrows(SusuException.class, () -> group.recordContribution("Ama", 1, 0L));
        assertThrows(SusuException.class, () -> group.recordContribution("Ama", 1, -100L));
    }

    @Test
    void rejectsPaymentAboveWhatIsOwed() {
        assertThrows(SusuException.class, () -> pay("Ama", 1, "50.01"));
        pay("Ama", 1, "40");
        assertThrows(SusuException.class, () -> pay("Ama", 1, "10.01"));
        assertEquals(1000L, group.owed("Ama"));                // unchanged by the rejected payment
        pay("Ama", 1, "10");                                   // exact remainder is fine
        assertEquals(0L, group.owed("Ama"));
        assertThrows(SusuException.class, () -> pay("Ama", 1, "0.01"));
    }

    @Test
    void moneyIsExact_noRoundingErrors() {
        SusuGroup g = new SusuGroup(Money.parse("0.30"));
        g.addMember("A");
        g.addMember("B");
        // 0.10 + 0.20 must equal exactly 0.30 (it does not with doubles: 0.1 + 0.2 = 0.30000000000000004)
        g.recordContribution("A", 1, Money.parse("0.10"));
        g.recordContribution("A", 1, Money.parse("0.20"));
        g.recordContribution("B", 1, Money.parse("0.30"));
        CloseResult r = g.closeWeek();
        assertTrue(r.closed());
        assertEquals(60L, r.payout().amount());
        assertEquals("GHS 0.60", Money.format(r.payout().amount()));
    }

    @Test
    void cannotAddMemberAfterContributionsStart() {
        pay("Ama", 1, "10");
        assertThrows(SusuException.class, () -> group.addMember("Yaw"));
        assertEquals(3, group.members().size());
    }

    @Test
    void onlyTheCurrentWeekAcceptsContributions() {
        assertThrows(SusuException.class, () -> pay("Ama", 2, "10"));
        assertThrows(SusuException.class, () -> pay("Ama", 0, "10"));
    }

    @Test
    void needsAtLeastTwoMembers() {
        SusuGroup lonely = new SusuGroup(5000L);
        lonely.addMember("Ama");
        assertThrows(SusuException.class, () -> lonely.recordContribution("Ama", 1, 100L));
        assertThrows(SusuException.class, lonely::closeWeek);
    }

    @Test
    void fullCycle_paysEveryoneInJoinOrder_thenFinishes() {
        for (int week = 1; week <= 3; week++) {
            for (String m : List.of("Ama", "Kofi", "Esi")) {
                pay(m, week, "50");
            }
            assertTrue(group.closeWeek().closed());
        }
        assertEquals(SusuGroup.Status.COMPLETED, group.status());
        assertEquals(List.of("Ama", "Kofi", "Esi"),
                group.history().stream().map(Payout::member).toList());
        assertTrue(group.history().stream().allMatch(p -> p.amount() == 15000L));
        assertThrows(SusuException.class, () -> pay("Ama", 3, "1"));
        assertThrows(SusuException.class, group::closeWeek);
    }

    @Test
    void invalidGroupSetup_isRejected() {
        assertThrows(SusuException.class, () -> new SusuGroup(0L));
        assertThrows(SusuException.class, () -> group.addMember("   "));
    }
}
