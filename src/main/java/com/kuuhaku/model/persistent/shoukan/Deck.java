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
import com.kuuhaku.model.enums.FrameColor;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.shoukan.BaseValues;
import com.kuuhaku.model.records.shoukan.Origin;
import com.kuuhaku.utils.Calc;
import com.kuuhaku.utils.Graph;
import com.kuuhaku.utils.IO;
import com.kuuhaku.utils.Utils;
import com.kuuhaku.utils.json.JSONArray;
import org.apache.commons.collections4.bag.TreeBag;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.jdesktop.swingx.graphics.BlendComposite;
import org.knowm.xchart.RadarChart;

import javax.persistence.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Entity
@Table(name = "deck")
public class Deck extends DAO<Deck> {
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
	@OrderColumn(name = "index")
	@JoinTable(name = "deck_senshi",
			joinColumns = @JoinColumn(name = "deck_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "senshi_card_id", referencedColumnName = "card_id"))
	@Fetch(FetchMode.SUBSELECT)
	private List<Senshi> senshi = new ArrayList<>();

	@ManyToMany
	@OrderColumn(name = "index")
	@JoinTable(name = "deck_evogear",
			joinColumns = @JoinColumn(name = "deck_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "evogear_card_id", referencedColumnName = "card_id"))
	@Fetch(FetchMode.SUBSELECT)
	private List<Evogear> evogear = new ArrayList<>();

	@ManyToMany
	@OrderColumn(name = "index")
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

	public int getEvoWeight() {
		return evogear.stream()
				.mapToInt(e -> e.getTier() + (-1 + e.getCharms().size()))
				.sum();
	}

	public List<Field> getFields() {
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
		BufferedImage bi = IO.getResourceAsImage("shoukan/deck.webp");
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);

		Graph.applyTransformed(g2d, g -> {
			g.setColor(frame.getThemeColor());
			g.setComposite(BlendComposite.Color);
			g.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		});

		List<Drawable<?>> allCards = new ArrayList<>(){{
			addAll(senshi);
			addAll(evogear);
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
		int healers = 0;
		int controllers = 0;

		for (Drawable<?> d : allCards) {
			if (d instanceof Senshi s) {
				if (s.getTags().contains("HEALER")) {
					healers++;
				} else if (s.getTags().contains("CONTROLLER")) {
					controllers++;
				}
			} else if (d instanceof Evogear e) {
				if (e.getTags().contains("HEALER")) {
					healers++;
				} else if (e.getTags().contains("CONTROLLER")) {
					controllers++;
				}
			}

			totalMPCost.addAndGet(d.getMPCost());
			totalHPCost.addAndGet(d.getHPCost());
			totalDmg.addAndGet(d.getDmg());
			totalDef.addAndGet(d.getDef());
		}

		RadarChart rc = new RadarChart(600, 500);
		rc.setRadiiLabels(new String[]{
				locale.get("str/attack"),
				locale.get("str/defense"),
				locale.get("str/mana_sustain"),
				locale.get("str/hp_sustain"),
				locale.get("str/control"),
				locale.get("str/divergence")
		});
		rc.addSeries("A", new double[]{
				Calc.clamp(Calc.prcnt(totalDmg.get(), (totalDmg.get() + totalDef.get()) / 1.5), 0, 1),
				Calc.clamp(Calc.prcnt(totalDef.get(), (totalDmg.get() + totalDef.get()) / 1.5), 0, 1),
				Calc.clamp(avgMana / totalMPCost.get(), 0, 1),
				Calc.clamp((healers * (1 / 36d) * base.hp()) - totalHPCost.get(), 0, 1),
				Calc.clamp(controllers / 36d, 0, 1),
				Calc.clamp(1, 0, 1)
		});

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

		g2d.setFont(new Font("Arial", Font.PLAIN, 30));
		g2d.setColor(Color.WHITE);
		Graph.drawMultilineString(g2d, locale.get("str/deck_resume"), 600, 45, 350);
		Graph.drawMultilineString(g2d,
				base.hp()
						+ "\n" + Utils.roundToString(avgMana, 1)
						+ "\n" + (allCards.size() + field.size()) + "-(" + senshi.size() + "/" + evogear.size() + "/" + field.size() + ")"
						+ "\n{" + weight + "%;0x" + color + "}"
						+ "\n" + Utils.roundToString((float) totalMPCost.get() / allCards.size(), 1)
						+ "\n" + Utils.roundToString((float) totalHPCost.get() / allCards.size(), 1)
						+ "\n" + Utils.roundToString((float) totalDmg.get() / allCards.size(), 1)
						+ "\n" + Utils.roundToString((float) totalDef.get() / allCards.size(), 1)
				, 950, 45, 150, 0,
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
					g2d.drawString(s.replace("-", " "), x + m.stringWidth("MMMMM") - m.stringWidth(s), y);
				}
		);

		Graph.applyTransformed(g2d, 30, 520, g -> {
			Origin ori = getOrigins();
			if (ori.major() == Race.NONE) return;

			Race syn = ori.synergy();

			if (ori.minor() == Race.NONE) {
				g.drawImage(ori.major().getImage(), 0, 0, 150, 150, null);
				g.setFont(new Font("Arial", Font.BOLD, 60));
				g.setColor(ori.major().getColor());

				String text = locale.get("str/deck_origin_pure", syn.getName(locale));
				Graph.drawOutlinedString(g, text, 175, (150 + 75) / 2, 2, Color.BLACK);

				g.setFont(new Font("Arial", Font.PLAIN, 30));
				g.setColor(Color.WHITE);
				Graph.drawMultilineString(g,
						ori.major().getMajor(locale)
								+ "\n\n" + locale.get("minor/pureblood"),
						0, 210, 1100
				);
			} else {
				g.drawImage(syn.getImage(), 0, 0, 150, 150, null);
				g.setFont(new Font("Arial", Font.BOLD, 60));
				g.setColor(ori.major().getColor());

				String text = locale.get("str/deck_origin", syn.getName(locale));
				Graph.drawOutlinedString(g, text, 175, (150 + 75) / 2, 2, Color.BLACK);

				int majOffset = g.getFontMetrics().stringWidth(text.substring(0, text.length() - 10));
				int minOffset = g.getFontMetrics().stringWidth(text.substring(0, text.length() - 5));
				Graph.applyTransformed(g, 175, 150 / 2 - 75 / 2, g1 -> {
					g1.drawImage(ori.major().getImage(), majOffset + 5, 10, 75, 75, null);
					g1.drawImage(ori.minor().getImage(), minOffset + 5, 10, 75, 75, null);
				});

				g.setFont(new Font("Arial", Font.PLAIN, 30));
				g.setColor(Color.WHITE);
				Graph.drawMultilineString(g,
						ori.major().getMajor(locale)
								+ "\n\n" + ori.minor().getMinor(locale)
								+ "\n\n" + syn.getSynergy(locale),
						0, 210, 1100
				);
			}
		});

		Graph.applyTransformed(g2d, 1212, 14, g -> {
			int i = 0;
			for (Senshi s : senshi) {
				if (i > 36) break;
				g.drawImage(s.render(locale, this), 120 * (i++ % 9), 182 * (i / 9), 113, 175, null);
			}
		});

		Graph.applyTransformed(g2d, 1571, 768, g -> {
			int i = 0;
			for (Evogear e : evogear) {
				if (i > 24) break;
				g.drawImage(e.render(locale, this), 120 * (i++ % 6), 182 * (i / 6), 113, 175, null);
			}
		});

		Graph.applyTransformed(g2d, 1185, 1314, g -> {
			int i = 0;
			for (Field f : field) {
				if (i > 3) break;
				g.drawImage(f.render(locale, this), 120 * (i++ % 6), 0, 113, 175, null);
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
				5
		);
	}
}
