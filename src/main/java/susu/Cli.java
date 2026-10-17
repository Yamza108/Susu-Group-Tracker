package susu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;

public class Cli {
    private static final String HELP = String.join("\n",
            "Commands:",
            "  create <amount>              create the group, e.g. create 50.00",
            "  add <name>                   add a member (before any contribution is recorded)",
            "  pay <week> <amount> <name>   record a contribution, e.g. pay 1 30 Esi",
            "  close                        close the current week (pays out only if everyone paid in full)",
            "  report                       show current week, who owes what, and payout history",
            "  help                         show this help",
            "  quit                         exit");

    private final PrintStream out;
    private SusuGroup group;

    public Cli(PrintStream out) {
        this.out = out;
    }

    public static void main(String[] args) throws IOException {
        Cli cli = new Cli(System.out);
        cli.out.println("Susu manager. Type 'help' for commands.");
        cli.run(new InputStreamReader(System.in));
    }

    public void run(Reader input) throws IOException {
        BufferedReader reader = new BufferedReader(input);
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                return;
            }
            try {
                handle(line);
            } catch (SusuException e) {
                out.println("Error: " + e.getMessage());
            }
        }
    }

    private void handle(String line) {
        String[] parts = line.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String rest = parts.length > 1 ? parts[1].trim() : "";

        switch (command) {
            case "help" -> out.println(HELP);
            case "create" -> create(rest);
            case "add" -> add(rest);
            case "pay" -> pay(rest);
            case "close" -> close();
            case "report" -> report();
            default -> out.println("Error: unknown command '" + command + "'. Type 'help'.");
        }
    }

    private SusuGroup requireGroup() {
        if (group == null) {
            throw new SusuException("Create a group first, e.g. 'create 50.00'.");
        }
        return group;
    }

    private void create(String rest) {
        if (group != null) {
            throw new SusuException("A group already exists. Restart the program to create another.");
        }
        group = new SusuGroup(Money.parse(rest));
        out.println("Group created. Weekly contribution: " + Money.format(group.weeklyAmount()));
    }

    private void add(String rest) {
        SusuGroup g = requireGroup();
        g.addMember(rest);
        out.println("Added member '" + g.members().get(g.members().size() - 1)
                + "' (payout position " + g.members().size() + ").");
    }

    private void pay(String rest) {
        SusuGroup g = requireGroup();
        String[] p = rest.split("\\s+", 3);
        if (p.length < 3) {
            throw new SusuException("Usage: pay <week> <amount> <name>");
        }
        int week;
        try {
            week = Integer.parseInt(p[0]);
        } catch (NumberFormatException e) {
            throw new SusuException("Invalid week '" + p[0] + "'.");
        }
        long amount = Money.parse(p[1]);
        g.recordContribution(p[2], week, amount);
        long stillOwed = g.owed(p[2]);
        out.println("Recorded " + Money.format(amount) + " from " + p[2].trim() + " for week " + week + ". "
                + (stillOwed == 0 ? "Paid in full." : "Still owes " + Money.format(stillOwed) + "."));
    }

    private void close() {
        SusuGroup g = requireGroup();
        CloseResult r = g.closeWeek();
        if (r.closed()) {
            out.println("Week " + r.week() + " closed. " + r.payout().member()
                    + " receives " + Money.format(r.payout().amount()) + ".");
            if (g.status() == SusuGroup.Status.COMPLETED) {
                out.println("Every member has now received the pot. The susu is complete.");
            } else {
                out.println("Week " + g.currentWeek() + " begins, " + g.currentRecipient() + " is next.");
            }
        } else {
            out.println("Week " + r.week() + " cannot close. No payout made.");
            r.owing().forEach((name, amt) -> out.println(name + " owes " + Money.format(amt)));
        }
    }

    private void report() {
        out.print(Report.render(requireGroup()));
    }
}
