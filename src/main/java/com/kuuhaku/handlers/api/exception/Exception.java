package com.kuuhaku.handlers.api.exception;

public class Exception {
	private final int code;
	private final String cause;
	private final String[] stacktrace;

	public Exception(int code, String cause, String[] stacktrace) {
		this.code = code;
		this.cause = cause;
		this.stacktrace = stacktrace;
	}

	public int getCode() {
		return code;
	}

	public String getCause() {
		return cause;
	}

	public String[] getStacktrace() {
		return stacktrace;
	}
}
