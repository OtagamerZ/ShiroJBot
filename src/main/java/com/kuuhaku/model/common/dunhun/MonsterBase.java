package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.Team;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.dunhun.MonsterStats;
import com.kuuhaku.model.persistent.localized.LocalizedMonster;
import com.kuuhaku.util.Calc;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.CascadeType.ALL;

@MappedSuperclass
public abstract class MonsterBase<T extends MonsterBase<T>> extends Actor<T> {
	public static final double[] hpTable = new double[1000];
	public static final double[] statTable = new double[1000];

	static {
		for (int i = 0; i < 1000; i++) {
			hpTable[i] = 1 + i / 10d;
			statTable[i] = Math.pow(1.26, i / 10d);
		}
	}

	@Embedded
	protected MonsterStats stats;

	@OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "id", referencedColumnName = "id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
	@Fetch(FetchMode.SUBSELECT)
	protected Set<LocalizedMonster> infos = new HashSet<>();

	public MonsterBase() {
	}

	public MonsterBase(String id) {
		super(id);
	}

	public MonsterStats getStats() {
		return stats;
	}

	public LocalizedMonster getInfo(I18N locale) {
		return infos.parallelStream()
				.filter(ld -> ld.getLocale().is(locale))
				.map(ld -> ld.setUwu(locale.isUwu()))
				.findAny().orElseThrow();
	}

	@Override
	public Race getRace() {
		return stats.getRace();
	}

	@Override
	public int getMaxAp() {
		int bonus = 0;
		if (getGame().getPartySize() > 1 && getTeam() == Team.KEEPERS) {
			bonus = getGame().getPartySize() / 2;
		}

		return (int) Calc.clamp(getStats().getMaxAp() + getModifiers().getMaxAp().get() + getGame().getAreaLevel() / 5d + bonus, 1, getApCap() + bonus);
	}

	@Override
	public int getApCap() {
		return (int) (5 + getStats().getMaxAp() + getModifiers().getMaxAp().get());
	}

	@Override
	public int getInitiative() {
		return getGame().getAreaLevel() / 3 + stats.getInitiative() + (int) getModifiers().getInitiative().get();
	}

	@Override
	public double getCritical() {
		return 5 * (1 + getModifiers().getCritical().get());
	}

	@Override
	public int getAggroScore() {
		int flat = getSenshi().getDmg() / 10 + getSenshi().getDfs() / 20 + getHp() / 150;
		double mult = switch (getRarityClass()) {
			case NORMAL -> 1;
			case MAGIC -> 1.5;
			case RARE -> 2.25;
			case UNIQUE -> 10;
		};

		return (int) Math.max(1, flat * (1 + getModifiers().getAggroMult().get()) * (getGame().getAreaLevel() + 1) * mult);
	}

	public int getKillXp() {
		if (stats.isMinion()) return 0;

		double mult = switch (getRarityClass()) {
			case MAGIC -> 1.5;
			case RARE -> 2.25;
			default -> 1;
		};

		if (getGame() != null) {
			mult *= (1 + getGame().getAreaLevel() / 10d) * Math.pow(1.1, getGame().getModifiers().size());
		}

		return (int) (stats.getKillXp() * mult);
	}

	public void load() {

	}
}
