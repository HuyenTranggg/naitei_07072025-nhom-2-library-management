package com.group2.library_management.controller.api;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.group2.library_management.common.constants.Endpoints;
import com.group2.library_management.dto.request.EditionQueryParameters;
import com.group2.library_management.dto.response.BaseApiResponse;
import com.group2.library_management.dto.response.ClientEditionDetailResponse;
import com.group2.library_management.dto.response.EditionListResponse;
import com.group2.library_management.service.EditionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController("apiEditionController")
@RequestMapping(Endpoints.ApiV1.Editions.BASE_URL)
@RequiredArgsConstructor
public class EditionController {
    private final EditionService editionService;
    private final MessageSource messageSource;

    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    @GetMapping
    public ResponseEntity<BaseApiResponse<Page<EditionListResponse>>> getAllEditions(
            @Valid @ModelAttribute EditionQueryParameters params) {
        
        Page<EditionListResponse> editionPage = editionService.getAllEditions(params);
        
        String successMessage = getMessage("success.editions.get_all");

        BaseApiResponse<Page<EditionListResponse>> response = new BaseApiResponse<>(
                HttpStatus.OK.value(),
                editionPage,
                successMessage
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping(Endpoints.ApiV1.Editions.BY_ID_ACTION)
    public ResponseEntity<BaseApiResponse<ClientEditionDetailResponse>> getEditionById(
            @PathVariable("id") Integer id) {
        
        ClientEditionDetailResponse editionDetail = editionService.getEditionDetailForClient(id);

        String successMessage = getMessage("success.edition.get_detail");
        
        BaseApiResponse<ClientEditionDetailResponse> response = new BaseApiResponse<>(
                HttpStatus.OK.value(),
                editionDetail,
                successMessage
        );
        
        return ResponseEntity.ok(response);
    }
}
