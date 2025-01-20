package myblog.handlers;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;


public class ResponseWriter {
    private final PrintWriter out;

    public ResponseWriter(PrintWriter out) {
        this.out = out;
    }

    public void sendJson(int statusCode, String statusText, boolean success, String message) {
        String content = String.format("{\"success\": %b, \"message\": \"%s\"}", 
            success, message.replace("\"", "\\\""));
        sendContent(statusCode, statusText, "application/json", content);
    }

    public void sendJsonWithRedirect(boolean success, String redirect, String message) {
        String content = String.format("{\"success\": %b, \"redirect\": \"%s\", \"message\": \"%s\"}", 
            success, redirect, message.replace("\"", "\\\""));
        sendContent(200, "OK", "application/json", content);
    }

    public void sendRedirect(String location) {
        out.println("HTTP/1.1 303 See Other");
        out.println("Location: " + location);
        out.println();
        out.flush();
    }

    public void sendContent(int statusCode, String statusText, String contentType, String content) {
        out.println(String.format("HTTP/1.1 %d %s", statusCode, statusText));
        out.println("Content-Type: " + contentType + "; charset=UTF-8");
        out.println("Content-Length: " + content.getBytes(StandardCharsets.UTF_8).length);
        out.println();
        out.println(content);
        out.flush();
    }

    // Common error response methods
    public void send404NotFound() {
        sendContent(404, "Not Found", "text/html", 
            "<html><body><h1>404 Not Found</h1><p>The requested resource was not found on this server.</p></body></html>");
    }

    public void send400BadRequest(String message) {
        sendContent(400, "Bad Request", "text/html",
            String.format("<html><body><h1>Error</h1><p>%s</p></body></html>", message));
    }

    public void send500InternalError(String message) {
        sendContent(500, "Internal Server Error", "text/html",
            String.format("<html><body><h1>500 Internal Server Error</h1><p>%s</p></body></html>", message));
    }


} 