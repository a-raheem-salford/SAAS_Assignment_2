import java.io.*;
import java.net.*;
import java.util.logging.*;
import java.nio.file.*;
import java.util.concurrent.*;

public class SecureWebServer {
    private static final Logger logger = Logger.getLogger(SecureWebServer.class.getName());
    private static final int PORT = 8080;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        configureLogger();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Server started on port: " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(() -> handleRequest(clientSocket)); // Multi-threading
            }
        } catch (IOException e) {
            logger.severe("Server error: " + e.getMessage());
        }
    }

    private static void configureLogger() {
        try {
            FileHandler fileHandler = new FileHandler("server.log");
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            logger.severe("Logger setup failed: " + e.getMessage());
        }
    }

    private static void handleRequest(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                OutputStream out = clientSocket.getOutputStream()) {

            String requestLine = in.readLine();
            if (requestLine == null)
                return;

            String[] parts = requestLine.split(" ");
            String method = parts[0];
            String path = parts[1];

            // Handle root path as index.html
            if (path.equals("/")) {
                path = "/index.html";
            }

            // Security: Sanitize path
            Path sanitizedPath = sanitizePath(path);
            if (sanitizedPath == null) {
                sendResponse(out, "HTTP/1.1 403 Forbidden\r\n\r\nAccess Denied.");
                return;
            }

            // Only support GET method
            if (!method.equalsIgnoreCase("GET")) {
                sendResponse(out, "HTTP/1.1 405 Method Not Allowed\r\n\r\nMethod Not Allowed.");
                return;
            }

            // Serve file if it exists
            if (Files.exists(sanitizedPath) && !Files.isDirectory(sanitizedPath)) {
                byte[] fileBytes = Files.readAllBytes(sanitizedPath);
                String contentType = Files.probeContentType(sanitizedPath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                String responseHeaders = "HTTP/1.1 200 OK\r\nContent-Type: " + contentType + "\r\n\r\n";
                out.write(responseHeaders.getBytes());
                out.write(fileBytes);
            } else {
                sendResponse(out, "HTTP/1.1 404 Not Found\r\n\r\nFile Not Found.");
            }

        } catch (IOException e) {
            logger.warning("Request handling failed: " + e.getMessage());
        }
    }

    private static Path sanitizePath(String requestPath) {
        try {
            Path root = Paths.get("www").toAbsolutePath();
            Path resolvedPath = root.resolve(requestPath.substring(1)).normalize();
    
            // Make sure the resolved path is still inside www
            if (!resolvedPath.startsWith(root)) {
                return null; // Block directory traversal
            }
            return resolvedPath;
        } catch (InvalidPathException e) {
            return null;
        }
    }

    private static void sendResponse(OutputStream out, String response) {
        try {
            out.write(response.getBytes());
        } catch (IOException e) {
            logger.warning("Failed to send response: " + e.getMessage());
        }
    }
}