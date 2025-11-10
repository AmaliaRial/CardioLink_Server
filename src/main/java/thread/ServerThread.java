package thread;

import jdbc.*;
import pojos.DiagnosisFile;
import pojos.Patient;
import pojos.User;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ServerThread {

    private static final String PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2CFFsdTRyHeLWEKkKMV+JaiNzSlmxKUaYM37JBAq8fn6k6McbnsfftWQd4f6HvkwJBsDoVSszn54k64lJEZng7FX2u2+INqW2xOF4OxwcCKqv09n+BGqdx2thxQ6iaEEa/kPeDcsbCyaIF9BA/n+nGpWqacqpHUAtRXPA1cw9hpiY2Pfqhc5hkdJfnV6LlvhM5th/zxhfKT4KnJVynNzLj3zhLXWvRIfFCXZ7/zRkDhQ7hwRnjRY8h+1Fy08eIbSrv07t32vJH6cby2u/vFyvirB21KCZv7KMjkKaPbiZomBwVPkIr9ZgFGvCI2lbukmmZbw0MSKv6L3TjCb0pKUCwIDAQAB";
    private static final String PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDYIUWx1NHId4tYQqQoxX4lqI3NKWbEpRpgzfskECrx+fqToxxuex9+1ZB3h/oe+TAkGwOhVKzOfniTriUkRmeDsVfa7b4g2pbbE4Xg7HBwIqq/T2f4Eap3Ha2HFDqJoQRr+Q94NyxsLJogX0ED+f6calappyqkdQC1Fc8DVzD2GmJjY9+qFzmGR0l+dXouW+Ezm2H/PGF8pPgqclXKc3MuPfOEtda9Eh8UJdnv/NGQOFDuHBGeNFjyH7UXLTx4htKu/Tu3fa8kfpxvLa7+8XK+KsHbUoJm/soyOQpo9uJmiYHBU+Qiv1mAUa8IjaVu6SaZlvDQxIq/ovdOMJvSkpQLAgMBAAECggEABi9oxv6rL1UHr4S8cuCnv1YmRhBWH06w9DrMlTOPYbx6SLkVaUgBbAGaoWd9q9Zy/T8hd6pKWzWua/fLichsa7ARHYUn2sgtEbSdytGZaAW7Sq5wEmdsWttkGuzKiwGilo9jIb9nRS7YyP8uuqyaqVpZ+12dJan7RFWNG/Shs1cjjk2WhzgIxXqN4UTKMZQD5DBcQmX/4r4Ddixl68KOxnN4gTXEHN0UhCwKPCdHvdnIiFzykHu72EtBCdGfc5RHXv/VD2cFZYlDJ5pVB5MWv3ukiQVAkG4NRZDzq4yadVZ0MbDEmRrzwqkX9/y9XSVXW+1Nii7DFiUlFfs6ibPtSQKBgQDyuzt9KEnOBFcHQN44uBc10opApMKoV9uVoZbwxv/rsLN9iouXAuUbrJqRPNMBhNpWpM/Tf39B9jgNMmfuEznJYFsTU+KuTZsOTjhlNDjQuquamrUoqGDQg5NeH+mhcrl4MYkYuRcC4SrldpcYfu5KhNBXZ1iz03dCbfpnbMn53wKBgQDj8cgNQr9AzkbBpDT9RhD3BhvoIaY6JebGtISbi1D39e6NwwCjQ5vLhDkWBl9zfgq1jhSCGV7mFCtSI0k0diK61uZlm0+7Mldc6LXnpZjEdal20fABL1KuICAfLoaBW8m2m6B6cXsfVvTtLydQI+NZoOt9OkfErBiOg0L0hwsDVQKBgGpwE9wECKkgWhFCLq/sebEOS7WhCgLL0+w/WXLnsF1ntK1+TUvA5zpFa9n4NAbcfOm1h7SUmfcQwu92hQBuyc42RHmrNSF9wlp5jl1Ckw9ka891u67CdwG4UKzbjZVQO2grQJToxOBsYGUSpZsGPfPLXZiWJt1kA03L8BveJos9AoGAVVsrm3OcJItZyZdQ1GrRXX8nIhS/p1ScB1p/sbNInaG1M9aKvZhKlbosmkfGpHvVTMkoetM/Sw7QbhCSkBeQx8BDRFcVUzb1qe/mdhj3jNG2pKzWn8r1vgh/ns2QRo51iXDbdh5aiZDJZKvcn9DgiKaOqDUTvNzo0Szr/J85C4UCgYEA3/lPBNpPfjzeOHaS2eoRS8W5TuPtrQ1rUHBpDD5ixWOYSNeSZqSS4gXZduvND/Dm6kLrGg/e6qBFr4G+CKxcrCibsBbTkkP9nak5DMQJ3EMAQEUucQcVQ/cQ/AXdW/PjQbtbm5/blMcPlo1mxfy3Ggd32rX7y+V0Bw8NgiUGtvA=";
    private static final String DataBase_Address = "jdbc:sqlite:CardioLink.db";
    private static final int port = 9000;


    public static void main(String[] args) throws IOException, SQLException {
        // Conexión y gestores se crean aquí para evitar doble creación de schema
        try (Connection connection = DriverManager.getConnection(DataBase_Address)) {
            System.out.println("Connection to database established.");

            // Crear ConnectionManager una sola vez (su constructor maneja schema)
            ConnectionManager conman = new ConnectionManager();

            // Crear user manager con el connection manager ya creado
            JDBCUserManager userMan = new JDBCUserManager(conman);

            // NOTA: no llamar conman.ensureSchema(connection) aquí si el constructor ya lo hace,
            // así evitamos intentar crear columnas/tabla duplicadas.

            ServerSocket serverSocket = new ServerSocket(port);
            try {
                while (true) {
                    try {
                        Socket socket = serverSocket.accept();
                        System.out.println("Server running on port " + port);
                        new Thread(new ServerClientThread(socket, connection, serverSocket)).start();
                    } catch (java.net.SocketException se) {
                        if (serverSocket.isClosed()) {
                            System.out.println("Server socket closed. Shutting down...");
                            break; // exit the loop cleanly
                        } else {
                            throw se; // unexpected
                        }
                    }
                }
            } finally {
                releaseResources(serverSocket);
            }
        } catch (SQLException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, "Server error", ex);
        }
    }

    private static void releaseResourcesPatient(DataInputStream inputStream, Socket socket, DataOutputStream outputStream) {

        try {
            inputStream.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            outputStream.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private static void releaseResourcesDoctor(DataInputStream inputStream, Socket socket, DataOutputStream outputStream) {

        try {
            inputStream.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            outputStream.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void releaseResources(ServerSocket serverSocket) {
        try {
            serverSocket.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void adminLogoff(DataInputStream inputStream, Socket socket) {
        try {
            inputStream.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void adminShutdown(DataInputStream inputStream, Socket socket,ServerSocket serverSocket,DataOutputStream outputStream) {
        try {
            inputStream.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            outputStream.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            serverSocket.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static class ServerClientThread implements Runnable{
        Socket socket;
        Connection connection;
        ServerSocket serverSocket;
        ConnectionManager conMan;
        JDBCPatientManager patientMan;
        JDBCDoctorManager doctorMan;
        JDBCUserManager userMan;

        private ServerClientThread(Socket socket, Connection connection,ServerSocket serverSocket){
            this.socket = socket;
            this.connection = connection;
            this.serverSocket = serverSocket;
        }
        @Override
        public void run(){
            DataInputStream inputStream = null;
            DataOutputStream outputStream = null;
            try{
                inputStream = new DataInputStream(socket.getInputStream());
                outputStream = new DataOutputStream(socket.getOutputStream());
                conMan = new ConnectionManager();
                patientMan = new JDBCPatientManager(conMan);
                doctorMan = new JDBCDoctorManager(conMan);
                userMan = new JDBCUserManager(conMan);
                String clientType = inputStream.readUTF();
                if(clientType.equalsIgnoreCase("Patient")){
                    System.out.println("Is a patient");
                    new Thread(new ServerPatientThread(socket,inputStream, outputStream, doctorMan, patientMan, conMan, userMan)).start();
                } else if(clientType.equalsIgnoreCase("Doctor")){
                    System.out.println("Is a doctor");

                    new Thread(new ServerDoctorThread(socket, inputStream, outputStream, doctorMan, patientMan, conMan, userMan)).start();

                } else if(clientType.equalsIgnoreCase("Admin")){
                    System.out.println("Is an administrator");
                    new Thread(new ServerAdminThread(socket,inputStream,serverSocket)).start();
                }
            }catch(IOException ex){
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    // SERVER ADMIN THREAD
    private static class ServerAdminThread implements  Runnable{
        Socket socket;
        DataInputStream inputStream;
        ServerSocket serverSocket;
        private ServerAdminThread(Socket socket, DataInputStream inputStream,ServerSocket serverSocket) {
            this.socket = socket;
            this.inputStream = inputStream;
            this.serverSocket = serverSocket;
        }
        @Override
        public void run(){
            DataOutputStream outputStream = null;
            try{
                String option;
                while (true) {
                    option = inputStream.readUTF();
                    switch (option.toLowerCase()){
                        case "shut down":
                            /*outputStream = new DataOutputStream(socket.getOutputStream());
                            outputStream.writeUTF("The server is closing...");
                            adminShutdown(inputStream,socket,serverSocket,outputStream);
                            //System.exit(0);*/

                            outputStream = new DataOutputStream(socket.getOutputStream());
                            // ask (or just expect) a password as the very next UTF
                            // If you don’t want to show a prompt, keep this line if you like
                            // outputStream.writeUTF("PASSWORD?");
                            outputStream.flush();

                            String password = inputStream.readUTF();   // <-- read password sent by client
                            if (password.equals("cardiolink_admin_pass")) {  // <-- check password
                                outputStream.writeUTF("The server is closing...");
                                outputStream.flush();
                                adminShutdown(inputStream, socket, serverSocket, outputStream);
                                return; // do NOT System.exit(0)
                            } else {
                                outputStream.writeUTF("Invalid password.");
                                outputStream.flush();
                                // keep the admin session open for another command
                                break;
                            }

                        case "exit":
                            adminLogoff(inputStream,socket);
                            return;
                    }
                }
            } catch (java.io.EOFException eof) {
                // admin app closed the socket — this is normal
                System.out.println("Admin disconnected");
            }catch (IOException ex){
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, "Admin I/O error", ex);
            }finally{
                adminLogoff(inputStream,socket);
            }
        }
    }

    // SERVER PATIENT THREAD
    private static class ServerPatientThread implements Runnable {
        private final Socket socket;
        private final DataInputStream inputStream;
        private final DataOutputStream outputStream;
        private final ConnectionManager conMan;
        private final JDBCPatientManager patientMan;
        private final JDBCUserManager userMan;
        private final JDBCDoctorManager doctorMan;

        // Buffers during recording
        private final List<Integer> ECG = new ArrayList<>();
        private final List<Integer> EDA = new ArrayList<>();
        private ArrayList<String> currentSymptoms = new ArrayList<>();
        private Patient loggedPatient = null;

        private ServerPatientThread(Socket socket, DataInputStream inputStream, DataOutputStream outputStream, JDBCDoctorManager doctorMan, JDBCPatientManager patientMan, ConnectionManager conMan, JDBCUserManager userMan) {
            this.socket = socket;
            this.inputStream = inputStream;
            this.outputStream = outputStream;
            this.patientMan = new JDBCPatientManager(conMan);
            this.doctorMan = new JDBCDoctorManager(conMan);
            this.conMan = conMan;
            this.userMan = new JDBCUserManager(conMan);
        }


        @Override
        public void run() {

            ArrayList<Integer> ECG = new ArrayList<>();
            ArrayList<Integer> EDA = new ArrayList<>();
            String loggedUsername = null;
            try {
                // Main loop: read commands (UTF strings) from client
                while (!socket.isClosed()) {
                    String command;
                    try {
                        command = inputStream.readUTF();
                    } catch (IOException e) {
                        System.out.println("Client disconnected (readUTF failed).");
                        break;
                    }

                    switch (command) {
                        case "SIGNUP":
                            handleSignup();
                            break;

                        case "LOGIN":
                            handleLogin();
                            break;

                        case "START":
                            // client starting recording - we can ACK
                            outputStream.writeUTF("ACK");
                            outputStream.writeUTF("Ready to receive data");
                            outputStream.flush();
                            break;

                        case "DATA":
                            handleDataFrame();
                            break;

                        case "END":

                            handleEndOfRecording();

                            // Clear arrays to prepare for next recording
                            ECG.clear();
                            EDA.clear();
                            break;
                        /** NOT NEEDED TO HANDLE ANY MORE (symptoms is not a class)
                         case "SYMPTOMS":
                         handleSymptoms();
                         break;
                         */

                        case "ERROR":
                            // Client informs of an error
                            String errMsg = inputStream.readUTF();
                            System.err.println("Client reported error: " + errMsg);
                            break;

                        default:
                            // Unknown command
                            System.out.println("Received unknown command: " + command);
                            outputStream.writeUTF("ERROR");
                            outputStream.writeUTF("Unknown command: " + command);
                            outputStream.flush();
                            break;
                    } // end switch
                } // end while
            } catch (IOException | ParseException ex) {
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                System.out.println("Client disconnected");
                releaseResourcesPatient(inputStream, socket,outputStream);
            }
        }

        private void handleSignup() throws Exception {
            // Read payload in same order sent by client
            String username = inputStream.readUTF();
            String password = inputStream.readUTF();
            String name = inputStream.readUTF();
            String surname = inputStream.readUTF();
            String birthday = inputStream.readUTF();
            String sex = inputStream.readUTF();
            String email = inputStream.readUTF();
            String phone = inputStream.readUTF();
            String dni = inputStream.readUTF();
            String insurance = inputStream.readUTF();
            String emergencyContact = inputStream.readUTF();

            // Validate server-side as well (basic)
            if (!dni.matches("\\d{8}[A-Z]")) {
                outputStream.writeUTF("ERROR");
                outputStream.writeUTF("Invalid DNI format.");
                outputStream.flush();
                return;
            }
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                outputStream.writeUTF("ERROR");
                outputStream.writeUTF("Invalid email format.");
                outputStream.flush();
                return;
            }
            if (!phone.matches("\\d{7,15}")) {
                outputStream.writeUTF("ERROR");
                outputStream.writeUTF("Invalid phone format.");
                outputStream.flush();
                return;
            }

            Patient p = new Patient();
            p.setNamePatient(name);
            p.setSurnamePatient(surname);
            p.setEmailPatient(email);
            p.setDniPatient(dni);

            try {
                p.setSexPatient(parseSex(sex));
            } catch (IllegalArgumentException ex) {
                outputStream.writeUTF("ERROR");
                outputStream.writeUTF("Invalid sex value: " + ex.getMessage());
                outputStream.flush();
                return;
            }

            p.setPhoneNumberPatient(Integer.parseInt(phone));
            p.setHealthInsuranceNumberPatient(Integer.parseInt(insurance));
            p.setDobPatient(new java.text.SimpleDateFormat("yyyy-MM-dd").parse(birthday));
            p.setEmergencyContactPatient(Integer.parseInt(emergencyContact));


            String encryptedPass = Encryption.encrypt(password, PUBLIC_KEY);
            try {
                patientMan.addPatient(p);
                userMan.register(username, encryptedPass, "PATIENT");
                outputStream.writeUTF("ACK");
                outputStream.writeUTF("Sign up successful. You can log in now.");
                outputStream.flush();
            } catch (Exception ex) {
                outputStream.writeUTF("ERROR");
                outputStream.writeUTF("Sign up failed: " + ex.getMessage());
                outputStream.flush();
            }
        }

        private pojos.enums.Sex parseSex(String sexStr) {
            if (sexStr == null) {
                throw new IllegalArgumentException("Sex value is null");
            }
            String s = sexStr.trim().toUpperCase();

            // Map common cases
            if (s.equals("M") || s.equals("MALE") || s.equals("MAN") || s.equals("H") || s.equals("HOMBRE")) {
                return pojos.enums.Sex.MALE;
            }
            if (s.equals("F") || s.equals("FEMALE") || s.equals("W") || s.equals("WOMAN") || s.equals("MUJER")) {
                return pojos.enums.Sex.FEMALE;
            }

            // Try direct enum name (in case client already sends MALE/FEMALE or other valid names)
            try {
                return pojos.enums.Sex.valueOf(s);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Valor de sexo no reconocido: " + sexStr);
            }
        }

        private void handleLogin() throws IOException {

            try {
                String username = inputStream.readUTF();
                String password = inputStream.readUTF();
                boolean logged = userMan.verifyPassword(username, password);
                if(logged){
                    User u = userMan.getUserByUsername(username);
                    int userId = u.getIdUser();
                    loggedPatient = patientMan.getPatientByUserId(userId);
                }

                outputStream.writeUTF("LOGIN_RESULT");
                if (logged) {
                    outputStream.writeBoolean(true);
                    outputStream.writeUTF("Login successful. Welcome " + loggedPatient.getNamePatient() + "!");
                } else {
                    outputStream.writeBoolean(false);
                    outputStream.writeUTF("Invalid username or password.");
                }
                outputStream.flush();
            } catch (Exception e) {
                outputStream.writeUTF("LOGIN_RESULT");
                outputStream.writeBoolean(false);
                outputStream.writeUTF("Login error: " + e.getMessage());
                outputStream.flush();
            }
        }

        private void handleDataFrame() {
            try {
                int blockNumber = inputStream.readInt();
                int seq = inputStream.readInt();
                int ecg = inputStream.readInt();
                int eda = inputStream.readInt();
                ECG.add(ecg);
                EDA.add(eda);
                System.out.printf("Received - Block %d Seq %d ECG %d EDA %d%n", blockNumber, seq, ecg, eda);
            } catch (IOException e) {
                System.err.println("Error reading DATA: " + e.getMessage());
            }
        }

        private void handleEndOfRecording() throws IOException {
            outputStream.writeUTF("ACK");
            outputStream.writeUTF("Recording finished. Saving data...");
            outputStream.flush();

            if (loggedPatient == null) {
                outputStream.writeUTF("ERROR");
                outputStream.writeUTF("No patient logged in.");
                outputStream.flush();
                return;
            }

            try {
                DiagnosisFile diag = new DiagnosisFile(0);
                diag.setPatientId(loggedPatient.getIdPatient());
                diag.setDate(LocalDate.now());
                diag.setSensorDataECG(serializeToCSV(ECG));
                diag.setSensorDataEDA(serializeToCSV(EDA));
                diag.setSymptoms(currentSymptoms);
                diag.setDiagnosis("Pending"); // default, doctor modifies later
                diag.setMedication("Pending");

                saveDiagnosisFile(diag);

                outputStream.writeUTF("ACK");
                outputStream.writeUTF("Data saved successfully in diagnosisFile.");
                outputStream.flush();
            } catch (Exception e) {
                outputStream.writeUTF("ERROR");
                outputStream.writeUTF("Failed to save recording: " + e.getMessage());
                outputStream.flush();
            } finally {
                ECG.clear();
                EDA.clear();
            }
        }
        /**THIS IS NOT NEEDED SINCE ITS NOT A CLASS ANY MORE
         private void handleSymptoms() throws IOException {
         int count = inputStream.readInt();
         currentSymptoms = new ArrayList<>();
         for (int i = 0; i < count; i++) {
         int id = inputStream.readInt();
         if (id > 0) {
         currentSymptoms.add(new Symptoms(id));
         }
         }
         String timestamp = inputStream.readUTF();
         System.out.printf("Received symptoms %s at %s%n", currentSymptoms, timestamp);

         outputStream.writeUTF("ACK");
         outputStream.writeUTF("Symptoms received.");
         outputStream.flush();
         }
         */

        private void saveDiagnosisFile(DiagnosisFile file) throws SQLException {
            // Use JDBCDoctorManager to insert a new record
            try (var c = conMan.getConnection();
                 var ps = c.prepareStatement(
                         "INSERT INTO diagnosisFile (symptoms, diagnosis, medication, date, patientId, sensorDataECG, sensorDataEDA) VALUES (?,?,?,?,?,?,?)")) {

                String symptomsSerialized = file.getSymptoms() == null ? "" :
                        file.getSymptoms().stream()
                                //.map(Symptoms::getNameSymptom) NOT A CLASS GET NAME DIRECT FROM STRING
                                .collect(Collectors.joining(", "));

                ps.setString(1, symptomsSerialized);
                ps.setString(2, file.getDiagnosis());
                ps.setString(3, file.getMedication());
                ps.setDate(4, java.sql.Date.valueOf(file.getDate()));
                ps.setInt(5, file.getPatientId());
                ps.setString(6, file.getSensorDataECG());
                ps.setString(7, file.getSensorDataEDA());
                ps.executeUpdate();
            }
        }

        private String serializeToCSV(List<Integer> list) {
            return list.stream().map(Object::toString).collect(Collectors.joining(","));
        }

    }
    // SERVER DOCTOR THREAD

    private static class ServerDoctorThread implements Runnable {
        private final Socket socket;
        private final DataInputStream inputStream;
        private final DataOutputStream outputStream;
        private final ConnectionManager conMan;
        private final JDBCPatientManager patientMan;
        private final JDBCDoctorManager doctorMan;
        private final JDBCUserManager userMan;

        // Estado de sesión del doctor (null si no autenticado)
        private Integer loggedDoctorUserId = null;
        private Integer loggedDoctorId = null;

        // Firma unificada con ServerPatientThread
        private ServerDoctorThread(Socket socket,
                                   DataInputStream inputStream,
                                   DataOutputStream outputStream,
                                   JDBCDoctorManager doctorMan,
                                   JDBCPatientManager patientMan,
                                   ConnectionManager conMan,
                                   JDBCUserManager userMan) {
            this.socket = socket;
            this.inputStream = inputStream;
            this.outputStream = outputStream;
            this.conMan = conMan;
            // Inicializar managers a partir de la misma conexión (coherente con ServerPatientThread)
            this.patientMan = new JDBCPatientManager(conMan);
            this.doctorMan = new JDBCDoctorManager(conMan);
            this.userMan = new JDBCUserManager(conMan);
        }

        @Override
        public void run() {
            try {
                while (!socket.isClosed()) {
                    String command;
                    try {
                        command = inputStream.readUTF();
                    } catch (IOException e) {
                        System.out.println("Doctor disconnected (readUTF failed).");
                        break;
                    }

                    switch (command) {
                        case "SIGNUP":
                            handleSignup();
                            break;

                        case "LOGIN":
                            handleLogin();
                            break;

                        case "LIST_PATIENTS":
                            handleListPatients();
                            break;

                        case "GET_DIAGNOSIS":
                            handleGetDiagnosis();
                            break;

                        case "UPDATE_DIAGNOSIS":
                            handleUpdateDiagnosis();
                            break;

                        case "UPDATE_MEDICATION":
                            handleUpdateMedication();
                            break;

                        case "LOGOUT":
                        case "EXIT":
                            loggedDoctorUserId = null;
                            loggedDoctorId = null;
                            outputStream.writeUTF("ACK");
                            outputStream.writeUTF("Goodbye");
                            outputStream.flush();
                            return;

                        case "ERROR":
                            String err = inputStream.readUTF();
                            System.err.println("Client reported error: " + err);
                            break;

                        default:
                            outputStream.writeUTF("ERROR");
                            outputStream.writeUTF("Unknown command: " + command);
                            outputStream.flush();
                            break;
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, "Doctor thread IO error", ex);
            } finally {
                System.out.println("Doctor disconnected");
                releaseResourcesDoctor(inputStream, socket, outputStream);
            }
        }




        private void handleSignup() throws IOException {
            try {
                String username = inputStream.readUTF();
                String password = inputStream.readUTF();
                String name = inputStream.readUTF();
                String surname = inputStream.readUTF();
                String birthday = inputStream.readUTF(); // "yyyy-MM-dd"
                String sex = inputStream.readUTF();
                String email = inputStream.readUTF();
                String specialty = inputStream.readUTF();
                String licenseNumber = inputStream.readUTF();
                String dni = inputStream.readUTF();

                if (!dni.matches("\\d{8}[A-Z]")) {
                    outputStream.writeUTF("ERROR");
                    outputStream.writeUTF("Invalid DNI format.");
                    outputStream.flush();
                    return;
                }
                if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                    outputStream.writeUTF("ERROR");
                    outputStream.writeUTF("Invalid email format.");
                    outputStream.flush();
                    return;
                }

                String encryptedPass = Encryption.encrypt(password, PUBLIC_KEY);

                try (var c = conMan.getConnection()) {
                    c.setAutoCommit(false);
                    try {

                        int newUserId;
                        try (var psUser = c.prepareStatement(
                                "INSERT INTO users (username, password, role) VALUES (?,?,?)",
                                java.sql.Statement.RETURN_GENERATED_KEYS)) {
                            psUser.setString(1, username);
                            psUser.setString(2, encryptedPass);
                            psUser.setString(3, "DOCTOR");
                            psUser.executeUpdate();
                            try (var keys = psUser.getGeneratedKeys()) {
                                if (!keys.next()) {
                                    throw new SQLException("No user id generated");
                                }
                                newUserId = keys.getInt(1);
                            }
                        }

                        //
                        try (var check = c.prepareStatement("SELECT idDoctor FROM doctors WHERE userId = ?")) {
                            check.setInt(1, newUserId);
                            try (var rs = check.executeQuery()) {
                                if (rs.next()) {
                                    c.rollback();
                                    outputStream.writeUTF("ERROR");
                                    outputStream.writeUTF("Doctor already registered for this user.");
                                    outputStream.flush();
                                    return;
                                }
                            }
                        }

                        //
                        try (java.sql.PreparedStatement psDoc = c.prepareStatement(
                                "INSERT INTO doctors (userId, nameDoctor, surnameDoctor, dniDoctor, dobDoctor, emailDoctor, sexDoctor, specialty, licenseNumber) VALUES (?,?,?,?,?,?,?,?,?)")) {
                            psDoc.setInt(1, newUserId);          // userId obtenido al crear el user
                            psDoc.setString(2, name);
                            psDoc.setString(3, surname);
                            psDoc.setString(4, dni);
                            psDoc.setString(5, birthday);
                            psDoc.setString(6, email);
                            psDoc.setString(7, sex);
                            psDoc.setString(8, specialty);
                            psDoc.setString(9, licenseNumber);

                            System.out.println("Insertando doctor: userId=" + newUserId + " dni=" + dni + " email=" + email);

                            psDoc.executeUpdate();
                        }

                        c.commit();
                        outputStream.writeUTF("ACK");
                        outputStream.writeUTF("Doctor sign up successful. You can log in now.");
                        outputStream.flush();
                        return;
                    } catch (SQLException ex) {
                        try { c.rollback(); } catch (SQLException ignore) {}
                        String msg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
                        if (msg.contains("UNIQUE") || msg.contains("constraint failed")) {
                            outputStream.writeUTF("ERROR");
                            outputStream.writeUTF("User or doctor already exists (unique constraint).");
                        } else {
                            outputStream.writeUTF("ERROR");
                            outputStream.writeUTF("Sign up DB error: " + msg);
                        }
                        outputStream.flush();
                        return;
                    } finally {
                        try { c.setAutoCommit(true); } catch (SQLException ignore) {}
                    }
                }
            } catch (Exception ex) {
                outputStream.writeUTF("ERROR");
                outputStream.writeUTF("Sign up failed: " + ex.getMessage());
                outputStream.flush();
            }
        }



        private void handleLogin() throws IOException {
            try {
                String username = inputStream.readUTF();
                String password = inputStream.readUTF();
                boolean logged = userMan.verifyPassword(username, password);

                outputStream.writeUTF("LOGIN_RESULT");

                if (!logged) {
                    outputStream.writeBoolean(false);
                    outputStream.writeUTF("Invalid username or password.");
                    outputStream.flush();
                    return;
                }

                User u = userMan.getUserByUsername(username);
                if (u == null || u.getRole() == null || !u.getRole().equalsIgnoreCase("DOCTOR")) {
                    outputStream.writeBoolean(false);
                    outputStream.writeUTF("User is not a doctor.");
                    outputStream.flush();
                    return;
                }

                try (var c = conMan.getConnection();
                     var ps = c.prepareStatement("SELECT idDoctor FROM doctors WHERE userId = ?")) {
                    ps.setInt(1, u.getIdUser());
                    try (var rs = ps.executeQuery()) {
                        if (rs.next()) {
                            loggedDoctorUserId = u.getIdUser();
                            loggedDoctorId = rs.getInt("idDoctor");
                            outputStream.writeBoolean(true);
                            outputStream.writeUTF("Login successful. Welcome doctor " + username);
                        } else {
                            outputStream.writeBoolean(false);
                            outputStream.writeUTF("Doctor record not found in doctors table.");
                        }
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, "DB error during doctor login", ex);
                    outputStream.writeBoolean(false);
                    outputStream.writeUTF("Login error: " + ex.getMessage());
                }
                outputStream.flush();
            } catch (Exception e) {
                outputStream.writeUTF("LOGIN_RESULT");
                outputStream.writeBoolean(false);
                outputStream.writeUTF("Login error: " + e.getMessage());
                outputStream.flush();
            }
        }

        private void handleListPatients() throws IOException {
            if (loggedDoctorUserId == null) {
                outputStream.writeUTF("ERROR");
                outputStream.writeUTF("Not authenticated as doctor.");
                outputStream.flush();
                return;
            }

            try (var c = conMan.getConnection();
                 var ps = c.prepareStatement("SELECT idPatient, namePatient, surnamePatient, dniPatient FROM patients");
                 var rs = ps.executeQuery()) {

                ArrayList<Integer> ids = new ArrayList<>();
                ArrayList<String> names = new ArrayList<>();
                ArrayList<String> surnames = new ArrayList<>();
                ArrayList<String> dnis = new ArrayList<>();
                while (rs.next()) {
                    ids.add(rs.getInt("idPatient"));
                    names.add(rs.getString("namePatient"));
                    surnames.add(rs.getString("surnamePatient"));
                    dnis.add(rs.getString("dniPatient"));
                }
                outputStream.writeUTF("PATIENT_LIST");
                outputStream.writeInt(ids.size());
                for (int i = 0; i < ids.size(); i++) {
                    outputStream.writeInt(ids.get(i));
                    outputStream.writeUTF(names.get(i) == null ? "" : names.get(i));
                    outputStream.writeUTF(surnames.get(i) == null ? "" : surnames.get(i));
                    outputStream.writeUTF(dnis.get(i) == null ? "" : dnis.get(i));
                }
                outputStream.flush();
            } catch (SQLException ex) {
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, "Error listing patients", ex);
                outputStream.writeUTF("ERROR");
                outputStream.writeUTF("Failed to list patients: " + ex.getMessage());
                outputStream.flush();
            }
        }

        private void handleGetDiagnosis() throws IOException {
            if (loggedDoctorUserId == null) {
                outputStream.writeUTF("ERROR");
                outputStream.writeUTF("Not authenticated as doctor.");
                outputStream.flush();
                return;
            }

            int patientId = inputStream.readInt();
            try (var c = conMan.getConnection();
                 var ps = c.prepareStatement(
                         "SELECT id, date, diagnosis, medication, sensorDataECG, sensorDataEDA, symptoms FROM diagnosisFile WHERE patientId = ?")) {
                ps.setInt(1, patientId);
                try (var rs = ps.executeQuery()) {
                    ArrayList<Integer> ids = new ArrayList<>();
                    ArrayList<String> dates = new ArrayList<>();
                    ArrayList<String> diagnoses = new ArrayList<>();
                    ArrayList<String> medications = new ArrayList<>();
                    ArrayList<String> ecgs = new ArrayList<>();
                    ArrayList<String> edas = new ArrayList<>();
                    ArrayList<String> symptoms = new ArrayList<>();
                    while (rs.next()) {
                        ids.add(rs.getInt("id"));
                        dates.add(rs.getDate("date") != null ? rs.getDate("date").toString() : "");
                        diagnoses.add(rs.getString("diagnosis"));
                        medications.add(rs.getString("medication"));
                        ecgs.add(rs.getString("sensorDataECG"));
                        edas.add(rs.getString("sensorDataEDA"));
                        symptoms.add(rs.getString("symptoms"));
                    }
                    outputStream.writeUTF("DIAGNOSIS_LIST");
                    outputStream.writeInt(ids.size());
                    for (int i = 0; i < ids.size(); i++) {
                        outputStream.writeInt(ids.get(i));
                        outputStream.writeUTF(dates.get(i) == null ? "" : dates.get(i));
                        outputStream.writeUTF(diagnoses.get(i) == null ? "" : diagnoses.get(i));
                        outputStream.writeUTF(medications.get(i) == null ? "" : medications.get(i));
                        outputStream.writeUTF(ecgs.get(i) == null ? "" : ecgs.get(i));
                        outputStream.writeUTF(edas.get(i) == null ? "" : edas.get(i));
                        outputStream.writeUTF(symptoms.get(i) == null ? "" : symptoms.get(i));
                    }
                    outputStream.flush();
                }
            } catch (SQLException ex) {
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, "Error retrieving diagnosis", ex);
                outputStream.writeUTF("ERROR");
                outputStream.writeUTF("Failed to get diagnosis: " + ex.getMessage());
                outputStream.flush();
            }
        }

        private void handleUpdateDiagnosis() throws IOException {
            if (loggedDoctorUserId == null) {
                outputStream.writeUTF("ERROR");
                outputStream.writeUTF("Not authenticated as doctor.");
                outputStream.flush();
                return;
            }

            int diagnosisId = inputStream.readInt();
            String newDiagnosis = inputStream.readUTF();
            try (var c = conMan.getConnection();
                 var ps = c.prepareStatement("UPDATE diagnosisFile SET diagnosis = ? WHERE id = ?")) {
                ps.setString(1, newDiagnosis);
                ps.setInt(2, diagnosisId);
                int updated = ps.executeUpdate();
                if (updated > 0) {
                    outputStream.writeUTF("ACK");
                    outputStream.writeUTF("Diagnosis updated");
                } else {
                    outputStream.writeUTF("ERROR");
                    outputStream.writeUTF("Diagnosis not found");
                }
                outputStream.flush();
            } catch (SQLException ex) {
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, "Error updating diagnosis", ex);
                outputStream.writeUTF("ERROR");
                outputStream.writeUTF("Failed to update diagnosis: " + ex.getMessage());
                outputStream.flush();
            }
        }

        private void handleUpdateMedication() throws IOException {
            if (loggedDoctorUserId == null) {
                outputStream.writeUTF("ERROR");
                outputStream.writeUTF("Not authenticated as doctor.");
                outputStream.flush();
                return;
            }

            int diagnosisId = inputStream.readInt();
            String newMedication = inputStream.readUTF();
            try (var c = conMan.getConnection();
                 var ps = c.prepareStatement("UPDATE diagnosisFile SET medication = ? WHERE id = ?")) {
                ps.setString(1, newMedication);
                ps.setInt(2, diagnosisId);
                int updated = ps.executeUpdate();
                if (updated > 0) {
                    outputStream.writeUTF("ACK");
                    outputStream.writeUTF("Medication updated");
                } else {
                    outputStream.writeUTF("ERROR");
                    outputStream.writeUTF("Diagnosis not found");
                }
                outputStream.flush();
            } catch (SQLException ex) {
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, "Error updating medication", ex);
                outputStream.writeUTF("ERROR");
                outputStream.writeUTF("Failed to update medication: " + ex.getMessage());
                outputStream.flush();
            }
        }
    }


}
