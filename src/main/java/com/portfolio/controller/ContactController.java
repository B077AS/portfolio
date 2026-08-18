package com.portfolio.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.portfolio.model.ContactRequest;
import com.portfolio.model.ErrorResponse;
import com.portfolio.model.SuccessResponse;
import com.portfolio.service.EmailService;
import com.portfolio.util.ContactRateLimiter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ContactController {

    private static final String SUCCESS_MESSAGE = "Message sent - I'll get back to you soon.";

    private final EmailService emailService;
    private final ContactRateLimiter rateLimiter;

    @PostMapping("/contact")
    @ResponseBody
    public ResponseEntity<?> submit(@Valid @ModelAttribute ContactRequest form,
                                     BindingResult bindingResult,
                                     HttpServletRequest request) {

        if (StringUtils.hasText(form.getWebsite())) {
            log.debug("Honeypot tripped from {}", clientIp(request));
            return SuccessResponse.of(SUCCESS_MESSAGE);
        }

        if (bindingResult.hasErrors()) {
            return ErrorResponse.of(HttpStatus.BAD_REQUEST, "Please fill in every field with a valid email address.");
        }

        if (!rateLimiter.tryAcquire(clientIp(request))) {
            return ErrorResponse.of(HttpStatus.TOO_MANY_REQUESTS, "Too many messages sent recently - please try again later.");
        }

        try {
            emailService.sendContactMessage(form.getName(), form.getEmail(), form.getMessage());
            return SuccessResponse.of(SUCCESS_MESSAGE);
        } catch (Exception e) {
            return ErrorResponse.of(HttpStatus.BAD_GATEWAY, "Something went wrong sending your message - please try again later.");
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
