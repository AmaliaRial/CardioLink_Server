package jdbc;

import pojos.DiagnosisFile;
import pojos.enums.Sex;
import jdbcInterfaces.PatientManager;
import pojos.Patient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JDBCPatientManager implements PatientManager {

    private Connection c;
    private final ConnectionManager conMan;
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd-MM-yyyy");
    private static final DateTimeFormatter DF_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public JDBCPatientManager(ConnectionManager conMan) {
        this.conMan = conMan;
    }

    public void addPatient(Patient patient) throws SQLException {
        String query = "INSERT INTO patients (userId, namePatient, surnamePatient, dniPatient, dobPatient, emailPatient, sexPatient, phoneNumberPatient, healthInsuranceNumberPatient, emergencyContactPatient) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection c = conMan.getConnection();
             PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, patient.getUserId());
            ps.setString(2, patient.getNamePatient());
            ps.setString(3, patient.getSurnamePatient());
            ps.setString(4, patient.getDniPatient());

            java.sql.Date dobSql = patient.getDobPatient();
            String dobStr = null;
            if (dobSql != null) {
                dobStr = SDF.format(dobSql);
            }
            ps.setString(5, dobStr);

            ps.setString(6, patient.getEmailPatient());
            ps.setString(7, patient.getSexPatient().toString());
            ps.setInt(8, patient.getPhoneNumberPatient());
            ps.setInt(9, patient.getHealthInsuranceNumberPatient());
            ps.setInt(10, patient.getEmergencyContactPatient());

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("Inserting patient failed, no rows affected.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error inserting patient into the database: " + e.getMessage());
        }
    }

    public void registerPatient(String username, String password, Patient p) throws SQLException {
        String userSql = "INSERT INTO users(username,password,role) VALUES(?,?, 'PATIENT')";
        String patientSql = "INSERT INTO patients(" +
                "userId, namePatient, surnamePatient, dniPatient, dobPatient, emailPatient, " +
                "sexPatient, phoneNumberPatient, healthInsuranceNumberPatient, emergencyContactPatient) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?)";

        try (Connection c = conMan.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement psUser = c.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement psPatient = c.prepareStatement(patientSql)) {

                psUser.setString(1, username);
                psUser.setString(2, password);
                psUser.executeUpdate();

                try (ResultSet keys = psUser.getGeneratedKeys()) {
                    if (!keys.next()) {
                        c.rollback();
                        throw new SQLException("No se obtuvo id generado para user");
                    }
                    int userId = keys.getInt(1);
                    p.setUserId(userId);

                    Integer uid = p.getUserId();
                    if (uid == null || uid <= 0) psPatient.setNull(1, java.sql.Types.INTEGER);
                    else psPatient.setInt(1, uid);

                    psPatient.setString(2, p.getNamePatient());
                    psPatient.setString(3, p.getSurnamePatient());
                    psPatient.setString(4, p.getDniPatient());

                    if (p.getDobPatient() != null) {
                        String dobStr = SDF.format(p.getDobPatient());
                        psPatient.setString(5, dobStr);
                    } else {
                        psPatient.setNull(5, java.sql.Types.VARCHAR);
                    }

                    psPatient.setString(6, p.getEmailPatient());

                    Sex sex = p.getSexPatient();
                    psPatient.setString(7, sex == null ? null : sex.name());

                    Integer phone = p.getPhoneNumberPatient();
                    if (phone == null || phone <= 0) psPatient.setNull(8, java.sql.Types.INTEGER);
                    else psPatient.setInt(8, phone);

                    Integer health = p.getHealthInsuranceNumberPatient();
                    if (health == null || health <= 0) psPatient.setNull(9, java.sql.Types.INTEGER);
                    else psPatient.setInt(9, health);

                    Integer emergency = p.getEmergencyContactPatient();
                    if (emergency == null || emergency <= 0) psPatient.setNull(10, java.sql.Types.INTEGER);
                    else psPatient.setInt(10, emergency);

                    psPatient.executeUpdate();
                    c.commit();
                }
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    @Override
    public Patient getPatientById(int idPatient) throws SQLException {
        String sql = "SELECT * FROM patients WHERE idPatient = ?";
        try (Connection c = conMan.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idPatient);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String sexStr = rs.getString("sexPatient");
                Sex sexEnum = null;

                if (sexStr != null) {
                    try {
                        if (sexStr.equalsIgnoreCase("MALE")) sexEnum = Sex.MALE;
                        else if (sexStr.equalsIgnoreCase("FEMALE")) sexEnum = Sex.FEMALE;
                        else sexEnum = Sex.valueOf(sexStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        System.out.println("⚠️ Invalid sex value in DB: " + sexStr);
                    }
                }

                String dobStr = rs.getString("dobPatient");
                java.sql.Date dobSql = null;
                if (dobStr != null && !dobStr.isEmpty()) {
                    try {
                        java.util.Date utilDate = SDF.parse(dobStr);
                        dobSql = new java.sql.Date(utilDate.getTime());
                    } catch (java.text.ParseException e) {
                        System.out.println("Invalid dobPatient format: " + dobStr);
                    }
                }

                Patient p = new Patient(rs.getInt("idPatient"),
                        rs.getString("namePatient"),
                        rs.getString("surnamePatient"),
                        rs.getString("dniPatient"),
                        dobSql,
                        rs.getString("emailPatient"),
                        sexEnum,
                        rs.getInt("phoneNumberPatient"),
                        rs.getInt("healthInsuranceNumberPatient"),
                        rs.getInt("emergencyContactPatient"),
                        rs.getInt("userId"));
                return p;
            }
            return null;
        }
    }
    public Patient getPatientByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM patients WHERE userId = ?";
        try (Connection c = conMan.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String sexStr = rs.getString("sexPatient");
                Sex sexEnum = null;

                if (sexStr != null) {
                    try {
                        if (sexStr.equalsIgnoreCase("MALE")) sexEnum = Sex.MALE;
                        else if (sexStr.equalsIgnoreCase("FEMALE")) sexEnum = Sex.FEMALE;
                        else sexEnum = Sex.valueOf(sexStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        System.out.println("⚠️ Invalid sex value in DB: " + sexStr);
                    }
                }

                String dobStr = rs.getString("dobPatient");
                java.sql.Date dobSql = null;
                if (dobStr != null && !dobStr.isEmpty()) {
                    try {
                        java.util.Date utilDate = SDF.parse(dobStr);
                        dobSql = new java.sql.Date(utilDate.getTime());
                    } catch (java.text.ParseException e) {
                        System.out.println("Invalid dobPatient format: " + dobStr);
                    }
                }

                Patient p = new Patient(rs.getInt("idPatient"),
                        rs.getString("namePatient"),
                        rs.getString("surnamePatient"),
                        rs.getString("dniPatient"),
                        dobSql,
                        rs.getString("emailPatient"),
                        sexEnum,
                        rs.getInt("phoneNumberPatient"),
                        rs.getInt("healthInsuranceNumberPatient"),
                        rs.getInt("emergencyContactPatient"),
                        rs.getInt("userId"));
                return p;
            }
            return null;
        }
    }

    @Override
    public List<DiagnosisFile> getAllDiagnosisFilesFromPatient(int idPatient) {
        List<DiagnosisFile> diagnosisFiles = new ArrayList<>();

        String sql = "SELECT df.id, df.symptoms, df.diagnosis, df.medication, df.date, df.patientId, df.status " +
                "FROM diagnosisFiles df " +
                "WHERE df.patientId = ? AND df.status = 1";

        try (Connection c = conMan.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, idPatient);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");

                    ArrayList<String> symptoms = new ArrayList<>();
                    String symptomsStr = rs.getString("symptoms");
                    if (symptomsStr != null && !symptomsStr.isEmpty()) {
                        for (String symptom : symptomsStr.split(",")) {
                            symptoms.add(symptom.trim());
                        }
                    }

                    String diagnosis = rs.getString("diagnosis");
                    String medication = rs.getString("medication");

                    LocalDate date = null;
                    String dateStr = rs.getString("date");
                    if (dateStr != null) {
                        dateStr = dateStr.trim();
                        if (dateStr.matches("\\d+")) {
                            try {
                                long millis = Long.parseLong(dateStr);
                                date = java.time.Instant.ofEpochMilli(millis)
                                        .atZone(java.time.ZoneId.systemDefault())
                                        .toLocalDate();
                            } catch (NumberFormatException ignored) {
                            }
                        } else {
                            boolean parsed = false;
                            try {
                                date = LocalDate.parse(dateStr, DF_DATE_FORMATTER);
                                parsed = true;
                            } catch (DateTimeParseException ignored) {
                            }
                            if (!parsed) {
                                try {
                                    date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                                    parsed = true;
                                } catch (DateTimeParseException ignored) {
                                }
                            }
                            if (!parsed) {
                                try {
                                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(dateStr,
                                            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS"));
                                    date = ldt.toLocalDate();
                                } catch (DateTimeParseException e2) {
                                    try {
                                        java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(dateStr,
                                                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
                                        date = ldt.toLocalDate();
                                    } catch (DateTimeParseException ignored2) {
                                    }
                                }
                            }
                        }
                    } else {
                        long millis = rs.getLong("date");
                        if (!rs.wasNull()) {
                            date = java.time.Instant.ofEpochMilli(millis)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDate();
                        }
                    }

                    int patientId = rs.getInt("patientId");
                    boolean status = rs.getBoolean("status");

                    DiagnosisFile diagnosisFile = new DiagnosisFile(id, symptoms, diagnosis, medication, date, patientId, status);
                    diagnosisFiles.add(diagnosisFile);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return diagnosisFiles;
    }

    @Override
    public String getFragmentOfRecording(int idDiagnosisFile, int position) throws SQLException {
        String sql = "SELECT data FROM recordings WHERE diagnosisFileId = ? AND sequence = ?";

        try (Connection c = conMan.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, idDiagnosisFile);
            ps.setInt(2, position);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("data");
            } else {
                return "No fragment found for the given DiagnosisFileId and position.";
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving fragment from the database.");
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Boolean> getSateOfFragmentsOfRecordingByID(int idDiagnosisFile) throws SQLException {
        List<Boolean> anomalyStates = new ArrayList<>();

        String sql = "SELECT anomaly FROM recordings WHERE diagnosisFileId = ?";

        try (Connection c = conMan.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, idDiagnosisFile);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                boolean anomaly = rs.getBoolean("anomaly");
                anomalyStates.add(anomaly);
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving anomaly states from the database.");
            e.printStackTrace();
            return Collections.emptyList();
        }

        return anomalyStates;
    }

    @Override
    public void AddNewDiagnosisFile(DiagnosisFile diagnosisFile) throws SQLException {
        String sql = "INSERT INTO diagnosisFiles (symptoms, diagnosis, medication, date, patientId, status) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection c = conMan.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            String symptomsSerialized = (diagnosisFile.getSymptoms() == null || diagnosisFile.getSymptoms().isEmpty())
                    ? null
                    : String.join(", ", diagnosisFile.getSymptoms());

            ps.setString(1, symptomsSerialized);
            ps.setString(2, diagnosisFile.getDiagnosis());
            ps.setString(3, diagnosisFile.getMedication());

            LocalDate dfDate = diagnosisFile.getDate();
            String dfDateStr = null;
            if (dfDate != null) {
                dfDateStr = dfDate.format(DF_DATE_FORMATTER);
            }
            if (dfDateStr != null) {
                ps.setString(4, dfDateStr);
            } else {
                ps.setNull(4, java.sql.Types.VARCHAR);
            }

            ps.setInt(5, diagnosisFile.getPatientId());
            ps.setBoolean(6, diagnosisFile.getStatus());

            ps.executeUpdate();
            System.out.println("New DiagnosisFile added successfully.");

        } catch (SQLException e) {
            System.out.println("Error adding new DiagnosisFile to the database.");
            e.printStackTrace();
        }
    }

    @Override
    public int returnIdOfLastDiagnosisFile() throws SQLException {
        String sql = "SELECT MAX(idDiagnosisFile) AS lastId FROM diagnosisFiles";

        try (Connection c = conMan.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("lastId");
            } else {
                throw new SQLException("No DiagnosisFiles found in the database.");
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving the last DiagnosisFile ID from the database.");
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void saveFragmentOfRecording(int idDiagnosisFile, String fragmentData, int sequence) throws SQLException {
        String sql = "INSERT INTO recordings (diagnosisFileId, data, sequence, anomaly) " +
                "VALUES (?, ?, ?, ?)";

        System.out.println("DEBUG SQL saveFragmentOfRecording: " + sql);
        Connection c = conMan.getConnection();
        System.out.println("coje la connexion");
        PreparedStatement ps = c.prepareStatement(sql);
        System.out.println("prepara el statement");

        ps.setInt(1, idDiagnosisFile);
        ps.setString(2, fragmentData);
        ps.setInt(3, sequence);
        ps.setBoolean(4, false);

        ps.executeUpdate();
        System.out.println("Fragment of recording saved successfully.");
    }

    @Override
    public int getNextSequenceNumber(Connection c, int idDiagnosisFile) {
        String sql = "SELECT MAX(sequence) AS maxSeq FROM recordings WHERE diagnosisFileId = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idDiagnosisFile);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int maxSeq = rs.getInt("maxSeq");
                return maxSeq + 1;
            } else {
                return 1;
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving next sequence number from the database.");
            e.printStackTrace();
            return 1;
        }
    }

    @Override
    public void updateSymptomsInDiagnosisFile(int idDiagnosisFile, String selectedSymptoms) {
        String sql = "UPDATE diagnosisFiles SET symptoms = ? WHERE idDiagnosisFile = ?";

        try (Connection c = conMan.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, selectedSymptoms);
            ps.setInt(2, idDiagnosisFile);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Symptoms updated successfully for DiagnosisFile ID: " + idDiagnosisFile);
            } else {
                System.out.println("No DiagnosisFile found with ID: " + idDiagnosisFile);
            }

        } catch (SQLException e) {
            System.out.println("Error updating symptoms in DiagnosisFile.");
            e.printStackTrace();
        }
    }

    @Override
    public String getDoctornameByPatient(Patient loggedPatient) {
        String sql = "SELECT nameDoctor + surnameDoctor FROM doctors WHERE idDoctor = ?";
        try (Connection c = conMan.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int docID = loggedPatient.getDoctorId();

            ps.setInt(1, docID);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("nameDoctor");
            } else {
                throw new SQLException("No Doctor assigned");
            }

        } catch (SQLException e) {
            System.out.println("Error selecting doctor name.");
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<String> getAllFragmentsOfRecording(int id_DiagnosisFile) throws SQLException {
        List<String> fragments = new ArrayList<>();

        String sql = "SELECT data FROM recordings WHERE diagnosisFileId = ? ORDER BY sequence ASC";

        try (Connection c = conMan.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id_DiagnosisFile);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    String fragment = rs.getString("data");
                    fragments.add(fragment);
                }

            }

        } catch (SQLException e) {
            System.out.println("Error getting fragments of recording.");
            e.printStackTrace();
        }

        return fragments;
    }
}
