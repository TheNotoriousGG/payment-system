package org.example.individualsapi.util;

public class StrUtils {

    public static String usernameFromEmail(String email) {
        return email.substring(0, email.indexOf("@"));
    }
}
