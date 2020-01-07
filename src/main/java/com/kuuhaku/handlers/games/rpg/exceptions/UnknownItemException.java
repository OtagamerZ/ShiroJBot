package com.kuuhaku.handlers.games.rpg.exceptions;

public class UnknownItemException extends RuntimeException {
	public UnknownItemException() throws UnknownItemException {
		super("Item not found");
		throw this;
	}
}
