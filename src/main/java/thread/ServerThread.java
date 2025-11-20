package thread;

import jdbc.*;
import pojos.DiagnosisFile;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.security.crypto.bcrypt.BCrypt;
import pojos.enums.Sex;

public class ServerThread {

    private static final String DataBase_Address = "jdbc:sqlite:CardioLink.db";
    private static final int port = 9000;
    private static int workload = 12;
    private static String hash = "$2a$06$.rCVZVOThsIa96pEDOxvGuRRgzG64bnptJ0938xuqzv18d3ZpQhstC";


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
                releaseResourcesPatient(inputStream, socket, outputStream);
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

            Patient p = new Patient();
            p.setNamePatient(name);
            p.setSurnamePatient(surname);
            p.setEmailPatient(email);
            p.setDniPatient(dniClean);

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

            // Parse fecha en formato dd-MM-yyyy
            try {
                Date parsedDob = new SimpleDateFormat("dd-MM-yyyy").parse(birthday);
                p.setDobPatient(parsedDob);
            } catch (ParseException pe) {
                outputStream.writeUTF("ERROR");
                outputStream.writeUTF("Invalid birthday format. Use dd-MM-yyyy (ej: 31-12-1990).");
                outputStream.flush();
                return;
            }

            p.setEmergencyContactPatient(Integer.parseInt(emergencyContact));


            String encryptedPass = hashPassword(password);
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

        private Sex parseSex(String sexStr) {
            if (sexStr == null) {
                throw new IllegalArgumentException("Sex value is null");
            }
            String s = sexStr.trim().toUpperCase();

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

        private void handleLogin() throws IOException {

            try {
                String username = inputStream.readUTF();
                String password = inputStream.readUTF();
                //boolean logged = userMan.verifyPassword(username, password);
                boolean logged = checkPassword(password, hash);
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
                    fragmentData = patientMan.getFracmentofRecoring(idDiagnosisFile, position);
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

                // Parsear DOB en formato dd-MM-yyyy y convertir a yyyy-MM-dd para la DB
                String dobForDb;
                try {
                    Date parsed = new SimpleDateFormat("dd-MM-yyyy").parse(birthday);
                    dobForDb = new SimpleDateFormat("yyyy-MM-dd").format(parsed);
                } catch (ParseException pe) {
                    outputStream.writeUTF("ERROR");
                    outputStream.writeUTF("Invalid birthday format. Use dd-MM-yyyy (ej: 31-12-1990).");
                    outputStream.flush();
                    return;
                }

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
                            psDoc.setString(5, dobForDb); // usamos yyyy-MM-dd en la BDD
                            psDoc.setString(6, email);
                            psDoc.setString(7, sex);
                            psDoc.setString(8, specialty);
                            psDoc.setString(9, licenseNumber);

                            System.out.println("Insertando doctor: userId=" + newUserId + " dni=" + dniClean + " email=" + email);

                            psDoc.executeUpdate();
                        }

                        c.commit();
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


        private void handleLogin() throws IOException {
            try {
                String username = inputStream.readUTF();
                String password = inputStream.readUTF();
                boolean logged = checkPassword(password, hash);

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

        public void SendDiagnosisFilesTODO(int idDoctor) {
            DataOutputStream outputStream = null;

            try {
                outputStream = new DataOutputStream(socket.getOutputStream());

                List<DiagnosisFile> diagnosisFilesTODO = doctorMan.listDiagnosisFilesTODO(idDoctor);

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

        public DiagnosisFile ReciveAndUpdateDiagnosisFile() {
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
                    fragmentData = doctorMan.getFracmentofRecoring(idDiagnosisFile, position);
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
