package myblog.handlers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import myblog.services.EmailService;

public class AuthenticationHandler {
    private static final String PYTHON_SCRIPT_PATH = "target/python/auth.py";
    
    public static class AuthResult {
        public final boolean success;
        public final String message;

        public AuthResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    private static AuthResult executePythonAuth(String command, String username, String password) {
        return executePythonAuth(command, username, password, null, null);
    }


    private static AuthResult executePythonAuth(String command, String username, String password, String email, String verificationToken) {
        try {
            System.out.println("Starting Python authentication process...");
            
            ProcessBuilder processBuilder;
            if (command.equals("pending_register")) {
                processBuilder = new ProcessBuilder(
                    "python",
                    PYTHON_SCRIPT_PATH,
                    command,
                    username,
                    password,
                    email,
                    verificationToken
                );
            } else if (command.equals("verify_register")) {
                processBuilder = new ProcessBuilder(
                    "python",
                    PYTHON_SCRIPT_PATH,
                    command,
                    username,
                    verificationToken  // For verify command, we use token instead of password
                );
            } else {
                processBuilder = new ProcessBuilder(
                    "python",
                    PYTHON_SCRIPT_PATH,
                    command,
                    username,
                    password
                );
            }
            
            // Set environment variables for the Python process
            Map<String, String> env = processBuilder.environment();
            env.put("PYTHONIOENCODING", "utf-8");
            
            processBuilder.redirectErrorStream(true);
            System.out.println("Executing " + command + " command: " + PYTHON_SCRIPT_PATH);
            
            Process process = processBuilder.start();
            
            // Read all output from the Python script
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                System.err.println("pyline: " + line);
                if (line.startsWith("SUCCESS: ")) {
                    return new AuthResult(true, line.substring(9));
                } else if (line.startsWith("ERROR: ")) {
                    return new AuthResult(false, line.substring(7));
                }
            }
            
            return new AuthResult(false, "No valid response from authentication service");
            
        } catch (Exception e) {
            System.err.println(command + " failed: " + e.getMessage());
            e.printStackTrace();
            return new AuthResult(false, "Authentication service error: " + e.getMessage());
        }
    }

    public static AuthResult authenticate(String username, String password) {

        return executePythonAuth("check", username, password);
 
    }

    public static AuthResult register(String username, String password, String email) {

        // Send verification email and get token
        String verificationToken = EmailService.sendVerificationEmail(email, username);
        if (verificationToken == null) {
            return new AuthResult(false, "Failed to send verification email");
        }

        // Create pending registration
        return executePythonAuth("pending_register", username, password, email, verificationToken);
    }

    public static AuthResult verifyEmail(String username, String token) {
        if (username == null || token == null) {
            return new AuthResult(false, "Invalid verification data");
        }
        
        return executePythonAuth("verify_register", username, null, null, token);
    }


} 