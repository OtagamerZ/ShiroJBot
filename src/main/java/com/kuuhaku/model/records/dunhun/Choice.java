package com.kuuhaku.model.records.dunhun;

import com.github.ygimenez.model.ButtonWrapper;
import com.github.ygimenez.model.ThrowingFunction;

import java.util.Objects;

public record Choice(String id, Object label, ThrowingFunction<ButtonWrapper, String> action) {
	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		Choice choice = (Choice) o;
		return Objects.equals(id, choice.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
