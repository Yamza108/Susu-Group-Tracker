package susu;

import java.util.Map;

public record CloseResult(boolean closed, int week, Payout payout, Map<String, Long> owing) {
}
