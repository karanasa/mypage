package myblog;

import myblog.handlers.WebServerHandler;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class BasicWebServer {
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server is running on port " + PORT);

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected from: " + clientSocket.getInetAddress());

                Thread handler = new Thread(new WebServerHandler(clientSocket));
                handler.start();
            } catch (IOException e) {
                System.out.println("Error handling client: " + e.getMessage());
            }
        }
    }
} 