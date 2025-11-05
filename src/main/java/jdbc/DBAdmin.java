package jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
//Esta clase la he creado para administrar la base de datos, en este caso borrar doctores por idDoctor.
public class DBAdmin {

    public static void main(String[] args) {
        // Backup antes de ejecutar: cp CardioLink.db CardioLink.db.bak
        ConnectionManager cm = new ConnectionManager();
        try {
            deleteDoctorsByIds(cm, 1, 2); // borrar idDoctor 1 y 2
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            cm.close();
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
}
