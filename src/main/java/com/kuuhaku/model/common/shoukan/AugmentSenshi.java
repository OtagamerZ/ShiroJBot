/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.model.common.shoukan;

import com.kuuhaku.interfaces.shoukan.Proxy;
import com.kuuhaku.model.enums.shoukan.Charm;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.shoukan.Senshi;

import java.util.Objects;

public class AugmentSenshi extends Senshi implements Proxy<Senshi> {
	private final Senshi original;

	public AugmentSenshi(Senshi aug, Senshi original) {
		super(aug.getId(), aug.getCard(), aug.getRace(), aug.getBase());

		this.original = original;
		while (!aug.getEquipments().isEmpty()) {
			getEquipments().add(aug.getEquipments().removeFirst());
		}
	}

	@Override
	public Senshi getOriginal() {
		return original;
	}

	@Override
	public Race getRace() {
		return original.getRace().fuse(super.getRace());
	}

	@Override
	public int getDmg() {
		return super.getDmg() + original.getDmg();
	}

	@Override
	public int getDfs() {
		return super.getDfs() + original.getDfs();
	}

	@Override
	public int getDodge() {
		return super.getDodge() + original.getDodge();
	}

	@Override
	public int getBlock() {
		return super.getBlock() + original.getBlock();
	}

	@Override
	public boolean hasCharm(Charm charm, boolean pop) {
		return super.hasCharm(charm, pop) || original.hasCharm(charm, pop);
	}

	public void destroy() {
		getHand().getGraveyard().addAll(getEquipments());
		replace(original);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		AugmentSenshi that = (AugmentSenshi) o;
		return Objects.equals(original, that.original);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), original);
	}
}
