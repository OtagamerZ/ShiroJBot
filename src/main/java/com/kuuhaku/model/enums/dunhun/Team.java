package com.kuuhaku.model.enums.dunhun;

public enum Team {
	HUNTERS, KEEPERS;

	public Team getOther() {
		return switch (this) {
			case HUNTERS -> KEEPERS;
			case KEEPERS -> HUNTERS;
		};
	}
}
