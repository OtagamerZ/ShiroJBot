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
		return new Exception(500, "Erro interno", new String[]{});
	}

	@ExceptionHandler(InvalidTokenException.class)
	public Exception invalidToken(InvalidTokenException e) {
		return new Exception(403, e.toString(), e.getStackTrace());
	}

	@ExceptionHandler(NoResultException.class)
	public Exception noResult(NoResultException e) {
		return new Exception(404, e.toString(), e.getStackTrace());
	}

	@Override
	public String getErrorPath() {
		return "/error";
	}
}
