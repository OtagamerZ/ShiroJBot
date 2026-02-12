package com.kuuhaku.model.persistent.dunhun;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.dunhun.Usable;
import com.kuuhaku.model.common.dunhun.*;
import com.kuuhaku.model.common.dunhun.context.ActorContext;
import com.kuuhaku.model.common.shoukan.MultMod;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.util.Utils;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.intellij.lang.annotations.Language;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "boss", schema = "dunhun")
public class Boss extends MonsterBase<Boss> {
	@Language("Groovy")
	@Column(name = "on_start", columnDefinition = "TEXT")
	private String onStart;

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
	public void setHp(int value) {
		int half = getMaxHp() / 2;
		if (value <= half && !enraged && onEnrage != null) {
			value = half;

			try {
				enraged = true;
				Utils.exec(getId(), onEnrage, Map.of(
						"ctx", new ActorContext(this, null)
				));

				EffectProperties<?> props = new UniqueProperties<>("ENRAGE", null, 1);
				props.setDamageTaken(new MultMod(-1));
				getModifiers().addEffect(props);

				props = new PermanentProperties<>(null);
				props.setDamageTaken(new MultMod(-0.5));
				props.setMaxAp(new MultMod(0.5));
				getModifiers().addEffect(props);

				Combat comb = getGame().getCombat();
				int idx = comb.getTurns().indexOf(this);
				if (idx > -1) {
					comb.getCurrent().setAp(0);
					while (comb.getTurns().peekNext() != this) {
						comb.getTurns().getNext();
					}
				}

				comb.getHistory().add(getGame().getString("str/boss_enraged", getName()));
			} catch (Exception e) {
				Constants.LOGGER.warn("Failed to enrage {}", getId(), e);
			}
		}

		super.setHp(value);
	}

	@Override
	public int getMaxHp() {
		return (int) Math.max(1, stats.getBaseHp() * (1 + getLevel() / 5d));
	}

	@Override
	public void trigger(Trigger trigger, Actor<?> target, Usable usable, AtomicInteger value) {
		super.trigger(trigger, target, usable, value);
		if (trigger == Trigger.ON_COMBAT && onStart != null) {
			try {
				Utils.exec(getId(), onStart, Map.of(
						"ctx", new ActorContext(this, null)
				));
			} catch (Exception e) {
				Constants.LOGGER.warn("Failed to initialize {}", getId(), e);
			}
		}
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

	public static Boss getRandom(Node node) {
		List<String> bosses = DAO.queryAllNative(String.class, "SELECT id FROM boss");

		Set<String> pool = node.getEnemyPool();
		if (!pool.isEmpty()) {
			bosses.removeIf(a -> !pool.contains(a));
		}

		if (bosses.isEmpty()) return null;

		return DAO.find(Boss.class, Utils.getRandomEntry(node.getSeed(), bosses));
	}
}
