package myblog.handlers;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RequestParser {
    public static Map<String, String> parseFormData(String body) throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<>();
        String[] pairs = body.split("&");
        
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name());
                params.put(key, value);
            }
        }
        
        return params;
    }
} 