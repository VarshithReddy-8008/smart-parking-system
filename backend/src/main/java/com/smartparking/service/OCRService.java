package com.smartparking.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

/**
 * OCRService — bridges Java backend to the Python anpr_engine.py via ProcessBuilder.
 * Saves the webcam image temporarily, executes the Python script, and parses the result.
 */
@Service
@Slf4j
public class OCRService {

    @Value("${anpr.python.path:python}")
    private String pythonPath;

    @Value("${anpr.engine.path:anpr/anpr_engine.py}")
    private String engineScriptPath;

    @Value("${anpr.image.storage:anpr/images}")
    private String imageStoragePath;

    private final ObjectMapper mapper = new ObjectMapper();

    public static class OcrResult {
        public boolean success;
        public String vehicleNumber;
        public double confidence;
        public String message;
        public String imagePath;
    }

    /**
     * Process a base64-encoded JPEG image from the webcam.
     * Returns OCR detection result.
     */
    public OcrResult processBase64Image(String base64Image) {
        OcrResult result = new OcrResult();
        String imagePath = null;

        try {
            // 1. Decode base64 to file
            imagePath = saveBase64Image(base64Image);
            result.imagePath = imagePath;

            // 2. Build Python command
            String projectRoot = new File(".").getAbsolutePath().replace("\\.", "");
            String scriptPath = Paths.get(projectRoot, engineScriptPath).toString();
            String absoluteImagePath = new File(imagePath).getAbsolutePath();

            log.info("Running ANPR engine: {} {} {}", pythonPath, scriptPath, absoluteImagePath);

            ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath, absoluteImagePath);
            pb.redirectErrorStream(false);
            Process process = pb.start();

            // 3. Read stdout (JSON output)
            String output = new String(process.getInputStream().readAllBytes()).trim();
            String errorOutput = new String(process.getErrorStream().readAllBytes()).trim();

            int exitCode = process.waitFor();
            log.info("ANPR engine output: {}", output);
            if (!errorOutput.isEmpty()) {
                log.warn("ANPR engine stderr: {}", errorOutput);
            }

            // 4. Parse JSON result
            if (output.isEmpty()) {
                result.success = false;
                result.message = "OCR engine returned no output. Check Python installation.";
                return result;
            }

            JsonNode node = mapper.readTree(output);
            result.success = node.path("success").asBoolean(false);
            result.vehicleNumber = node.path("vehicleNumber").asText("");
            result.confidence = node.path("confidence").asDouble(0.0);
            result.message = node.path("message").asText("Unknown error");

        } catch (Exception e) {
            log.error("OCR processing failed: {}", e.getMessage());
            result.success = false;
            result.message = "OCR engine error: " + e.getMessage();
            result.vehicleNumber = "";
            result.confidence = 0.0;
        }

        return result;
    }

    /**
     * Decodes a base64 image string (optionally with data URI prefix) and saves it to disk.
     * Returns the relative path to the saved image.
     */
    private String saveBase64Image(String base64Image) throws Exception {
        // Strip the data URI prefix if present (e.g., "data:image/jpeg;base64,...")
        String pureBase64 = base64Image;
        if (base64Image.contains(",")) {
            pureBase64 = base64Image.substring(base64Image.indexOf(",") + 1);
        }

        byte[] imageBytes = Base64.getDecoder().decode(pureBase64);

        // Create storage directory if not exists
        String projectRoot = new File(".").getAbsolutePath().replace("\\.", "");
        Path storageDir = Paths.get(projectRoot, imageStoragePath);
        Files.createDirectories(storageDir);

        String filename = "scan_" + UUID.randomUUID() + ".jpg";
        Path filePath = storageDir.resolve(filename);

        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(imageBytes);
        }

        return imageStoragePath + "/" + filename;
    }
}
