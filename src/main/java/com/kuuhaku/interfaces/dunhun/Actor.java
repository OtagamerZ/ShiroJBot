package com.kuuhaku.interfaces.dunhun;

import com.kuuhaku.game.Dunhun;
import com.kuuhaku.model.common.dunhun.ActorModifiers;
import com.kuuhaku.model.common.dunhun.Combat;
import com.kuuhaku.model.common.shoukan.RegDeg;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.Team;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.dunhun.Skill;
import com.kuuhaku.model.persistent.shoukan.Senshi;

import java.awt.image.BufferedImage;
import java.util.List;

public interface Actor {
	String getId();

	String getName(I18N locale);

	Race getRace();

	int getHp();

	int getMaxHp();

	int getHpDelta();

	default void modHp(int value) {
		modHp(value, false);
	}

	default void modHp(int value, boolean crit) {
		if (value == 0 || getHp() == 0) return;
		if (crit) value *= 2;

		Dunhun game = getGame();
		if (game != null && value < 0) {
			Senshi sen = asSenshi(game.getLocale());
			value = -value;

			if (sen.isDefending()) {
				value = (int) -Math.max(value / 10f, (2.5 * Math.pow(value, 2)) / (sen.getDfs() + 2.5 * value));
			} else {
				value = (int) -Math.max(value / 5f, (5 * Math.pow(value, 2)) / (sen.getDfs() + 5 * value));
			}

			if (sen.isSleeping()) {
				sen.reduceSleep(999);
			}
		}

		int diff = getHp();
		setHp(getHp() + value);
		diff = getHp() - diff;

		if (diff == 0) return;

		if (game != null && game.getCombat() != null) {
			Combat comb = game.getCombat();
			if (value < 0) {
				comb.trigger(Trigger.ON_DAMAGE, this);
			} else {
				comb.trigger(Trigger.ON_HEAL, this);
			}

			I18N locale = game.getLocale();
			comb.getHistory().add(locale.get(diff < 0 ? "str/actor_damage" : "str/actor_heal",
					getName(locale), Math.abs(diff), crit ? ("**(" + locale.get("str/critical") + ")**") : ""
			));
		}
	}

	void setHp(int value);

	default void revive(int value) {
		if (getHp() >= value) return;

		setHp(value);
		Dunhun game = getGame();
		if (game != null) {
			asSenshi(game.getLocale()).setAvailable(true);
		}
	}

	int getAp();

	int getMaxAp();

	void modAp(int value);

	int getInitiative();

	double getCritical();

	int getAggroScore();

	boolean hasFleed();

	void setFleed(boolean flee);

	default boolean isSkipped() {
		return hasFleed() || getHp() <= 0;
	}

	ActorModifiers getModifiers();

	RegDeg getRegDeg();

	Senshi asSenshi(I18N locale);

	BufferedImage render(I18N locale);

	List<Skill> getSkills();

	default Skill getSkill(String id) {
		return getSkills().stream()
				.filter(skill -> skill.getId().equals(id))
				.findFirst()
				.orElse(null);
	}

	Team getTeam();

	void setTeam(Team team);

	Dunhun getGame();

	void setGame(Dunhun game);

	Actor fork() throws CloneNotSupportedException;

	default Actor copy() {
		try {
			return fork();
		} catch (Exception e) {
			return this;
		}
	}
}
