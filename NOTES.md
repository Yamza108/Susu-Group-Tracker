# NOTES

## Assumptions (rules the brief did not cover)

1. **Roster locks when contributions start.** Members can be added only until the first
   contribution is recorded. After that, adding is rejected. This keeps the payout order and the
   pot size fixed, so nobody can join late and collect a pot they did not fund.
2. **Minimum 2 members.** Recording a contribution or closing a week needs at least 2 members.
3. **Only the current week is open.** `pay` takes a week number, but it must equal the current
   week. Closed weeks cannot be changed and future weeks cannot be pre-paid.
4. **The recipient also pays.** Everyone, including that week's recipient, contributes. This
   matches the brief's example (Ama pays 50, then receives 150).
5. **The susu ends after one full rotation.** After the last member has received the pot the
   group is COMPLETED and further payments/closes are rejected. One group = one cycle.
6. **Names are unique ignoring case and extra spaces** ("Ama", "ama", " AMA " are the same
   member). Names may contain spaces.
7. **Currency and size.** All amounts are GHS with at most 2 decimals. A single amount is
   capped at GHS 1,000,000.00 to keep the arithmetic safe.
8. **Closing with debts is not an error.** It returns a "cannot close" result listing who owes
   what, changes nothing, and the week stays open.
9. **Overpayment is rejected, not carried forward.** A payment above what the member owes this
   week is refused, as the brief requires.
10. **One group per run, kept in memory.** Restarting the program starts fresh.

## Decisions and why

- **Money as integer pesewas (`long`), never `double`.** 0.10 + 0.20 is exactly 0.30 here
  (there is a test for it). Text such as "12.345" is rejected rather than rounded. Parsing uses
  a strict pattern plus `BigDecimal`, so inputs like `1e2` or `-5` are refused.
- **Domain logic separated from I/O.** `SusuGroup` has no printing or parsing of user text, so
  the rules are tested directly. `Cli` is a thin layer, and `Report` only formats.
- **Validation throws `SusuException`; the CLI catches it** and prints `Error: ...`, so bad
  input never crashes the program or changes state. Every check runs before any state is
  modified, so a rejected payment leaves everything unchanged (tested).
- **`closeWeek()` returns a `CloseResult`** (closed or not, payout, who owes) instead of
  throwing, because "cannot close" is a normal outcome, not a failure.
- **`pay <week> <amount> <name>`** puts the name last so names with spaces work without quotes.
- **No dependencies except JUnit 5**, and Java records for small immutable data.

## What I would improve with more time

- **Persistence** (a file or database) so a group survives a restart, and support for several
  groups at once.
- **Contribution log with timestamps** instead of a running total per member, for audit and
  for undoing a mistaken entry (a reversal entry, not a delete).
- **Late-joiner and dropout policies**, e.g. allow joining at the end of the rotation, and
  define what happens if a member quits mid-cycle.
- **Payout order options** (lottery, or swapping turns by agreement) beyond join order.
- **Multiple cycles**: start a new rotation automatically when the first one completes.
- **Late fees / reminders** for members who are behind.
- **Wrapping the same `SusuGroup` in a small HTTP API** with proper error codes, and more
  tests (property-based tests on money, concurrency if it becomes a server).
