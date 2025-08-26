package com.group2.library_management.dto.response;

import java.util.List;

import com.group2.library_management.entity.enums.DeletionStatus;

public record BookResponse(
    Integer id,
    String title,
    List<String> authors,
    List<String> genres,
    Integer editionCount,
    DeletionStatus deletionStatus
) {}
