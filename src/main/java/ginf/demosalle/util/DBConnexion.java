package ginf.demosalle.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utilitaire de connexion JDBC directe à MySQL.
 * ⚠️  Changez les 3 constantes selon votre config MySQL Workbench.
 */
public class DBConnexion {

    // ══════════════════════════════════════════════════════════════
    //  ⚙️  CONFIGURATION — À modifier selon votre MySQL Workbench
    // ══════════════════════════════════════════════════════════════

    /** Nom exact de votre base de données */
    private static final String DB_NAME = "gestion_salle_sport";

    /** Utilisateur MySQL (généralement "root") */
    private static final String DB_USER = "root";

    /** Mot de passe MySQL Workbench (vide "" si aucun) */
    private static final String DB_PASSWORD = "Root1234!";

    // ══════════════════════════════════════════════════════════════
    //  Ne pas modifier en dessous
    // ══════════════════════════════════════════════════════════════

    private static final String DB_URL =
            "jdbc:mysql://localhost:3307/" + DB_NAME
                    + "?useSSL=false"
                    + "&serverTimezone=UTC"
                    + "&allowPublicKeyRetrieval=true"
                    + "&useUnicode=true"
                    + "&characterEncoding=UTF-8";

    /**
     * Retourne une nouvelle connexion JDBC à MySQL.
     * À fermer après utilisation dans un bloc try-with-resources.
     *
     * Exemple d'utilisation :
     * <pre>
     *   try (Connection conn = DBConnexion.getConnection()) {
     *       PreparedStatement ps = conn.prepareStatement("SELECT * FROM utilisateur");
     *       ResultSet rs = ps.executeQuery();
     *       ...
     *   }
     * </pre>
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL introuvable : " + e.getMessage(), e);
        }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /**
     * Teste la connexion à la base de données.
     * Retourne true si la connexion réussit, false sinon.
     */
    public static boolean testerConnexion() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("❌ Connexion échouée : " + e.getMessage());
            return false;
        }
    }
}