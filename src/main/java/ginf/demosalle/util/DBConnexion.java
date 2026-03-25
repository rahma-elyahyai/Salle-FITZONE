package ginf.demosalle.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnexion {

    // 🔹 CONFIG NEON (à récupérer depuis Neon dashboard)
    private static final String DB_URL =
            "jdbc:postgresql://ep-summer-silence-amt5cfva-pooler.c-5.us-east-1.aws.neon.tech/neondb?sslmode=require";
    private static final String DB_USER = "neondb_owner";
    private static final String DB_PASSWORD = "npg_Nk8X4jruydDK";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver"); // ✅ Driver PostgreSQL
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver PostgreSQL introuvable : " + e.getMessage(), e);
        }

        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static boolean testerConnexion() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("❌ Connexion échouée : " + e.getMessage());
            return false;
        }
    }
}