package com.group2.library_management.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.group2.library_management.dto.response.EditionResponse;
import com.group2.library_management.repository.EditionRepository;
import com.group2.library_management.service.EditionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EditionServiceImpl implements EditionService{

    private final EditionRepository editionRepository;

    @Override
    public List<EditionResponse> getEditionsByBookId(Integer bookId) {
        return editionRepository.findByBookIdOrderByPublicationDateDesc(bookId)
                .stream()
                .map(EditionResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
