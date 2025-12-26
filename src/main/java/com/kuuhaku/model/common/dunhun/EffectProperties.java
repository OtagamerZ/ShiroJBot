package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.model.common.dunhun.context.EffectContext;
import com.kuuhaku.model.common.shoukan.ValueMod;

import java.lang.reflect.Field;
import java.util.Objects;

public class EffectProperties<T> {
	private final EffectContext<T> owner;
	private ValueMod maxHp;
	private ValueMod maxAp;
	private ValueMod initiative;
	private ValueMod critical;
	private ValueMod spellDamage;
	private ValueMod aggro;
	private ValueMod magicFind;
	private ValueMod healing;
	private ValueMod damageTaken;
	private EffectBase effect;

	private final Field[] fieldCache = getClass().getDeclaredFields();

	public EffectProperties(EffectContext<T> owner) {
		this.owner = owner;
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

	public boolean isSafeToRemove() {
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

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		EffectProperties<?> that = (EffectProperties<?>) o;
		return Objects.equals(owner, that.owner);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(owner);
	}
}
