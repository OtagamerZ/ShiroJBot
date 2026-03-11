package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.model.common.dunhun.context.EffectContext;
import com.kuuhaku.model.common.shoukan.ValueMod;
import com.kuuhaku.model.enums.shoukan.ElementType;
import com.kuuhaku.model.persistent.dunhun.Skill;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class EffectProperties<T> {
	private final long SERIAL = ThreadLocalRandom.current().nextLong();

	private final EffectContext<T> owner;
	private ValueMod maxHp;
	private ValueMod maxAp;
	private ValueMod damage;
	private ValueMod defense;
	private ValueMod dodge;
	private ValueMod parry;
	private ValueMod power;
	private ValueMod initiative;
	private ValueMod critical;
	private ValueMod spellDamage;
	private ValueMod aggro;
	private ValueMod magicFind;
	private ValueMod healing;
	private ValueMod damageTaken;
	private ValueMod degenResist;
	private ValueMod maxSummons;
	private EffectBase effect;
	private ElementType resist;
	private Skill skill;

	public static final Field[] fieldCache = EffectProperties.class.getDeclaredFields();
	private int priority;
	private int expiration;
	private String icon;

	public EffectProperties(EffectContext<T> owner) {
		this(owner, -1);
	}

	public EffectProperties(EffectContext<T> owner, int expiration) {
		this.owner = owner;
		this.expiration = expiration;
	}

	public EffectContext<T> getOwner() {
		return owner;
	}

	public ValueMod getMaxHp() {
		return maxHp;
	}

	public void setMaxHp(ValueMod maxHp) {
		this.maxHp = maxHp;
	}

	public ValueMod getMaxAp() {
		return maxAp;
	}

	public void setMaxAp(ValueMod maxAp) {
		this.maxAp = maxAp;
	}

	public ValueMod getDamage() {
		return damage;
	}

	public void setDamage(ValueMod damage) {
		this.damage = damage;
	}

	public ValueMod getDefense() {
		return defense;
	}

	public void setDefense(ValueMod defense) {
		this.defense = defense;
	}

	public ValueMod getDodge() {
		return dodge;
	}

	public void setDodge(ValueMod dodge) {
		this.dodge = dodge;
	}

	public ValueMod getParry() {
		return parry;
	}

	public void setParry(ValueMod parry) {
		this.parry = parry;
	}

	public ValueMod getPower() {
		return power;
	}

	public void setPower(ValueMod power) {
		this.power = power;
	}

	public ValueMod getInitiative() {
		return initiative;
	}

	public void setInitiative(ValueMod initiative) {
		this.initiative = initiative;
	}

	public ValueMod getCritical() {
		return critical;
	}

	public void setCritical(ValueMod critical) {
		this.critical = critical;
	}

	public ValueMod getSpellDamage() {
		return spellDamage;
	}

	public void setSpellDamage(ValueMod spellDamage) {
		this.spellDamage = spellDamage;
	}

	public ValueMod getAggro() {
		return aggro;
	}

	public void setAggro(ValueMod aggro) {
		this.aggro = aggro;
	}

	public ValueMod getMagicFind() {
		return magicFind;
	}

	public void setMagicFind(ValueMod magicFind) {
		this.magicFind = magicFind;
	}

	public ValueMod getHealing() {
		return healing;
	}

	public void setHealing(ValueMod healing) {
		this.healing = healing;
	}

	public ValueMod getDamageTaken() {
		return damageTaken;
	}

	public void setDamageTaken(ValueMod damageTaken) {
		this.damageTaken = damageTaken;
	}

	public ValueMod getDegenResist() {
		return degenResist;
	}

	public void setDegenResist(ValueMod degenResist) {
		this.degenResist = degenResist;
	}

	public ValueMod getMaxSummons() {
		return maxSummons;
	}

	public void setMaxSummons(ValueMod maxSummons) {
		this.maxSummons = maxSummons;
	}

	public EffectBase getEffect() {
		return effect;
	}

	public void setEffect(EffectBase effect) {
		this.effect = effect;
	}

	public ElementType getResist() {
		return resist;
	}

	public void setResist(ElementType resist) {
		this.resist = resist;
	}

	public Skill getSkill() {
		return skill;
	}

	public void setSkill(Skill skill) {
		this.skill = skill;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public int getExpiration() {
		return expiration;
	}

	public void setExpiration(int expiration) {
		this.expiration = expiration;
	}

	public void decExpiration() {
		if (expiration > 0) {
			this.expiration--;
		}

		for (Field f : fieldCache) {
			try {
				if (f.get(this) instanceof ValueMod v) {
					v.decExpiration();
					if (v.getExpiration() == 0) f.set(this, null);
				}
			} catch (IllegalAccessException ignore) {
			}
		}
	}

	public boolean isSafeToRemove() {
		if (expiration == 0) return true;
		else if (effect != null && !effect.isClosed()) return false;
		else if (resist != null) return false;

		for (Field f : fieldCache) {
			try {
				if (f.get(this) instanceof ValueMod m) {
					if (m.isExpired()) {
						f.set(this, null);
						continue;
					}

					return false;
				}
			} catch (IllegalAccessException ignore) {
			}
		}

		return true;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		EffectProperties<?> that = (EffectProperties<?>) o;
		return SERIAL == that.SERIAL;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(SERIAL);
	}
}
