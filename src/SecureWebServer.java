import java.io.*;
import java.net.*;
import java.util.logging.*;
import java.nio.file.*;

public class SecureWebServer {
    private static final Logger logger = Logger.getLogger(SecureWebServer.class.getName());
    private static final int PORT = 8080;

    public static void main(String[] args) {
        configureLogger();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Server started on port: " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                handleRequest(clientSocket);
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

            // Security: Sanitize path
            Path sanitizedPath = sanitizePath(path);
            if (sanitizedPath == null) {
                sendResponse(out, "HTTP/1.1 403 Forbidden\r\n\r\n");
                return;
            }

        } catch (IOException e) {
            logger.warning("Request handling failed: " + e.getMessage());
        }
    }

    private static Path sanitizePath(String requestPath) {
        try {
            Path resolvedPath = Paths.get("./www").resolve(requestPath.substring(1)).normalize();
            if (!resolvedPath.startsWith(Paths.get("./www").toAbsolutePath())) {
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