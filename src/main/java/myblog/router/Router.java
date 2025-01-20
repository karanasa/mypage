package myblog.router;

import myblog.handlers.RequestHandler;
import java.util.HashMap;
import java.util.Map;

public class Router {
    private final Map<String, Map<String, RequestHandler>> routes = new HashMap<>();
    private String currentPath;  // Store the current path
    private Map<String, String> queryParams = new HashMap<>();

    public void addRoute(String method, String path, RequestHandler handler) {
        routes.computeIfAbsent(method, k -> new HashMap<>()).put(path, handler);
    }

    public RequestHandler getHandler(String method, String path) {
        // Parse query parameters
        queryParams.clear();
        String[] parts = path.split("\\?", 2);
        String basePath = parts[0];
        
        if (parts.length > 1) {
            String[] params = parts[1].split("&");
            for (String param : params) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length == 2) {
                    queryParams.put(keyValue[0], keyValue[1]);
                }
            }
        }
        
        currentPath = basePath;
        Map<String, RequestHandler> methodRoutes = routes.get(method);
        
        if (methodRoutes != null) {
            RequestHandler handler = methodRoutes.get(basePath);
            if (handler != null) {
                return handler;
            }
            
            if (method.equals("GET")) {
                return methodRoutes.get("*");
            }
        }
        return null;
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public String getQueryParam(String name) {
        return queryParams.get(name);
    }
} 