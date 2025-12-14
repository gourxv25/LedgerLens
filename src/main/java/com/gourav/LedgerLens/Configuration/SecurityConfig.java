package com.gourav.LedgerLens.Configuration;

import com.gourav.LedgerLens.Security.CustomOAuth2UserService;
import com.gourav.LedgerLens.Security.OAuthLoginSuccessHandler;
import com.gourav.LedgerLens.Security.JwtAuthenticationFIlter;
import com.gourav.LedgerLens.Security.LedgerLensUserDetailsService;

import jakarta.servlet.http.Cookie;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@EnableWebSecurity
@Configuration
@Slf4j
public class SecurityConfig {

    private final LedgerLensUserDetailsService userDetailsService;
    private final JwtAuthenticationFIlter jwtAuthenticationFilter;
    private final OAuthLoginSuccessHandler successHandler;
    private final CustomOAuth2UserService oauth2UserService;
    private final BCryptPasswordEncoder passwordEncoder;

    public SecurityConfig(
            LedgerLensUserDetailsService userDetailsService,
            @Lazy JwtAuthenticationFIlter jwtAuthenticationFilter,
            OAuthLoginSuccessHandler successHandler,
            CustomOAuth2UserService oauth2UserService,
            BCryptPasswordEncoder passwordEncoder
    ) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.successHandler = successHandler;
        this.oauth2UserService = oauth2UserService;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {

        log.info("Configuring AuthenticationProvider");

        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);

        log.info("AuthenticationProvider configured successfully");
        return authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        log.info("Starting Spring Security filter chain configuration");

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/**",
                                "/oauth2/**",
                                "/public/**",
                                "/pubsub/push",
                                "/gmail/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 ->
                        oauth2
                                .userInfoEndpoint(userInfo ->
                                        userInfo.oidcUserService(oauth2UserService)
                                )
                                .successHandler(successHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {

                            log.info("Logout requested");

                            try {
                                Cookie cookie = new Cookie("LL-JWT", null);
                                cookie.setPath("/");
                                cookie.setMaxAge(0);
                                cookie.setHttpOnly(true);
                                cookie.setSecure(true);
                                response.addCookie(cookie);

                                response.setStatus(200);
                                response.getWriter().write("Logout successful");

                                log.info("Logout completed successfully");

                            } catch (IOException e) {
                                log.error("Error while writing logout response", e);
                                throw e; // handled by GlobalExceptionHandler
                            }
                        })
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        log.info("Spring Security filter chain configured successfully");
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        log.info("Configuring CORS settings");

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "https://otilia-undated-joelle.ngrok-free.dev"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "LL-JWT"
        ));
        configuration.setExposedHeaders(List.of(
                "Set-Cookie",
                "LL-JWT"
        ));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        log.info("CORS configuration applied successfully");
        return source;
    }
}
