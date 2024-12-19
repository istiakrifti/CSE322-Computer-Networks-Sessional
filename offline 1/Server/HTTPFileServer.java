package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HTTPFileServer {
    public static final int PORT = 5101;

    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = new ServerSocket(PORT);

        System.out.println("Server Started...");
        System.out.println("Listening for connection on PORT " + PORT + "...");

        while (true) {

            Socket clientSocket = serverSocket.accept();
            System.out.println("Connection received...");

            new ServerThread(clientSocket).start();
        }
    }
}
