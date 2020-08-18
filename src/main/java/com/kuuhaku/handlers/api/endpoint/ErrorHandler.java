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

import com.kuuhaku.handlers.api.exception.InvalidTokenException;
import com.kuuhaku.handlers.api.exception.NotEnoughArgsException;
import com.kuuhaku.handlers.api.exception.RatelimitException;
import com.kuuhaku.handlers.api.exception.UnauthorizedException;
import org.json.JSONException;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

import javax.persistence.NoResultException;

@RestController
@ControllerAdvice
public class ErrorHandler implements ErrorController {
	@ExceptionHandler({HttpMessageNotWritableException.class, ConversionNotSupportedException.class})
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ResponseStatusException internalError() {
		return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
	public ResponseStatusException methodNotAllowed() {
		return new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed");
	}

	@ExceptionHandler(InvalidTokenException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public ResponseStatusException invalidToken() {
		return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
	}

	@ExceptionHandler(NoResultException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ResponseStatusException noResult() {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, "No entity found with that ID");
	}

	@ExceptionHandler(UnauthorizedException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public ResponseStatusException unauthorized() {
		return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access not authorized");
	}

	@ExceptionHandler(NotEnoughArgsException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ResponseStatusException notEnoughArgs() {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough arguments were given to this request");
	}

	@ExceptionHandler({
			IllegalArgumentException.class,
			NullPointerException.class,
			NumberFormatException.class,
			JSONException.class,
			MethodArgumentNotValidException.class,
			MissingServletRequestParameterException.class,
			MissingServletRequestPartException.class,
			TypeMismatchException.class
	})
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ResponseStatusException wrongArgs() {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong arguments in the request");
	}

	@ExceptionHandler(RatelimitException.class)
	@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
	public ResponseStatusException ratelimited() {
		return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "You are being ratelimited");
	}

	@Override
	public String getErrorPath() {
		return "/error";
	}
}