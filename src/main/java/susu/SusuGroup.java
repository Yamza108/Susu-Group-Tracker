package susu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class SusuGroup {
    public static final int MIN_MEMBERS = 2;

    public enum Status { FORMING, ACTIVE, COMPLETED }

    private final long weeklyAmount;                                  // pesewas, per member per week
    private final List<String> members = new ArrayList<>();           // display names, in join order
    private final Map<String, Long> paidThisWeek = new LinkedHashMap<>(); // key -> pesewas paid
    private final List<Payout> history = new ArrayList<>();
    private int currentWeek = 1;
    private boolean started = false;      // true once the first contribution is recorded
    private boolean completed = false;    // true once every member has received the pot

    public SusuGroup(long weeklyAmount) {
        if (weeklyAmount <= 0) {
            throw new SusuException("Weekly contribution must be greater than zero.");
        }
        if (weeklyAmount > Money.MAX_PESEWAS) {
            throw new SusuException("Weekly contribution is too large.");
        }
        this.weeklyAmount = weeklyAmount;
    }

    public void addMember(String name) {
        String clean = cleanName(name);
        if (completed) {
            throw new SusuException("This susu has finished; members cannot be added.");
        }
        if (started) {
            throw new SusuException("Members cannot be added once contributions have started (week 1 is under way).");
        }
        if (find(clean) != null) {
            throw new SusuException("Member '" + clean + "' already exists.");
        }
        members.add(clean);
        paidThisWeek.put(key(clean), 0L);
    }

    public void recordContribution(String name, int week, long amount) {
        ensureRunning();
        String member = find(name == null ? "" : name.trim());
        if (member == null) {
            throw new SusuException("Unknown member '" + name + "'.");
        }
        if (week != currentWeek) {
            throw new SusuException("Week " + week + " is not open. The current week is " + currentWeek + ".");
        }
        if (amount <= 0) {
            throw new SusuException("Amount must be greater than zero.");
        }
        long owed = owed(member);
        if (amount > owed) {
            throw new SusuException(member + " only owes " + Money.format(owed)
                    + " for week " + currentWeek + "; cannot pay " + Money.format(amount) + ".");
        }
        paidThisWeek.merge(key(member), amount, Long::sum);
        started = true;
    }

    public CloseResult closeWeek() {
        ensureRunning();
        Map<String, Long> owing = owingThisWeek();
        if (!owing.isEmpty()) {
            return new CloseResult(false, currentWeek, null, owing);
        }
        String recipient = members.get(currentWeek - 1);
        long pot = Math.multiplyExact(weeklyAmount, (long) members.size());
        Payout payout = new Payout(currentWeek, recipient, pot);
        history.add(payout);

        int closedWeek = currentWeek;
        if (currentWeek == members.size()) {
            completed = true;           // everyone has now received the pot
        } else {
            currentWeek++;
            paidThisWeek.replaceAll((k, v) -> 0L);
        }
        return new CloseResult(true, closedWeek, payout, Collections.emptyMap());
    }

    public long weeklyAmount() {
        return weeklyAmount;
    }

    public int currentWeek() {
        return currentWeek;
    }

    public Status status() {
        if (completed) {
            return Status.COMPLETED;
        }
        return started ? Status.ACTIVE : Status.FORMING;
    }

    public List<String> members() {
        return List.copyOf(members);
    }

    public List<Payout> history() {
        return List.copyOf(history);
    }

    /** Who receives the pot this week, or null if the susu is finished or has too few members. */
    public String currentRecipient() {
        if (completed || members.size() < MIN_MEMBERS) {
            return null;
        }
        return members.get(currentWeek - 1);
    }

    /** Pesewas the member still owes for the current week (0 if paid in full). */
    public long owed(String name) {
        String member = find(name == null ? "" : name.trim());
        if (member == null) {
            throw new SusuException("Unknown member '" + name + "'.");
        }
        if (completed) {
            return 0L;
        }
        return weeklyAmount - paidThisWeek.get(key(member));
    }

    public List<String> paidInFull() {
        List<String> result = new ArrayList<>();
        if (!completed) {
            for (String m : members) {
                if (owed(m) == 0) {
                    result.add(m);
                }
            }
        }
        return result;
    }

    public Map<String, Long> owingThisWeek() {
        Map<String, Long> owing = new LinkedHashMap<>();
        if (!completed) {
            for (String m : members) {
                long o = owed(m);
                if (o > 0) {
                    owing.put(m, o);
                }
            }
        }
        return owing;
    }

    private void ensureRunning() {
        if (completed) {
            throw new SusuException("This susu has finished: every member has received the pot.");
        }
        if (members.size() < MIN_MEMBERS) {
            throw new SusuException("Add at least " + MIN_MEMBERS + " members before recording or closing.");
        }
    }

    private String find(String name) {
        for (String m : members) {
            if (key(m).equals(key(name))) {
                return m;
            }
        }
        return null;
    }

    private static String cleanName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new SusuException("Member name cannot be empty.");
        }
        return name.trim().replaceAll("\\s+", " ");
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
