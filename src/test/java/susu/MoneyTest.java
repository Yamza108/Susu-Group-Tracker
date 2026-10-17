package susu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MoneyTest {
    @Test
    void parsesWholeAndDecimalAmounts() {
        assertEquals(5000L, Money.parse("50"));
        assertEquals(5000L, Money.parse("50.00"));
        assertEquals(5050L, Money.parse("50.5"));
        assertEquals(1L, Money.parse("0.01"));
    }

    @Test
    void rejectsBadInput() {
        for (String bad : new String[] {"", "abc", "1e2", "-1", "0", "0.00", "12.345", "1,000", null}) {
            assertThrows(SusuException.class, () -> Money.parse(bad), "should reject: " + bad);
        }
    }

    @Test
    void formatsWithTwoDecimals() {
        assertEquals("GHS 150.00", Money.format(15000L));
        assertEquals("GHS 0.05", Money.format(5L));
        assertEquals("20.00", Money.plain(2000L));
    }
}
