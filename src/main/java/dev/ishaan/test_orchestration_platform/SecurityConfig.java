package dev.ishaan.test_orchestration_platform;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())                                    // not needed for a token-based API
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/").permitAll()                            // simple root status message, no token needed
                        .requestMatchers("/api/auth/login").permitAll()              // login itself needs no token
                        .requestMatchers("/error").permitAll()                       // internal error path, no token available here
                        .requestMatchers("/actuator/**").permitAll()                 // health/metrics checks — no token available for monitoring tools
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()  // Swagger's docs data and UI page
                        .requestMatchers(HttpMethod.POST, "/api/test-runs").hasRole("ADMIN")  // only admins can create runs
                        .anyRequest().authenticated()                                 // everything else just requires login
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);  // run our filter first

        return http.build();
    }

}