package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.AffixType;
import com.kuuhaku.model.persistent.dunhun.Affix;
import com.kuuhaku.model.persistent.dunhun.Monster;
import com.kuuhaku.model.persistent.dunhun.MonsterStats;
import com.kuuhaku.model.persistent.localized.LocalizedMonster;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SenshiActor extends Monster {
	private final Senshi parent;

	public SenshiActor(Senshi parent) {
		super(parent.getId());

		this.parent = parent;
		stats = new MonsterStats(
				(int) (switch (parent.getCard().getRarity()) {
					case UNCOMMON -> 400;
					case RARE -> 500;
					case EPIC -> 725;
					case LEGENDARY -> 850;
					default -> 350;
				} * Calc.rng(0.9, 1.25, getId().hashCode())),
				parent.getRace(),
				(int) (parent.getDmg() * 0.60),
				(int) (parent.getDfs() * 0.60),
				parent.getDodge(),
				parent.getParry(),
				switch (parent.getCard().getRarity()) {
					case EPIC, LEGENDARY -> 1;
					default -> 0;
				},
				switch (parent.getCard().getRarity()) {
					case EPIC, LEGENDARY -> 4;
					default -> 0;
				},
				switch (parent.getCard().getRarity()) {
					case RARE, EPIC -> 10;
					case LEGENDARY -> 12;
					default -> 8;
				}
		);


		List<String> skills;
		if (stats.getAttack() > 0) {
			skills = DAO.queryAllNative(String.class, "SELECT id FROM skill WHERE req_attributes <> -1");
		} else {
			skills = DAO.queryAllNative(String.class, "SELECT id FROM skill WHERE req_attributes <> -1 AND spell");
		}

		stats.getSkills().addAll(Utils.getRandomN(skills, 3, 1, getId().hashCode()));
	}

	@Override
	public LocalizedMonster getInfo(I18N locale) {
		return new LocalizedMonster(I18N.EN, parent.getId(), parent.getCard().getName());
	}

	@Override
	public Card getCard() {
		return parent.getCard();
	}

	@Override
	public Set<Affix> getAffixes() {
		Set<Affix> affs = super.getAffixes();

		Set<AffixType> type = new HashSet<>();
		affs.removeIf(a -> {
			if (type.contains(a.getType())) {
				return true;
			}

			type.add(a.getType());
			return false;
		});

		return affs;
	}
}
