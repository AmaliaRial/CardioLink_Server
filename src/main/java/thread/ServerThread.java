package thread;

import jdbc.*;
import pojos.DiagnosisFile;
import pojos.Doctor;
import pojos.Patient;
import pojos.User;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.sql.Date;
import java.util.Deque;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.security.crypto.bcrypt.BCrypt;
import pojos.enums.Sex;

import javax.print.Doc;
import javax.swing.*;

public class ServerThread {

    private static final String DataBase_Address = "jdbc:sqlite:CardioLink.db";
    private static final int port = 9000;
    private static int workload = 12;
    //private static String hash = "$2a$06$.rCVZVOThsIa96pEDOxvGuRRgzG64bnptJ0938xuqzv18d3ZpQhstC";


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
                    } catch (SocketException se) {
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
            } catch (EOFException eof) {
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
        private static final Logger LOGGER = Logger.getLogger(ServerThread.class.getName());

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

        // STATE MACHINE
        private enum State {
            AUTH,               // SIGNUP / LOGIN / QUIT
            MAIN_MENU,          // START / VIEW_PATIENT / LOG_OUT / QUIT
            VIEW_PATIENT,       // VIEW_DIAGNOSIS_FILE / BACK_TO_MENU
            VIEW_DIAGNOSIS_FILE,// VIEW_RECORDING / BACK_TO_PATIENT
            VIEW_RECORDING      // CHANGE_FRAGMENT / DOWNLOAD_RECORDING / BACK_TO_DIAGNOSIS_FILE
        }
        private State state = State.AUTH;

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

            try {
                boolean running = true;

                while (running && !socket.isClosed()) {

                    String command = readCommand();
                    if (command == null) { // client disconnected
                        break;
                    }

                    // Normalize command (you can also support arguments here)
                    command = command.trim();
                    if (command.isEmpty()) {
                        continue;
                    }
                    System.out.println(command);

                    switch (state) {
                        case AUTH:
                            running = handleAuthCommand(command);
                            System.out.println(state);
                            break;

                        case MAIN_MENU:
                            running = handleMainMenuCommand(command);
                            System.out.println(state);
                            break;

                        case VIEW_PATIENT:
                            running = handleViewPatientCommand(command);
                            break;

                        case VIEW_DIAGNOSIS_FILE:
                            running = handleDiagnosisFileCommand(command);
                            break;

                        case VIEW_RECORDING:
                            running = handleRecordingCommand(command);
                            break;

                        default:
                            // Should never happen
                            outputStream.writeUTF("ERROR Internal server state");
                            running = false;
                            break;
                    }
                }

            } catch (IOException | ParseException ex) {
                LOGGER.log(Level.SEVERE, "Server thread error", ex);
            } catch (Exception e) {
                // Last-resort catch, avoid killing the server because of one client
                LOGGER.log(Level.SEVERE, "Unexpected error in client handler", e);
            } finally {
                System.out.println("Client disconnected");
                releaseResourcesPatient(inputStream, socket, outputStream);
            }
        }

        /**
         * Centralized method to read a command from the client.
         * Returns null if the client disconnects or read fails.
         */
        private String readCommand() {
            try {
                return inputStream.readUTF();
            } catch (IOException e) {
                System.out.println("Client disconnected (readUTF failed).");
                return null;
            }
        }

        /* ============================ STATE HANDLERS ============================ */

        /**
         * AUTH state: expects SIGNUP / LOGIN / QUIT
         */
        private boolean handleAuthCommand(String command) throws Exception {
            switch (command) {
                case "SIGNUP":
                    handleSignup();
                         // implement to return Patient or null
                    if (loggedPatient != null) {
                        // Login OK: go to main menu
                        state = State.MAIN_MENU;
                        outputStream.writeUTF("LOGIN_OK");
                    } else {
                        // Login failed
                        outputStream.writeUTF("SIGNUP_FAILED");
                    }// you implement this
                    return true;   // keep connection open

                case "LOGIN":
                    handleLogin(); // implement to return Patient or null
                    if (loggedPatient != null) {
                        // Login OK: go to main menu
                        state = State.MAIN_MENU;
                        outputStream.writeUTF("LOGIN_OK");
                    } else {
                        // Login failed
                        outputStream.writeUTF("LOGIN_FAILED");
                    }
                    return true;

                case "QUIT":
                    // Client wants to close connection
                    outputStream.writeUTF("BYE");
                    return false; // stop main loop

                default:
                    outputStream.writeUTF("ERROR Unknown command in AUTH state");
                    return true;
            }
        }

        /**
         * MAIN_MENU state: START / VIEW_PATIENT / LOG_OUT / QUIT
         */
        private boolean handleMainMenuCommand(String command) throws IOException {
            switch (command) {
                case "START":
                    // Example: start a new measurement session, etc
                    handleStart();
                    return true;

                case "VIEW_PATIENT":
                    // Optionally send patient info here
                    handleViewPatientOverview();
                    state = State.VIEW_PATIENT;
                    return true;

                case "LOG_OUT":
                    System.out.println("patient wants to log out");
                    loggedPatient = null;
                    state = State.AUTH;
                    outputStream.writeUTF("LOGGED_OUT");
                    return true;

                case "QUIT":
                    outputStream.writeUTF("BYE");
                    return false;

                default:
                    outputStream.writeUTF("ERROR Unknown command in MAIN_MENU state");
                    return true;
            }
        }

        /**
         * VIEW_PATIENT state: VIEW_DIAGNOSIS_FILE / BACK_TO_MENU
         */
        private boolean handleViewPatientCommand(String command) throws IOException {
            switch (command) {
                case "VIEW_DIAGNOSIS_FILE":
                    // You probably want some selection logic:
                    // handleViewDiagnosisFile(loggedPatient.getIdPatient());

                    state = State.VIEW_DIAGNOSIS_FILE;
                    return true;

                case "BACK_TO_MENU":
                    state = State.MAIN_MENU;
                    return true;

                case "QUIT":
                    outputStream.writeUTF("BYE");
                    return false;

                default:
                    outputStream.writeUTF("ERROR Unknown command in VIEW_PATIENT state");
                    return true;
            }
        }

        /**
         * VIEW_DIAGNOSIS_FILE state: VIEW_RECORDING / BACK_TO_PATIENT
         */
        private boolean handleDiagnosisFileCommand(String command) throws IOException {
            switch (command) {
                case "VIEW_RECORDING":
                    // handleViewRecordingList(selectedDiagnosisFileId);
                    handleViewRecording();
                    state = State.VIEW_RECORDING;
                    return true;

                case "BACK_TO_PATIENT":
                    state = State.VIEW_PATIENT;
                    return true;

                case "QUIT":
                    outputStream.writeUTF("BYE");
                    return false;

                default:
                    outputStream.writeUTF("ERROR Unknown command in VIEW_DIAGNOSIS_FILE state");
                    return true;
            }
        }

        /**
         * VIEW_RECORDING state: CHANGE_FRAGMENT / DOWNLOAD_RECORDING / BACK_TO_DIAGNOSIS_FILE
         */
        private boolean handleRecordingCommand(String command) throws IOException {
            switch (command) {
                case "CHANGE_FRAGMENT":
                    // handleChangeFragment(selectedRecordingId, nextFragmentIndex);
                    handleChangeFragment();
                    return true;

                case "DOWNLOAD_RECORDING":
                    // handleDownloadRecording(selectedRecordingId);
                    handleDownloadRecording();
                    return true;

                case "BACK_TO_DIAGNOSIS_FILE":
                    state = State.VIEW_DIAGNOSIS_FILE;
                    return true;

                case "QUIT":
                    outputStream.writeUTF("BYE");
                    return false;

                default:
                    outputStream.writeUTF("ERROR Unknown command in VIEW_RECORDING state");
                    return true;
            }
        }

        /* ============================ BUSINESS LOGIC STUBS ============================ */

        // Adapt these methods to your existing code

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

            // Sanitize y validar DNI (acepta minúscula y formatos con guiones/espacios)
            String dniClean = dni == null ? "" : dni.replaceAll("[^0-9A-Za-z]", "").toUpperCase();
            if (!dniClean.matches("\\d{8}[A-Z]")) {
                outputStream.writeUTF("ERROR");
                outputStream.writeUTF("Invalid DNI format. Expected 8 dígitos y una letra (ej: 12345678A).");
                outputStream.flush();
                return;
            }

            // Validate server-side as well (basic)
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



            String encryptedPass = hashPassword(password);
            try {
                userMan.register(username, encryptedPass, "PATIENT");
                int userId = userMan.getUserId(username);

                // Parse fecha en formato dd-MM-yyyy
                Date parsedDob = null;
                try {
                    java.util.Date utilDate = new SimpleDateFormat("dd-MM-yyyy").parse(birthday);
                    parsedDob = new Date(utilDate.getTime());
                } catch (ParseException pe) {
                    outputStream.writeUTF("ERROR");
                    outputStream.writeUTF("Invalid birthday format. Use dd-MM-yyyy (ej: 31-12-1990).");
                    outputStream.flush();
                    return;
                }
                Sex sexp = parseSex(sex);
                int phonenumber = Integer.parseInt(phone);
                int insuranceNumber = Integer.parseInt(insurance);
                int emergencyContactnum = Integer.parseInt(emergencyContact);

                Patient patient = new Patient(name, surname, dni, parsedDob, email, sexp, phonenumber, insuranceNumber, emergencyContactnum,  userId);
                patientMan.addPatient(patient);
                this.loggedPatient=patient;

                outputStream.writeUTF("ACK");
                outputStream.writeUTF("Sign up successful. You can log in now.");
                outputStream.flush();
            } catch (Exception ex) {
                outputStream.writeUTF("ERROR");
                outputStream.writeUTF("Sign up failed: " + ex.getMessage());
                outputStream.flush();
            }
        }

        /**
         * Login should return a Patient (or User) object on success, null on failure.
         */
        private void handleLogin() throws IOException {

            try {
                String username = inputStream.readUTF();
                String password = inputStream.readUTF();
                //boolean logged = userMan.verifyPassword(username, password);
                String storedpw = userMan.getPassword(username);
                boolean logged = checkPassword(password, storedpw);
                if (logged) {
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

        private void handleStart() throws IOException {
            // Here you can start ECG/EDA acquisition, etc.
            // Use ECG and EDA lists if needed.
            DiagnosisFile df = new DiagnosisFile(loggedPatient.getIdPatient());
            try {
                patientMan.AddNewDiagnosisFile(df);
                df.setId(patientMan.returnIdOfLastDiagnosisFile());
                outputStream.writeUTF("READY_TO_RECORD");
                String message= inputStream.readUTF();
                if (!message.equals("STOP")) {
                    patientMan.saveFragmentOfRecording(df.getId(), message);
                } else if(message.equals("STOP")){
                    handleEndOfRecording();
                    outputStream.writeUTF("RECORDING_STOP");

                    outputStream.writeUTF("SELECT_SYMPTOMS");
                    String selectedSymptoms = inputStream.readUTF();
                    if (selectedSymptoms != null && !selectedSymptoms.isEmpty()) {
                        patientMan.updateSymptomsInDiagnosisFile(df.getId(), selectedSymptoms);
                        outputStream.writeUTF("SYMPTOMS_RECEIVED");
                    }

                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }


        }

        private void handleViewPatientOverview() throws IOException {
            // Send basic info about loggedPatient to client
            if (loggedPatient != null) {
                String docName = patientMan.getDoctornameByPatient(loggedPatient);
                List<DiagnosisFile> diagnosisFiles = patientMan.getAllDiagnosisFilesFromPatient(loggedPatient.getIdPatient());
                loggedPatient.setDiagnosisList(diagnosisFiles);
                outputStream.writeUTF(docName+";"+loggedPatient.listOfDiagnosisFilesToString());
                outputStream.writeUTF("PATIENT_OVERVIEW_SENT");

            } else {
                outputStream.writeUTF("ERROR: No patient logged in");
            }
        }

        private void handleViewRecording() throws IOException {
            try {
                String adreess = inputStream.readUTF();
                if(!adreess.isEmpty()) {
                    String[] partes = adreess.split(",");
                    int id_diagnosisFile = Integer.parseInt(partes[0].trim());
                    int sequence = Integer.parseInt(partes[1].trim());
                    String fragment= patientMan.getFragmentOfRecording(id_diagnosisFile,sequence);
                    List<Boolean> states = patientMan.getSateOfFragmentsOfRecordingByID(id_diagnosisFile);

                    StringBuilder stateStringB = new StringBuilder();

                    for (Boolean state : states) {
                        stateStringB.append(state).append(",");
                    }
                    if (stateStringB.length() > 0) {
                        stateStringB.deleteCharAt(stateStringB.length() - 1);
                    }

                    String stateString = stateStringB.toString();
                    outputStream.writeUTF(fragment);
                    outputStream.writeUTF(stateString);

                }else{
                    outputStream.writeUTF("FAILED_TO_CONNET");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        private void handleChangeFragment() throws IOException {
            try {
                String adreess = inputStream.readUTF();
                if(!adreess.isEmpty()) {
                    String[] partes = adreess.split(",");
                    int id_diagnosisFile = Integer.parseInt(partes[0].trim());
                    int sequence = Integer.parseInt(partes[1].trim());
                    String fragment= patientMan.getFragmentOfRecording(id_diagnosisFile,sequence);

                    outputStream.writeUTF(fragment);

                }else{
                    outputStream.writeUTF("FAILED_TO_CONNET");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            outputStream.writeUTF("FRAGMENT_CHANGED");
        }

        private void handleDownloadRecording() {
            try {
                String idDiagnosisFile = inputStream.readUTF();
                outputStream.writeUTF("SENDING_RECORDING");
                List<String> fragmentList= patientMan.getAllFragmentsOfRecording(Integer.parseInt(idDiagnosisFile));

                String[] signals = joinFragmentsAsStrings(fragmentList);
                String ecgString = signals[0];
                String edaString = signals[1];

                outputStream.writeUTF(ecgString);
                outputStream.writeUTF(edaString);
            } catch (IOException | SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public String[] joinFragmentsAsStrings(List<String> fragments) {

            StringBuilder ecgBuilder = new StringBuilder();
            StringBuilder edaBuilder = new StringBuilder();

            for (String fragment : fragments) {
                // fragment tiene formato "ecg1,ecg2,...;eda1,eda2,..."
                String[] parts = fragment.split(";");
                String ecgPart = parts[0].trim();
                String edaPart = parts[1].trim();

                ecgBuilder.append(ecgPart).append(",");
                edaBuilder.append(edaPart).append(",");
            }

            // Quitar la última coma si existe
            if (ecgBuilder.length() > 0) {
                ecgBuilder.deleteCharAt(ecgBuilder.length() - 1);
            }
            if (edaBuilder.length() > 0) {
                edaBuilder.deleteCharAt(edaBuilder.length() - 1);
            }

            String ecgString = ecgBuilder.toString();
            String edaString = edaBuilder.toString();

            // [0] = ECG, [1] = EDA
            return new String[] { ecgString, edaString };
        }


        /* ============================ RESOURCE CLEANUP ============================ */

        private void releaseResourcesPatient(DataInputStream in,
                                             Socket socket,
                                             DataOutputStream out) {
            // Your existing cleanup implementation
            try {
                if (in != null) in.close();
            } catch (IOException ignored) { }
            try {
                if (out != null) out.close();
            } catch (IOException ignored) { }
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ignored) { }
        }

        private Sex parseSex(String sexStr) {
            if (sexStr == null) {
                throw new IllegalArgumentException("Sex value is null");
            }
            String s = sexStr.toUpperCase();

            // Map common cases
            if (s.equals("M") || s.equals("MALE") || s.equals("MAN") || s.equals("H") || s.equals("HOMBRE")) {
                return Sex.MALE;
            }
            if (s.equals("F") || s.equals("FEMALE") || s.equals("W") || s.equals("WOMAN") || s.equals("MUJER")) {
                return Sex.FEMALE;
            }

            // Try direct enum name (in case client already sends MALE/FEMALE or other valid names)
            try {
                return Sex.valueOf(s);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Valor de sexo no reconocido: " + sexStr);
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
                //diag.setSensorDataECG(serializeToCSV(ECG));
               // diag.setSensorDataEDA(serializeToCSV(EDA));
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

        /**
         * THIS IS NOT NEEDED SINCE ITS NOT A CLASS ANY MORE
         * private void handleSymptoms() throws IOException {
         * int count = inputStream.readInt();
         * currentSymptoms = new ArrayList<>();
         * for (int i = 0; i < count; i++) {
         * int id = inputStream.readInt();
         * if (id > 0) {
         * currentSymptoms.add(new Symptoms(id));
         * }
         * }
         * String timestamp = inputStream.readUTF();
         * System.out.printf("Received symptoms %s at %s%n", currentSymptoms, timestamp);
         * <p>
         * outputStream.writeUTF("ACK");
         * outputStream.writeUTF("Symptoms received.");
         * outputStream.flush();
         * }
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
                //ps.setString(6, file.getSensorDataECG());
                //ps.setString(7, file.getSensorDataEDA());
                ps.executeUpdate();
            }
        }

        public void sendAllDiagnosisFilesFromPatientToPatient(int idPatient) {
            DataOutputStream outputStream = null;

            try {
                // Abrir el flujo de salida para enviar los datos al cliente
                outputStream = new DataOutputStream(socket.getOutputStream());

                // Obtener todos los archivos de diagnóstico del paciente
                List<DiagnosisFile> diagnosisFilesFromPatient = patientMan.getAllDiagnosisFilesFromPatient(idPatient);

                // Si no hay archivos de diagnóstico, notificar al cliente
                if (diagnosisFilesFromPatient.isEmpty()) {
                    outputStream.writeUTF("No diagnosis files available for this patient.");
                    outputStream.flush();
                    return;
                }

                // Enviar al cliente la cantidad de archivos de diagnóstico disponibles
                outputStream.writeUTF("All Diagnosis Files from Patient");
                outputStream.writeInt(diagnosisFilesFromPatient.size());

                // Usar un String normal para concatenar la lista de diagnóstico
                String listDiagnosis = "";
                for (DiagnosisFile diagnosisFile : diagnosisFilesFromPatient) {
                    listDiagnosis += diagnosisFile.toString() + "\n";  // Concatenar la información de cada archivo
                }

                // Enviar la lista de archivos de diagnóstico al cliente
                outputStream.writeUTF(listDiagnosis);

                // Asegurarse de que todos los datos se hayan enviado correctamente
                outputStream.flush();

            } catch (IOException ex) {
                // Manejo de excepciones en caso de errores durante el envío
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, "Error sending diagnosis files from patient", ex);
                try {
                    if (outputStream != null) {
                        // Enviar un mensaje de error al cliente si ocurre una excepción
                        outputStream.writeUTF("Error sending diagnosis files from patient.");
                        outputStream.flush();
                    }
                } catch (IOException e) {
                    Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, "Error responding to client", e);
                }
            }
        }
        public void sendFragmentofRecording(String fragment) {
            DataOutputStream outputStream = null;

            try {
                outputStream = new DataOutputStream(socket.getOutputStream());

                // 1) Parsear el String recibido: "idDiagnosisFile,position"
                String[] parts = fragment.split(",");
                if (parts.length != 2) {
                    outputStream.writeBoolean(false);
                    outputStream.writeUTF("Error: fragment format must be 'idDiagnosisFile,position'");
                    outputStream.flush();
                    return;
                }

                int idDiagnosisFile = Integer.parseInt(parts[0].trim());
                int position = Integer.parseInt(parts[1].trim());

                System.out.println("Request fragment -> idDiagnosisFile="
                        + idDiagnosisFile + ", position=" + position);

                // 2) Obtener el fragmento desde la BD
                String fragmentData;
                try {
                    fragmentData = patientMan.getFragmentOfRecording(idDiagnosisFile, position);
                } catch (SQLException e) {
                    System.out.println("SQL error while retrieving recording fragment.");
                    e.printStackTrace();
                    outputStream.writeBoolean(false);
                    outputStream.writeUTF("SQL error while retrieving fragment: " + e.getMessage());
                    outputStream.flush();
                    return;
                }

                // 3) Enviar la respuesta al cliente
                if (fragmentData == null) {
                    // Error interno o excepción capturada dentro de getFracmentofRecoring
                    outputStream.writeBoolean(false);
                    outputStream.writeUTF("Error retrieving fragment from the database.");
                } else if ("No fragment found for the given DiagnosisFileId and position."
                        .equals(fragmentData)) {
                    // Caso en el que la consulta no devolvió filas
                    outputStream.writeBoolean(false);
                    outputStream.writeUTF(fragmentData);
                } else {
                    // Fragmento encontrado correctamente
                    outputStream.writeBoolean(true);    // indica éxito
                    outputStream.writeUTF(fragmentData); // aquí va el dato real (columna "data")
                }

                outputStream.flush();

            } catch (IOException e) {
                System.out.println("I/O error in sendFracmentofRecording.");
                e.printStackTrace();
            } catch (NumberFormatException e) {
                System.out.println("Error parsing idDiagnosisFile or position from fragment string.");
                e.printStackTrace();
                try {
                    if (outputStream != null) {
                        outputStream.writeBoolean(false);
                        outputStream.writeUTF("Error: unable to parse idDiagnosisFile or position.");
                        outputStream.flush();
                    }
                } catch (IOException ignored) { }
            }
        }
        public void SendStateOfFragmentsOfRecordingByID(int idDiagnosisFile) {
            try {
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

                // 1. Obtener el estado de los fragmentos desde la base de datos
                List<Boolean> statesList = patientMan.getSateOfFragmentsOfRecordingByID(idDiagnosisFile);

                // 2. Verificar si hay estados disponibles
                if (statesList.isEmpty()) {
                    outputStream.writeUTF("No states found for this diagnosis file");
                    outputStream.flush();
                    return;
                }

                // 3. Crear un string con los estados "true,false,true,false"
                StringBuilder statesStr = new StringBuilder();
                for (Boolean b : statesList) {
                    statesStr.append(b).append(",");
                }

                // Eliminar la última coma si existe
                if (statesStr.length() > 0 && statesStr.charAt(statesStr.length() - 1) == ',') {
                    statesStr.deleteCharAt(statesStr.length() - 1);
                }

                // 4. Enviar la respuesta al cliente
                outputStream.writeUTF("StatesOfFragments");  // cabecera para indicar lo que estamos enviando
                outputStream.writeInt(statesList.size());    // cuántos estados estamos enviando
                outputStream.writeUTF(statesStr.toString()); // el string de estados

                outputStream.flush();

            } catch (SQLException | IOException e) {
                System.out.println("Error while sending states of fragments");
                e.printStackTrace();
            }
        }


        private String serializeToCSV(List<Integer> list) {
            return list.stream().map(Object::toString).collect(Collectors.joining(","));
        }

    }

    // SERVER DOCTOR THREAD
    private static class ServerDoctorThread implements Runnable {
        private static final Logger LOGGER = Logger.getLogger(ServerDoctorThread.class.getName());

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
        private Doctor loggedDoctor=null;

        // All the states that appear in your scheme
        private enum State {
            AUTH,                // SIGN_UP / LOG_IN / QUIT
            DOCTOR_MENU,         // main menu after login
            SEARCH_PATIENT,      // screen where doctor searches/chooses a patient
            VIEW_PATIENT,        // info of one patient
            VIEW_DIAGNOSISFILE,  // list/detail of diagnosis files
            VIEW_RECORDING,      // recording and fragments
            RECENTLY_FINISH,     // list of recently finished diagnosis
            COMPLETE_DIAGNOSISFILE // finishing a diagnosis file
        }

        // Stack of states to support "go back to previous"
        private final Deque<State> stateStack = new ArrayDeque<>();


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
            stateStack.push(State.AUTH);
        }

        @Override
        public void run() {
            try {
                boolean running = true;

                while (running && !socket.isClosed()) {

                    String command = readCommand();
                    if (command == null) {
                        break; // client disconnected
                    }
                    command = command.trim();
                    if (command.isEmpty()) {
                        continue;
                    }
                    System.out.printf(command);
                    switch (currentState()) {
                        case AUTH:
                            running = handleAuthCommand(command);
                            System.out.println(currentState());
                            break;

                        case DOCTOR_MENU:
                            running = handleDoctorMenuCommand(command);
                            System.out.println(currentState());
                            break;

                        case SEARCH_PATIENT:
                            running = handleSearchPatientCommand(command);
                            break;

                        case VIEW_PATIENT:
                            running = handleViewPatientCommand(command);
                            break;

                        case VIEW_DIAGNOSISFILE:
                            running = handleViewDiagnosisFileCommand(command);
                            break;

                        case VIEW_RECORDING:
                            running = handleViewRecordingCommand(command);
                            break;

                        case RECENTLY_FINISH:
                            running = handleRecentlyFinishCommand(command);
                            break;

                        case COMPLETE_DIAGNOSISFILE:
                            running = handleCompleteDiagnosisFileCommand(command);
                            break;
                    }
                }

            } catch (IOException | ParseException ex) {
                LOGGER.log(Level.SEVERE, "Error in doctor server thread", ex);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error in doctor server thread", e);
            } finally {
                System.out.println("Doctor client disconnected");
                releaseResources(inputStream, socket, outputStream);
            }
        }

        /* ============================ STATE STACK HELPERS ============================ */

        private State currentState() {
            return stateStack.peek();
        }

        private void goTo(State next) {
            stateStack.push(next);
        }

        // In your scheme "BACK_TO_MENU" really means: go back to previous node
        private void goBack() {
            if (stateStack.size() > 1) {
                stateStack.pop();
            }
            // if size == 1 we stay in AUTH
        }

        /* ============================ IO HELPER ============================ */

        private String readCommand() {
            try {
                return inputStream.readUTF();
            } catch (IOException e) {
                System.out.println("Doctor client disconnected (readUTF failed).");
                return null;
            }
        }

        /* ============================ STATE HANDLERS ============================ */

        // AUTH: SIGN_UP / LOG_IN / QUIT
        private boolean handleAuthCommand(String command) throws IOException, ParseException {
            switch (command) {
                case "SIGNUP":
                case "SIGN_UP":
                    handleSignupDoctor();
                    if (this.loggedDoctor != null) {
                        goTo(State.DOCTOR_MENU);
                        outputStream.writeUTF("SIGNUP_OK");
                    } else {
                        outputStream.writeUTF("SIGNUP_FAILED");
                    }
                    return true;

                case "LOGIN":
                case "LOG_IN":
                    handleLoginDoctor();
                    if (this.loggedDoctor != null) {
                        goTo(State.DOCTOR_MENU);
                        outputStream.writeUTF("LOGIN_OK");
                    } else {
                        outputStream.writeUTF("LOGIN_FAILED");
                    }
                    return true;

                case "QUIT":
                    outputStream.writeUTF("BYE");
                    return false;

                default:
                    outputStream.writeUTF("ERROR Unknown command in AUTH state");
                    return true;
            }
        }

        // DOCTOR_MENU: from here you go to SEARCH_PATIENT or RECENTLY_FINISH
        private boolean handleDoctorMenuCommand(String command) throws IOException {
            switch (command) {
                case "SEARCH_PATIENT":
                    // show search UI / ask for criteria
                    doOpenSearchPatient();
                    goTo(State.SEARCH_PATIENT);   // next "BACK" will return here
                    return true;

                case "RECENTLY_FINISH":
                    doListRecentlyFinished();
                    goTo(State.RECENTLY_FINISH);  // next "BACK" will return here
                    return true;

                case "LOG_OUT":
                    // already at menu – optionally go back to AUTH:
                    goBack();
                    return true;

                case "QUIT":
                    outputStream.writeUTF("BYE");
                    return false;

                default:
                    outputStream.writeUTF("ERROR Unknown command in DOCTOR_MENU state");
                    return true;
            }
        }

        // SEARCH_PATIENT: here the doctor searches and selects a patient
        // Next: VIEW_PATIENT or back to DOCTOR_MENU
        private boolean handleSearchPatientCommand(String command) throws IOException {
            switch (command) {
                case "VIEW_PATIENT":
                    // here you normally would read which patient was selected
                    doSelectPatientAndShowInfo();
                    goTo(State.VIEW_PATIENT);      // stack: ... -> DOCTOR_MENU -> SEARCH_PATIENT -> VIEW_PATIENT
                    return true;

                case "BACK_TO_MENU":              // go back one level to DOCTOR_MENU
                    goBack();
                    return true;

                case "QUIT":
                    outputStream.writeUTF("BYE");
                    return false;

                default:
                    outputStream.writeUTF("ERROR Unknown command in SEARCH_PATIENT state");
                    return true;
            }
        }

        // VIEW_PATIENT: you came from SEARCH_PATIENT
        // Next: VIEW_DIAGNOSISFILE or back to SEARCH_PATIENT
        private boolean handleViewPatientCommand(String command) throws IOException {
            switch (command) {
                case "VIEW_DIAGNOSISFILE":
                    goTo(State.VIEW_DIAGNOSISFILE);   // ★1
                    return true;

                case "BACK_TO_MENU":                  // really: back to SEARCH_PATIENT
                    goBack();                         // pops VIEW_PATIENT -> SEARCH_PATIENT on top
                    return true;

                case "QUIT":
                    outputStream.writeUTF("BYE");
                    return false;

                default:
                    outputStream.writeUTF("ERROR Unknown command in VIEW_PATIENT state");
                    return true;
            }
        }

        // VIEW_DIAGNOSISFILE: shared node ★1 (from VIEW_PATIENT or COMPLETE_DIAGNOSISFILE)
        // Next: DOWNLOAD_DIAGNOSISFILE / VIEW_RECORDING / back to previous (VIEW_PATIENT or COMPLETE_DIAGNOSISFILE)
        private boolean handleViewDiagnosisFileCommand(String command) throws IOException {
            switch (command) {
                case "DOWNLOAD_DIAGNOSISFILE":
                    doDownloadDiagnosisFile();
                    return true;

                case "VIEW_RECORDING":
                    doViewRecording();
                    goTo(State.VIEW_RECORDING);       // ★2
                    return true;

                case "BACK_TO_MENU":
                    // previous could be VIEW_PATIENT or COMPLETE_DIAGNOSISFILE
                    goBack();
                    return true;

                case "QUIT":
                    outputStream.writeUTF("BYE");
                    return false;

                default:
                    outputStream.writeUTF("ERROR Unknown command in VIEW_DIAGNOSISFILE state");
                    return true;
            }
        }

        // VIEW_RECORDING: shared node ★2
        // Next: CHANGE_FRAGMENT / DOWNLOAD_RECORDING / back to VIEW_DIAGNOSISFILE
        private boolean handleViewRecordingCommand(String command) throws IOException {
            switch (command) {
                case "CHANGE_FRAGMENT":
                    doChangeFragment();
                    return true;

                case "DOWNLOAD_RECORDING":
                    doDownloadRecording();
                    return true;

                case "BACK_TO_MENU":
                    // back to whatever was before (VIEW_DIAGNOSISFILE)
                    goBack();
                    return true;

                case "QUIT":
                    outputStream.writeUTF("BYE");
                    return false;

                default:
                    outputStream.writeUTF("ERROR Unknown command in VIEW_RECORDING state");
                    return true;
            }
        }

        // RECENTLY_FINISH: you came from DOCTOR_MENU
        // Next: COMPLETE_DIAGNOSISFILE or back to DOCTOR_MENU
        private boolean handleRecentlyFinishCommand(String command) throws IOException {
            switch (command) {
                case "COMPLETE_DIAGNOSISFILE":
                    doCompleteDiagnosisFileSelection();
                    goTo(State.COMPLETE_DIAGNOSISFILE);
                    return true;

                case "BACK_TO_MENU":
                    // back to DOCTOR_MENU
                    goBack();
                    return true;

                case "QUIT":
                    outputStream.writeUTF("BYE");
                    return false;

                default:
                    outputStream.writeUTF("ERROR Unknown command in RECENTLY_FINISH state");
                    return true;
            }
        }

        // COMPLETE_DIAGNOSISFILE: you came from RECENTLY_FINISH
        // Next: VIEW_DIAGNOSISFILE (★1) / VIEW_RECORDING (★2) / CANCEL(back to RECENTLY_FINISH)
        private boolean handleCompleteDiagnosisFileCommand(String command) throws IOException {
            switch (command) {
                case "VIEW_DIAGNOSISFILE":           // ★1 path
                    goTo(State.VIEW_DIAGNOSISFILE);
                    return true;

                case "VIEW_RECORDING":               // ★2 path
                    doViewRecording();
                    goTo(State.VIEW_RECORDING);
                    return true;

                case "CANCEL":
                case "BACK_TO_MENU":
                    // back to RECENTLY_FINISH
                    goBack();
                    return true;

                case "QUIT":
                    outputStream.writeUTF("BYE");
                    return false;

                default:
                    outputStream.writeUTF("ERROR Unknown command in COMPLETE_DIAGNOSISFILE state");
                    return true;
            }
        }

        /* ============================ BUSINESS LOGIC STUBS ============================ */

        private void handleSignupDoctor() throws IOException {
            try {
                String username = inputStream.readUTF();
                String password = inputStream.readUTF();
                String name = inputStream.readUTF();
                String surname = inputStream.readUTF();
                String birthday = inputStream.readUTF(); // "dd-MM-yyyy"
                String sex = inputStream.readUTF();
                String email = inputStream.readUTF();
                String specialty = inputStream.readUTF();
                String licenseNumber = inputStream.readUTF();
                String dni = inputStream.readUTF();

                // Sanitize y validar DNI
                String dniClean = dni == null ? "" : dni.replaceAll("[^0-9A-Za-z]", "").toUpperCase();
                if (!dniClean.matches("\\d{8}[A-Z]") && !dniClean.matches("[XYZ]\\d{7}[A-Z]")) {
                    outputStream.writeUTF("ERROR");
                    outputStream.writeUTF(
                            "Invalid DNI/NIE format. Expected 8 dígitos + letra (12345678A) " +
                                    "o NIE tipo X1234567L."
                    );
                    outputStream.flush();
                    return;
                }


                if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                    outputStream.writeUTF("ERROR");
                    outputStream.writeUTF("Invalid email format.");
                    outputStream.flush();
                    return;
                }

                // Parse fecha en formato dd-MM-yyyy
                Date parsedDob = null;
                try {
                    java.util.Date utilDate = new SimpleDateFormat("dd-MM-yyyy").parse(birthday);
                    parsedDob = new Date(utilDate.getTime());
                } catch (ParseException pe) {
                    outputStream.writeUTF("ERROR");
                    outputStream.writeUTF("Invalid birthday format. Use dd-MM-yyyy (ej: 31-12-1990).");
                    outputStream.flush();
                    return;
                }
                String dob = String.valueOf(parsedDob.getTime());

                String encryptedPass = hashPassword(password);

                try (var c = conMan.getConnection()) {
                    c.setAutoCommit(false);
                    try {

                        int newUserId;
                        try (var psUser = c.prepareStatement(
                                "INSERT INTO users (username, password, role) VALUES (?,?,?)",
                                Statement.RETURN_GENERATED_KEYS)) {
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
                        try (PreparedStatement psDoc = c.prepareStatement(
                                "INSERT INTO doctors (userId, nameDoctor, surnameDoctor, dniDoctor, dobDoctor, emailDoctor, sexDoctor, specialty, licenseNumber) VALUES (?,?,?,?,?,?,?,?,?)")) {
                            psDoc.setInt(1, newUserId);          // userId obtenido al crear el user
                            psDoc.setString(2, name);
                            psDoc.setString(3, surname);
                            psDoc.setString(4, dniClean);
                            psDoc.setString(5, dob); // usamos yyyy-MM-dd en la BDD
                            psDoc.setString(6, email);
                            psDoc.setString(7, sex);
                            psDoc.setString(8, specialty);
                            psDoc.setString(9, licenseNumber);

                            System.out.println("Insertando doctor: userId=" + newUserId + " dni=" + dniClean + " email=" + email);

                            psDoc.executeUpdate();
                        }
                        c.commit();
                        loggedDoctor= doctorMan.getDoctorbyUserId(newUserId);
                        loggedDoctorId=loggedDoctor.getIdDoctor();
                        outputStream.writeUTF("ACK");
                        outputStream.writeUTF("Doctor sign up successful. You can log in now.");
                        outputStream.flush();
                        return;
                    } catch (SQLException ex) {
                        try {
                            c.rollback();
                        } catch (SQLException ignore) {
                        }
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
                        try {
                            c.setAutoCommit(true);
                        } catch (SQLException ignore) {
                        }
                    }
                }
            } catch (Exception ex) {
                outputStream.writeUTF("ERROR");
                outputStream.writeUTF("Sign up failed: " + ex.getMessage());
                outputStream.flush();
            }
        }

        private void handleLoginDoctor() throws IOException {
            try {
                String username = inputStream.readUTF();
                String password = inputStream.readUTF();
                String storedpw = userMan.getPassword(username);
                boolean logged = checkPassword(password, storedpw);

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
                            loggedDoctor= doctorMan.getDoctorbyUserId(loggedDoctorUserId);
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

        private void doOpenSearchPatient() throws IOException {
            try {
                List<String> InsuranceNumbers= doctorMan.getAllPatientsInsuranceNumberbyDoctor();
                String InsuranceNumbersMessage=  String.join(", ", InsuranceNumbers);

                outputStream.writeUTF(InsuranceNumbersMessage);

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        private void doSelectPatientAndShowInfo() throws IOException {
            try {
            // Read chosen patient from client, set selectedPatient, send info
                int PatientHIN = inputStream.readInt();
                Patient selectedPatient = doctorMan.getPatientByHIN(PatientHIN);
                List<DiagnosisFile> diagnosisFiles = doctorMan.getAllDiagnosisFilesFromPatient(selectedPatient.getIdPatient());
                selectedPatient.setDiagnosisList(diagnosisFiles);
                outputStream.writeUTF("PATIENT_OVERVIEW_SENT");
                outputStream.writeUTF(selectedPatient.toString());

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }



        private void doDownloadDiagnosisFile() throws IOException {
            outputStream.writeUTF("DOWNLOAD_DIAGNOSISFILE_STARTED");
            try{
                String idDiagnosisFile = inputStream.readUTF();
                int idDF = Integer.parseInt(idDiagnosisFile);
                DiagnosisFile diagnosisFile = doctorMan.getDiagnosisFileByID(idDF);
                outputStream.writeUTF(diagnosisFile.toString());
            } catch (IOException | SQLException e) {
                throw new RuntimeException(e);
            }
        }

        private void doViewRecording() throws IOException {
            try {
                String adreess = inputStream.readUTF();
                if(!adreess.isEmpty()) {
                    String[] partes = adreess.split(",");
                    int id_diagnosisFile = Integer.parseInt(partes[0].trim());
                    int sequence = Integer.parseInt(partes[1].trim());
                    String fragment= doctorMan.getFragmentOfRecording(id_diagnosisFile,sequence);
                    List<Boolean> states = doctorMan.getSateOfFragmentsOfRecordingByID(id_diagnosisFile);

                    StringBuilder stateStringB = new StringBuilder();

                    for (Boolean state : states) {
                        stateStringB.append(state).append(",");
                    }
                    if (stateStringB.length() > 0) {
                        stateStringB.deleteCharAt(stateStringB.length() - 1);
                    }

                    String stateString = stateStringB.toString();
                    outputStream.writeUTF(fragment);
                    outputStream.writeUTF(stateString);

                }else{
                    outputStream.writeUTF("FAILED_TO_CONNET");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        private void doChangeFragment() throws IOException {
            try {
                String adreess = inputStream.readUTF();
                if(!adreess.isEmpty()) {
                    String[] partes = adreess.split(",");
                    int id_diagnosisFile = Integer.parseInt(partes[0].trim());
                    int sequence = Integer.parseInt(partes[1].trim());
                    String fragment= doctorMan.getFragmentOfRecording(id_diagnosisFile,sequence);

                    outputStream.writeUTF(fragment);

                }else{
                    outputStream.writeUTF("FAILED_TO_CONNET");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            outputStream.writeUTF("FRAGMENT_CHANGED");
        }

        private void doDownloadRecording() throws IOException {
            try {
                String idDiagnosisFile = inputStream.readUTF();
                int idDF = Integer.parseInt(idDiagnosisFile);
                outputStream.writeUTF("DOWNLOAD_RECORDING_STARTED");
                List<String> fragmentList = doctorMan.getAllFragmentsOfRecording(idDF);
                List<Boolean> stateList = doctorMan.getSateOfFragmentsOfRecordingByID(idDF);

                String[] signals = joinFragmentsAsStrings(fragmentList);
                String ecgString = signals[0];
                String edaString = signals[1];

                outputStream.writeUTF(ecgString);
                outputStream.writeUTF(edaString);
                outputStream.writeUTF("DOWNLOAD_FINISHED");
            } catch (IOException | SQLException e) {
                throw new RuntimeException(e);
            }
            outputStream.writeUTF("DOWNLOAD_RECORDING_FINISHED");
        }

        public String[] joinFragmentsAsStrings(List<String> fragments) {

            StringBuilder ecgBuilder = new StringBuilder();
            StringBuilder edaBuilder = new StringBuilder();

            for (String fragment : fragments) {
                // fragment tiene formato "ecg1,ecg2,...;eda1,eda2,..."
                String[] parts = fragment.split(";");
                String ecgPart = parts[0].trim();
                String edaPart = parts[1].trim();

                ecgBuilder.append(ecgPart).append(",");
                edaBuilder.append(edaPart).append(",");
            }

            // Quitar la última coma si existe
            if (ecgBuilder.length() > 0) {
                ecgBuilder.deleteCharAt(ecgBuilder.length() - 1);
            }
            if (edaBuilder.length() > 0) {
                edaBuilder.deleteCharAt(edaBuilder.length() - 1);
            }

            String ecgString = ecgBuilder.toString();
            String edaString = edaBuilder.toString();

            // [0] = ECG, [1] = EDA
            return new String[] { ecgString, edaString };
        }

        private void doListRecentlyFinished() throws IOException {
            outputStream.writeUTF("RECENTLY_FINISH_LIST");
            List<DiagnosisFile> recentDF = null;
            recentDF = doctorMan.listDiagnosisFilesTODO();

            outputStream.writeUTF(recentDF.toString());

            outputStream.writeUTF("RECENTLY_FINISHED");
        }

        private void doCompleteDiagnosisFileSelection() throws IOException {
            outputStream.writeUTF("COMPLETE_DIAGNOSISFILE_READY");
            String idDF = inputStream.readUTF();
            int idDiagnosisFile = Integer.parseInt(idDF);
            String diagnosisString = inputStream.readUTF();
            try {
                DiagnosisFile diagnosisFile = doctorMan.getDiagnosisFileByID(idDiagnosisFile);
                diagnosisFile.setDiagnosis(diagnosisString);
                doctorMan.UpDateDiagnosisFile(diagnosisFile);
                outputStream.writeUTF("COMPLETE_DIAGNOSISFILE_SAVED");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }


        /* ============================ CLEANUP ============================ */

        private void releaseResources(DataInputStream in, Socket s, DataOutputStream out) {
            try { if (in != null) in.close(); } catch (IOException ignored) {}
            try { if (out != null) out.close(); } catch (IOException ignored) {}
            try { if (s != null && !s.isClosed()) s.close(); } catch (IOException ignored) {}
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

        public void SendDiagnosisFilesTODO(int idDoctor) {
            DataOutputStream outputStream = null;

            try {
                outputStream = new DataOutputStream(socket.getOutputStream());

                List<DiagnosisFile> diagnosisFilesTODO = doctorMan.listDiagnosisFilesTODO();

                // Si no hay archivos recientemente terminados, notificar al cliente
                if (diagnosisFilesTODO.isEmpty()) {
                    outputStream.writeUTF("No recently finished diagnosis files available.");
                    outputStream.flush();
                    return;
                }

                // Enviar al cliente la cantidad de archivos de diagnóstico disponibles
                outputStream.writeUTF("Recently Finished Diagnosis Files");
                outputStream.writeInt(diagnosisFilesTODO.size());

                String listDiagnosis = "";
                for (DiagnosisFile diagnosisFile : diagnosisFilesTODO) {
                    listDiagnosis += diagnosisFile.toString() + "\n";
                }
                outputStream.writeUTF(listDiagnosis);

                outputStream.flush();  // Asegurarse de que todos los datos se han enviado

            } catch (IOException ex) {
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, "Error sending recently finished diagnosis files", ex);
                try {
                    outputStream.writeUTF("Error sending recently finished diagnosis files.");
                    outputStream.flush();
                } catch (IOException e) {
                    Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, "Error responding to client", e);
                }
            }
        }

        public void sendAllDiagnosisFilesFromPatientToDoctor(int idPatient) {
            DataOutputStream outputStream = null;

            try {
                outputStream = new DataOutputStream(socket.getOutputStream());

                List<DiagnosisFile> diagnosisFilesFromPatient = doctorMan.getAllDiagnosisFilesFromPatient(idPatient);

                if (diagnosisFilesFromPatient.isEmpty()) {
                    outputStream.writeUTF("No diagnosis files available for this patient.");
                    outputStream.flush();
                    return;
                }

                outputStream.writeUTF("All Diagnosis Files from Patient");
                outputStream.writeInt(diagnosisFilesFromPatient.size());

                String listDiagnosis = "";
                for (DiagnosisFile diagnosisFile : diagnosisFilesFromPatient) {
                    listDiagnosis += diagnosisFile.toString() + "\n";  // Concatenar la información de cada archivo
                }

                // Enviar la lista de archivos de diagnóstico al cliente
                outputStream.writeUTF(listDiagnosis);

                // Asegurarse de que todos los datos se hayan enviado correctamente
                outputStream.flush();

            } catch (IOException ex) {
                // Manejo de excepciones en caso de errores durante el envío
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, "Error sending diagnosis files from patient", ex);
                try {
                    if (outputStream != null) {
                        // Enviar un mensaje de error al cliente si ocurre una excepción
                        outputStream.writeUTF("Error sending diagnosis files from patient.");
                        outputStream.flush();
                    }
                } catch (IOException e) {
                    Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, "Error responding to client", e);
                }
            }
        }

        public DiagnosisFile ReceiveUpdatedDiagnosisFile() {
            DiagnosisFile updatedDiagnosisFile = null;

            try {
                // Leer el diagnóstico actualizado enviado por el cliente
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                String diagnosisString = inputStream.readUTF();

                // Convertir el String recibido en un objeto DiagnosisFile
                updatedDiagnosisFile = convertStringToDiagnosisFile(diagnosisString);

                // Actualizar el diagnóstico en el DoctorManager
                doctorMan.UpDateDiagnosisFile(updatedDiagnosisFile);

                // Confirmación de que el diagnóstico fue actualizado correctamente
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                outputStream.writeUTF("Diagnosis file updated successfully.");
                outputStream.flush();

            } catch (IOException e) {
                System.out.println("Error receiving and updating diagnosis file: " + e.getMessage());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            return updatedDiagnosisFile;
        }

        private DiagnosisFile convertStringToDiagnosisFile(String diagnosisString) {
            // Eliminar la parte inicial y final del String (MedicalRecord{...})
            diagnosisString = diagnosisString.replace("MedicalRecord{", "")
                    .replace("}", "");

            // Dividir la cadena en partes separadas por coma
            String[] parts = diagnosisString.split(", ");

            // Crear un objeto DiagnosisFile
            DiagnosisFile diagnosisFile = new DiagnosisFile();

            // Asignar los valores de cada parte al objeto DiagnosisFile
            for (String part : parts) {
                String[] keyValue = part.split("=");
                String key = keyValue[0].trim();
                String value = keyValue[1].trim().replace("'", "");

                // Asignar los valores al objeto DiagnosisFile
                switch (key) {
                    case "id":
                        diagnosisFile.setId(Integer.parseInt(value));
                        break;
                    case "symptoms":
                        // Convertir los síntomas a un ArrayList<String>
                        ArrayList<String> symptomsList = new ArrayList<>();
                        for (String symptom : value.split(",")) {
                            symptomsList.add(symptom.trim());
                        }
                        diagnosisFile.setSymptoms(symptomsList);
                        break;
                    case "diagnosis":
                        diagnosisFile.setDiagnosis(value);
                        break;
                    case "medication":
                        diagnosisFile.setMedication(value);
                        break;
                    case "date":
                        diagnosisFile.setDate(LocalDate.parse(value));
                        break;
                    case "patient id":
                        diagnosisFile.setPatientId(Integer.parseInt(value));
                        break;
                    case "status":
                        diagnosisFile.setStatus(Boolean.parseBoolean(value));
                        break;
                }
            }
            return diagnosisFile;
        }

        public void sendFragmentofRecording(String fragment) {
            DataOutputStream outputStream = null;

            try {
                outputStream = new DataOutputStream(socket.getOutputStream());

                // 1) Parsear el String recibido: "idDiagnosisFile,position"
                String[] parts = fragment.split(",");
                if (parts.length != 2) {
                    outputStream.writeBoolean(false);
                    outputStream.writeUTF("Error: fragment format must be 'idDiagnosisFile,position'");
                    outputStream.flush();
                    return;
                }

                int idDiagnosisFile = Integer.parseInt(parts[0].trim());
                int position = Integer.parseInt(parts[1].trim());

                System.out.println("Request fragment -> idDiagnosisFile="
                        + idDiagnosisFile + ", position=" + position);

                // 2) Obtener el fragmento desde la BD usando el doctorManager
                String fragmentData;
                try {
                    fragmentData = doctorMan.getFragmentOfRecording(idDiagnosisFile, position);
                } catch (SQLException e) {
                    System.out.println("SQL error while retrieving recording fragment.");
                    e.printStackTrace();
                    outputStream.writeBoolean(false);
                    outputStream.writeUTF("SQL error while retrieving fragment: " + e.getMessage());
                    outputStream.flush();
                    return;
                }

                // 3) Enviar la respuesta al cliente
                if (fragmentData == null) {
                    // Error interno o excepción capturada dentro de getFracmentofRecoring
                    outputStream.writeBoolean(false);
                    outputStream.writeUTF("Error retrieving fragment from the database.");
                } else if ("No fragment found for the given DiagnosisFileId and position."
                        .equals(fragmentData)) {
                    // Caso en el que la consulta no devolvió filas
                    outputStream.writeBoolean(false);
                    outputStream.writeUTF(fragmentData);
                } else {
                    // Fragmento encontrado correctamente
                    outputStream.writeBoolean(true);    // indica éxito
                    outputStream.writeUTF(fragmentData); // aquí va el dato real (columna "data")
                }

                outputStream.flush();

            } catch (IOException e) {
                System.out.println("I/O error in sendFracmentofRecording.");
                e.printStackTrace();
            } catch (NumberFormatException e) {
                System.out.println("Error parsing idDiagnosisFile or position from fragment string.");
                e.printStackTrace();
                try {
                    if (outputStream != null) {
                        outputStream.writeBoolean(false);
                        outputStream.writeUTF("Error: unable to parse idDiagnosisFile or position.");
                        outputStream.flush();
                    }
                } catch (IOException ignored) { }
            }
        }

        public void sendStateOfFragmentsOfRecordingByID(int idDiagnosisFile) {
            try {
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

                // 1. Obtener el estado de los fragmentos desde la base de datos
                List<Boolean> statesList = doctorMan.getSateOfFragmentsOfRecordingByID(idDiagnosisFile);

                // 2. Verificar si hay estados disponibles
                if (statesList.isEmpty()) {
                    outputStream.writeUTF("No states found for this diagnosis file");
                    outputStream.flush();
                    return;
                }

                // 3. Crear un string con los estados "true,false,true,false"
                StringBuilder statesStr = new StringBuilder();
                for (Boolean b : statesList) {
                    statesStr.append(b).append(",");
                }

                // Eliminar la última coma si existe
                if (statesStr.length() > 0 && statesStr.charAt(statesStr.length() - 1) == ',') {
                    statesStr.deleteCharAt(statesStr.length() - 1);
                }

                // 4. Enviar la respuesta al cliente
                outputStream.writeUTF("StatesOfFragments");  // cabecera para indicar lo que estamos enviando
                outputStream.writeInt(statesList.size());    // cuántos estados estamos enviando
                outputStream.writeUTF(statesStr.toString()); // el string de estados

                outputStream.flush();

            } catch (SQLException | IOException e) {
                System.out.println("Error while sending states of fragments");
                e.printStackTrace();
            }
        }



    }



    public static String hashPassword(String password_plaintext) {
        String salt = BCrypt.gensalt(workload);
        String hashed_password = BCrypt.hashpw(password_plaintext, salt);

        return(hashed_password);
    }

    public static boolean checkPassword(String password_plaintext, String stored_hash) {
        boolean password_verified = false;

        if (null == stored_hash || !stored_hash.startsWith("$2a$"))
            throw new IllegalArgumentException("Invalid hash provided for comparison");

        password_verified = BCrypt.checkpw(password_plaintext, stored_hash);

        return (password_verified);
    }

}
