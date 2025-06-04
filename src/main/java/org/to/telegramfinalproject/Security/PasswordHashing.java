package org.to.telegramfinalproject.Security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordHashing {
    public PasswordHashing() {
    }

    public static String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();

            for(byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not found!", e);
        }
    }

    public static boolean verify(String password, String hashedPasswordFromDB) {
        return hash(password).equals(hashedPasswordFromDB);
    }
}

