package com.group2.library_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateBookRequest {

    @NotBlank(message = "{admin.books.validation.title_blank}")
    @Size(max = 255)
    private String title;

    @NotEmpty(message = "{admin.books.validation.authors_empty}")
    private List<Integer> authorIds;

    @NotEmpty(message = "{admin.books.validation.genres_empty}")
    private List<Integer> genreIds;

    private String description;
}
