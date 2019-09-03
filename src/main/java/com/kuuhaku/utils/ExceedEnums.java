package com.kuuhaku.utils;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum ExceedEnums {
	IMANITY("Imanity"), SEIREN("Seiren"), WEREBEAST("Werebeast"), ELF("Elf"), EXMACHINA("Ex-Machina"), FLUGEL("Flügel");

	private String name;

	ExceedEnums(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public static ExceedEnums getByName(String name) {
		return Arrays.stream(ExceedEnums.values()).filter(e -> e.getName().equalsIgnoreCase(name)).collect(Collectors.toList()).get(0);
	}
}
