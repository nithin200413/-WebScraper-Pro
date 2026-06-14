package com.webscraper.auth;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    /** Hash a plaintext password with a generated salt. */
    public static String hash(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(12));
    }

    /** Verify a plaintext password against a stored hash. */
    public static boolean verify(String plain, String hash) {
        try {
            return BCrypt.checkpw(plain, hash);
        } catch (Exception e) {
            return false;
        }
    }
}
