package ginf.demosalle.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class PasswordUtil {

    private PasswordUtil() {}

    public static String hash(String plaintext) {
        return BCrypt.withDefaults().hashToString(12, plaintext.toCharArray());
    }

    public static boolean verify(String plaintext, String hashed) {
        BCrypt.Result result = BCrypt.verifyer().verify(plaintext.toCharArray(), hashed);
        return result.verified;
    }
}