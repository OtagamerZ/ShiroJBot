package com.kuuhaku.handlers.api.endpoint;

import com.kuuhaku.handlers.api.exception.Exception;
import com.kuuhaku.handlers.api.exception.InvalidTokenException;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.NoResultException;

@RestController
@ControllerAdvice
public class ErrorHandler implements ErrorController {
	@RequestMapping("/error")
	public Exception error() {
		return new Exception(404, "Page not found", new String[]{});
	}

	@ExceptionHandler(InvalidTokenException.class)
	public Exception invalidToken(Exception e) {
		return new Exception(403, e.getCause(), e.getStacktrace());
	}

	@ExceptionHandler(NoResultException.class)
	public Exception noResult(Exception e) {
		return new Exception(404, e.getCause(), e.getStacktrace());
	}

	@Override
	public String getErrorPath() {
		return "/error";
	}
}
