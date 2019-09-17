package com.kuuhaku.handlers.games.RPG.Exceptions;

public class NameTakenException extends RuntimeException {
	public NameTakenException() throws NameTakenException {
		super("This name is in use");
		throw this;
	}
}
