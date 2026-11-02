package ca.sfu.cmpt276.nywfareconfig.security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static boolean exceedsLimit(String password) {
        return password != null && password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES;
    }

    public static List<String> validate(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errors.add("Password is required.");
            return errors;
        }

        if (password.length() < MIN_LENGTH) {
            errors.add("Password must be at least 8 characters.");
        }
        if (exceedsLimit(password)) {
            errors.add("Password must be at most 72 characters.");
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasSymbol = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c)) {
                hasSymbol = true;
            }
        }

        if (!hasUpper) {
            errors.add("Password must include an uppercase letter.");
        }
        if (!hasLower) {
            errors.add("Password must include a lowercase letter.");
        }
        if (!hasSymbol) {
            errors.add("Password must include a symbol, such as ! @ # $ %.");
        }

        return errors;
    }
}