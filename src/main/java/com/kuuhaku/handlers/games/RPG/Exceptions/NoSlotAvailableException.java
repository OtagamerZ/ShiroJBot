package com.kuuhaku.handlers.games.RPG.Exceptions;

public class NoSlotAvailableException extends RuntimeException {
	public NoSlotAvailableException() throws NoSlotAvailableException {
		super("No slot is available to put this item");
		throw this;
	}
}
