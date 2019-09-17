package com.kuuhaku.handlers.games.RPG.Exceptions;

public class UnknownItemException extends RuntimeException {
	public UnknownItemException() throws UnknownItemException {
		super("Item not found");
		throw this;
	}
}
