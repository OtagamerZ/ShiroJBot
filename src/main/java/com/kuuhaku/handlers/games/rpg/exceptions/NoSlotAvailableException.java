package com.kuuhaku.handlers.games.rpg.exceptions;

public class NoSlotAvailableException extends RuntimeException {
	public NoSlotAvailableException() throws NoSlotAvailableException {
		super("No slot is available to put this item");
		throw this;
	}
}
