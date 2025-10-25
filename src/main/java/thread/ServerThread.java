package thread;

import jdbc.*;
import pojos.DiagnosisFile;
import pojos.Patient;
import pojos.Symptoms;
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
    private static final ConnectionManager conman = new ConnectionManager();
    private static final int port = 9000;


    public static void main(String[] args) throws IOException, SQLException {
        JDBCUserManager userMan = new JDBCUserManager(conman);

        try (Connection connection = DriverManager.getConnection(DataBase_Address)){
            System.out.println("Connection to database established.");
            conman.ensureSchema(connection);

            ServerSocket serverSocket = new ServerSocket(port);
            try {
                while (true) {
                    Socket socket = serverSocket.accept();
                    System.out.println("Server running on port " + port);
                    new Thread(new ServerClientThread(socket, connection, serverSocket)).start();
                }
            }finally {
                releaseResources(serverSocket);
            }
        }catch (SQLException ex){
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
                    //new Thread(new ServerDoctorThread(socket,connection,inputStream)).start();
                } else if(clientType.equalsIgnoreCase("Admin")){
                    System.out.println("Is an administrator");
                    new Thread(new ServerAdminThread(socket,inputStream,serverSocket)).start();
                }
            }catch(IOException ex){
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

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
                            outputStream = new DataOutputStream(socket.getOutputStream());
                            outputStream.writeUTF("The server is closing...");
                            adminShutdown(inputStream,socket,serverSocket,outputStream);
                            System.exit(0);
                        case "exit":
                            adminLogoff(inputStream,socket);
                            return;
                    }
                }
            }catch (IOException ex){
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
            }finally{
                System.out.println("Admin disconnected");
                adminLogoff(inputStream,socket);
            }
        }
    }

    //TODO IMPLEMENT DOCTOR AND CLIENT TREADS WITH HERE INPUT STREAMS AND IN THEIR APP OUTPUT STREAMS

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
        private ArrayList<Symptoms> currentSymptoms = new ArrayList<>();
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

                        case "SYMPTOMS":
                            handleSymptoms();
                            break;

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
            p.setSexPatient(pojos.enums.Sex.valueOf(sex.toUpperCase()));
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

        private void saveDiagnosisFile(DiagnosisFile file) throws SQLException {
            // Use JDBCDoctorManager to insert a new record
            try (var c = conMan.getConnection();
                 var ps = c.prepareStatement(
                         "INSERT INTO diagnosisFile (symptoms, diagnosis, medication, date, patientId, sensorDataECG, sensorDataEDA) VALUES (?,?,?,?,?,?,?)")) {

                String symptomsSerialized = file.getSymptoms() == null ? "" :
                        file.getSymptoms().stream()
                                .map(Symptoms::getNameSymptom)
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



}
