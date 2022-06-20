/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.model.enums.FrameColor;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.shoukan.Origin;
import com.kuuhaku.utils.Graph;
import com.kuuhaku.utils.IO;
import org.apache.commons.collections4.bag.TreeBag;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.jdesktop.swingx.graphics.BlendComposite;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.RadarChart;

import javax.persistence.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "deck")
public class Deck extends DAO {
	@Transient
	public static final Deck INSTANCE = new Deck();

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private int id;

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "account_uid")
	@Fetch(FetchMode.JOIN)
	private Account account;

	@Column(name = "current", nullable = false)
	private boolean current;

	@ManyToMany
	@JoinTable(name = "deck_senshi",
			joinColumns = @JoinColumn(name = "deck_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "senshi_card_id", referencedColumnName = "card_id"))
	@Fetch(FetchMode.SUBSELECT)
	private List<Senshi> senshi = new ArrayList<>();

	@ManyToMany
	@JoinTable(name = "deck_evogear",
			joinColumns = @JoinColumn(name = "deck_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "evogear_card_id", referencedColumnName = "card_id"))
	@Fetch(FetchMode.SUBSELECT)
	private List<Evogear> evogear = new ArrayList<>();

	@ManyToMany
	@JoinTable(name = "deck_field",
			joinColumns = @JoinColumn(name = "deck_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "field_card_id", referencedColumnName = "card_id"))
	@Fetch(FetchMode.SUBSELECT)
	private List<Field> field = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	@Column(name = "frame", nullable = false)
	private FrameColor frame = FrameColor.PINK;

	@ManyToOne
	@JoinColumn(name = "cover_id")
	@Fetch(FetchMode.JOIN)
	private Card cover;

	@Column(name = "use_foil", nullable = false)
	private boolean useFoil;

	public Deck() {
	}

	public Deck(Account account) {
		this.account = account;
	}

	public int getId() {
		return id;
	}

	public Account getAccount() {
		return account;
	}

	public boolean isCurrent() {
		return current;
	}

	public void setCurrent(boolean current) {
		this.current = current;
	}

	public List<Senshi> getSenshi() {
		return senshi;
	}

	public List<Evogear> getEvogear() {
		return evogear;
	}

	public List<Field> getField() {
		return field;
	}

	public FrameColor getFrame() {
		return frame;
	}

	public void setFrame(FrameColor frame) {
		this.frame = frame;
	}

	public Card getCover() {
		return cover;
	}

	public void setCover(Card cover) {
		this.cover = cover;
	}

	public boolean isUsingFoil() {
		return useFoil;
	}

	public void setUseFoil(boolean useFoil) {
		this.useFoil = useFoil;
	}

	public BufferedImage render(I18N locale) {
		BufferedImage bi = IO.toColorSpace(Objects.requireNonNull(IO.getResourceAsImage("shoukan/deck.png")), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);

		Graph.applyTransformed(g2d, g -> {
			g.setColor(frame.getThemeColor());
			g.setComposite(BlendComposite.Color);
			g.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		});

		RadarChart rc = new RadarChart(600, 500);
		rc.setRadiiLabels(new String[]{
				locale.get("str/attack"),
				locale.get("str/defense"),
				locale.get("str/mana_sustain"),
				locale.get("str/hp_sustain"),
				locale.get("str/control"),
				locale.get("str/divergence")
		});
		rc.addSeries("A", new double[]{0.5, 0.6, 0.3, 0.2, 0.8, 1});

		rc.getStyler()
				.setLegendVisible(false)
				.setChartTitleVisible(false)
				.setPlotBorderVisible(false)
				.setChartTitleBoxVisible(false)
				.setChartBackgroundColor(new Color(0, 0, 0, 0))
				.setPlotBackgroundColor(new Color(0, 0, 0, 0))
				.setSeriesColors(new Color[]{Graph.withOpacity(frame.getThemeColor(), 0.5f)})
				.setChartFontColor(Color.white);

		g2d.drawImage(BitmapEncoder.getBufferedImage(rc), 0, 0, null);

		Graph.applyTransformed(g2d, 1212, 14, g -> {
			int x = 0;
			int y = 0;
			for (Senshi s : senshi) {
				g.drawImage(s.render(locale, this), 120 * (x++ % 9), 182 * (y++ / 9), 113, 175, null);
			}
		});

		Graph.applyTransformed(g2d, 1571, 768, g -> {
			int x = 0;
			int y = 0;
			for (Evogear e : evogear) {
				g.drawImage(e.render(locale, this), 120 * (x++ % 6), 182 * (y++ / 6), 113, 175, null);
			}
		});

		g2d.dispose();

		return bi;
	}

	public Origin getOrigins() {
		TreeBag<Race> races = new TreeBag<>();
		for (Senshi s : senshi) {
			races.addAll(
					Arrays.stream(s.getRace().split())
							.filter(r -> r != Race.NONE)
							.toList()
			);
		}

		List<Race> out = races.stream().limit(2).collect(Collectors.toList());
		while (out.size() < 2) {
			out.add(Race.NONE);
		}

		return new Origin(out);
	}
}
