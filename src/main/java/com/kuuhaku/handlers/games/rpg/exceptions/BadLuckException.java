package com.kuuhaku.handlers.games.rpg.exceptions;

public class BadLuckException extends RuntimeException {
	public BadLuckException() throws BadLuckException {
		super("No item dropped");
		throw this;
	}
}
