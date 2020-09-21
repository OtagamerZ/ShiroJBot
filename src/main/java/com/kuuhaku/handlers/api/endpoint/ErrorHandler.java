/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.handlers.api.endpoint;

import com.kuuhaku.handlers.api.exception.Exception;
import com.kuuhaku.handlers.api.exception.*;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.bind.annotation.*;

import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletResponse;

@RestController
@ControllerAdvice
public class ErrorHandler implements ErrorController {
	@RequestMapping("/error")
	public void handleError(HttpServletResponse http) {
		http.setHeader("Location", "https://http.cat/" + http.getStatus());
		http.setStatus(HttpServletResponse.SC_FOUND);
	}

	@ExceptionHandler({HttpMessageNotWritableException.class, ConversionNotSupportedException.class})
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public Exception internalError() {
		return new Exception(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
	}

	@ExceptionHandler(InvalidTokenException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public Exception invalidToken() {
		return new Exception(HttpStatus.UNAUTHORIZED, "Invalid token");
	}

	@ExceptionHandler(UnauthorizedException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public Exception unauthorized() {
		return new Exception(HttpStatus.UNAUTHORIZED, "Access not authorized");
	}

	@ExceptionHandler(NoResultException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public Exception noResult() {
		return new Exception(HttpStatus.NOT_FOUND, "No entity found with that ID");
	}

	@ExceptionHandler(NotEnoughArgsException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Exception notEnoughArgs() {
		return new Exception(HttpStatus.BAD_REQUEST, "Not enough arguments were given to this request");
	}

	@ExceptionHandler(RatelimitException.class)
	@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
	public Exception ratelimited() {
		return new Exception(HttpStatus.TOO_MANY_REQUESTS, "You are being ratelimited");
	}

	@Override
	public String getErrorPath() {
		return "/error";
	}
}