package com.mini_banking_app.web;

import com.mini_banking_app.shared.domain.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Порушення бізнес-правил, не оброблені в контролері, показуються сторінкою з поясненням (HTTP 400).
 * Інші винятки обробляє стандартний механізм Spring Boot (сторінка error.html без стеку викликів).
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ModelAndView domain(DomainException e) {
        log.debug("Порушення бізнес-правила: {}", e.getMessage());
        ModelAndView mav = new ModelAndView("error", HttpStatus.BAD_REQUEST);
        mav.addObject("status", 400);
        mav.addObject("error", "Операцію не виконано");
        mav.addObject("message", e.getMessage());
        return mav;
    }
}
