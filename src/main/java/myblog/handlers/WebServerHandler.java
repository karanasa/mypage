package myblog.handlers;

import myblog.router.Router;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.Map;

public class WebServerHandler implements Runnable {
    private final Socket clientSocket;
    private final Router router;

    public WebServerHandler(Socket socket) {
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
            handleRequest(in, out);
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Error handling client request: " + e.getMessage());
        }
    }

    private void handleRequest(BufferedReader in, PrintWriter out) throws IOException {
        String requestLine = in.readLine();
        
        // Early return for null requests
        if (requestLine == null || requestLine.trim().isEmpty()) {
            System.out.println("Received empty request - closing connection");
            return;
        }

        String[] requestParts = requestLine.split(" ");
        // Check for malformed requests
        if (requestParts.length < 2) {
            System.out.println("Malformed request: " + requestLine);
            return;
        }

        String method = requestParts[0];
        String path = requestParts[1];

        // Filter out common noise requests
        if (path.equals("/favicon.ico") || 
            path.equals("/apple-touch-icon.png") || 
            path.equals("/apple-touch-icon-precomposed.png")) {
            ResponseWriter writer = new ResponseWriter(out);
            writer.send404NotFound();
            return;
        }

        // Only log actual web requests
        System.out.println("\n====== New Request ======");
        System.out.println("Method: " + method);
        System.out.println("Path: " + path);
        System.out.println("Remote Address: " + clientSocket.getRemoteSocketAddress());

        int contentLength = readHeaders(in);
        String body = readBody(in, method, contentLength);
        
        if (contentLength > 0) {
            System.out.println("Request Body: " + body);
        }
        
        System.out.println("========================\n");
        
        RequestHandler handler = router.getHandler(method, path);
        if (handler != null) {
            handler.handle(out, body);
        } else {
            ResponseWriter writer = new ResponseWriter(out);
            writer.send404NotFound();
        }
    }

    private Router initializeRouter() {
        Router router = new Router();
        
        // Add verification endpoint
        router.addRoute("GET", "/verify", this::handleVerify);
        
        // Generic handler for GET requests to serve static files
        router.addRoute("GET", "*", this::handleStaticFiles);
        
        // Add authentication handlers
        router.addRoute("POST", "/login", this::handleLogin);
        router.addRoute("POST", "/register", this::handleRegister);
        
        return router;
    }

    private void handleVerify(PrintWriter out, String body) {
        ResponseWriter writer = new ResponseWriter(out);
        String token = router.getQueryParam("token");
        String username = router.getQueryParam("username");
        
        if (token == null || username == null) {
            writer.send400BadRequest("Invalid verification link");
            return;
        }

        AuthenticationHandler.AuthResult result = AuthenticationHandler.verifyEmail(username, token);
        if (result.success) {
            try {
                writer.sendRedirect("/login?message=" + 
                    URLEncoder.encode("Email verified successfully! You can now login.", 
                    StandardCharsets.UTF_8.toString()));
            } catch (UnsupportedEncodingException e) {
                writer.send500InternalError("An unexpected error occurred");
            }
        } else {
            writer.send400BadRequest("Invalid or expired verification link");
        }
    }

    private void handleStaticFiles(PrintWriter out, String body) {
        String path = router.getCurrentPath();
        
        // Default to login.html for root path
        if (path.equals("/")) {
            path = "/login.html";
        } else {
            // If path has no extension, assume it's an HTML file
            if (!path.contains(".")) {
                path = path + ".html";
            }
        }
        
        // Determine content type based on file extension
        String contentType = "text/plain";  // default
        if (path.endsWith(".html")) {
            contentType = "text/html";
        } else if (path.endsWith(".css")) {
            contentType = "text/css";
        } else if (path.endsWith(".js")) {
            contentType = "application/javascript";
        }
        
        FileHandler.sendFileResponse(new ResponseWriter(out),path,contentType);
    }

    private void handleLogin(PrintWriter out, String body) {
        ResponseWriter writer = new ResponseWriter(out);
        try {
            Map<String, String> params = RequestParser.parseFormData(body);
            String username = params.getOrDefault("username", "");
            String password = params.getOrDefault("password", "");

            AuthenticationHandler.AuthResult result = AuthenticationHandler.authenticate(username, password);
            if (result.success) {
                writer.sendJsonWithRedirect(true, "/mypage", "Login successful");
            } else {
                writer.sendJson(200, "OK", false, result.message);
            }
        } catch (UnsupportedEncodingException e) {
            writer.sendJson(400, "Bad Request", false, "Invalid request encoding");
        }
    }

    private void handleRegister(PrintWriter out, String body) {
        ResponseWriter writer = new ResponseWriter(out);
        try {
            Map<String, String> params = RequestParser.parseFormData(body);
            String username = params.getOrDefault("username", "");
            String password = params.getOrDefault("password", "");
            String email = params.getOrDefault("email", "");
            String confirmPassword = params.getOrDefault("confirm-password", "");

            if (!password.equals(confirmPassword)) {
                writer.sendJson(200, "OK", false, "Passwords do not match");
                return;
            }

            AuthenticationHandler.AuthResult result = AuthenticationHandler.register(username, password, email);
            if (result.success) {
                writer.sendJsonWithRedirect(true, "/register-confirmation", "こんにちは.");
            } else {
                writer.sendJson(200, "OK", false, result.message);
            }
        } catch (UnsupportedEncodingException e) {
            writer.sendJson(400, "Bad Request", false, "Invalid request encoding");
        }
    }

    private int readHeaders(BufferedReader in) throws IOException {
        String line;
        int contentLength = 0;
        
        System.out.println("--- Headers ---");
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            System.out.println(line);
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.substring(15).trim());
            }
        }
        System.out.println("-------------");
        return contentLength;
    }

    private String readBody(BufferedReader in, String method, int contentLength) throws IOException {
        // Only read body for POST requests with content
        if (!method.equals("POST") || contentLength == 0) {
            return "";
        }

        char[] body = new char[contentLength];
        in.read(body, 0, contentLength);
        return new String(body);
    }
} 