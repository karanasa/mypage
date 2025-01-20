package myblog.handlers;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class FileHandler {
    private static final String HOME_PATH = "src/main/java/myblog/";
    private static final String STATIC_PATH = HOME_PATH + "static";

    public static void sendFileResponse(ResponseWriter writer, String filePath, String contentType) {
        if (filePath.endsWith(".html")) {
            filePath = "/html" + filePath;
        }

        try (BufferedReader fileReader = new BufferedReader(
                new InputStreamReader(new FileInputStream(STATIC_PATH + filePath), StandardCharsets.UTF_8))) {
            
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = fileReader.readLine()) != null) {
                content.append(line).append("\n");
            }
            
            String contentStr = content.toString();
            writer.sendContent(200, "OK", contentType, contentStr);
            
        } catch (FileNotFoundException e) {
            writer.send404NotFound();
        } catch (IOException e) {
            writer.send500InternalError("An unexpected error occurred");
        }
    }
} 