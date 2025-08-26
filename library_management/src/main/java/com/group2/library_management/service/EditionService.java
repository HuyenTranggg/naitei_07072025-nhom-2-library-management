package com.group2.library_management.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import com.group2.library_management.dto.request.UpdateEditionRequest;
import com.group2.library_management.dto.request.EditionQueryParameters;
import com.group2.library_management.dto.response.*;
import com.group2.library_management.entity.enums.BookStatus;

public interface EditionService {
    Page<EditionListResponse> getAllEditions(EditionQueryParameters params);

    List<EditionResponse> getEditionsByBookId(Integer bookId);

    EditionDetailResponse getEditionDetailById(Integer id);

    void deleteEdition(Integer id);

    Page<EditionListResponse> searchEditionAndBookInstance(String keyword, String filterBy, BookStatus bookStatus, LocalDate fromDate, LocalDate toDate, Pageable pageable);

    EditionUpdateResponse findEditionForUpdate(Integer id);

    void updateEdition(Integer id, UpdateEditionRequest request, MultipartFile coverImageFile);

    ClientEditionDetailResponse getEditionDetailForClient(Integer id);
}
