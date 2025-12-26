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

	private transient Actor<?> master;

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
	public int getLevel() {
		if (isMinion()) {
			return master.getLevel();
		}

		return getGame().getAreaLevel();
	}

	@Override
	public int getMaxAp() {
		int flat = 1 + getStats().getMaxAp() + getLevel() / 5;
		if (getGame().getPartySize() > 1 && getTeam() == Team.KEEPERS) {
			flat += getGame().getPartySize() / 2;
		}

		return (int) Calc.clamp(getModifiers().getMaxAp(flat), 1, getApCap());
	}

	@Override
	public int getApCap() {
		return (int) getModifiers().getMaxAp(4 + getStats().getMaxAp());
	}

	@Override
	public int getInitiative() {
		int flat = stats.getInitiative() * getLevel() / 3;

		return (int) Math.max(1, getModifiers().getInitiative(flat));
	}

	@Override
	public double getCritical() {
		return getModifiers().getCritical(5);
	}

	@Override
	public int getThreatScore() {
		int flat = getSenshi().getDmg() / 10 + getSenshi().getDfs() / 20 + getHp() / 150;
		double mult = switch (getRarityClass()) {
			case NORMAL -> 1;
			case MAGIC -> 1.5;
			case RARE -> 2.25;
			case UNIQUE -> 10;
		};

		return (int) Math.max(1, getModifiers().getAggro(flat * (getLevel() + 1) * mult));
	}

	public int getKillXp() {
		if (isMinion()) return 0;

		double mult = switch (getRarityClass()) {
			case MAGIC -> 1.5;
			case RARE -> 2.25;
			default -> 1;
		};

		if (getGame() != null) {
			mult *= (1 + getLevel() / 10d) * Math.pow(1.1, getGame().getModifiers().size());
		}

		return (int) (stats.getKillXp() * mult);
	}

	public Loot generateLoot() {
		return stats.generateLoot(this);
	}

	public Actor<?> getMaster() {
		return master;
	}

	public void setMaster(Actor<?> master) {
		if (master instanceof MonsterBase<?> m) {
			if (m.getMaster().equals(this)) {
				m.master = null;
			}
		}

		this.master = master;
		if (master != null) {
			Combat cbt = master.getGame().getCombat();

			//noinspection SizeReplaceableByIsEmpty
			while (master.getMinions().size() >= Actor.MAX_SUMMONS) {
				Actor<?> old = master.getMinions().removeFirst();
				if (cbt != null) {
					cbt.getActors(old.getTeam()).remove(old);
				}
			}

			master.getMinions().add(this);
		}
	}

	public boolean isMinion() {
		return master != null;
	}

	public void load() {

	}
}
