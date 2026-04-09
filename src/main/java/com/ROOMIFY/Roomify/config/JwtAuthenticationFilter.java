package com.ROOMIFY.Roomify.config;

import com.ROOMIFY.Roomify.Component.JwtUtil;
import com.ROOMIFY.Roomify.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Log the request path and auth header
        logger.info("Request path: {}", request.getServletPath());
        logger.info("Authorization header: {}", authHeader != null ? "Present" : "Missing");

        // Skip authentication for public endpoints
        String requestPath = request.getServletPath();
        if (requestPath.startsWith("/api/auth/") ||
                requestPath.startsWith("/api/public/")) {
            logger.info("Skipping authentication for public endpoint: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("No valid Authorization header found for: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        logger.info("JWT token extracted, length: {}", jwt.length());

        try {
            userEmail = jwtUtil.extractEmail(jwt);
            logger.info("Extracted email from token: {}", userEmail);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                logger.info("Loaded user details for: {}, authorities: {}", userEmail, userDetails.getAuthorities());

                if (jwtUtil.validateToken(jwt)) {
                    logger.info("Token validated successfully for: {}", userEmail);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    logger.info("Authentication set in SecurityContext for: {}", userEmail);
                } else {
                    logger.warn("Token validation failed for: {}", userEmail);
                }
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }
}