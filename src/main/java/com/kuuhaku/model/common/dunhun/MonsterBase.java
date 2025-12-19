package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.Team;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.dunhun.MonsterStats;
import com.kuuhaku.model.persistent.localized.LocalizedMonster;
import com.kuuhaku.model.records.dunhun.Loot;
import com.kuuhaku.util.Calc;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.CascadeType.ALL;

@MappedSuperclass
public abstract class MonsterBase<T extends MonsterBase<T>> extends Actor<T> {
	public static final double[] hpTable = new double[MAX_LEVEL];
	public static final double[] statTable = new double[MAX_LEVEL];

	static {
		for (int i = 0; i < MAX_LEVEL; i++) {
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
				.findAny()
				.orElseGet(() -> new LocalizedMonster(locale, getId(), getId() + ":" + locale));
	}

	@Override
	public Race getRace() {
		return stats.getRace();
	}

	@Override
	public int getMaxAp() {
		int flat = getStats().getMaxAp() + getGame().getAreaLevel() / 5;
		if (getGame().getPartySize() > 1 && getTeam() == Team.KEEPERS) {
			flat += getGame().getPartySize() / 2;
		}

		return (int) Calc.clamp( getModifiers().getMaxAp().apply(flat), 1, getApCap() + flat);
	}

	@Override
	public int getApCap() {
		return (int) getModifiers().getMaxAp().apply(5 + getStats().getMaxAp());
	}

	@Override
	public int getInitiative() {
		int flat = stats.getInitiative() * getGame().getAreaLevel() / 3;

		return (int) Math.max(1, getModifiers().getInitiative().apply(flat));
	}

	@Override
	public double getCritical() {
		return getModifiers().getCritical().apply(5);
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

		return (int) Math.max(1, getModifiers().getAggro().apply(flat) * (getGame().getAreaLevel() + 1) * mult);
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

	public Loot generateLoot() {
		return stats.generateLoot(this);
	}

	public void load() {

	}
}
