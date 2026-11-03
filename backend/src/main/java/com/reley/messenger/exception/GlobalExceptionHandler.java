package com.reley.messenger.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GlobalException.class)
    public String handleGlobalException(
            GlobalException exception,
            Model model
    ) {
        model.addAttribute("error", exception.getDescription());

        return exception.getView();
    }

    @ExceptionHandler(Exception.class)
    public String handleException(
            Exception exception,
            Model model
    ) {
        model.addAttribute(
                "error",
                exception.getMessage()
        );

        return "error";
    }
}