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
		return new Exception(500, "Erro interno do servidor");
	}

	@ExceptionHandler(InvalidTokenException.class)
	public Exception invalidToken() {
		return new Exception(403, "Token inválido");
	}

	@ExceptionHandler(NoResultException.class)
	public Exception noResult() {
		return new Exception(404, "Nenhuma entidade com o ID informado encontrada");
	}

	@ExceptionHandler(NoResultException.class)
	public Exception unauthorized() {
		return new Exception(403, "Login nao autorizado");
	}

	@Override
	public String getErrorPath() {
		return "/error";
	}
}
