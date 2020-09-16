/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent;

import com.kuuhaku.model.enums.KawaiponRarity;

import javax.persistence.*;
import java.awt.*;

@Entity
@Table(name = "raritycolors")
public class RarityColors {
	@Id
	@Enumerated(EnumType.STRING)
	private KawaiponRarity rarity;

	@Column(columnDefinition = "CHAR(6) NOT NULL DEFAULT 'FFFFFF'")
	private String primaryColor = "FFFFFF";

	@Column(columnDefinition = "CHAR(6) NOT NULL DEFAULT '000000'")
	private String secondaryColor = "000000";

	public KawaiponRarity getRarity() {
		return rarity;
	}

	public Color getPrimary() {
		return Color.decode("#" + primaryColor);
	}

	public Color getSecondary() {
		return Color.decode("#" + secondaryColor);
	}

	public void setPrimaryColor(String primaryColor) {
		this.primaryColor = primaryColor;
	}

	public void setSecondaryColor(String secondaryColor) {
		this.secondaryColor = secondaryColor;
	}
}
