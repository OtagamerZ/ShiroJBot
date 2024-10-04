package com.kuuhaku.interfaces.dunhun;

import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.Team;
import com.kuuhaku.model.persistent.dunhun.Skill;
import com.kuuhaku.model.persistent.shoukan.Senshi;

import java.awt.image.BufferedImage;
import java.util.List;

public interface Actor {
	String getName(I18N locale);

	int getHp();
	int getMaxHp();
	void modHp(int value);

	int getAp();
	int getMaxAp();
	void modAp(int value);

	int getInitiative();
	boolean hasFleed();
	void setFleed(boolean flee);

	Senshi asSenshi(I18N locale);
	BufferedImage render(I18N locale);

	List<Skill> getSkills();

	Team getTeam();
	void setTeam(Team team);
}
