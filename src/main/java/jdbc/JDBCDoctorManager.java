package jdbc;

import jdbcInterfaces.DoctorManager;
import pojos.Doctor;
import pojos.enums.Sex;
import pojos.DiagnosisFile;
import pojos.Patient;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JDBCDoctorManager implements DoctorManager {

    private Connection c;
    private ConnectionManager conMan;

    private static final DateTimeFormatter DOB_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DF_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public JDBCDoctorManager(ConnectionManager conMan) {
        this.conMan = conMan;
        try {
            this.c = conMan.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addDoctor(Doctor d) throws SQLException {
        String query = "INSERT INTO doctors (userId, nameDoctor, surnameDoctor, dniDoctor, dobDoctor, emailDoctor, sexDoctor, specialty, licenseNumber) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement psDoc = c.prepareStatement(query)) {
            psDoc.setInt(1, d.getUserId());
            psDoc.setString(2, d.getNameDoctor());
            psDoc.setString(3, d.getSurnameDoctor());
            psDoc.setString(4, d.getDniDoctor());

            java.sql.Date dobSql = d.getDobDoctor();
            String dobStr = null;
            if (dobSql != null) {
                LocalDate dobLocal = dobSql.toLocalDate();
                dobStr = dobLocal.format(DOB_FORMATTER);
            }
            psDoc.setString(5, dobStr);

            psDoc.setString(6, d.getEmailDoctor());
            psDoc.setString(7, d.getSexDoctor().toString());
            psDoc.setString(8, d.getSpecialty());
            psDoc.setString(9, d.getLicenseNumber());

            psDoc.executeUpdate();
        }
    }

    @Override
    public List<String> getAllPatientsInsuranceNumberbyDoctor() throws SQLException {
        List<String> insuranceNumbers = new ArrayList<>();

        String sql = "SELECT healthInsuranceNumberPatient " +
                "FROM patients ";

        try (Connection c = conMan.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    String insurance = String.valueOf(rs.getInt("healthInsuranceNumberPatient"));
                    insuranceNumbers.add(insurance);
                }

            }

        } catch (SQLException e) {
            System.out.println("Error retrieving insurance numbers from database.");
            e.printStackTrace();
        }

        return insuranceNumbers;
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

    @Override
    public DiagnosisFile getDiagnosisFileByID(int idDiagnosisFile) throws SQLException {
        String sql = "SELECT * FROM diagnosisFiles WHERE idDiagnosisFile = ?";
        try (Connection c = conMan.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idDiagnosisFile);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                ArrayList<String> symptoms = new ArrayList<>();
                String symptomsStr = rs.getString("symptoms");
                if (symptomsStr != null && !symptomsStr.isEmpty()) {
                    for (String symptom : symptomsStr.split(",")) {
                        symptoms.add(symptom.trim());
                    }
                }

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
                                        java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS"));
                                date = ldt.toLocalDate();
                            } catch (DateTimeParseException e2) {
                                try {
                                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(dateStr,
                                            java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
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

                DiagnosisFile df = new DiagnosisFile(
                        rs.getInt("idDiagnosisFile"),
                        symptoms,
                        rs.getString("diagnosis"),
                        rs.getString("medication"),
                        date,
                        rs.getInt("patientId"),
                        rs.getBoolean("status")
                );
                return df;
            }
            return null;
        }
    }

    @Override
    public Doctor getDoctorbyUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM doctors WHERE userId = ?";
        try (Connection c = conMan.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String sexStr = rs.getString("sexDoctor");
                Sex sexEnum = null;

                if (sexStr != null) {
                    try {
                        if (sexStr.equalsIgnoreCase("MALE")) sexEnum = Sex.MALE;
                        else if (sexStr.equalsIgnoreCase("FEMALE")) sexEnum = Sex.FEMALE;
                        else sexEnum = Sex.valueOf(sexStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        System.out.println(" Invalid sex value in DB: " + sexStr);
                    }
                }

                String dobStr = rs.getString("dobDoctor");
                java.sql.Date dobSql = null;
                if (dobStr != null && !dobStr.isEmpty()) {
                    LocalDate dobLocal = LocalDate.parse(dobStr, DOB_FORMATTER);
                    dobSql = java.sql.Date.valueOf(dobLocal);
                }

                Doctor d = new Doctor(
                        rs.getInt("idDoctor"),
                        rs.getInt("userId"),
                        rs.getString("nameDoctor"),
                        rs.getString("surnameDoctor"),
                        rs.getString("dniDoctor"),
                        dobSql,
                        rs.getString("emailDoctor"),
                        sexEnum,
                        rs.getString("specialty"),
                        rs.getString("licenseNumber")
                );
                return d;
            }
            return null;
        }
    }

    @Override
    public Patient getPatientByDiganosisFileID(int idDiagnosisFile) throws SQLException {
        String query = "SELECT p.* FROM patients p " +
                "INNER JOIN diagnosisFiles df ON p.idPatient = df.patientId " +
                "WHERE df.idDiagnosisFile = ?";

        try (Connection c = conMan.getConnection();
             PreparedStatement stmt = c.prepareStatement(query)) {

            stmt.setInt(1, idDiagnosisFile);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Patient patient = new Patient();
                    patient.setIdPatient(rs.getInt("idPatient"));
                    patient.setUserId(rs.getInt("userId"));
                    patient.setNamePatient(rs.getString("namePatient"));
                    patient.setSurnamePatient(rs.getString("surnamePatient"));
                    patient.setDniPatient(rs.getString("dniPatient"));

                    LocalDate date = null;
                    String dateStr = rs.getString("dobPatient");
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
                                            java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS"));
                                    date = ldt.toLocalDate();
                                } catch (DateTimeParseException e2) {
                                    try {
                                        java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(dateStr,
                                                java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
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

                    patient.setDobPatient(Date.valueOf(date));
                    patient.setEmailPatient(rs.getString("emailPatient"));
                    patient.setSexPatient(Sex.valueOf(rs.getString("sexPatient")));
                    patient.setPhoneNumberPatient(rs.getInt("phoneNumberPatient"));
                    patient.setHealthInsuranceNumberPatient(rs.getInt("healthInsuranceNumberPatient"));
                    patient.setEmergencyContactPatient(rs.getInt("emergencyContactPatient"));
                    return patient;
                } else {
                    throw new SQLException("No diagnosis file found with ID: " + idDiagnosisFile);
                }
            }
        }
    }

    @Override
    public void downloadFileInComputer(DiagnosisFile diagnosisFile) throws IOException {
        String fileName = diagnosisFile.getDiagnosis() + ".csv";

        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write("ID,PatientID,Diagnosis,Medication,Date,Status\n");

            writer.write(diagnosisFile.getId() + "," +
                    diagnosisFile.getPatientId() + "," +
                    diagnosisFile.getDiagnosis() + "," +
                    diagnosisFile.getMedication() + "," +
                    diagnosisFile.getDate() + "," +
                    diagnosisFile.getStatus() + "\n\n");

            writer.write("Symptoms:\n");
            if (diagnosisFile.getSymptoms() != null && !diagnosisFile.getSymptoms().isEmpty()) {
                for (String symptom : diagnosisFile.getSymptoms()) {
                    writer.write("- " + symptom + "\n");
                }
            } else {
                writer.write("No symptoms provided.\n");
            }

            System.out.println("Archivo descargado correctamente: " + fileName);
        }
    }

    public Patient getPatientByHIN(int healthInsuranceNumber) throws SQLException {
        String sql = "SELECT * FROM patients WHERE healthInsuranceNumberPatient = ?";
        try {
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setInt(1, healthInsuranceNumber);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String sexStr = rs.getString("sexPatient");
                Sex sexEnum = null;

                if (sexStr != null) {
                    try {
                        if (sexStr.equalsIgnoreCase("M")) sexEnum = Sex.MALE;
                        else if (sexStr.equalsIgnoreCase("F")) sexEnum = Sex.FEMALE;
                        else sexEnum = Sex.valueOf(sexStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        System.out.println("Invalid sex value in DB: " + sexStr);
                    }
                }

                LocalDate dobLocal = null;
                String dobStr = rs.getString("dobPatient");
                if (dobStr != null) {
                    dobStr = dobStr.trim();
                    if (dobStr.matches("\\d+")) {
                        // stored as epoch millis (e.g. 889657200000)
                        try {
                            long millis = Long.parseLong(dobStr);
                            dobLocal = java.time.Instant.ofEpochMilli(millis)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDate();
                        } catch (NumberFormatException ignored) {
                        }
                    } else {
                        boolean parsed = false;
                        try {
                            // dd-MM-yyyy (e.g. 03-07-2004)
                            dobLocal = LocalDate.parse(dobStr, DF_DATE_FORMATTER);
                            parsed = true;
                        } catch (DateTimeParseException ignored) {
                        }
                        if (!parsed) {
                            try {
                                // ISO yyyy-MM-dd
                                dobLocal = LocalDate.parse(dobStr, DateTimeFormatter.ISO_LOCAL_DATE);
                                parsed = true;
                            } catch (DateTimeParseException ignored) {
                            }
                        }
                        if (!parsed) {
                            try {
                                // dd-MM-yyyy HH:mm:ss.SSS
                                java.time.LocalDateTime ldt =
                                        java.time.LocalDateTime.parse(dobStr,
                                                java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS"));
                                dobLocal = ldt.toLocalDate();
                            } catch (DateTimeParseException e2) {
                                try {
                                    // dd-MM-yyyy HH:mm:ss
                                    java.time.LocalDateTime ldt =
                                            java.time.LocalDateTime.parse(dobStr,
                                                    java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
                                    dobLocal = ldt.toLocalDate();
                                } catch (DateTimeParseException ignored2) {
                                }
                            }
                        }
                    }
                }

                java.sql.Date dobSql = (dobLocal != null) ? java.sql.Date.valueOf(dobLocal) : null;

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
        } catch (SQLException e) {
            System.out.println("Error in the database");
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void modifyDiagnosisFile(DiagnosisFile diagnosisFile) {
        try {
            String template = "UPDATE diagnosisFiles SET symptoms = ?, diagnosis = ?, medication = ?, date = ?, patientId = ?, status = ? WHERE idDiagnosisFile = ?;";

            PreparedStatement ps = c.prepareStatement(template);

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
            ps.setInt(7, diagnosisFile.getId());

            ps.executeUpdate();

            ps.close();
        } catch (SQLException e) {
            System.out.println("Error in the database");
            e.printStackTrace();
        }
    }

    @Override
    public List<DiagnosisFile> listDiagnosisFilesTODO() {
        List<DiagnosisFile> diagnosisFiles = new ArrayList<>();

        String sql = "SELECT df.idDiagnosisFile, df.symptoms, df.diagnosis, df.medication, df.date, df.patientId, df.status " +
                "FROM diagnosisFiles df " +
                "JOIN patients p ON df.patientId = p.idPatient " +
                "WHERE df.status = FALSE";

        try (PreparedStatement ps = c.prepareStatement(sql)) {

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("idDiagnosisFile");
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
                                            java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS"));
                                    date = ldt.toLocalDate();
                                } catch (DateTimeParseException e2) {
                                    try {
                                        java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(dateStr,
                                                java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
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
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return diagnosisFiles;
    }

    @Override
    public List<DiagnosisFile> getAllDiagnosisFilesFromPatient(int idPatient) {
        List<DiagnosisFile> diagnosisFiles = new ArrayList<>();

        String sql = "SELECT df.idDiagnosisFile, df.symptoms, df.diagnosis, df.medication, df.date, df.patientId, df.status " +
                "FROM diagnosisFiles df " +
                "WHERE df.patientId = ? " +
                "AND df.status = TRUE";

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idPatient);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("idDiagnosisFile");
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
                                    date = java.time.LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
                                    parsed = true;
                                } catch (java.time.format.DateTimeParseException e1) {
                                }
                            }
                            if (!parsed) {
                                try {
                                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(dateStr,
                                            java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS"));
                                    date = ldt.toLocalDate();
                                } catch (java.time.format.DateTimeParseException e2) {
                                    try {
                                        java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(dateStr,
                                                java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
                                        date = ldt.toLocalDate();
                                    } catch (java.time.format.DateTimeParseException ignored2) {
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
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return diagnosisFiles;
    }

    @Override
    public void UpDateDiagnosisFile(DiagnosisFile diagnosisFile) throws SQLException {
        String sql = "UPDATE diagnosisFiles SET symptoms = ?, diagnosis = ?, medication = ?, date = ?, patientId = ?, status = ? WHERE idDiagnosisFile = ?";

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
            ps.setBoolean(6, true);  // Assuming 'status' is a boolean
            ps.setInt(7, diagnosisFile.getId());  // Set the ID to identify the record to update

            // Execute the update
            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Error updating DiagnosisFile in the database.");
            e.printStackTrace();
        }
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
}
