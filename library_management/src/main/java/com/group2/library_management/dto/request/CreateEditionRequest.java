package com.group2.library_management.dto.request;

import com.group2.library_management.entity.enums.BookFormat;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Getter
@Setter
public class CreateEditionRequest {

    @NotNull(message = "{validation.edition.book.notnull}")
    private Integer bookId;

    @NotBlank(message = "{validation.edition.title.notblank}")
    private String title;

    @NotBlank(message = "{validation.edition.isbn.notblank}")
    private String isbn;

    @NotNull(message = "{validation.edition.publisher.notnull}")
    private Integer publisherId;

    @NotNull(message = "{validation.edition.publicationDate.notnull}")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate publicationDate;

    @NotNull(message = "{validation.edition.pageNumber.notnull}")
    @Positive
    private Integer pageNumber;

    @NotBlank(message = "{validation.edition.language.notblank}")
    private String language;

    @NotNull(message = "{validation.edition.format.notnull}")
    private BookFormat format;

    @PositiveOrZero
    private Integer initialQuantity = 0;

    @PositiveOrZero
    private Integer availableQuantity = 0;
}
