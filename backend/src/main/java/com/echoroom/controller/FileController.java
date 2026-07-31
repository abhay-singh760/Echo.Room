package com.echoroom.controller;

import com.echoroom.exception.ApiExceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "webp", "pdf", "doc", "docx", "txt", "zip", "csv", "xlsx"
    );
    private static final long MAX_SIZE_BYTES = 15L * 1024 * 1024;

    @Value("${app.uploads.dir}")
    private String uploadsDir;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(Authentication auth, @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new BadRequestException("File is empty");
        if (file.getSize() > MAX_SIZE_BYTES) throw new BadRequestException("File exceeds 15MB limit");

        String originalName = Path.of(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename()).getFileName().toString();
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BadRequestException("File type ." + ext + " is not allowed");
        }

        Path dir = Paths.get(uploadsDir);
        Files.createDirectories(dir);

        String storedName = UUID.randomUUID() + "." + ext;
        Path target = dir.resolve(storedName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return ResponseEntity.ok(Map.of(
                "url", "/uploads/" + storedName,
                "name", originalName
        ));
    }
}
