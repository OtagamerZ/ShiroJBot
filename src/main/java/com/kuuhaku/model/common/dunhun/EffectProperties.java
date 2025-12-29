package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.model.common.dunhun.context.EffectContext;
import com.kuuhaku.model.common.shoukan.ValueMod;

import java.lang.reflect.Field;

public class EffectProperties<T> {
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
	private EffectBase effect;

	public static final Field[] fieldCache = EffectProperties.class.getDeclaredFields();
	private int priority;
	private int duration;

	public EffectProperties(EffectContext<T> owner) {
		this(owner, -1);
	}

	public EffectProperties(EffectContext<T> owner, int duration) {
		this.owner = owner;
		this.duration = duration;
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

	public EffectBase getEffect() {
		return effect;
	}

	public void setEffect(EffectBase effect) {
		this.effect = effect;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public int getDuration() {
		return duration;
	}

	public boolean isSafeToRemove() {
		if (duration > 0) duration--;
		if (duration == 0) return true;

		boolean safe = true;
		for (Field f : fieldCache) {
			try {
				if (f.get(this) instanceof ValueMod v) {
					v.decExpiration();

					if (v.isExpired()) f.set(this, null);
					else safe = false;
				}
			} catch (IllegalAccessException ignore) {
			}
		}

		if (effect != null && effect.isClosed()) {
			effect = null;
			safe = false;
		}

		return safe;
	}

	public EffectProperties<T> copyWithOwner(EffectContext<T> source, Actor<?> owner) {
		try {
			EffectProperties<T> clone = new EffectProperties<>(source, duration);
			clone.priority = priority;
			clone.effect = switch (effect) {
				case PersistentEffect e -> new PersistentEffect(source, owner, e.getEffect());
				case TriggeredEffect e ->
						new TriggeredEffect(source, owner, e.getLimit(), e.getEffect(), e.getTriggers());
				default -> null;
			};

			for (Field field : fieldCache) {
				if (field.get(this) instanceof ValueMod v) {
					field.set(clone, v.copy());
				}
			}

			return clone;
		} catch (IllegalAccessException e) {
			throw new AssertionError(e);
		}
	}
}
