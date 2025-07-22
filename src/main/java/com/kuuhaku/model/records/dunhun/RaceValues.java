package com.kuuhaku.model.records.dunhun;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record RaceValues(
		@Column(name = "hp", nullable = false)
		int hp,
		@Column(name = "attack", nullable = false)
		int attack,
		@Column(name = "defense", nullable = false)
		int defense,
		@Column(name = "dodge", nullable = false)
		int dodge,
		@Column(name = "parry", nullable = false)
		int parry,
		@Column(name = "critical", nullable = false)
		double critical,
		@Column(name = "power", nullable = false)
		double power
) {
	public RaceValues() {
		this(0, 0, 0, 0, 0, 0, 0);
	}

	public RaceValues mix(RaceValues other) {
		return new RaceValues(
				(hp + other.hp()) / 2,
				(attack + other.attack()) / 2,
				(defense + other.defense()) / 2,
				(dodge + other.dodge()) / 2,
				(parry + other.parry()) / 2,
				(critical + other.critical()) / 2,
				(power + other.power()) / 2
		);
	}
}
