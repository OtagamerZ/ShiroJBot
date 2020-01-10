package com.kuuhaku.handlers.api.exception;

public class Exception {
	private final int code;
	private final String message;

	public Exception(int code, String message) {
		this.code = code;
		this.message = message;
	}

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}
