package com.group2.library_management.exception;

import java.io.IOException;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@ControllerAdvice(basePackages = "com.group2.library_management.controller.admin")
@RequiredArgsConstructor
public class WebExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(Exception.class)
    public ModelAndView handleWebException(HttpServletRequest req, Exception ex) {
        ModelAndView mav = new ModelAndView();
        mav.addObject("exception", ex);
        mav.addObject("url", req.getRequestURL());
        mav.setViewName("error"); // Tên file templates/error.html
        return mav;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleNotFound(HttpServletRequest req, ResourceNotFoundException ex) {
        ModelAndView mav = new ModelAndView();
        mav.addObject("message", ex.getMessage());
        mav.setViewName("404");
        return mav;
    }

    @ExceptionHandler(ImportValidationException.class)
    public String handleImportValidationWebException(
            ImportValidationException ex, 
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) { // Thêm HttpServletRequest vào tham số

        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        redirectAttributes.addFlashAttribute("validationErrors", ex.getErrors());
        
        // Lấy URL của trang mà người dùng đã gửi request từ đó (Referer Header)
        String referer = request.getHeader("Referer");

        // Kiểm tra xem Referer có tồn tại không để tránh lỗi
        if (referer != null && !referer.isEmpty()) {
            // Chuyển hướng người dùng về lại đúng trang họ vừa ở
            return "redirect:" + referer;
        }
        // Nếu không lấy được Referer, chuyển hướng về một trang mặc định an toàn
        return "redirect:/"; 
    }

    @ExceptionHandler(IOException.class)
    public String handleIOException(
                IOException ex, 
                RedirectAttributes redirectAttributes,
                HttpServletRequest request) { // Thêm HttpServletRequest vào tham số

        redirectAttributes.addFlashAttribute("errorMessage", "error.file.process" + ex.getMessage());
        
        // Lấy URL của trang mà người dùng đã gửi request từ đó (Referer Header)
        String referer = request.getHeader("Referer");

        // Kiểm tra xem Referer có tồn tại không để tránh lỗi
        if (referer != null && !referer.isEmpty()) {
            // Chuyển hướng người dùng về lại đúng trang họ vừa ở
            return "redirect:" + referer;
        }
        // Nếu không lấy được Referer, chuyển hướng về một trang mặc định an toàn
        return "redirect:/"; 
    }

    @ExceptionHandler(CannotDeleteResourceException.class)
    public ResponseEntity<Map<String, String>> handleCannotDeleteResource(CannotDeleteResourceException ex, WebRequest request){
        Map<String, String> errorDetails = Map.of("error", ex.getMessage());
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(BookInstanceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleBookInstanceNotFound(BookInstanceNotFoundException ex, WebRequest request){
        Map<String, String> errorDetails = Map.of("error", ex.getMessage());
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OperationFailedException.class)
    public String handleOperationFailedException(OperationFailedException ex, 
                                                 RedirectAttributes redirectAttributes,
                                                 HttpServletRequest request) {
        
        String messageKey = ex.getMessage();
        
        String localizedErrorMessage = messageSource.getMessage(
            messageKey, 
            null, 
            ex.getMessage(),
            LocaleContextHolder.getLocale()
        );

        redirectAttributes.addFlashAttribute("errorMessage", localizedErrorMessage);

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/admin/dashboard");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object handleDataIntegrity(DataIntegrityViolationException ex,
                                      RedirectAttributes redirectAttributes,
                                      HttpServletRequest request) {
        String msg = messageSource.getMessage("admin.editions.message.cannot_delete",
                                              null,
                                              ex.getMessage(),
                                              LocaleContextHolder.getLocale());
        redirectAttributes.addFlashAttribute("errorMessage", msg);

        String referer = request.getHeader("Referer");

        if (referer != null && !referer.isEmpty()) {

            return "redirect:" + referer;
        }
        return "redirect:/";
    }
}
