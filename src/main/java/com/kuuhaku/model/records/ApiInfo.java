package com.kuuhaku.model.records;

import java.util.Locale;

public record ApiInfo(String url, String auth) {
	public ApiInfo(String id) {
		this(
				System.getenv((id + "_URL").toUpperCase(Locale.ROOT)),
				System.getenv((id + "_AUTH").toUpperCase(Locale.ROOT))
		);
	}
}
