package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.model.common.dunhun.context.EffectContext;
import com.kuuhaku.model.common.shoukan.ValueMod;

import java.lang.reflect.Field;
import java.util.Objects;

public class UniqueProperties<T> extends EffectProperties<T> {
	private final Object identifier;

	public UniqueProperties(EffectContext<T> owner) {
		this(owner, owner);
	}

	public UniqueProperties(EffectContext<T> owner, int duration) {
		this(owner, owner, duration);
	}

	public UniqueProperties(Object identifier, EffectContext<T> owner) {
		super(owner);
		this.identifier = identifier;
	}

	public UniqueProperties(Object identifier, EffectContext<T> owner, int duration) {
		super(owner, duration);
		this.identifier = identifier;
	}

	public Object getIdentifier() {
		return identifier;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		UniqueProperties<?> that = (UniqueProperties<?>) o;
		return Objects.equals(identifier, that.identifier);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(identifier);
	}

	@Override
	public UniqueProperties<T> copyWithOwner(EffectContext<T> source, Actor<?> owner) {
		try {
			UniqueProperties<T> clone = new UniqueProperties<>(identifier, source, getDuration());
			clone.setPriority(getPriority());
			if (getEffect() != null) {
				clone.setEffect(switch (getEffect()) {
					case PersistentEffect e -> new PersistentEffect(source, owner, e.getEffect());
					case TriggeredEffect e ->
							new TriggeredEffect(source, owner, e.getLimit(), e.getEffect(), e.getTriggers());
					default -> null;
				});
			}

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
