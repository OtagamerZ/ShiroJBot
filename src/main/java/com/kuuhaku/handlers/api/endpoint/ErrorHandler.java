package com.kuuhaku.handlers.api.endpoint;

import com.kuuhaku.handlers.api.exception.Exception;
import com.kuuhaku.handlers.api.exception.InvalidTokenException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.persistence.NoResultException;

@ControllerAdvice
public class ErrorHandler {
	@ExceptionHandler(InvalidTokenException.class)
	public Exception invalidToken(Exception e) {
		return new Exception(403, e.getCause(), e.getStacktrace());
	}

	@ExceptionHandler(NoResultException.class)
	public Exception noResult(Exception e) {
		return new Exception(404, e.getCause(), e.getStacktrace());
	}
}
