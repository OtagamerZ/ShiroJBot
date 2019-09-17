package com.kuuhaku.handlers.games.RPG.Enums;

public enum Equipment {
	HEAD("Elmo"), CHEST("Peitoral"), LEG("Calça"), FOOT("Botas"), ARM("Luvas"), NECK("Colar"), BAG("Bolsa"), RING("Anel"), WEAPON("Arma/Escudo"), MISC("Não equipável");

	private final String name;

	Equipment(String name) {
		this.name = name;
	}

	public static Equipment byName(String name) throws IllegalArgumentException {
		switch (name.toLowerCase()) {
			case "elmo":
				return Equipment.HEAD;
			case "peitoral":
				return Equipment.CHEST;
			case "calça":
				return Equipment.LEG;
			case "botas":
				return Equipment.FOOT;
			case "luvas":
				return Equipment.ARM;
			case "colar:":
				return Equipment.NECK;
			case "bolsa":
				return Equipment.BAG;
			case "anel":
				return Equipment.RING;
			case "arma":
			case "escudo":
				return Equipment.WEAPON;
			case "misc":
				return Equipment.MISC;
			default: throw new IllegalArgumentException();
		}
	}

	public String getName() {
		return name;
	}
}
