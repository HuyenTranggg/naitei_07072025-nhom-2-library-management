package com.group2.library_management.service.impl;

import com.group2.library_management.exception.FileStorageException;
import com.group2.library_management.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import com.google.common.io.Files;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;
    private final String uploadDir;

    // Constructor to initialize the upload directory
    public FileStorageServiceImpl(@Value("${application.upload-dir}") String uploadDir) {
        this.uploadDir = uploadDir;
        this.fileStorageLocation = Paths.get(uploadDir, "images", "editions").toAbsolutePath().normalize();

        try {
            // Create the directory if it does not exist
            java.nio.file.Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("error.file.cannot_create_dir", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file, Integer editionId) {
        // Check if the file is valid
        if (file == null || file.isEmpty()) {
            return null; // No new file, do nothing
        }

        // Get the original file name for validation
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        String fileExtension = Files.getFileExtension(originalFilename);

        // Create a new file name
        String newFilename = "edition-" + editionId + "." + fileExtension;

        try {
            // Create the full physical path to save the file
            Path targetLocation = this.fileStorageLocation.resolve(newFilename);

            // Copy file into the target location
            try (InputStream inputStream = file.getInputStream()) {
                java.nio.file.Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            // Return the path to store in the database as required
            return "/uploads/images/editions/" + newFilename;

        } catch (IOException ex) {
            throw new FileStorageException("error.file.could_not_store", ex, originalFilename);
        }
    }

    // deleteFile
    @Override
    public void deleteFile(String filename) {
        // filePath is the relative path stored in the database
        if (filename == null || filename.isBlank()) {
            return;
        }

        try {
            // Create the absolute path to the file
            Path targetLocation = this.fileStorageLocation.resolve(filename);

            // Files.deleteIfExists is a safe way, it will not throw an exception if the file does not exist
            java.nio.file.Files.deleteIfExists(targetLocation);
        } catch (IOException ex) {
            System.err.println("RROR: Could not delete rollback file: " + filename);
            ex.printStackTrace();
        }
    }
}
