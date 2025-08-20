package com.group2.library_management.controller.admin;

import com.group2.library_management.dto.request.UpdateEditionRequest;
import com.group2.library_management.entity.Edition;
import com.group2.library_management.entity.Publisher;
import com.group2.library_management.exception.ResourceNotFoundException;
import com.group2.library_management.dto.mapper.EditionMapper;
import com.group2.library_management.service.EditionService;
import com.group2.library_management.service.PublisherService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/admin/editions")
@RequiredArgsConstructor
// @PreAuthorize("hasRole('ADMIN')")
// commented because we have not implemented security yet
@Slf4j
public class EditionController {

    private final EditionService editionService;
    private final PublisherService publisherService;
    private final EditionMapper editionMapper;

    private final MessageSource messageSource;

    private static final String VIEW_EDITION_EDIT = "admin/edition/edit";
    private static final String REDIRECT_TO_BOOKS_LIST = "redirect:/admin/books";
    private static final String REDIRECT_TO_EDITION_EDIT = "redirect:/admin/editions/%d/edit";

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Integer id, Model model) {
        Edition edition = editionService.getById(id);

        UpdateEditionRequest requestDto = editionMapper.toUpdateRequest(edition);
        List<Publisher> publishers = publisherService.findAll(); // Get publishers list

        model.addAttribute("edition", edition); 
        model.addAttribute("editionRequest", requestDto);
        model.addAttribute("publishers", publishers); // Add publishers list to model
        model.addAttribute("activeMenu", "books");
        return VIEW_EDITION_EDIT; // Return to edit page
    }

    // Handle update form submission
    @PostMapping("/{id}")
    public String processEditionUpdate(@PathVariable Integer id,
                                       @Valid @ModelAttribute("editionRequest") UpdateEditionRequest request,
                                       BindingResult bindingResult,
                                       @RequestParam("coverImageFile") MultipartFile coverImageFile,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {

        // Validation failed
        if (bindingResult.hasErrors()) {
            model.addAttribute("edition", editionService.getById(id));
            model.addAttribute("publishers", publisherService.findAll());
            model.addAttribute("activeMenu", "books");
            return VIEW_EDITION_EDIT; // Return to edit page
        }

        editionService.updateEdition(id, request, coverImageFile);
        
        redirectAttributes.addFlashAttribute(
            "successMessage",
            messageSource.getMessage("admin.editions.message.update_success", null, LocaleContextHolder.getLocale())
        );
        return REDIRECT_TO_BOOKS_LIST;
    }
}
