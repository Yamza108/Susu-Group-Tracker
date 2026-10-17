## What you built and how

I built a Java 17 command-line program that manages one rotating savings group (susu). The domain
rules live in `SusuGroup`, which has no I/O and is fully unit tested. A thin `Cli` class parses
commands (create, add, pay, close, report) and `Report` formats the output.

Key decisions:
- Money is stored as integer pesewas in a `long`, never floating point, so there are no rounding
  errors (0.10 + 0.20 is exactly 0.30). More than 2 decimals, zero, negative and non-numeric amounts
  are rejected.
- All validation runs before any state changes, so a rejected action never leaves the group
  half-updated. Errors are `SusuException`s which the CLI prints as `Error: ...`.
- Closing a week is a normal outcome, not an exception: `closeWeek()` returns a result saying whether
  it closed, the payout, or who still owes what. If anyone owes, nothing is paid and the week stays open.
- Rules the brief did not cover (documented in NOTES.md): the roster locks once the first contribution
  is recorded; at least 2 members are needed; only the current week accepts payments; the recipient
  also contributes; the susu ends after one full rotation; names are unique ignoring case.
- 18 JUnit 5 tests cover the brief's example end to end, partial payments, "week cannot close",
  unknown/duplicate members, zero/negative/over-payments, exact money, late-joiner rejection and a
  full cycle.

## How to run it

Requires Java 17+ and Maven.

Tests: `mvn test`

Run: `mvn -q compile` then `java -cp target/classes susu.Cli`
(without Maven: `mkdir -p out && javac -d out src/main/java/susu/*.java && java -cp out susu.Cli`)

Commands: `create 50.00`, `add Ama`, `pay 1 30 Esi` (week, amount, name), `close`, `report`, `help`, `quit`.

## Known limitations

- Data is in memory only: restarting the program starts a fresh group; one group per run.
- One rotation only; the susu completes after every member has received the pot.
- Roster is fixed once contributions start (no late joiners, no dropouts handled).
- Payout order is join order only; no undo/reversal of a mistaken payment.
With more time: persistence, a timestamped contribution log with reversals, late-joiner/dropout
policies, multiple cycles, late fees, and a small HTTP API.
