package com.gourav.LedgerLens.Configuration;

import com.gourav.LedgerLens.Security.CustomOAuth2UserService;
import com.gourav.LedgerLens.Security.OAuthLoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.gourav.LedgerLens.Security.JwtAuthenticationFIlter;
import com.gourav.LedgerLens.Security.LedgerLensUserDetailsService;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    private final LedgerLensUserDetailsService userDetailsService;
    private final JwtAuthenticationFIlter jwtAuthenticationFilter;
    private final OAuthLoginSuccessHandler successHandler;
    private final CustomOAuth2UserService oauth2UserService;
    private final BCryptPasswordEncoder passwordEncoder;

    public SecurityConfig(LedgerLensUserDetailsService userDetailsService,
                    @Lazy JwtAuthenticationFIlter jwtAuthenticationFilter,
                          OAuthLoginSuccessHandler successHandler,
                          CustomOAuth2UserService oauth2UserService,
                          BCryptPasswordEncoder passwordEncoder
                          ){
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.successHandler = successHandler;
        this.oauth2UserService = oauth2UserService;
        this.passwordEncoder = passwordEncoder;
                    }
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/auth/**","/oauth2/**","/public/**").permitAll()
                .anyRequest().authenticated()
            )
                .oauth2Login(oauth2 ->
                        oauth2
                                .userInfoEndpoint(userInfo ->
                                        userInfo
                                                .oidcUserService(oauth2UserService)   // add this line for Google (OIDC)
                                )
                                .successHandler(successHandler)
                )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173")); // Adjust as needed
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    

}
