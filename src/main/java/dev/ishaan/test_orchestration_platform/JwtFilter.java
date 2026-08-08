package dev.ishaan.test_orchestration_platform;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");             // look for the "Authorization" header

        if (authHeader != null && authHeader.startsWith("Bearer ")) {        // must be formatted "Bearer <token>"
            String token = authHeader.substring(7);                          // strip off "Bearer " to get just the token

            if (jwtUtil.isTokenValid(token)) {
                String username = jwtUtil.extractUsername(token);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(username, null, java.util.Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authToken);  // mark this request as authenticated
            }
        }

        filterChain.doFilter(request, response);                             // continue to the next step, regardless
    }

}