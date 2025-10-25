package jdbc;

import pojos.enums.Sex;
import jdbcInterfaces.PatientManager;
import pojos.Patient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

public class JDBCPatientManager implements PatientManager {

    private final ConnectionManager conMan;
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");

    public JDBCPatientManager(ConnectionManager conMan) {
        this.conMan = conMan;
    }

    @Override
    public void addPatient(Patient p) throws SQLException {
        String sql = "INSERT INTO patients(" +
                "namePatient, surnamePatient, dniPatient, dobPatient, emailPatient, " +
                "sexPatient, phoneNumberPatient, healthInsuranceNumberPatient, emergencyContactPatient) " +
                "VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection c = conMan.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, p.getNamePatient());
            ps.setString(2, p.getSurnamePatient());
            ps.setString(3, p.getDniPatient());
            // Guardar la fecha como String en formato dd/MM/yyyy
            String dobString = p.getDobPatient() != null ? SDF.format(p.getDobPatient()) : null;
            ps.setString(4, dobString);
            ps.setString(5, p.getEmailPatient());

            Sex sex = p.getSexPatient();
            ps.setString(6, sex == null ? null : sex.name());
            ps.setInt(7, p.getPhoneNumberPatient());
            ps.setInt(8, p.getHealthInsuranceNumberPatient());
            ps.setInt(9, p.getEmergencyContactPatient());
            ps.executeUpdate();
        }
    }

}
