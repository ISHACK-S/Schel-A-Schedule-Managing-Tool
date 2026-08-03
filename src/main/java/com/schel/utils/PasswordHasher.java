package com.schel.utils;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordHasher {
    private static final int WORKLOAD = 12;

    private PasswordHasher() {}

    public static String hash(String plainPassword) {
        if (plainPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(WORKLOAD));
    }

    public static boolean verify(String plainPassword, String hashed) {
        if (plainPassword == null || hashed == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashed);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}