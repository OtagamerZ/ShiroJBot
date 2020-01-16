package com.kuuhaku.handlers.api.endpoint;

import com.kuuhaku.handlers.api.exception.Exception;
import com.kuuhaku.handlers.api.exception.InvalidTokenException;
import com.kuuhaku.handlers.api.exception.UnauthorizedException;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.persistence.NoResultException;

@RestController
@ControllerAdvice
public class ErrorHandler implements ErrorController {
	@RequestMapping("/error")
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public Exception error() {
		return new Exception(500, "Erro interno do servidor");
	}

	@ExceptionHandler(InvalidTokenException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public Exception invalidToken() {
		return new Exception(403, "Token inválido");
	}

	@ExceptionHandler(NoResultException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public Exception noResult() {
		return new Exception(404, "Nenhuma entidade com o ID informado encontrada");
	}

	@ExceptionHandler(UnauthorizedException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public Exception unauthorized() {
		return new Exception(403, "Login não autorizado");
	}

	@Override
	public String getErrorPath() {
		return "/error";
	}
}
