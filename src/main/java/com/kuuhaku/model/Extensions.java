package com.kuuhaku.model;

import org.apache.commons.lang3.StringUtils;

public class Extensions {
	private static final String[] ext = new String[]{
			".com", ".br", ".net", ".org", ".gov",
			".gg", ".xyz", ".site", ".blog", ".tv",
			".biz", ".fly", ".gl", ".ru", ".es",
			".tech"
	};

	public static boolean checkExtension(String str) {
		return StringUtils.containsAny(str, ext);
	}
}
