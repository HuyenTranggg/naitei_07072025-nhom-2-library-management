package com.group2.library_management.service;

import java.util.List;

import com.group2.library_management.dto.response.EditionResponse;

public interface EditionService {
    List<EditionResponse> getEditionsByBookId(Integer bookId);
}
