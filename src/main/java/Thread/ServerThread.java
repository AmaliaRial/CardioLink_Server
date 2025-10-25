package Thread;

import jdbc.ConnectionManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerThread {

    private static final String DataBase_Address = "jdbc:sqlite:CardioLink.db";
    private static final ConnectionManager conman = new ConnectionManager();

    public static void main(String[] args) throws IOException, SQLException {

        try (Connection connection = DriverManager.getConnection(DataBase_Address)){
            System.out.println("Connection to database established.");
            conman.ensureSchema(connection);

            ServerSocket serverSocket = new ServerSocket(9000);
            try {
                while (true) {
                    Socket socket = serverSocket.accept();
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
        private ServerClientThread(Socket socket, Connection connection,ServerSocket serverSocket){
            this.socket = socket;
            this.connection = connection;
            this.serverSocket = serverSocket;
        }
        @Override
        public void run(){
            DataInputStream inputStream = null;
            try{
                inputStream = new DataInputStream(socket.getInputStream());
                String clientType = inputStream.readUTF();
                if(clientType.equalsIgnoreCase("Patient")){
                    System.out.println("Is a patient");
                    //new Thread(new ServerPatientThread(socket,connection,inputStream)).start();
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
        Socket socket;
        Connection connection;
        DataInputStream inputStream;
        DataOutputStream outputStream;

        private ServerPatientThread(Socket socket, Connection connection, DataInputStream inputStream, DataOutputStream outputStream) {
            this.socket = socket;
            this.connection = connection;
            this.inputStream = inputStream;
            this.outputStream = outputStream;
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
                            loggedUsername = handleLogin();
                            break;

                        case "START":
                            // client starting recording - we can ACK
                            outputStream.writeUTF("ACK");
                            outputStream.writeUTF("Ready to receive data");
                            outputStream.flush();
                            break;

                        case "DATA":
                            // Expect blockNumber, seq, emg, ecg
                            try {
                                int blockNumber = inputStream.readInt();
                                int seq = inputStream.readInt();
                                int eda = inputStream.readInt();
                                int ecg = inputStream.readInt();
                                EDA.add(eda);
                                ECG.add(ecg);
                                System.out.printf("Received - Block %d Seq %d EMG %d ECG %d\n",
                                        blockNumber, seq, eda, ecg);
                            } catch (IOException ioe) {
                                System.err.println("Failed reading DATA ints: " + ioe.getMessage());
                            }
                            break;

                        case "END":
                            // Recording finished — persist arrays
                            outputStream.writeUTF("ACK");
                            outputStream.writeUTF("Recording finished. Saving data...");
                            outputStream.flush();

                            // Save recording (placeholder)
                            try {
                                saveRecordingToDBorFile(loggedUsername, ECG, EDA);
                                outputStream.writeUTF("ACK");
                                outputStream.writeUTF("Data saved successfully");
                                outputStream.flush();
                            } catch (Exception ex) {
                                outputStream.writeUTF("ERROR");
                                outputStream.writeUTF("Failed saving recording: " + ex.getMessage());
                                outputStream.flush();
                            }

                            // Clear arrays to prepare for next recording
                            ECG.clear();
                            EDA.clear();
                            break;

                        case "SYMPTOMS":
                            // get number of symptoms and the IDs and timestamp
                            try {
                                int count = inputStream.readInt();
                                ArrayList<Integer> symptomIds = new ArrayList<>();
                                for (int i = 0; i < count; ++i) {
                                    int id = inputStream.readInt();
                                    symptomIds.add(id);
                                }
                                String timestamp = inputStream.readUTF(); // client timestamp
                                // Save symptoms to DB (placeholder)
                                saveSymptomsToDB(loggedUsername, symptomIds, timestamp);

                                outputStream.writeUTF("ACK");
                                outputStream.writeUTF("Symptoms saved");
                                outputStream.flush();
                            } catch (IOException ioe) {
                                outputStream.writeUTF("ERROR");
                                outputStream.writeUTF("Failed to read symptoms: " + ioe.getMessage());
                                outputStream.flush();
                            }
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
            } catch (IOException ex) {
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                System.out.println("Client disconnected");
                releaseResourcesPatient(inputStream, socket,outputStream);
            }
        }
    }

}
