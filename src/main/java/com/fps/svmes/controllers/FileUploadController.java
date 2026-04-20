package com.fps.svmes.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/file-upload")
@Tag(name = "File Upload API", description = "API for file uploads used by form designer")
public class FileUploadController {

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    @Value("${file.upload.base-url:}")
    private String baseUrl;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
            log.info("Upload directory created at: {}", uploadPath);
        } catch (IOException e) {
            log.error("Could not create upload directory: {}", uploadPath, e);
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload a file", description = "Upload a file and return the file URL")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        try {
            // Generate unique filename to prevent collisions
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            // Save the file
            Path targetLocation = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("File uploaded successfully: {} -> {}", originalFilename, uniqueFilename);

            // Build the response URL
            String fileUrl;
            if (baseUrl != null && !baseUrl.isEmpty()) {
                fileUrl = baseUrl + "/file-upload/files/" + uniqueFilename;
            } else {
                fileUrl = "/file-upload/files/" + uniqueFilename;
            }

            // Return response in the format expected by Element Plus el-upload
            Map<String, Object> response = new HashMap<>();
            response.put("name", originalFilename);
            response.put("url", fileUrl);
            response.put("filename", uniqueFilename);

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }

    @GetMapping("/files/{filename:.+}")
    @Operation(summary = "Download/view a file", description = "Retrieve an uploaded file by filename")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        try {
            Path filePath = uploadPath.resolve(filename).normalize();

            // Security check: ensure the file is within the upload directory
            if (!filePath.startsWith(uploadPath)) {
                log.warn("Attempted path traversal attack: {}", filename);
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.warn("File not found or not readable: {}", filename);
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType;
            try {
                contentType = Files.probeContentType(filePath);
            } catch (IOException e) {
                contentType = "application/octet-stream";
            }
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            log.error("Invalid file path: {}", filename, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/files/{filename:.+}")
    @Operation(summary = "Delete a file", description = "Delete an uploaded file by filename")
    public ResponseEntity<?> deleteFile(@PathVariable String filename) {
        try {
            Path filePath = uploadPath.resolve(filename).normalize();

            // Security check: ensure the file is within the upload directory
            if (!filePath.startsWith(uploadPath)) {
                log.warn("Attempted path traversal attack: {}", filename);
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid filename"));
            }

            if (Files.deleteIfExists(filePath)) {
                log.info("File deleted: {}", filename);
                return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filename, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to delete file: " + e.getMessage()));
        }
    }
}
