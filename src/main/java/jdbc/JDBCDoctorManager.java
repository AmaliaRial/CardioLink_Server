package jdbc;

import jdbcInterfaces.DoctorManager;
import pojos.enums.Sex;
import pojos.DiagnosisFile;
import pojos.Patient;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JDBCDoctorManager implements DoctorManager {

    private Connection c;
    private ConnectionManager conMan;

    public JDBCDoctorManager(ConnectionManager conMan) {
        this.conMan = conMan;
        try {
            this.c = conMan.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
/**
    @Override
    public List <DiagnosisFile> listRecentlyFinishedFiles(){
        List<DiagnosisFile> recentFiles = new ArrayList<>();
        try {
            String sql = "SELECT * FROM diagnosisFile WHERE status = ?";
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setString(1, "false");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                DiagnosisFile file = new DiagnosisFile();
                file.setId(rs.getInt("id"));
                file.setDiagnosis(rs.getString("diagnosis"));
                file.setMedication(rs.getString("medication"));
                file.setDate(rs.getDate("date").toLocalDate());
                file.setPatientId(rs.getInt("patientId"));
                file.setSensorDataECG(rs.getString("sensorDataECG"));
                file.setSensorDataEDA(rs.getString("sensorDataEDA"));
                file.setStatus(rs.getBoolean("status"));
                String symptomsStr = rs.getString("symptoms");
                List<String> symptoms = Arrays.asList(symptomsStr.split(","));
                file.setSymptoms((ArrayList<String>) symptoms);
                recentFiles.add(file);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return recentFiles;
    }*/

    /**
    @Override
    public List <DiagnosisFile> listAllFinishedFiles(){
        List<DiagnosisFile> recentFiles = new ArrayList<>();
        try {
            String sql = "SELECT * FROM diagnosisFile WHERE status = ?";
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setString(1, "true");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                DiagnosisFile file = new DiagnosisFile();
                file.setId(rs.getInt("id"));
                file.setDiagnosis(rs.getString("diagnosis"));
                file.setMedication(rs.getString("medication"));
                file.setDate(rs.getDate("date").toLocalDate());
                file.setPatientId(rs.getInt("patientId"));
                file.setSensorDataECG(rs.getString("sensorDataECG"));
                file.setSensorDataEDA(rs.getString("sensorDataEDA"));
                file.setStatus(rs.getBoolean("status"));
                String symptomsStr = rs.getString("symptoms");
                List<String> symptoms = Arrays.asList(symptomsStr.split(","));
                file.setSymptoms((ArrayList<String>) symptoms);
                recentFiles.add(file);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return recentFiles;
    }
*/
    @Override
    public void downloadFileInComputer(DiagnosisFile diagnosisFile) throws IOException {
        // Define the file name based on the diagnosis
        String fileName = diagnosisFile.getDiagnosis() + ".csv";

        try (FileWriter writer = new FileWriter(fileName)) {
            // Write header to the CSV file
            writer.write("ID,PatientID,Diagnosis,Medication,Date,Status\n");

            // Write DiagnosisFile data to the CSV file
            writer.write(diagnosisFile.getId() + "," +
                    diagnosisFile.getPatientId() + "," +
                    diagnosisFile.getDiagnosis() + "," +
                    diagnosisFile.getMedication() + "," +
                    diagnosisFile.getDate() + "," +
                    diagnosisFile.getStatus() + "\n\n");

            // Write Symptoms section
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


    /**
    public List<DiagnosisFile> getDiagnosisFilesByPatientId(int patientId) {
        List<DiagnosisFile> diagnosisFiles = new ArrayList<>();
        try {
            String template = "SELECT * FROM diagnosisFile WHERE patientId = ?";
            PreparedStatement ps = c.prepareStatement(template);
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                DiagnosisFile file = new DiagnosisFile();
                file.setId(rs.getInt("id"));
                file.setDiagnosis(rs.getString("diagnosis"));
                file.setMedication(rs.getString("medication"));
                file.setDate(rs.getDate("date").toLocalDate());
                file.setPatientId(rs.getInt("patientId"));
                file.setSensorDataECG(rs.getString("sensorDataECG"));
                file.setSensorDataEDA(rs.getString("sensorDataEDA"));
                file.setStatus(rs.getBoolean("status"));
                String symptomsStr = rs.getString("symptoms");
                List<String> symptoms = Arrays.asList(symptomsStr.split(","));
                file.setSymptoms((ArrayList<String>) symptoms);
                diagnosisFiles.add(file);
            }
        } catch (SQLException e) {
            System.out.println("Error in the database");
            e.printStackTrace();
        }
        return diagnosisFiles;
    }
*/

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
                        // Normalize possible single-letter or full names
                        if (sexStr.equalsIgnoreCase("M")) sexEnum = Sex.MALE;
                        else if (sexStr.equalsIgnoreCase("F")) sexEnum = Sex.FEMALE;
                        else sexEnum = Sex.valueOf(sexStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        System.out.println("⚠️ Invalid sex value in DB: " + sexStr);
                    }
                }

                Patient p = new Patient(rs.getInt("idPatient"),
                        rs.getString("namePatient"),
                        rs.getString("surnamePatient"),
                        rs.getString("dniPatient"),
                        rs.getDate("dobPatient"),
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
            // SQL query to update the diagnosis file
            String template = "UPDATE diagnosisFiles SET symptoms = ?, diagnosis = ?, medication = ?, date = ?, patientId = ?, status = ? WHERE id = ?;";

            // Prepare the statement
            PreparedStatement ps = c.prepareStatement(template);

            // Serialize the symptoms list into a comma-separated string
            String symptomsSerialized = (diagnosisFile.getSymptoms() == null || diagnosisFile.getSymptoms().isEmpty())
                    ? null
                    : String.join(", ", diagnosisFile.getSymptoms());

            // Set the parameters in the prepared statement
            ps.setString(1, symptomsSerialized);
            ps.setString(2, diagnosisFile.getDiagnosis());
            ps.setString(3, diagnosisFile.getMedication());
            ps.setDate(4, java.sql.Date.valueOf(diagnosisFile.getDate()));  // Convert LocalDate to SQL Date
            ps.setInt(5, diagnosisFile.getPatientId());
            ps.setBoolean(6, diagnosisFile.getStatus());  // Assuming 'status' is a boolean
            ps.setInt(7, diagnosisFile.getId());  // Set the ID for updating the correct record

            // Execute the update
            ps.executeUpdate();

            // Close the prepared statement
            ps.close();
        } catch (SQLException e) {
            System.out.println("Error in the database");
            e.printStackTrace();
        }
    }

/**
    @Override
    public void deleteDiagnosisFile(int  id) {
        try {
            String template = "DELETE FROM diagnosisFile WHERE id = ?";
            PreparedStatement ps;
            ps = c.prepareStatement(template);
            ps.setInt(1, id);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            System.out.println("Error in the database");
            e.printStackTrace();
        }
    }
*/
    @Override
    public List<DiagnosisFile> listDiagnosisFilesTODO(int idDoctor) {
        List<DiagnosisFile> diagnosisFiles = new ArrayList<>();

        String sql = "SELECT df.id, df.symptoms, df.diagnosis, df.medication, df.date, df.patientId, df.status " +
                "FROM diagnosisFiles df " +
                "JOIN patients p ON df.patientId = p.id " +
                "WHERE df.status = FALSE AND p.doctorId = ?";

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idDoctor);

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
                    LocalDate date = rs.getDate("date").toLocalDate();
                    int patientId = rs.getInt("patientId");
                    boolean status = rs.getBoolean("status");

                    DiagnosisFile diagnosisFile = new DiagnosisFile(id, symptoms, diagnosis, medication, date, patientId, status);
                    diagnosisFiles.add(diagnosisFile);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                // Manejo adicional si lo consideras necesario
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Manejo adicional si lo consideras necesario
        }

        return diagnosisFiles;
    }



    @Override
    public List<DiagnosisFile> getAllDiagnosisFilesFromPatient(int idPatient) {
        List<DiagnosisFile> diagnosisFiles = new ArrayList<>();

        String sql = "SELECT df.id, df.symptoms, df.diagnosis, df.medication, df.date, df.patientId, df.status " +
                "FROM diagnosisFiles df " +
                "WHERE df.patientId = ?";

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            // Establecer el id del paciente en la consulta
            ps.setInt(1, idPatient);

            try (ResultSet rs = ps.executeQuery()) {
                // Procesar los resultados
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
                    LocalDate date = rs.getDate("date").toLocalDate();
                    int patientId = rs.getInt("patientId");
                    boolean status = rs.getBoolean("status");

                    // Crear el objeto DiagnosisFile y agregarlo a la lista
                    DiagnosisFile diagnosisFile = new DiagnosisFile(id, symptoms, diagnosis, medication, date, patientId, status);
                    diagnosisFiles.add(diagnosisFile);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                // Manejo de errores si es necesario
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Manejo de errores si es necesario
        }

        return diagnosisFiles;
    }

}
