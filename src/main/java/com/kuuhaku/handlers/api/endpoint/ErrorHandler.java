package com.kuuhaku.handlers.api.endpoint;

import com.kuuhaku.handlers.api.exception.Exception;
import com.kuuhaku.handlers.api.exception.InvalidTokenException;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ControllerAdvice
public class ErrorHandler implements ErrorController {
	@RequestMapping("/error")
	public Exception error() {
		return new Exception(500, "Erro interno", new String[]{});
	}

	@ExceptionHandler(InvalidTokenException.class)
	public Exception collectData(Exception e) {
		return new Exception(403, e.getCause(), e.getStacktrace());
	}

	@Override
	public String getErrorPath() {
		return "/error";
	}
}
