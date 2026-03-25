package ginf.demosalle.util;


import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Utilitaire BCrypt pour le hachage et la vérification des mots de passe.
 * Utilise at.favre.lib:bcrypt — disponible sur Maven Central.
 */
public class PasswordUtil {

    private static final int COST = 12;

    private PasswordUtil() {}

    /** Hache un mot de passe en clair → hash BCrypt à stocker dans `mdp` */
    public static String hash(String plain) {
        return BCrypt.withDefaults().hashToString(COST, plain.toCharArray());
    }

    /** Vérifie qu'un mot de passe en clair correspond au hash stocké dans `mdp` */
    public static boolean verify(String plain, String hashed) {
        if (plain == null || hashed == null) return false;
        return BCrypt.verifyer().verify(plain.toCharArray(), hashed).verified;
    }
}