package myblog;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import myblog.router.Router;
import myblog.handlers.RequestHandler;

public class BasicWebServer {
    public static void main(String[] args) throws IOException {
        // Create server socket on port 8080
        ServerSocket serverSocket = new ServerSocket(80);
        System.out.println("Server is running on port 80");

        while (true) {
            try {
                // Wait for client connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected from: " + clientSocket.getInetAddress());

                // Handle client request in a new thread
                Thread handler = new Thread(new ClientHandler(clientSocket));
                handler.start();
            } catch (IOException e) {
                System.out.println("Error handling client: " + e.getMessage());
            }
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final Router router;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            this.router = initializeRouter();
        }
        
        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8),
                    true)
            ) {
                // Read HTTP request
                String requestLine = in.readLine();
                if (requestLine == null) return;

                // Parse request
                String[] requestParts = requestLine.split(" ");
                String method = requestParts[0];
                String path = requestParts[1];

                System.out.println("requestLine: " + requestLine);

                // Read headers (only once)
                String header;
                int contentLength = 0;
                int i=0;
                while (!(header = in.readLine()).isEmpty()) {
                    System.out.println("header "+ ++i +": "+ header);
                    if (header.startsWith("Content-Length: ")) {
                        contentLength = Integer.parseInt(header.split(": ")[1]);
                    }
                }

                // Replace the if-else chain with router
                RequestHandler handler = router.getHandler(method, path);
                if (handler != null) {
                    // Read body for POST requests
                    String body = "";
                    if (method.equals("POST")) {
                        char[] bodyChars = new char[contentLength];
                        in.read(bodyChars, 0, contentLength);
                        body = new String(bodyChars);
                    }
                    
                    handler.handle(out, body);
                } else {
                    handle404(out);
                }

                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Error handling client request: " + e.getMessage());
            }
        }


        private Router initializeRouter() {
            Router router = new Router();
            
            // GET routes
            router.addRoute("GET", "/", (out, body) -> handleRoot(out));
            router.addRoute("GET", "/login", (out, body) -> handleRoot(out));
            router.addRoute("GET", "/register", (out, body) -> handleRegister(out));
            router.addRoute("GET", "/styles.css", (out, body) -> handleCSS(out));
            
            // POST routes
            router.addRoute("POST", "/login", (out, body) -> handleLogin(out, body));
            
            return router;
        }

        private void handleRoot(PrintWriter out) throws IOException {
            sendFileResponse(out, "login.html", "text/html");
        }

        private void handleRegister(PrintWriter out) throws IOException {
            sendFileResponse(out, "register.html", "text/html");
        }

        private void handleLogin(PrintWriter out, String body) throws IOException {
            System.out.println("Login attempt with data: " + body);

            // Parse the form data
            String username = "";
            String password = "";
            String[] pairs = body.split("&");
            for (int i=0;i<pairs.length;i++) {
                String[] keyValue = pairs[i].split("=");
                if (i==0) {
                    username = keyValue[1];
                } else if (i==1) {
                    password = keyValue[1];
                }
            }
            if (username.equals("kouhin") && password.equals("1024")) {
                sendFileResponse(out, "index.html", "text/html");
            } else {

                out.println("HTTP/1.1 401 Unauthorized");
                out.println("Content-Type: text/html; charset=UTF-8");
                out.println();
                out.println("<html><body><h1>Login Failed!</h1><p>Invalid username or password</p></body></html>");
            }

        }

        private void handle404(PrintWriter out) {
            out.println("HTTP/1.1 404 Not Found");
            out.println("Content-Type: text/html");
            out.println();
            out.println("<html><body><h1>404 Not Found</h1></body></html>");
        }

        private void handleCSS(PrintWriter out) throws IOException {
            sendFileResponse(out, "styles.css", "text/css");
        }


        private void sendFileResponse(PrintWriter out, String filePath, String contentType) throws IOException {
            final String  homepath = "src/main/java/myblog/";
            try (BufferedReader fileReader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(homepath + filePath), StandardCharsets.UTF_8))) {
                
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = fileReader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                
                byte[] contentBytes = content.toString().getBytes(StandardCharsets.UTF_8);
                
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: " + contentType + "; charset=UTF-8");
                out.println("Content-Length: " + contentBytes.length);
                out.println();
                
                out.print(content.toString());
                out.flush();
            } catch (FileNotFoundException e) {
                System.out.println(filePath + " not found: " + e.getMessage());
                handle404(out);
            }
        }
    }
} 