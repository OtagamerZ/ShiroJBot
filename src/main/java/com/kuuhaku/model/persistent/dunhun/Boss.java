package com.kuuhaku.model.persistent.dunhun;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.common.dunhun.Actor;
import com.kuuhaku.model.common.dunhun.Combat;
import com.kuuhaku.model.common.dunhun.MonsterBase;
import com.kuuhaku.model.common.dunhun.context.ActorContext;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Flag;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.util.Utils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.intellij.lang.annotations.Language;

import java.util.Map;

@Entity
@Table(name = "boss", schema = "dunhun")
public class Boss extends MonsterBase<Boss> {
	@Language("Groovy")
	@Column(name = "on_enrage", columnDefinition = "TEXT")
	private String onEnrage;

	private transient boolean enraged;

	public Boss() {
	}

	public Boss(String id) {
		super(id);
	}

	@Override
	public String getName(I18N locale) {
		return getInfo(locale).getName();
	}

	@Override
	public int getHp() {
		int hp = super.getHp();
		if (hp <= getMaxHp() / 2 && !enraged) {
			try {
				enraged = true;
				if (onEnrage != null) {
					Utils.exec(getId(), onEnrage, Map.of(
							"ctx", new ActorContext(this)
					));
				}

				Senshi s = getSenshi();
				s.setFlag(Flag.NO_SLEEP);
				s.setFlag(Flag.NO_PARALYSIS);
				s.setFlag(Flag.NO_STASIS);

				Combat comb = getGame().getCombat();
				comb.getHistory().add(getGame().getLocale().get("str/boss_enraged", getName()));
			} catch (Exception e) {
				Constants.LOGGER.warn("Failed to enrage {}", getId(), e);
			}
		}

		return hp;
	}

	@Override
	public void setHp(int value) {
		int half = getMaxHp() / 2;
		if (onEnrage != null && getHp() > half && value <= half && !enraged) {
			value = getMaxHp() / 2;
		}

		super.setHp(value);
	}

	@Override
	public int getMaxHp() {
		return (int) Math.max(1, stats.getBaseHp() * (1 + getGame().getAreaLevel() / 5d));
	}

	@Override
	public Senshi createSenshi() {
		Senshi senshi = super.createSenshi();
		senshi.setFlag(Flag.NO_PARALYSIS);
		senshi.setFlag(Flag.NO_SLEEP);
		senshi.setFlag(Flag.NO_STUN);
		senshi.setFlag(Flag.NO_STASIS);

		return senshi;
	}

	@Override
	public Actor<?> copy() {
		Boss clone = new Boss(getId());
		clone.stats = stats;
		clone.infos = infos;
		clone.onEnrage = onEnrage;
		clone.getModifiers().copyFrom(getModifiers());
		clone.getBinding().bind(getBinding());
		clone.setHp(getHp());
		clone.setAp(getAp());

		return clone;
	}

	public static Boss getRandom() {
		return DAO.query(Boss.class, "SELECT b FROM Boss b ORDER BY random()");
	}
}
