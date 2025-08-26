package com.group2.library_management.config;

import com.group2.library_management.common.constants.Endpoints;
import com.group2.library_management.service.JpaUserDetailsService;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import lombok.RequiredArgsConstructor;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JpaUserDetailsService userDetailsService;
    private final CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Value("${jwt.public.key}")
    private RSAPublicKey publicKey;

    @Value("${jwt.private.key}")
    private RSAPrivateKey privateKey;

    @Bean
    public JwtEncoder jwtEncoder() {
        JWK jwk = new RSAKey.Builder(this.publicKey).privateKey(this.privateKey).build();
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }
    
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(this.publicKey).build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .authorizeHttpRequests(authorize -> {
                authorize
                    .requestMatchers(
                        Endpoints.ApiV1.Auth.REGISTER_ENDPOINT, 
                        Endpoints.ApiV1.Auth.LOGIN_ENDPOINT, 
                        Endpoints.ApiV1.Auth.REFRESH_ENDPOINT
                    ).permitAll()
                    .requestMatchers(HttpMethod.GET, 
                        Endpoints.ApiV1.Books.ALL_BOOKS_PATHS,
                        Endpoints.ApiV1.Editions.ALL_EDITIONS_PATHS
                    ).permitAll()
                    .anyRequest().authenticated();
            })
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> {
                authorize
                    .requestMatchers("/vendors/**", "/css/**", "/js/**", "/images/**", "/admin/login", "/admin/access-denied", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .requestMatchers(Endpoints.Admin.ALL_STRINGS).hasRole("ADMIN")
                    .anyRequest().authenticated();
            })
            .formLogin(form -> {
                form
                    .loginPage(Endpoints.Admin.Login.LOGIN_ACTION_STRINGS)
                    .loginProcessingUrl(Endpoints.Admin.Login.LOGIN_ACTION_STRINGS)
                    .usernameParameter("email")
                    .defaultSuccessUrl(Endpoints.Admin.Dashboard.DASHBOARD_ACTION_STRINGS, true)
                    .failureHandler(customAuthenticationFailureHandler)
                    .permitAll();
            })
            .logout(logout -> {
                logout
                    .logoutUrl(Endpoints.Admin.Logout.LOGOUT_ACTION_STRINGS)
                    .logoutSuccessUrl(Endpoints.Admin.Logout.LOGOUT_SUCCESS_ACTION_STRINGS)
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll();
            })
            .exceptionHandling(exceptions -> {
                exceptions
                    .accessDeniedPage(Endpoints.Admin.Login.ACCESS_DENIED_ACTION_STRINGS);
            });
        return http.build();
    }
}
