package jdbc;

import jdbcInterfaces.PatientManager;
import jdbcInterfaces.UserManager;

import java.sql.*;

public class ConnectionManager {

    private static final String URL = "jdbc:sqlite:CardioLink.db";
    private Connection c;
    private PatientManager patientMan;
    private UserManager userMan;

    public ConnectionManager() {
        connect();
        try {
            ensureSchema();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure DB schema", e);
        }
        patientMan = new JDBCPatientManager(this);
        userMan = new JDBCUserManager(this);
    }

    public Connection getConnection() throws SQLException {
        // Return a new connection for short-lived operations to avoid threading issues
        Connection conn = DriverManager.getConnection(URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys=ON");
        }
        return conn;
    }

    private void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection(URL);
            try (Statement st = c.createStatement()) {
                st.execute("PRAGMA foreign_keys=ON");
            }
        } catch (ClassNotFoundException cnfE) {
            System.out.println("Databases libraries not loaded");
            cnfE.printStackTrace();
        } catch (SQLException sqlE) {
            System.out.println("Error with database");
            sqlE.printStackTrace();
        }
    }

    private void ensureSchema() throws SQLException {
        if (c == null || c.isClosed()) connect();
        c.setAutoCommit(false);
        try (Statement st = c.createStatement()) {
            // Create in order: users -> patients -> doctors -> diagnosisFile
            st.execute("PRAGMA foreign_keys = OFF");

            st.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "idUser INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT UNIQUE NOT NULL," +
                    "password TEXT NOT NULL," +
                    "role TEXT NOT NULL DEFAULT 'PATIENT' CHECK(role IN ('PATIENT','DOCTOR'))" +
                    ");");

            st.execute("CREATE TABLE IF NOT EXISTS patients (" +
                    "idPatient INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "userId INTEGER UNIQUE," +
                    "namePatient TEXT," +
                    "surnamePatient TEXT," +
                    "dniPatient TEXT UNIQUE," +
                    "dobPatient DATE," +
                    "emailPatient TEXT UNIQUE," +
                    "sexPatient TEXT," +
                    "phoneNumberPatient INTEGER," +
                    "healthInsuranceNumberPatient INTEGER," +
                    "emergencyContactPatient INTEGER," +
                    "FOREIGN KEY(userId) REFERENCES users(idUser)" +
                    ");");

            st.execute("CREATE TABLE IF NOT EXISTS doctors (" +
                    "idDoctor INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "userId INTEGER UNIQUE," +
                    "nameDoctor TEXT," +
                    "surnameDoctor TEXT," +
                    "dniDoctor TEXT UNIQUE," +
                    "dobDoctor DATE," +
                    "emailDoctor TEXT," +
                    "sexDoctor TEXT," +
                    "specialty TEXT," +
                    "licenseNumber TEXT," +
                    "FOREIGN KEY(userId) REFERENCES users(idUser)" +
                    ");");

            st.execute("CREATE TABLE IF NOT EXISTS diagnosisFiles (" +
                    "idDiagnosisFile INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "symptoms TEXT," +
                    "diagnosis TEXT," +
                    "medication TEXT," +
                    "date DATE NOT NULL," +
                    "patientId INTEGER," +
                    "status BOOLEAN," +
                    "FOREIGN KEY(patientId) REFERENCES patients(idPatient) ON DELETE CASCADE" +
                    ");");


            st.execute("CREATE TABLE IF NOT EXISTS recordings (" +
                    "id_recording INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "diagnosisFileId INTEGER," +
                    "data TEXT," +
                    "sequence INTEGER," +
                    "anomaly BOOLEAN," +
                    "FOREIGN KEY(diagnosisFileId) REFERENCES diagnosisFiles(idDiagnosisFile) ON DELETE CASCADE" +
                    ");");

            st.execute("PRAGMA foreign_keys = ON");

            // Add legacy columns only if missing (safe ALTERs; SQLite can't add FK via ALTER)
            if (!columnExists("doctors", "sexDoctor")) {
                // sexDoctor already included in CREATE above; kept for safety in older DBs
                try {
                    st.execute("ALTER TABLE doctors ADD COLUMN sexDoctor TEXT");
                } catch (SQLException ignore) {
                    // ignore if another thread added meanwhile
                }
            }
            if (!columnExists("patients", "userId")) {
                try {
                    st.execute("ALTER TABLE patients ADD COLUMN userId INTEGER UNIQUE");
                } catch (SQLException ignore) {
                }
            }

            c.commit();
        } catch (SQLException ex) {
            c.rollback();
            throw ex;
        }
    }

    private boolean columnExists(String table, String column) throws SQLException {
        String sql = "PRAGMA table_info(" + table + ")";
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("name");
                if (column.equalsIgnoreCase(name)) return true;
            }
        }
        return false;
    }

    public PatientManager getPatientMan() { return patientMan; }

    public UserManager getUserMan() { return userMan; }

    public void close() {
        try {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
