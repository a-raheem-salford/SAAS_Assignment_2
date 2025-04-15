// File: FormDataSaver.java

import java.io.*;
import java.nio.file.*;

public class FormDataSaver {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("No form data received.");
            return;
        }

        String data = args[0];

        try {
            Path dataDir = Paths.get("./www/data");
            Files.createDirectories(dataDir); // Ensure the directory exists

            Path file = dataDir.resolve("submissions.txt");

            Files.write(file, (data + System.lineSeparator()).getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            System.out.println("Form data saved successfully.");
        } catch (IOException e) {
            System.err.println("Failed to save form data: " + e.getMessage());
        }
    }
}
