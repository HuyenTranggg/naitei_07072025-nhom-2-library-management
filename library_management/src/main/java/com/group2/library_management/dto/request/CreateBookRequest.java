package com.group2.library_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateBookRequest(
        @NotBlank(message = "{admin.books.add.validation.title_required}")
        @Size(max = 255, message = "{admin.books.add.validation.title_size}")
        String title,

        String description,

        @NotEmpty(message = "{admin.books.add.validation.authors_required}")
        List<String> authorIds,

        @NotEmpty(message = "{admin.books.add.validation.genres_required}")
        List<String> genreIds
) {}
