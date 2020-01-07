package com.kuuhaku.handlers.games.rpg.exceptions;

public class NameTakenException extends RuntimeException {
	public NameTakenException() throws NameTakenException {
		super("This name is in use");
		throw this;
	}
}
