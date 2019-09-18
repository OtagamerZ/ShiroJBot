package com.kuuhaku.handlers.games.RPG.Exceptions;

public class BadLuckException extends RuntimeException {
	public BadLuckException() throws BadLuckException {
		super("No item dropped");
		throw this;
	}
}
