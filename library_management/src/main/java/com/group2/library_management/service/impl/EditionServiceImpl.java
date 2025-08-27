package com.group2.library_management.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.group2.library_management.dto.response.ClientEditionDetailResponse;
import com.group2.library_management.dto.response.EditionDetailResponse;
import com.group2.library_management.dto.response.EditionListResponse;
import com.group2.library_management.dto.response.EditionResponse;
import com.group2.library_management.entity.Book;
import com.group2.library_management.entity.BookInstance;
import com.group2.library_management.entity.Edition;
import com.group2.library_management.entity.enums.DeletionStatus;
import com.group2.library_management.exception.DuplicateIsbnCreateEditionException;
import com.group2.library_management.exception.OperationFailedException;
import org.springframework.web.multipart.MultipartFile;

import com.group2.library_management.dto.response.EditionUpdateResponse;
import com.group2.library_management.entity.Publisher;
import com.group2.library_management.exception.ResourceNotFoundException;
import com.group2.library_management.repository.BookInstanceRepository;
import com.group2.library_management.repository.BookRepository;
import com.group2.library_management.repository.EditionRepository;
import com.group2.library_management.repository.PublisherRepository;
import com.group2.library_management.repository.specification.EditionSpecification;
import com.group2.library_management.service.*;
import com.group2.library_management.entity.enums.BookStatus;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import com.group2.library_management.repository.PublisherRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.group2.library_management.common.constants.PaginationConstants;
import com.group2.library_management.dto.mapper.EditionMapper;
import com.group2.library_management.dto.request.UpdateEditionRequest;
import com.group2.library_management.dto.request.EditionQueryParameters;
import com.group2.library_management.dto.request.CreateEditionRequest;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EditionServiceImpl implements EditionService {
    private final EditionRepository editionRepository;
    private final BookRepository bookRepository;
    private final PublisherRepository publisherRepository;
    private final BookInstanceRepository bookInstanceRepository;

    private final MessageSource messageSource;

    private final EditionMapper editionMapper;

    private final FileStorageService fileStorageService; // Service handle file storage

    private static final long MAX_FILE_SIZE_MB = 5;
    private static final long MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024;
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("image/jpeg", "image/png", "image/gif", "image/jpg", "image/webp");
    
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "title", "publicationDate", "isbn");
    
    @Override
    public Page<EditionListResponse> getAllEditions(EditionQueryParameters params) {
        int page = Optional.ofNullable(params.page()).orElse(PaginationConstants.DEFAULT_PAGE_NUMBER);
        int size = Optional.ofNullable(params.size()).orElse(PaginationConstants.DEFAULT_PAGE_SIZE);
        String[] sortParams = Optional.ofNullable(params.sort()).orElse(PaginationConstants.DEFAULT_SORT);
        
        // Xử lý tham số sắp xếp
        String sortField = sortParams[0];
        if (!StringUtils.hasText(sortField) || !ALLOWED_SORT_FIELDS.contains(sortField)) { 
            sortField = PaginationConstants.DEFAULT_SORT[0]; 
        }
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") 
                                   ? Sort.Direction.ASC 
                                   : Sort.Direction.DESC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(new Sort.Order(direction, sortField)));

        Specification<Edition> spec = Specification.unrestricted(); 

        if (StringUtils.hasText(params.keyword())) {
            spec = spec.and(EditionSpecification.searchEditionByKeyword(params.keyword()));
        }

        if (!CollectionUtils.isEmpty(params.publicationYears())) {
            spec = spec.and(EditionSpecification.hasPublicationYears(params.publicationYears()));
        }
        
        if (!CollectionUtils.isEmpty(params.publisherIds())) {
            spec = spec.and(EditionSpecification.hasPublishers(params.publisherIds()));
        }

        if (!CollectionUtils.isEmpty(params.languages())) {
            spec = spec.and(EditionSpecification.hasLanguages(params.languages()));
        }

        Page<Edition> editionPage = editionRepository.findAll(spec, pageable);

        return editionPage.map(editionMapper::toDto);
    }

    @Override
    public List<EditionResponse> getEditionsByBookId(Integer bookId) {
        // Get editions by book ID
        List<Edition> editions = editionRepository.findByBookIdOrderByPublicationDateDesc(bookId);

        // Use stream to process
        return editions.stream()
                .map(edition -> {
                    // Determine deletion status
                    DeletionStatus status = determineDeletionStatus(edition.getId());
                    // Call editionMapper.toResponseDto
                    return editionMapper.toResponseDto(edition, status);
                })
                .collect(Collectors.toList());
    }

    // Delete edition by ID
    @Override
    @Transactional
    public void deleteEdition(Integer id) {

        if (!editionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Edition not found with id: " + id);
        }

        DeletionStatus status = determineDeletionStatus(id);

        switch (status) {
            case CAN_HARD_DELETE:
                // Hard delete form database
                editionRepository.hardDeleteById(id);
                break;
            case CAN_SOFT_DELETE:
                // Soft delete
                editionRepository.deleteById(id);
                break;
            case CANNOT_DELETE:
                throw new OperationFailedException("admin.editions.message.cannot_delete");
        }
    }

    // Method to determine deletion status
    private DeletionStatus determineDeletionStatus(Integer editionId) {
        boolean anyInstanceExists = bookInstanceRepository.existsByEditionId(editionId);
        
        if (!anyInstanceExists) {
            // 0 copies exist -> can hard delete
            return DeletionStatus.CAN_HARD_DELETE;
        }

        boolean activeInstanceExists = bookInstanceRepository.existsByEditionIdAndDeleteAtIsNull(editionId);
        
        if (!activeInstanceExists) {
            // Have copies but all are soft deleted -> soft delete
            return DeletionStatus.CAN_SOFT_DELETE;
        }
        return DeletionStatus.CANNOT_DELETE;
    }

    @Override
    public EditionDetailResponse getEditionDetailById(Integer id) {
        return editionRepository.findById(id)
                .map(editionMapper::toDetailDto)
                .orElse(null);
    }

    @Override
    public Page<EditionListResponse> searchEditionAndBookInstance(String keyword, String filterBy, BookStatus bookStatus, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        Specification<Edition> spec = EditionSpecification.distinct();

        if("edition".equals(filterBy)){
            spec = spec.and(EditionSpecification.searchEditionByKeyword(keyword));
        }
        else {
            spec = spec.and(EditionSpecification.searchByBookInstanceFields(keyword, bookStatus, fromDate, toDate));
        }

        Page<Edition> editionsPage = editionRepository.findAll(spec, pageable);

        return editionsPage.map(editionMapper::toDto);
    }

    @Override
    public EditionUpdateResponse findEditionForUpdate(Integer id) {
        Edition edition = editionRepository.findById(id)
            .orElseThrow(() -> {
                String message = messageSource.getMessage(
                    "error.edition.not_found", new Object[]{id}, LocaleContextHolder.getLocale()
                );
                return new ResourceNotFoundException(message);
            });
        return editionMapper.toUpdateResponse(edition);
    }

    @Override
    @Transactional
    public void updateEdition(Integer id, UpdateEditionRequest request, MultipartFile coverImageFile) {

        Edition edition = editionRepository.findById(id)
            .orElseThrow(() -> {
                String message = messageSource.getMessage(
                    "error.edition.not_found", new Object[]{id}, LocaleContextHolder.getLocale()
                );
                return new ResourceNotFoundException(message);
            });

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
            editionRepository.saveAndFlush(edition);
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
        catch (ObjectOptimisticLockingFailureException e) {
            if (newCoverImageDbPath != null) {
                fileStorageService.deleteFile(newCoverImageDbPath);
            }
            String message = messageSource.getMessage(
                "error.edition.optimistic_lock", null, LocaleContextHolder.getLocale()
            );
            throw new OptimisticLockingFailureException(message);
        }
    }

    @Transactional
    public Edition createEdition(CreateEditionRequest request, MultipartFile coverImageFile) {
        // Kiểm tra ISBN duy nhất
        if (editionRepository.existsByIsbn(request.getIsbn())) {
            String message = messageSource.getMessage("validation.edition.isbn.unique", null, LocaleContextHolder.getLocale());
            throw new DuplicateIsbnCreateEditionException(message);
        }

        if (coverImageFile != null && !coverImageFile.isEmpty()) {
            validateCoverImage(coverImageFile);
        }

        Edition edition = editionMapper.toEntity(request);

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        Publisher publisher = publisherRepository.findById(request.getPublisherId())
                .orElseThrow(() -> new ResourceNotFoundException("Publisher not found"));

        edition.setBook(book);
        edition.setPublisher(publisher);

        Edition savedEdition = editionRepository.save(edition);

        if (coverImageFile != null && !coverImageFile.isEmpty()) {
            String coverImageUrl = fileStorageService.storeFile(coverImageFile, savedEdition.getId());
            savedEdition.setCoverImageUrl(coverImageUrl);
        }
        
        return savedEdition;
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

    @Override
    public ClientEditionDetailResponse getEditionDetailForClient(Integer id) {
        Edition edition = editionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException());
        
        return editionMapper.toClientDetailDto(edition);
    }
}
