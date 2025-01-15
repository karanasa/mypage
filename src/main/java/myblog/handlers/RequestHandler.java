package myblog.handlers;

import java.io.PrintWriter;
import java.io.IOException;

public interface RequestHandler {
    void handle(PrintWriter out, String body) throws IOException;
} 