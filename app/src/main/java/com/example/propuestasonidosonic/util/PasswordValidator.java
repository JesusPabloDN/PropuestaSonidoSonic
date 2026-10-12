package com.example.propuestasonidosonic.util;

import java.util.regex.Pattern;

public class PasswordValidator {
    private static final int MIN_LENGTH = 8;
    private static final Pattern HAS_UPPER = Pattern.compile("[A-Z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("[0-9]");
    private static final Pattern HAS_SPECIAL = Pattern.compile("[^a-zA-Z0-9\\s]");

    public static boolean isValid(String password) {
        if (password == null) {
            return false;
        }
        if (password.length() < MIN_LENGTH) {
            return false;
        }
        if (!HAS_UPPER.matcher(password).find()) {
            return false;
        }
        if (!HAS_DIGIT.matcher(password).find()) {
            return false;
        }
        return HAS_SPECIAL.matcher(password).find();
    }
}
