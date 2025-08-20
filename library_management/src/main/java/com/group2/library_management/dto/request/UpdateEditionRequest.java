package com.group2.library_management.dto.request;

import com.group2.library_management.entity.enums.BookFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Getter
@Setter
public class UpdateEditionRequest {

    @NotBlank(message = "{edition.title.notBlank}")
    @Size(max = 255, message = "{edition.title.size}")
    private String title;

    @NotBlank(message = "{edition.isbn.notBlank}")
    @Size(max = 20, message = "{edition.isbn.size}")
    private String isbn;

    @NotNull(message = "{edition.publisher.notNull}")
    private Integer publisherId;

    @NotNull(message = "{edition.publicationDate.notNull}")
    @PastOrPresent(message = "{edition.publicationDate.pastOrPresent}")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate publicationDate;

    @NotNull(message = "{edition.pageNumber.notNull}")
    @Positive(message = "{edition.pageNumber.positive}")
    private Integer pageNumber;

    @NotBlank(message = "{edition.language.notBlank}")
    private String language;

    @NotNull(message = "{edition.format.notNull}")
    private BookFormat format;
}
