package susu;

import java.math.BigDecimal;
import java.util.regex.Pattern;


public final class Money {
    public static final String CURRENCY = "GHS";

    public static final long MAX_PESEWAS = 100_000_000L;

    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d+)?");

    private Money() {
    }


    public static long parse(String text) {
        if (text == null || !AMOUNT.matcher(text.trim()).matches()) {
            throw new SusuException("Invalid amount '" + text + "'. Use a number like 50 or 50.00.");
        }
        BigDecimal value = new BigDecimal(text.trim());
        if (value.stripTrailingZeros().scale() > 2) {
            throw new SusuException("Amount '" + text + "' has more than 2 decimal places.");
        }
        if (value.signum() <= 0) {
            throw new SusuException("Amount must be greater than zero.");
        }
        BigDecimal pesewas = value.movePointRight(2);
        if (pesewas.compareTo(BigDecimal.valueOf(MAX_PESEWAS)) > 0) {
            throw new SusuException("Amount is too large (maximum is " + format(MAX_PESEWAS) + ").");
        }
        return pesewas.longValueExact();
    }

    public static String format(long pesewas) {
        return CURRENCY + " " + plain(pesewas);
    }

    public static String plain(long pesewas) {
        long abs = Math.abs(pesewas);
        return (pesewas < 0 ? "-" : "") + (abs / 100) + "." + String.format("%02d", abs % 100);
    }
}
