package com.group2.library_management.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.group2.library_management.dto.response.EditionDetailResponse;
import com.group2.library_management.dto.response.EditionListResponse;
import com.group2.library_management.dto.response.EditionResponse;
import com.group2.library_management.entity.Edition;
import com.group2.library_management.entity.Publisher;
import com.group2.library_management.exception.ResourceNotFoundException;
import com.group2.library_management.repository.EditionRepository;
import com.group2.library_management.repository.PublisherRepository;
import com.group2.library_management.service.*;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.group2.library_management.dto.mapper.EditionMapper;
import com.group2.library_management.dto.request.UpdateEditionRequest;

@Service
@RequiredArgsConstructor
public class EditionServiceImpl implements EditionService {
    private final EditionRepository editionRepository;
    private final EditionMapper editionMapper;

    private final PublisherRepository publisherRepository;
    private final FileStorageService fileStorageService; // Service handle file storage
    
    private static final long MAX_FILE_SIZE_MB = 5;
    private static final long MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024;
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("image/jpeg", "image/png", "image/gif", "image/jpg", "image/webp");

    private final MessageSource messageSource;

    @Override
    public Page<EditionListResponse> getAllEditions(Pageable pageable) {
        Page<Edition> editionsPage = editionRepository.findAll(pageable);
        return editionsPage.map(editionMapper::toDto);
    }

    @Override
    public List<EditionResponse> getEditionsByBookId(Integer bookId) {
        return editionRepository.findByBookIdOrderByPublicationDateDesc(bookId)
                .stream()
                .map(EditionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public EditionDetailResponse getEditionDetailById(Integer id) {
        return editionRepository.findById(id)
                .map(editionMapper::toDetailDto)
                .orElse(null);
    }

    @Override
    public Edition getById(Integer id) {
        return editionRepository.findById(id)
            .orElseThrow(() -> {
                String message = messageSource.getMessage(
                    "error.edition.not_found",
                    new Object[]{id},
                    LocaleContextHolder.getLocale()
                );
                return new ResourceNotFoundException(message);
            });
    }

    @Override
    public void updateEdition(Integer id, UpdateEditionRequest request, MultipartFile coverImageFile) {
        // Get edition
        Edition edition = getById(id);

        String newCoverImageDbPath = null;

        // Handle cover image file
        if (coverImageFile != null && !coverImageFile.isEmpty()) {
            validateCoverImage(coverImageFile);

            newCoverImageDbPath = fileStorageService.storeFile(coverImageFile, id);
            edition.setCoverImageUrl(newCoverImageDbPath);
        }

        // Get publisher
        Publisher publisher = publisherRepository.findById(request.getPublisherId())
            .orElseThrow(() -> {
                String message = messageSource.getMessage(
                    "error.publisher.not_found",
                    new Object[]{request.getPublisherId()},
                    LocaleContextHolder.getLocale()
                );
                return new ResourceNotFoundException(message);
            });

        // Use editionMapper to update edition fields
        editionMapper.updateFromRequest(request, edition);
        edition.setPublisher(publisher);

        // Save changes into database
        try {
            editionRepository.save(edition);
        } catch (DataIntegrityViolationException e) {
            if (newCoverImageDbPath != null) {
                String filenameToDelete = newCoverImageDbPath.substring(newCoverImageDbPath.lastIndexOf("/") + 1);
                fileStorageService.deleteFile(filenameToDelete);
            }
            String message = messageSource.getMessage(
                "error.edition.isbn.exists",
                null,
                LocaleContextHolder.getLocale()
            );
            throw new IllegalArgumentException(message);
        }
    }

    private void validateCoverImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            String message = messageSource.getMessage("error.file.is_empty", null, LocaleContextHolder.getLocale());
            throw new IllegalArgumentException(message);
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            String message = messageSource.getMessage("error.file.invalid.size", new Object[]{MAX_FILE_SIZE_MB}, LocaleContextHolder.getLocale());
            throw new IllegalArgumentException(message);
        }

        if (file.getContentType() == null || !ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            String message = messageSource.getMessage("error.file.invalid.type", null, LocaleContextHolder.getLocale());
            throw new IllegalArgumentException(message);
        }
    }
}
