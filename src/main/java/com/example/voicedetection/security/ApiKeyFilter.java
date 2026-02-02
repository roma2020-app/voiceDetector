package com.example.voicedetection.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    @Value("${api.security.key}")
    private String validApiKey;

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String apiKey = req.getHeader("x-api-key");

        if (apiKey == null || !apiKey.equals(validApiKey)) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json");
            res.getWriter().write("""
                {
                  "status": "error",
                  "message": "Invalid API Key"
                }
                """);
            return;
        }

        chain.doFilter(request, response);
    }
}
