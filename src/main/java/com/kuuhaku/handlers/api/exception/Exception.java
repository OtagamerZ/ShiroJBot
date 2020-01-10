package com.kuuhaku.handlers.api.exception;

public class Exception {
	private final int code;
	private final String cause;

	public Exception(int code, String cause) {
		this.code = code;
		this.cause = cause;
	}

	public int getCode() {
		return code;
	}

	public String getCause() {
		return cause;
	}
}
