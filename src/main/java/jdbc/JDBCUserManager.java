package jdbc;

import jdbcInterfaces.UserManager;
import pojos.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JDBCUserManager implements UserManager{
    private Connection c;
    private ConnectionManager conMan;


    public JDBCUserManager(ConnectionManager conMan) {
        this.conMan = conMan;
    }



    public boolean createUser(String username, String encryptedPassword, Role role) {
        Connection conn = null;
        try {
            conn = conMan.getConnection();
            conn.setAutoCommit(false);

            String insertUserSql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, encryptedPassword);
                ps.setString(3, role.name());
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        conn.rollback();
                        return false;
                    }
                    int idUser = keys.getInt(1);

                    if (role == Role.PATIENT) {
                        
                        String insertPatient = "INSERT INTO patients (" +
                                "namePatient, surnamePatient, dniPatient, dobPatient, emailPatient, " +
                                "sexPatient, phoneNumberPatient, healthInsuranceNumberPatient, emergencyContactPatient, userId) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                        try (PreparedStatement psp = conn.prepareStatement(insertPatient)) {
                            psp.setString(1, "");
                            psp.setString(2, ""); // surname mínimo
                            psp.setString(3, "unknown-" + idUser); // dni único mínimo
                            psp.setString(4, "1970-01-01"); // dob mínimo
                            psp.setString(5, "unknown-" + idUser + "@example.com");
                            psp.setString(6, "UNSPECIFIED"); // sex
                            psp.setLong(7, idUser);
                            psp.setLong(8, 1000000L + idUser);
                            psp.setLong(9, 0L);
                            psp.setInt(10, idUser);
                            psp.executeUpdate();
                        }
                    } else if (role == Role.DOCTOR) {
                        String insertDoctor = "INSERT INTO doctors (userId, specialty, licenseNumber) VALUES (?, ?, ?)";
                        try (PreparedStatement psd = conn.prepareStatement(insertDoctor)) {
                            psd.setInt(1, idUser);
                            psd.setString(2, null);
                            psd.setString(3, null);
                            psd.executeUpdate();
                        }
                    }
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                // ignorar
            }
            System.out.println("Error creando usuario: " + e.getMessage());
            return false;
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true);
            } catch (SQLException ignored) {}
        }
    }

    @Override
    public void register(String username, String encryptedPassword, String role) {
        Role r;
        try {
            r = Role.valueOf(role.toUpperCase());
        } catch (Exception ex) {
            r = Role.PATIENT;
        }
        boolean ok = createUser(username, encryptedPassword, r);
        if (!ok) {
            System.out.println("Registro fallido para usuario: " + username);
        }
    }


/* NOW WE USE HASHING DONE WITH BCRYPT INSTEAD OF ENCRYPTION/DECRYPTION. THE METHODS ARE IN SERVER THREAD
    @Override
    public boolean verifyPassword(String username, String inputPassword) {
        String sql = "SELECT password FROM users WHERE username = ?";
        try (Connection c = conMan.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String encryptedStoredPass = rs.getString("password");
                String hashed = BCrypt.hashpw(inputPassword, );
                return encryptedStoredPass.equals(inputPassword);
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException("Decryption error: " + e.getMessage(), e);
        }
    }
*/


    public User getUserByCredentials(String username, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection c = conMan.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User u = new User();
                u.setIdUser(rs.getInt("idUser"));
                u.setUsername(rs.getString("username"));
                u.setPassword(rs.getString("password"));
                u.setRole(rs.getString("role"));
                return u;
            }
            return null;
        }
    }

    @Override
    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ? ";
        try (Connection c = conMan.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User u = new User();
                u.setIdUser(rs.getInt("idUser"));
                u.setUsername(rs.getString("username"));
                u.setPassword(rs.getString("password"));
                u.setRole(rs.getString("role"));
                return u;
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public String getRole(String username) {
        String role = null;
        String sql = "SELECT role FROM users WHERE username = ?";
        try (PreparedStatement pstmt = conMan.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                role = rs.getString("role");
            }
        } catch (SQLException e) {
            System.out.println("Error in the database");
            e.printStackTrace();
        }
        return role;
    }

    public List<String> getAllUsers() {
        List<String> users = new ArrayList<>();
        String sql = "SELECT username FROM users";
        try (Statement st = conMan.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) users.add(rs.getString("username"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

}
