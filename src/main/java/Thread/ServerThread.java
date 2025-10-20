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


}
