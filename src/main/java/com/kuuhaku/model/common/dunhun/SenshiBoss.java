package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.common.shoukan.MultMod;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.dunhun.Boss;
import com.kuuhaku.model.persistent.dunhun.MonsterStats;
import com.kuuhaku.model.persistent.localized.LocalizedMonster;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;

import java.util.List;

public class SenshiBoss extends Boss {
	private final Senshi parent;

	public SenshiBoss(Senshi parent) {
		super(parent.getId());

		this.parent = parent;
		stats = new MonsterStats(
				Calc.rng(1500, 2000, getId().hashCode()),
				parent.getRace(),
				(int) (parent.getDmg() * 0.60),
				(int) (parent.getDfs() * 0.60),
				parent.getDodge(),
				parent.getParry(),
				Calc.rng(2, 3, getId().hashCode()),
				Calc.rng(8, 12, getId().hashCode()),
				Calc.rng(18, 24, getId().hashCode())
		);


		List<String> skills;
		if (stats.getAttack() > 0) {
			skills = DAO.queryAllNative(String.class, "SELECT id FROM skill");
		} else {
			skills = DAO.queryAllNative(String.class, "SELECT id FROM skill WHERE spell");

			EffectProperties<?> props = new PermanentProperties<>(null);
			props.setPower(new MultMod(0.5));
			getModifiers().addEffect(props);
		}

		stats.getSkills().addAll(Utils.getRandomN(skills,  5, 1, getId().hashCode()));
	}

	@Override
	public LocalizedMonster getInfo(I18N locale) {
		return new LocalizedMonster(I18N.EN, parent.getId(), parent.getCard().getName());
	}

	@Override
	public Card getCard() {
		return parent.getCard();
	}
}
