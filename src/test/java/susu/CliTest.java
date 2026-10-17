package susu;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

class CliTest {
    private String run(String script) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new Cli(new PrintStream(bytes, true)).run(new StringReader(script));
        return bytes.toString();
    }

    @Test
    void briefExample_throughTheCommandLine() throws IOException {
        String out = run(String.join("\n",
                "create 50.00", "add Ama", "add Kofi", "add Esi",
                "pay 1 50 Ama", "pay 1 50 Kofi", "pay 1 30 Esi",
                "close",
                "pay 1 20 Esi",
                "close",
                "report", "quit"));
        assertTrue(out.contains("Esi owes GHS 20.00"), out);
        assertTrue(out.contains("Ama receives GHS 150.00"), out);
        assertTrue(out.contains("Week 2 begins, Kofi is next"), out);
        assertTrue(out.contains("Week 1: Ama received GHS 150.00"), out);
    }

    @Test
    void errorsAreReportedNotThrown() throws IOException {
        String out = run(String.join("\n",
                "add Ama",                 // no group yet
                "create 50", "add Ama", "add Ama", "add Kofi", "pay 1 10 Nobody", "pay 1 -5 Ama",
                "pay 1 abc Ama", "pay x 5 Ama", "frobnicate"));
        assertTrue(out.contains("Create a group first"), out);
        assertTrue(out.contains("already exists"), out);
        assertTrue(out.contains("Unknown member"), out);
        assertTrue(out.contains("Invalid amount"), out);
        assertTrue(out.contains("Invalid week"), out);
        assertTrue(out.contains("unknown command"), out);
    }
}
