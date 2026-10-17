# Susu Manager

A small command-line program (Java) that manages one rotating savings group (susu).
Each week every member pays the same fixed amount (in GHS). When everyone has paid in full,
the whole pot is paid to the next member in join order.

## Requirements

- Java 17 or newer (JDK)
- Maven 3.8+ (only for the build and tests; the program itself has no dependencies)

## Run the tests

```bash
mvn test
```

18 JUnit 5 tests run (partial payments, "week cannot close", validation, exact money, the
example from the brief, a full cycle, and the CLI).

## Run the program

```bash
mvn -q compile
java -cp target/classes susu.Cli
```

Without Maven:

```bash
mkdir -p out && javac -d out src/main/java/susu/*.java
java -cp out susu.Cli
```

## Commands

| Command | What it does |
|---|---|
| `create <amount>` | Create the group, e.g. `create 50.00` |
| `add <name>` | Add a member (names may contain spaces; payout order = order added) |
| `pay <week> <amount> <name>` | Record a contribution; partial payments allowed |
| `close` | Close the current week. Pays out only if everyone has paid in full |
| `report` | Current week, who has paid in full, who owes what, payout history |
| `help` / `quit` | Help / exit |

Commands can also be piped in, which is handy for demos:

```bash
printf 'create 50\nadd Ama\nadd Kofi\nadd Esi\npay 1 50 Ama\npay 1 50 Kofi\npay 1 30 Esi\nclose\npay 1 20 Esi\nclose\nreport\n' \
  | java -cp target/classes susu.Cli
```

## Example session (from the brief)

```
> create 50.00
> add Ama / add Kofi / add Esi
> pay 1 50 Ama / pay 1 50 Kofi / pay 1 30 Esi
> close
Week 1 cannot close. No payout made.
Esi owes GHS 20.00
> pay 1 20 Esi
> close
Week 1 closed. Ama receives GHS 150.00.
Week 2 begins, Kofi is next.
```

## Project layout

```
src/main/java/susu/
  SusuGroup.java   all rules and state (no I/O)
  Money.java       exact money parsing/formatting (integer pesewas)
  Report.java      builds the report text
  Cli.java         command-line front end
  Payout.java, CloseResult.java, SusuException.java
src/test/java/susu/  SusuGroupTest, MoneyTest, CliTest
NOTES.md           assumptions, decisions, future improvements
```
