package susu;

import java.util.List;
import java.util.Map;

/** Builds the text report: current week, who has paid in full, who owes what, payout history. */
public final class Report {
    private Report() {
    }

    public static String render(SusuGroup g) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Susu report =====\n");
        sb.append("Weekly contribution: ").append(Money.format(g.weeklyAmount())).append('\n');
        sb.append("Members (payout order): ").append(g.members().isEmpty() ? "(none yet)" : String.join(", ", g.members())).append('\n');

        if (g.status() == SusuGroup.Status.COMPLETED) {
            sb.append("Status: COMPLETED - every member has received the pot.\n");
        } else {
            sb.append("Current week: ").append(g.currentWeek());
            if (g.currentRecipient() != null) {
                sb.append(" (pot goes to ").append(g.currentRecipient()).append(')');
            }
            sb.append('\n');

            List<String> paid = g.paidInFull();
            sb.append("Paid in full: ").append(paid.isEmpty() ? "(nobody)" : String.join(", ", paid)).append('\n');

            sb.append("Still owing:\n");
            Map<String, Long> owing = g.owingThisWeek();
            if (owing.isEmpty()) {
                sb.append("  (nobody)\n");
            } else {
                owing.forEach((name, amt) ->
                        sb.append("  ").append(name).append(" owes ").append(Money.format(amt)).append('\n'));
            }
        }

        sb.append("Payout history:\n");
        if (g.history().isEmpty()) {
            sb.append("  (no payouts yet)\n");
        } else {
            for (Payout p : g.history()) {
                sb.append("  Week ").append(p.week()).append(": ").append(p.member())
                        .append(" received ").append(Money.format(p.amount())).append('\n');
            }
        }
        return sb.toString();
    }
}
