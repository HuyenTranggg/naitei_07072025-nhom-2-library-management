package com.group2.library_management.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import com.group2.library_management.dto.request.UpdateEditionRequest;
import com.group2.library_management.dto.response.*;
import com.group2.library_management.entity.Edition;

public interface EditionService {
    Page<EditionListResponse> getAllEditions(Pageable pageable);
    List<EditionResponse> getEditionsByBookId(Integer bookId);
    EditionDetailResponse getEditionDetailById(Integer id);
    Edition getById(Integer id);
    void updateEdition(Integer id, UpdateEditionRequest request, MultipartFile coverImageFile);
}
