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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

 public class JDBCPatientManager implements PatientManager {

     private final ConnectionManager conMan;
     private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");

     public JDBCPatientManager(ConnectionManager conMan) {
         this.conMan = conMan;
     }

     /*@Override
     public void addPatient(Patient p) throws SQLException {
         String sql = "INSERT INTO patients(" +
                 "userId, namePatient, surnamePatient, dniPatient, dobPatient, emailPatient, " +
                 "sexPatient, phoneNumberPatient, healthInsuranceNumberPatient, emergencyContactPatient) " +
                 "VALUES (?,?,?,?,?,?,?,?,?,?)";
         try (Connection c = conMan.getConnection();
              PreparedStatement ps = c.prepareStatement(sql)) {

             Integer uid = p.getUserId();
             ps.setInt(1, uid);


             ps.setString(2, p.getNamePatient());
             ps.setString(3, p.getSurnamePatient());
             ps.setString(4, p.getDniPatient());

             if (p.getDobPatient() != null) {
                 ps.setDate(5, new java.sql.Date(p.getDobPatient().getTime()));
             } else {
                 ps.setNull(5, java.sql.Types.DATE);
             }

             ps.setString(6, p.getEmailPatient());

             Sex sex = p.getSexPatient();
             ps.setString(7, sex == null ? null : sex.name());

             Integer phone = p.getPhoneNumberPatient();
             if (phone == null || phone <= 0) ps.setNull(8, java.sql.Types.INTEGER);
             else ps.setInt(8, phone);

             Integer health = p.getHealthInsuranceNumberPatient();
             if (health == null || health <= 0) ps.setNull(9, java.sql.Types.INTEGER);
             else ps.setInt(9, health);

             Integer emergency = p.getEmergencyContactPatient();
             if (emergency == null || emergency <= 0) ps.setNull(10, java.sql.Types.INTEGER);
             else ps.setInt(10, emergency);

             ps.executeUpdate();
         }
     }*/

     public void addPatient(Patient patient) throws SQLException {
         String query = "INSERT INTO patients (userId, namePatient, surnamePatient, dniPatient, dobPatient, emailPatient, sexPatient, phoneNumberPatient, healthInsuranceNumberPatient, emergencyContactPatient) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

         try (Connection c = conMan.getConnection();
              PreparedStatement ps = c.prepareStatement(query)) {
             ps.setInt(1, patient.getUserId());  // Set the userId for the patient
             ps.setString(2, patient.getNamePatient());  // Set the name
             ps.setString(3, patient.getSurnamePatient());  // Set the surname
             ps.setString(4, patient.getDniPatient());  // Set the dni
             ps.setDate(5, patient.getDobPatient());  // Set the date of birth
             ps.setString(6, patient.getEmailPatient());  // Set the email
             ps.setString(7, patient.getSexPatient().toString());  // Set the sex
             ps.setInt(8, patient.getPhoneNumberPatient());  // Set the phone number
             ps.setInt(9, patient.getHealthInsuranceNumberPatient());  // Set the insurance number
             ps.setInt(10, patient.getEmergencyContactPatient());  // Set the emergency contact number

             int rowsAffected = ps.executeUpdate();  // Execute the insert statement

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

                 // Insert user
                 psUser.setString(1, username);
                 psUser.setString(2, password); // hashear en producción
                 psUser.executeUpdate();

                 try (ResultSet keys = psUser.getGeneratedKeys()) {
                     if (!keys.next()) {
                         c.rollback();
                         throw new SQLException("No se obtuvo id generado para user");
                     }
                     int userId = keys.getInt(1);
                     p.setUserId(userId);

                     // Preparar insert patient (mismos índices que en addPatient)
                     Integer uid = p.getUserId();
                     if (uid == null || uid <= 0) psPatient.setNull(1, java.sql.Types.INTEGER);
                     else psPatient.setInt(1, uid);

                     psPatient.setString(2, p.getNamePatient());
                     psPatient.setString(3, p.getSurnamePatient());
                     psPatient.setString(4, p.getDniPatient());

                     if (p.getDobPatient() != null) {
                         psPatient.setDate(5, new java.sql.Date(p.getDobPatient().getTime()));
                     } else {
                         psPatient.setNull(5, java.sql.Types.DATE);
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
         }
     }

     @Override
     public List<DiagnosisFile> getAllDiagnosisFilesFromPatient(int idPatient) {
         List<DiagnosisFile> diagnosisFiles = new ArrayList<>();

         String sql = "SELECT df.id, df.symptoms, df.diagnosis, df.medication, df.date, df.patientId, df.status " +
                 "FROM diagnosisFiles df " +
                 "WHERE df.patientId = ? AND df.status = TRUE";

         try (Connection c = conMan.getConnection();
              PreparedStatement ps = c.prepareStatement(sql)) {

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
             }

         } catch (SQLException e) {
             e.printStackTrace();
         }

         return diagnosisFiles;
     }

     @Override
     public String getFragmentOfRecording(int idDiagnosisFile, int position) throws SQLException {
         String sql = "SELECT data FROM recordings WHERE diagnosisFileId = ? AND sequence = ?";

         try (Connection c = conMan.getConnection();  // Assuming conMan.getConnection() returns a valid DB connection
              PreparedStatement ps = c.prepareStatement(sql)) {

             // Set the parameters for the prepared statement
             ps.setInt(1, idDiagnosisFile);  // Set the diagnosisFileId
             ps.setInt(2, position);  // Set the sequence/position of the recording

             // Execute the query
             ResultSet rs = ps.executeQuery();

             // Check if we have a result
             if (rs.next()) {
                 // Return the recording fragment (data)
                 return rs.getString("data");
             } else {
                 // If no result is found, return null or an appropriate message
                 return "No fragment found for the given DiagnosisFileId and position.";
             }

         } catch (SQLException e) {
             System.out.println("Error retrieving fragment from the database.");
             e.printStackTrace();
             return null;  // Return null or handle the error as needed
         }
     }

     @Override
     public List<Boolean> getSateOfFragmentsOfRecordingByID(int idDiagnosisFile) throws SQLException {
         List<Boolean> anomalyStates = new ArrayList<>();  // List to store the anomaly states

         String sql = "SELECT anomaly FROM recordings WHERE diagnosisFileId = ?";

         try (Connection c = conMan.getConnection();  // Assuming conMan.getConnection() returns a valid DB connection
              PreparedStatement ps = c.prepareStatement(sql)) {

             // Set the parameter for the prepared statement (diagnosisFileId)
             ps.setInt(1, idDiagnosisFile);

             // Execute the query
             ResultSet rs = ps.executeQuery();

             // Iterate through the result set and collect anomaly states
             while (rs.next()) {
                 boolean anomaly = rs.getBoolean("anomaly");
                 anomalyStates.add(anomaly);  // Add the anomaly state to the list
             }

         } catch (SQLException e) {
             System.out.println("Error retrieving anomaly states from the database.");
             e.printStackTrace();
             return Collections.emptyList();  // Return an empty list in case of an error
         }

         return anomalyStates;
     }

     @Override
     public void AddNewDiagnosisFile(DiagnosisFile diagnosisFile) throws SQLException {
         String sql = "INSERT INTO diagnosisFiles (symptoms, diagnosis, medication, date, patientId, status) " +
                 "VALUES (?, ?, ?, ?, ?, ?)";

         try (Connection c = conMan.getConnection();  // Assuming conMan.getConnection() returns a valid DB connection
              PreparedStatement ps = c.prepareStatement(sql)) {

             // Serialize the symptoms list into a comma-separated string
             String symptomsSerialized = (diagnosisFile.getSymptoms() == null || diagnosisFile.getSymptoms().isEmpty())
                     ? null
                     : String.join(", ", diagnosisFile.getSymptoms());

             // Set the parameters for the prepared statement
             ps.setString(1, symptomsSerialized);
             ps.setString(2, diagnosisFile.getDiagnosis());
             ps.setString(3, diagnosisFile.getMedication());
             ps.setDate(4, java.sql.Date.valueOf(diagnosisFile.getDate()));  // Convert LocalDate to SQL Date
             ps.setInt(5, diagnosisFile.getPatientId());
             ps.setBoolean(6, diagnosisFile.getStatus());  // Assuming 'status' is a boolean

             // Execute the insert
             ps.executeUpdate();
             System.out.println("New DiagnosisFile added successfully.");

         } catch (SQLException e) {
             System.out.println("Error adding new DiagnosisFile to the database.");
             e.printStackTrace();
         }
     }

     @Override
     public int returnIdOfLastDiagnosisFile() throws SQLException {
         String sql = "SELECT MAX(id) AS lastId FROM diagnosisFiles";

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
             throw e;  // Rethrow the exception after logging
         }
     }

     @Override
     public void saveFragmentOfRecording(int idDiagnosisFile, String fragmentData) throws SQLException {
         String sql = "INSERT INTO recordings (diagnosisFileId, data, anomaly, sequence) " +
                 "VALUES (?, ?, ?, ?)";

         try (Connection c = conMan.getConnection();
              PreparedStatement ps = c.prepareStatement(sql)) {

             // For simplicity, let's assume 'anomaly' is false and 'sequence' is auto-incremented
             ps.setInt(1, idDiagnosisFile);
             ps.setString(2, fragmentData);
             ps.setBoolean(3, false);  // Defaulting anomaly to false

             // Get the next sequence number for this diagnosisFileId
             int nextSequence = getNextSequenceNumber(c, idDiagnosisFile);
             ps.setInt(4, nextSequence);

             ps.executeUpdate();
             System.out.println("Fragment of recording saved successfully.");

         } catch (SQLException e) {
             System.out.println("Error saving fragment of recording to the database.");
             e.printStackTrace();
         }
     }

     @Override
     public int getNextSequenceNumber(Connection c, int idDiagnosisFile) {
         String sql = "SELECT MAX(sequence) AS maxSeq FROM recordings WHERE diagnosisFileId = ?";
         try (PreparedStatement ps = c.prepareStatement(sql)) {
             ps.setInt(1, idDiagnosisFile);
             ResultSet rs = ps.executeQuery();
             if (rs.next()) {
                 int maxSeq = rs.getInt("maxSeq");
                 return maxSeq + 1;  // Next sequence number
             } else {
                 return 1;  // Start from 1 if no records exist
             }
         } catch (SQLException e) {
             System.out.println("Error retrieving next sequence number from the database.");
             e.printStackTrace();
             return 1;  // Default to 1 in case of error
         }
     }

     @Override
     public void updateSymptomsInDiagnosisFile(int idDiagnosisFile, String selectedSymptoms) {
         String sql = "UPDATE diagnosisFiles SET symptoms = ? WHERE id = ?";

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