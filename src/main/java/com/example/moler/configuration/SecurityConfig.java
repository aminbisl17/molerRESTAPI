package com.example.moler.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.moler.authentication.JwtAuthFilter;
import com.example.moler.authentication.TenantFilter;

@Configuration
public class SecurityConfig {

    private final UserDetailsService menaxheriDetailsService;
    private final UserDetailsService puntoriDetailsService;

    public SecurityConfig(
            @Qualifier("menaxheriDetailsService")
            UserDetailsService menaxheriDetailsService,

            @Qualifier("puntoriDetailsService")
            UserDetailsService puntoriDetailsService
    ) {
        this.menaxheriDetailsService = menaxheriDetailsService;
        this.puntoriDetailsService = puntoriDetailsService;
    }

    // --------------------------------------------------
    // FILTER REGISTRATION
    // --------------------------------------------------

    @Bean
    public FilterRegistrationBean<TenantFilter> tenantReg(
            TenantFilter filter
    ) {
        FilterRegistrationBean<TenantFilter> registration =
                new FilterRegistrationBean<>(filter);

        registration.setEnabled(false);

        return registration;
    }

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtReg(
            JwtAuthFilter filter
    ) {
        FilterRegistrationBean<JwtAuthFilter> registration =
                new FilterRegistrationBean<>(filter);

        registration.setEnabled(false);

        return registration;
    }

    // --------------------------------------------------
    // PASSWORD ENCODER
    // --------------------------------------------------

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // --------------------------------------------------
    // MENAXHER AUTHENTICATION MANAGER
    // --------------------------------------------------

    @Primary
    @Bean("menaxheriAuthManager")
    public AuthenticationManager menaxheriAuthManager() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(menaxheriDetailsService);

        provider.setPasswordEncoder(passwordEncoder());

        return new ProviderManager(provider);
    }

    // --------------------------------------------------
    // PUNTOR AUTHENTICATION MANAGER
    // --------------------------------------------------

    @Bean("puntoriAuthManager")
    public AuthenticationManager puntoriAuthManager() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(puntoriDetailsService);

        provider.setPasswordEncoder(passwordEncoder());

        return new ProviderManager(provider);
    }

    // --------------------------------------------------
    // SECURITY FILTER CHAIN
    // --------------------------------------------------

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthFilter jwtAuthFilter,
            TenantFilter tenantFilter
    ) throws Exception {

        http
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth

                // OPTIONS
                .requestMatchers(
                        HttpMethod.OPTIONS, "/**"
                ).permitAll()

                // PUBLIC ENDPOINTS
         .requestMatchers(
        "/menaxheri/register",
        "/auth/**",

        // PUNTOR AUTH
        "/puntori/login",
        "/puntori/refresh",
        "/puntori/logout"
).permitAll()

                // MENAXHER ENDPOINTS
                .requestMatchers(
                        "/menaxheri/data",
                        "/menaxheri/puntori/**",
                        "/projekti/**"
                ).hasRole("MENAXHER")

                // PUNTOR ENDPOINTS
                .requestMatchers(
                        "/puntori/**"
                ).hasRole("PUNTOR")

                // Editing attendance (/prezenca/{id}, /v, /c) is manager-only
                .requestMatchers(HttpMethod.PATCH, "/prezenca/**").hasRole("MENAXHER")

                // Listing attendance is manager-only
                .requestMatchers(HttpMethod.GET,
                        "/prezenca",
                        "/prezenca/nv"
                ).hasRole("MENAXHER")

                .requestMatchers("/prezenca/**").hasAnyRole("PUNTOR", "MENAXHER")
                // EVERYTHING ELSE
                .anyRequest().authenticated()
            )

            .sessionManagement(sess ->
                    sess.sessionCreationPolicy(
                            SessionCreationPolicy.STATELESS
                    )
            )

            .addFilterBefore(
                    jwtAuthFilter,
                    UsernamePasswordAuthenticationFilter.class
            )

            .addFilterBefore(
                    tenantFilter,
                    JwtAuthFilter.class
            );

        return http.build();
    }
}
