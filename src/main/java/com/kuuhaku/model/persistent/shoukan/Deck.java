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
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.FrameColor;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.shoukan.BaseValues;
import com.kuuhaku.model.records.shoukan.Origin;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONArray;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.collections4.bag.TreeBag;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.jdesktop.swingx.graphics.BlendComposite;
import org.knowm.xchart.RadarChart;

import jakarta.persistence.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "deck")
public class Deck extends DAO<Deck> {
	@Transient
	public static final Deck INSTANCE = new Deck();

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private int id;

	@Column(name = "index", nullable = false)
	private int index;

	@Column(name = "name")
	private String name;

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "account_uid")
	@Fetch(FetchMode.JOIN)
	private Account account;

	@Column(name = "current", nullable = false)
	private boolean current;

	@ManyToMany
	@OrderColumn(name = "index")
	@JoinTable(name = "deck_senshi",
			joinColumns = @JoinColumn(name = "deck_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "senshi_card_id", referencedColumnName = "card_id")
	)
	@Fetch(FetchMode.SUBSELECT)
	private List<Senshi> senshi = new ArrayList<>();

	@ManyToMany
	@OrderColumn(name = "index")
	@JoinTable(name = "deck_evogear",
			joinColumns = @JoinColumn(name = "deck_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "evogear_card_id", referencedColumnName = "card_id")
	)
	@Fetch(FetchMode.SUBSELECT)
	private List<Evogear> evogear = new ArrayList<>();

	@ManyToMany
	@OrderColumn(name = "index")
	@JoinTable(name = "deck_field",
			joinColumns = @JoinColumn(name = "deck_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "field_card_id", referencedColumnName = "card_id")
	)
	@Fetch(FetchMode.SUBSELECT)
	private List<Field> field = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	@Column(name = "frame", nullable = false)
	private FrameColor frame = FrameColor.PINK;

	@ManyToOne
	@JoinColumn(name = "cover_id")
	@Fetch(FetchMode.JOIN)
	private Card cover;

	@Column(name = "use_chrome", nullable = false)
	private boolean useChrome;

	public Deck() {
	}

	public Deck(Account account) {
		this.account = account;
	}

	public int getId() {
		return id;
	}

	public int getIndex() {
		return index;
	}

	public String getName() {
		return Utils.getOr(name, "deck " + index);
	}

	public void setName(String name) {
		this.name = name;
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

	public int getMaxSenshiCopies() {
		int allowed = 3;
		if (getOrigins().minor() == Race.BEAST) {
			allowed++;
		}

		return allowed;
	}

	public boolean validateSenshi() {
		int allowed = getMaxSenshiCopies();
		HashBag<Senshi> bag = new HashBag<>(senshi);
		bag.removeIf(s -> bag.getCount(s) <= allowed);

		return bag.isEmpty() && Utils.between(senshi.size(), 30, 37);
	}

	public int countRace(Race race) {
		return (int) senshi.stream()
				.filter(s -> s.getRace() == race)
				.count();
	}

	public List<Evogear> getEvogear() {
		return evogear;
	}

	public int getMaxEvogearCopies(int tier) {
		int allowed = tier == 4 ? 1 : 3;
		if (getOrigins().major() == Race.BEAST) {
			allowed *= 2;
		}

		return allowed;
	}

	public boolean validateEvogear() {
		HashBag<Evogear> bag = new HashBag<>(evogear);
		bag.removeIf(e -> bag.getCount(e) <= getMaxEvogearCopies(e.getTier()));

		return bag.isEmpty() && Utils.between(evogear.size(), 0, 25);
	}

	public int getEvoWeight() {
		return evogear.stream()
				.mapToInt(e -> {
					int i = e.getTier() + (-1 + e.getCharms().size());
					if (!e.isSpell() && getOrigins().major() == Race.MACHINE) {
						return i - 1;
					} else if (e.isSpell() && getOrigins().major() == Race.MYSTICAL) {
						return i - 1;
					}

					return i;
				})
				.map(i -> Math.max(0, i))
				.sum();
	}

	public List<Field> getFields() {
		return field;
	}

	public boolean validateFields() {
		return Utils.between(field.size(), 0, 4);
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

	public boolean isUsingChrome() {
		return useChrome;
	}

	public void setUseChrome(boolean useChrome) {
		this.useChrome = useChrome;
	}

	public double getMetaDivergence() {
		// TODO Divegence
		return 1;
	}

	public BufferedImage render(I18N locale) {
		BufferedImage bi = IO.getResourceAsImage("shoukan/deck.webp");
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);

		Graph.applyTransformed(g2d, g -> {
			g.setColor(frame.getThemeColor());
			g.setComposite(BlendComposite.Color);
			g.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		});
		Graph.applyMask(bi, IO.getResourceAsImage("shoukan/deck_mask.webp"), 0, true);

		List<Drawable<?>> allCards = new ArrayList<>() {{
			addAll(senshi);
			addAll(evogear);
			addAll(field);
		}};
		AtomicInteger totalMPCost = new AtomicInteger();
		AtomicInteger totalHPCost = new AtomicInteger();
		AtomicInteger totalDmg = new AtomicInteger();
		AtomicInteger totalDef = new AtomicInteger();

		BaseValues base = getBaseValues();
		double avgMana = Calc.average((double) base.mpGain().apply(1), base.mpGain().apply(5), base.mpGain().apply(10));
		int weight = Calc.prcntToInt(getEvoWeight(), 24);
		String color = "FFFFFF";
		if (weight > 150) color = "FF0000";
		else if (weight > 100) color = "FFFF00";

		for (Drawable<?> d : allCards) {
			totalMPCost.addAndGet(d.getMPCost());
			totalHPCost.addAndGet(d.getHPCost());
			totalDmg.addAndGet(d.getDmg());
			totalDef.addAndGet(d.getDef());
		}

		double[] vals = Calc.clamp(new double[]{
				Calc.prcnt(totalDmg.get(), (totalDmg.get() + totalDef.get()) / 1.5),
				Calc.prcnt(totalDef.get(), (totalDmg.get() + totalDef.get()) / 1.5),
				((double) totalMPCost.get() / allCards.size()) / avgMana,
				Calc.prcnt(Set.copyOf(allCards).size(), allCards.size()),
				getMetaDivergence(),
				0
		}, 0, 1);
		vals[5] = Calc.average(vals[0], vals[1], vals[2], 0.5 + vals[3] * 0.5, 0.25 + vals[4] * 0.75);

		RadarChart rc = new RadarChart(600, 500);
		rc.setRadiiLabels(new String[]{
				locale.get("str/attack"),
				locale.get("str/defense"),
				locale.get("str/mp_cost"),
				locale.get("str/variety"),
				locale.get("str/divergence"),
				locale.get("str/overall_score")
		});
		rc.addSeries("A", vals);

		rc.getStyler()
				.setLegendVisible(false)
				.setChartTitleVisible(false)
				.setPlotBorderVisible(false)
				.setChartTitleBoxVisible(false)
				.setChartBackgroundColor(new Color(0, 0, 0, 0))
				.setPlotBackgroundColor(new Color(0, 0, 0, 0))
				.setSeriesColors(new Color[]{Graph.withOpacity(frame.getThemeColor(), 0.5f)})
				.setChartFontColor(Color.white);

		rc.paint(g2d, rc.getWidth(), rc.getHeight());

		g2d.setFont(Fonts.OPEN_SANS.deriveFont(Font.PLAIN, 30));
		g2d.setColor(Color.WHITE);
		Graph.drawMultilineString(g2d, locale.get("str/deck_analysis"), 600, 45, 400);
		Graph.drawMultilineString(g2d, """
						%s
						%s
						%s-(%s/%s/%s)
						{%s%%;0x%s}
						%s
						%s
						%s
						%s
												
						%s
						%s-(T4:-%s)
						%s
						""".formatted(
						base.hp(),
						Utils.roundToString(avgMana, 1),
						allCards.size(), senshi.size(), evogear.size(), field.size(),
						weight, color,
						Utils.roundToString((float) totalMPCost.get() / allCards.size(), 1),
						Utils.roundToString((float) totalHPCost.get() / allCards.size(), 1),
						Utils.roundToString((float) totalDmg.get() / allCards.size(), 1),
						Utils.roundToString((float) totalDef.get() / allCards.size(), 1),
						getMaxSenshiCopies(), getMaxEvogearCopies(1), getMaxEvogearCopies(4),
						3
				), 1175, 45, 175, 0,
				s -> {
					JSONArray values = Utils.extractGroups(s, "\\{(.+);(0x[\\da-fA-F]{6})}");
					if (values.isEmpty()) {
						g2d.setColor(Color.WHITE);
						return s;
					}

					g2d.setColor(Color.decode(values.getString(1)));
					return values.getString(0);
				},
				(s, x, y) -> {
					FontMetrics m = g2d.getFontMetrics();
					g2d.drawString(s.replace("-", " "), x - m.stringWidth(s), y);
				}
		);

		Graph.applyTransformed(g2d, 30, 520, g -> {
			Origin ori = getOrigins();
			if (ori.major() == Race.NONE) return;

			Race syn = ori.synergy();
			List<BufferedImage> icons = ori.images();

			String effects;
			if (ori.minor() == Race.NONE) {
				g.drawImage(icons.get(0), 0, 0, 150, 150, null);
				g.setFont(Fonts.OPEN_SANS.deriveFont(Font.BOLD, 60));
				g.setColor(ori.major().getColor());

				String text = locale.get("str/deck_origin_pure", syn.getName(locale));
				Graph.drawOutlinedString(g, text, 175, (150 + 75) / 2, 2, Color.BLACK);

				g.setFont(Fonts.OPEN_SANS.deriveFont(Font.PLAIN, 40));
				g.setColor(Color.WHITE);
				effects = ori.major().getMajor(locale)
						+ "\n\n- " + locale.get("minor/pureblood")
						+ "\n\n\"" + ori.major().getDescription(locale) + "\"";
			} else {
				g.drawImage(icons.get(2), 0, 0, 150, 150, null);
				g.setFont(Fonts.OPEN_SANS.deriveFont(Font.BOLD, 60));
				g.setColor(ori.synergy().getColor());

				String text = locale.get("str/deck_origin", syn.getName(locale));
				Graph.drawOutlinedString(g, text, 175, (150 + 75) / 2, 2, Color.BLACK);

				int majOffset = g.getFontMetrics().stringWidth(text.substring(0, text.length() - 10));
				int minOffset = g.getFontMetrics().stringWidth(text.substring(0, text.length() - 5));
				Graph.applyTransformed(g, 175, 150 / 2 - 75 / 2, g1 -> {
					g1.drawImage(icons.get(0), majOffset + 5, 10, 75, 75, null);
					g1.drawImage(icons.get(1), minOffset + 5, 10, 75, 75, null);
				});

				g.setFont(Fonts.OPEN_SANS.deriveFont(Font.PLAIN, 40));
				g.setColor(Color.WHITE);
				effects = ori.major().getMajor(locale)
						+ "\n\n- " + ori.minor().getMinor(locale)
						+ "\n\n- " + syn.getSynergy(locale)
						+ "\n\n\"" + syn.getDescription(locale) + "\"";
			}

			if (ori.demon()) {
				effects += "\n\n&" + Race.DEMON.getMinor(locale);
			}

			Graph.drawMultilineString(g, effects,
					0, 210, 1100, 6,
					s -> {
						String str = Utils.extract(s, "&(.+)", 1);

						if (str != null) {
							g.setColor(new Color(0xD72929));
							return str;
						}

						return s;
					}
			);
		});

		Graph.applyTransformed(g2d, 1212, 14, g -> {
			for (int i = 0; i < Math.min(senshi.size(), 36); i++) {
				Senshi s = senshi.get(i);
				g.drawImage(s.render(locale, this), 120 * (i % 9), 182 * (i / 9), 113, 175, null);
			}
		});

		Graph.applyTransformed(g2d, 1571, 768, g -> {
			for (int i = 0; i < Math.min(evogear.size(), 24); i++) {
				Evogear e = evogear.get(i);
				g.drawImage(e.render(locale, this), 120 * (i % 6), 182 * (i / 6), 113, 175, null);
			}
		});

		Graph.applyTransformed(g2d, 1185, 1314, g -> {
			for (int i = 0; i < Math.min(field.size(), 3); i++) {
				Field f = field.get(i);
				g.drawImage(f.render(locale, this), 120 * (i % 6), 0, 113, 175, null);
			}
		});

		g2d.drawImage(frame.getBack(this), 1252, 849, null);

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

		List<Race> out = races.stream()
				.distinct()
				.sorted(Comparator.comparingInt(races::getCount).reversed())
				.limit(2)
				.collect(Collectors.toList());
		while (out.size() < 2) {
			out.add(Race.NONE);
		}

		return new Origin(out);
	}

	public BaseValues getBaseValues() {
		int reduction = (int) Math.max(0, (Calc.prcnt(getEvoWeight(), 24) - 1) * 10);
		return new BaseValues(
				5000,
				t -> 5 - reduction,
				t -> 5
		);
	}

	public String toString(I18N locale) {
		return locale.get("str/deck_resume",
				senshi.size(), evogear.size(), field.size(),
				Utils.roundToString(Stream.of(senshi, evogear)
						.flatMap(List::stream)
						.mapToInt(d -> d.getHPCost())
						.average().orElse(0), 1),
				Utils.roundToString(Stream.of(senshi, evogear)
						.flatMap(List::stream)
						.mapToInt(d -> d.getMPCost())
						.average().orElse(0), 1)
		);
	}
}
