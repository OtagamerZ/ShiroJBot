/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2024  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent.shoukan;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.Shoukan;
import com.kuuhaku.interfaces.TitleLocked;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.kuuhaku.model.persistent.localized.LocalizedSlotSkin;
import com.kuuhaku.model.persistent.user.Title;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.ygimenez.json.JSONArray;
import jakarta.persistence.*;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;
import org.hibernate.type.SqlTypes;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "slot_skin", schema = "shiro")
public class SlotSkin extends DAO<SlotSkin> implements TitleLocked {
	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "id", referencedColumnName = "id")
	@Fetch(FetchMode.SUBSELECT)
	private Set<LocalizedSlotSkin> infos = new HashSet<>();

	@Column(name = "price")
	private int price;

	@Enumerated(EnumType.STRING)
	@Column(name = "currency")
	private Currency currency;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "titles", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray titles = new JSONArray();

	private transient List<Title> titleCache;

	public SlotSkin() {
	}

	public SlotSkin(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	public LocalizedSlotSkin getInfo(I18N locale) {
		return infos.parallelStream()
				.filter(ld -> ld.getLocale().is(locale))
				.map(ld -> ld.setUwu(locale.isUwu()))
				.findAny().orElseThrow();
	}

	public BufferedImage getImage(Side side, boolean legacy) {
		String s = side.name().toLowerCase();
		BufferedImage overlay = IO.getResourceAsImage("shoukan/overlay/" + s + (legacy ? "_legacy" : "") + ".png");

		BufferedImage bi = new BufferedImage(overlay.getWidth(), overlay.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.SD_HINTS);

		BufferedImage theme = IO.getImage(Shoukan.SKIN_PATH + id.toLowerCase() + "_" + s + ".png");
		Graph.applyMask(theme, IO.getResourceAsImage("shoukan/mask/slot_" + s + (legacy ? "_legacy" : "") + "_mask.png"), 0);

		g2d.drawImage(theme, 5, 5, null);
		g2d.drawImage(overlay, 0, 0, null);

		g2d.dispose();

		return bi;
	}

	@Override
	public List<Title> getTitles() {
		if (titleCache != null) return titleCache;

		List<Title> out = new ArrayList<>();
		if (titles != null) {
			for (Object title : titles) {
				if (title instanceof String s) {
					Title t = DAO.find(Title.class, s);
					if (t != null) {
						out.add(t);
					}
				}
			}
		}

		return titleCache = out;
	}

	public int getPrice() {
		return price;
	}

	public Currency getCurrency() {
		return currency;
	}
}
