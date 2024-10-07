package com.kuuhaku.interfaces.dunhun;

import com.kuuhaku.model.common.dunhun.ActorModifiers;
import com.kuuhaku.model.common.shoukan.RegDeg;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.Team;
import com.kuuhaku.model.enums.shoukan.Race;
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
	void modHp(int value);
	void revive(int hp);

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

	Actor fork() throws CloneNotSupportedException;

	default Actor copy() {
		try {
			return fork();
		} catch (Exception e) {
			return this;
		}
	}
}
