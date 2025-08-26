package com.group2.library_management.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.context.MessageSource;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import com.group2.library_management.common.constants.Endpoints;

import java.io.IOException;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final MessageSource messageSource;

    private final LocaleResolver localeResolver;
    
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        
        setDefaultFailureUrl(Endpoints.Admin.Login.failureUrl);

        Locale locale = localeResolver.resolveLocale(request);
        
        String errorMessage;

        // Check if the exception has a i18n
        if (exception.getMessage() != null && exception.getMessage().startsWith("{") && exception.getMessage().endsWith("}")) {
            String messageKey = exception.getMessage().substring(1, exception.getMessage().length() - 1);
            errorMessage = messageSource.getMessage(messageKey, null, messageKey, locale);
        } else {
            errorMessage = messageSource.getMessage("login.error", null, locale);
        }

        request.getSession().setAttribute("errorMessage", errorMessage);

        super.onAuthenticationFailure(request, response, exception);
    }
}
