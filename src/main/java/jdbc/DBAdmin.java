 package jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

// Clase para administración de la base de datos (borrado de pacientes, usuarios, etc.)
public class DBAdmin {

    public static void main(String[] args) {
        // Hacer backup antes de ejecutar manualmente: cp CardioLink.db CardioLink.db.bak
        ConnectionManager cm = new ConnectionManager();
        try {
            // Conservar id = 1 y borrar el resto
            deleteUsersExcept(cm, 1);
            deletePatientsExcept(cm, 1);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            cm.close();
        }
    }

    public static int deleteUsersExcept(ConnectionManager cm, int keepId) throws SQLException {
        try (Connection c = cm.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE idUser <> ?")) {
                    ps.setInt(1, keepId);
                    int deleted = ps.executeUpdate();

                    // Ajustar sqlite_sequence para que el siguiente id sea keepId + 1
                    int seqValue = keepId;
                    String updateSeq = "UPDATE sqlite_sequence SET seq = ? WHERE name = 'users'";
                    try (PreparedStatement psSeq = c.prepareStatement(updateSeq)) {
                        psSeq.setInt(1, seqValue);
                        int updated = psSeq.executeUpdate();
                        if (updated == 0) {
                            try (PreparedStatement psInsert = c.prepareStatement(
                                    "INSERT OR REPLACE INTO sqlite_sequence(name, seq) VALUES('users', ?)")) {
                                psInsert.setInt(1, seqValue);
                                psInsert.executeUpdate();
                            }
                        }
                    } catch (SQLException exSeq) {
                        // Si no es SQLite o no existe sqlite_sequence, ignorar el ajuste de secuencia
                        System.err.println("No se pudo actualizar sqlite_sequence para 'users': " + exSeq.getMessage());
                    }

                    c.commit();
                    System.out.println("Usuarios borrados (excepto idUser " + keepId + "): " + deleted);
                    return deleted;
                }
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public static int deleteDoctorsByIds(ConnectionManager cm, int... ids) throws SQLException {
        if (ids == null || ids.length == 0) return 0;
        StringBuilder sql = new StringBuilder("DELETE FROM doctors WHERE idDoctor IN (");
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(")");

        try (Connection c = cm.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                for (int i = 0; i < ids.length; i++) {
                    ps.setInt(i + 1, ids[i]);
                }
                int deleted = ps.executeUpdate();
                c.commit();
                System.out.println("Filas borradas: " + deleted);
                return deleted;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    // Borra todos los pacientes excepto el indicado por keepId y ajusta la secuencia para que el siguiente id sea keepId+1
    public static int deletePatientsExcept(ConnectionManager cm, int keepId) throws SQLException {
        try (Connection c = cm.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM patients WHERE idPatient <> ?")) {
                    ps.setInt(1, keepId);
                    int deleted = ps.executeUpdate();

                    int seqValue = keepId;
                    String updateSeq = "UPDATE sqlite_sequence SET seq = ? WHERE name = 'patients'";
                    try (PreparedStatement psSeq = c.prepareStatement(updateSeq)) {
                        psSeq.setInt(1, seqValue);
                        int updated = psSeq.executeUpdate();
                        if (updated == 0) {
                            try (PreparedStatement psInsert = c.prepareStatement(
                                    "INSERT OR REPLACE INTO sqlite_sequence(name, seq) VALUES('patients', ?)")) {
                                psInsert.setInt(1, seqValue);
                                psInsert.executeUpdate();
                            }
                        }
                    } catch (SQLException exSeq) {
                        System.err.println("No se pudo actualizar sqlite_sequence: " + exSeq.getMessage());
                    }

                    c.commit();
                    System.out.println("Pacientes borrados (excepto id " + keepId + "): " + deleted);
                    return deleted;
                }
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
