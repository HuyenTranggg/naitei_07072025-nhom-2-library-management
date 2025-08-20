package com.group2.library_management.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    
    /**
     * Save book cover image and return the relative path to store in the database.
     * @param file The uploaded image file.
     * @param editionId The ID of the edition to create the file name.
     * @return The relative path to store in the database (e.g., /uploads/images/editions/edition-48.jpg).
     */
    String storeFile(MultipartFile file, Integer editionId);

    void deleteFile(String filePath);
    
}
