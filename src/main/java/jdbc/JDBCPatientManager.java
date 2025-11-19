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
import java.util.List;

 public class JDBCPatientManager implements PatientManager {

     private final ConnectionManager conMan;
     private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");

     public JDBCPatientManager(ConnectionManager conMan) {
         this.conMan = conMan;
     }

     @Override
     public void addPatient(Patient p) throws SQLException {
         String sql = "INSERT INTO patients(" +
                 "userId, namePatient, surnamePatient, dniPatient, dobPatient, emailPatient, " +
                 "sexPatient, phoneNumberPatient, healthInsuranceNumberPatient, emergencyContactPatient) " +
                 "VALUES (?,?,?,?,?,?,?,?,?,?)";
         try (Connection c = conMan.getConnection();
              PreparedStatement ps = c.prepareStatement(sql)) {

             Integer uid = p.getUserId();
             if (uid == null || uid <= 0) {
                 ps.setNull(1, java.sql.Types.INTEGER);
             } else {
                 ps.setInt(1, uid);
             }

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
         }
     }

     @Override
     public List<DiagnosisFile> getAllDiagnosisFilesFromPatient(int idPatient) {
         List<DiagnosisFile> diagnosisFiles = new ArrayList<>();

         String sql = "SELECT df.id, df.symptoms, df.diagnosis, df.medication, df.date, df.patientId, df.status " +
                 "FROM diagnosisFiles df " +
                 "WHERE df.patientId = ?";

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

     @Override
     public String getFracmentofRecoring(int idDiagnosisFile, int position) {
         String string = new String();
         return string;
     }
 }