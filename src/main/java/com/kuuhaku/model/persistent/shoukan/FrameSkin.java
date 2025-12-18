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

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.TitleLocked;
import com.kuuhaku.model.enums.Currency;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.converter.ColorConverter;
import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.kuuhaku.model.persistent.localized.LocalizedFrameSkin;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.user.Title;
import com.kuuhaku.util.Calc;
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
@Table(name = "frame_skin", schema = "shiro")
public class FrameSkin extends DAO<FrameSkin> implements TitleLocked {
	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "id", referencedColumnName = "id")
	@Fetch(FetchMode.SUBSELECT)
	private Set<LocalizedFrameSkin> infos = new HashSet<>();

	@Column(name = "price")
	private int price;

	@Enumerated(EnumType.STRING)
	@Column(name = "currency")
	private Currency currency;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "titles", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray titles = new JSONArray();

	@Column(name = "is_legacy", nullable = false)
	private boolean legacy = false;

	@Column(name = "theme_color", length = 6)
	@Convert(converter = ColorConverter.class)
	private Color themeColor;

	@Column(name = "background_color", length = 6)
	@Convert(converter = ColorConverter.class)
	private Color backgroundColor;

	@Column(name = "primary_color", nullable = false, length = 6)
	@Convert(converter = ColorConverter.class)
	private Color primaryColor;

	@Column(name = "secondary_color", nullable = false, length = 6)
	@Convert(converter = ColorConverter.class)
	private Color secondaryColor;

	private transient List<Title> titleCache;
	private transient Color themeCache;

	public FrameSkin() {
	}

	public FrameSkin(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	public LocalizedFrameSkin getInfo(I18N locale) {
		return infos.parallelStream()
				.filter(ld -> ld.getLocale().is(locale))
				.map(ld -> ld.setUwu(locale.isUwu()))
				.findAny()
				.orElseGet(() -> new LocalizedFrameSkin(locale, id, id + ":" + locale, id + ":" + locale));
	}

	public BufferedImage getFront(boolean desc) {
		return IO.getResourceAsImage("shoukan/frames/front/" + id.toLowerCase() + (desc ? "" : "_nodesc") + ".png");
	}

	public BufferedImage getBack(Deck deck) {
		DeckStyling style = deck.getStyling();
		Card cover = deck.isCoverAllowed() ? style.getCover() : null;

		BufferedImage back = IO.getResourceAsImage("shoukan/frames/back/" + id.toLowerCase() + (cover != null ? "_t" : "") + ".png");
		assert back != null;

		BufferedImage canvas = new BufferedImage(back.getWidth(), back.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = canvas.createGraphics();

		if (cover != null) {
			g2d.drawImage(style.getCover().drawCardNoBorder(), 15, 16, 195, 318, null);
		}

		g2d.drawImage(back, 0, 0, null);

		return canvas;
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

	public boolean isLegacy() {
		return legacy;
	}

	public Color getThemeColor() {
		if (themeColor == null) {
			return themeCache = Graph.rotate(Color.ORANGE, Calc.rng(360));
		}

		return themeCache = themeColor;
	}

	public Color getBackgroundColor() {
		if (backgroundColor == null) {
			if (themeCache == null) getThemeColor();
			return Graph.rotate(themeCache, 90);
		}

		return backgroundColor;
	}

	public Color getPrimaryColor() {
		return primaryColor;
	}

	public Color getSecondaryColor() {
		return secondaryColor;
	}

	public int getPrice() {
		return price;
	}

	public Currency getCurrency() {
		return currency;
	}

	public Shape getBoundary() {
		if (legacy) {
			return new Rectangle(225, 350);
		}

		return new Polygon(
				new int[]{1, 14, 211, 224, 224, 211, 14, 1},
				new int[]{14, 1, 1, 14, 336, 349, 349, 336},
				8
		);
	}
}
