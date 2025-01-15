package myblog.router;

import myblog.handlers.RequestHandler;
import java.util.HashMap;
import java.util.Map;

public class Router {
    private final Map<String, Map<String, RequestHandler>> routes = new HashMap<>();

    public void addRoute(String method, String path, RequestHandler handler) {
        routes.computeIfAbsent(method, k -> new HashMap<>()).put(path, handler);
    }

    public RequestHandler getHandler(String method, String path) {
        Map<String, RequestHandler> methodRoutes = routes.get(method);
        if (methodRoutes != null) {
            return methodRoutes.get(path);
        }
        return null;
    }
} 