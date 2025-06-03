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

package com.kuuhaku.model.persistent.shoukan;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.interfaces.shoukan.EffectHolder;
import com.kuuhaku.model.common.MultiProcessor;
import com.kuuhaku.model.common.SupplyChain;
import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Arcade;
import com.kuuhaku.model.enums.shoukan.FieldType;
import com.kuuhaku.model.enums.shoukan.FrameSkin;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.shiro.Anime;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.shoukan.BaseValues;
import com.kuuhaku.model.records.shoukan.DeckEntry;
import com.kuuhaku.model.records.shoukan.Origin;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONArray;
import jakarta.persistence.*;
import kotlin.Pair;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.collections4.bag.TreeBag;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.jdesktop.swingx.graphics.BlendComposite;
import org.knowm.xchart.RadarChart;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "deck", schema = "kawaipon")
public class Deck extends DAO<Deck> {
	@Transient
	public static final Deck INSTANCE = new Deck();

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private int id;

	@Column(name = "name")
	private String name;

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "account_uid")
	@Fetch(FetchMode.JOIN)
	private Account account;

	@Column(name = "variant", nullable = false)
	private boolean variant = false;

	@Embedded
	private final DeckStyling styling = new DeckStyling();

	private transient List<Senshi> senshi = null;
	private transient List<Evogear> evogear = null;
	private transient List<Field> field = null;
	private transient Origin origin = null;

	public Deck() {
	}

	public Deck(Account account) {
		this.account = account;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return Utils.getOr(name, "deck " + account.getDecks().indexOf(this));
	}

	public void setName(String name) {
		this.name = name;
	}

	public Account getAccount() {
		return account;
	}

	public boolean isCurrent() {
		return account.getSettings().getCurrentDeck() == id;
	}

	public boolean isVariant() {
		return variant;
	}

	public void setVariant(boolean variant) {
		this.variant = variant;
	}

	public Hero getHero() {
		//noinspection ConstantValue
		if (true) return null;

		return DAO.query(Hero.class, "SELECT h FROM Hero h WHERE h.account.id = ?1 AND h.stats.evil = FALSE", account.getUid());
	}

	public FrameSkin getFrame() {
		if (account != null && !styling.getFrame().canUse(account)) {
			styling.setFrame(FrameSkin.PINK);
		}

		return styling.getFrame();
	}

	public SlotSkin getSkin() {
		SlotSkin defSkin = DAO.find(SlotSkin.class, "DEFAULT");
		if (styling.getSkin() == null || !styling.getSkin().canUse(account)) {
			styling.setSkin(defSkin);
		}

		return styling.getSkin();
	}

	public List<DeckEntry> getSenshiRaw() {
		if (account == null) return List.of();

		return DAO.queryAllUnmapped("""
						SELECT 'SENSHI', d.card_id, sc.id
						FROM senshi d
						INNER JOIN stashed_card sc ON sc.card_id = d.card_id
						WHERE sc.kawaipon_uid = ?1
							AND sc.deck_id = ?2
						%s
						""".formatted(styling.getSenshiOrder()), account.getUid(), id).stream()
				.map(o -> Utils.map(DeckEntry.class, o))
				.toList();
	}

	public List<Senshi> getSenshi() {
		if (senshi == null) {
			senshi = getSenshiRaw().stream()
					.map(de -> (Senshi) de.card())
					.toList();
		}

		return senshi;
	}

	public int getMaxSenshiCopies() {
		return 3;
	}

	public boolean validateSenshi() {
		int allowed = getMaxSenshiCopies();
		HashBag<String> bag = new HashBag<>();

		int count = 0;
		for (Senshi s : getSenshi()) {
			bag.add(s.getId());
			count++;
		}

		if (!Utils.between(count, 30, 36)) {
			return false;
		}

		bag.removeIf(s -> bag.getCount(s) <= allowed);
		return bag.isEmpty();
	}

	public int countRace(Race race) {
		return (int) getSenshi().stream()
				.filter(s -> s.getRace().isRace(race))
				.count();
	}

	public List<DeckEntry> getEvogearRaw() {
		if (account == null) return List.of();

		return DAO.queryAllUnmapped("""
						SELECT 'EVOGEAR', d.card_id, sc.id
						FROM evogear d
						INNER JOIN stashed_card sc ON sc.card_id = d.card_id
						WHERE sc.kawaipon_uid = ?1
						  AND sc.deck_id = ?2
						%s
						""".formatted(styling.getEvogearOrder()), account.getUid(), id).stream()
				.map(o -> Utils.map(DeckEntry.class, o))
				.toList();
	}

	public List<Evogear> getEvogear() {
		if (evogear == null) {
			evogear = getEvogearRaw().stream()
					.map(de -> (Evogear) de.card())
					.toList();
		}

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
		HashMap<String, Pair<Evogear, AtomicInteger>> evos = new HashMap<>();

		int count = 0;
		for (Evogear e : getEvogear()) {
			evos.computeIfAbsent(e.getId(), k -> new Pair<>(e, new AtomicInteger()))
					.getSecond()
					.getAndIncrement();
			count++;
		}

		if (!Utils.between(count, 0, 26)) {
			return false;
		}

		int t4 = 0, strats = 0;
		int t4Max = getMaxEvogearCopies(4);
		for (Pair<Evogear, AtomicInteger> e : evos.values()) {
			if (e.getFirst().getTier() == 4) {
				t4 += e.getSecond().get();
			}
			if (e.getFirst().getTags().contains("STRATAGEM")) {
				strats += e.getSecond().get();
			}

			if (t4 > t4Max || strats > 2) return false;
		}

		evos.values().removeIf(p -> p.getSecond().get() <= getMaxEvogearCopies(p.getFirst().getTier()));
		return evos.isEmpty();
	}

	public int getEvoWeight() {
		int weight = 0;
		double penalty = 0;

		for (Evogear e : getEvogear()) {
			int w = e.getTier();
			if ((!e.isSpell() && getOrigins().major() == Race.MACHINE) || (e.isSpell() && getOrigins().major() == Race.MYSTICAL)) {
				w -= 1;
			}

			if (e.getTags().contains("STRATAGEM")) {
				w *= 2;
			}

			weight += w;
			penalty += e.getCharms().size() * 0.75;
		}

		return (int) (weight + penalty);
	}

	public List<DeckEntry> getFieldsRaw() {
		if (account == null) return List.of();

		return DAO.queryAllUnmapped("""
						SELECT 'FIELD', d.card_id, sc.id
						FROM field d
						INNER JOIN stashed_card sc ON sc.card_id = d.card_id
						WHERE sc.kawaipon_uid = ?1
						  AND sc.deck_id = ?2
						""", account.getUid(), id).stream()
				.map(o -> Utils.map(DeckEntry.class, o))
				.toList();
	}

	public List<Field> getFields() {
		if (field == null) {
			field = getFieldsRaw().stream()
					.map(de -> (Field) de.card())
					.toList();
		}

		return field;
	}

	public boolean validateFields() {
		return Utils.between(getFields().size(), 0, 3);
	}

	public int countCard(Drawable<?> card) {
		List<DeckEntry> cards;

		if (card instanceof Senshi) {
			cards = getSenshiRaw();
		} else if (card instanceof Evogear) {
			cards = getEvogearRaw();
		} else {
			cards = getFieldsRaw();
		}

		return (int) cards.parallelStream()
				.filter(de -> de.id().equals(card.getId()))
				.count();
	}

	public DeckStyling getStyling() {
		return styling;
	}

	public boolean isCoverAllowed() {
		Card cover = styling.getCover();
		if (cover == null) {
			return true;
		}

		Anime anime = cover.getAnime();
		Pair<Integer, Integer> count = account.getKawaipon().countCards(anime);

		return Math.max(count.getFirst(), count.getSecond()) >= anime.getCount();
	}

	public double getMetaDivergence() {
		return DAO.queryNative(Double.class, """
				SELECT 1 - round(score * 1.0 / total, 2) AS divergence
				FROM (
				  SELECT count(1)                          AS total
					   , sum(iif(m.card_id IS NULL, 0, 1)) AS score
				  FROM stashed_card sc
						   LEFT JOIN v_shoukan_meta m ON sc.card_id = m.card_id
				  WHERE deck_id = ?1
				) x
				""", id);
	}

	public BufferedImage render(I18N locale) {
		BufferedImage bi = IO.getResourceAsImage("shoukan/deck.png");
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHints(Constants.SD_HINTS);

		Graph.applyTransformed(g2d, g -> {
			g.setColor(getFrame().getThemeColor());
			g.setComposite(BlendComposite.Color);
			g.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		});
		Graph.applyMask(bi, IO.getResourceAsImage("shoukan/mask/deck_mask.png"), 0, true);

		MultiProcessor.with(Executors.newVirtualThreadPerTaskExecutor(), new ArrayList<Consumer<Graphics2D>>())
				.addTask(renderData(locale))
				.addTask(renderRaceInfo(locale, bi))
				.addTask(renderCards(locale))
				.process(t -> {
					t.accept((Graphics2D) g2d.create());
					return null;
				});

		g2d.dispose();

		return bi;
	}

	public Consumer<Graphics2D> renderData(I18N locale) {
		return g2d -> {
			List<Drawable<?>> allCards = new ArrayList<>();
			allCards.addAll(getSenshi());
			allCards.addAll(getEvogear());
			allCards.addAll(getFields());

			BaseValues base = getBaseValues(null);
			double mp = base.mpGain().get();
			int weight = Calc.prcntToInt(getEvoWeight(), 24);
			String color = "FFFFFF";
			if (weight > 200) color = "FF0000";
			else if (weight > 100) color = "FFFF00";

			int totalDmg = 0, totalDfs = 0;
			for (EffectHolder<?> e : ListUtils.union(getSenshi(), getEvogear())) {
				totalDmg += e.getDmg();
				totalDfs += e.getDfs();
			}

			double[] vals = Calc.clamp(new double[]{
					Calc.prcnt(totalDmg, (totalDmg + totalDfs) / 1.5),
					Calc.prcnt(totalDfs, (totalDmg + totalDfs) / 1.5),
					getAverageMPCost() / mp,
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
					.setSeriesColors(new Color[]{Graph.withOpacity(getFrame().getThemeColor(), 0.5f)})
					.setChartFontColor(Color.WHITE);

			rc.paint(g2d, rc.getWidth(), rc.getHeight());

			g2d.setFont(Fonts.OPEN_SANS.derivePlain(30));
			g2d.setColor(Color.WHITE);
			Graph.drawMultilineString(g2d, locale.get("str/deck_analysis"), 600, 45, 400);
			Graph.drawMultilineString(g2d, """
							%s
							%s
							%s-(%s/%s/%s)
							{%s%%;0x%s}
							%s%%
							%s
							%s
							%s
							%s
							
							%s
							%s-(T4:-%s)
							%s
							""".formatted(
							base.hp(),
							Utils.roundToString(mp, 1),
							allCards.size(), getSenshi().size(), getEvogear().size(), getFields().size(),
							weight, color,
							Utils.roundToString(getMetaDivergence() * 100, 0),
							Utils.roundToString((float) getAverageMPCost(), 1),
							Utils.roundToString((float) getAverageHPCost(), 1),
							Utils.roundToString((float) totalDmg / allCards.size(), 0),
							Utils.roundToString((float) totalDfs / allCards.size(), 0),
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
		};
	}

	public Consumer<Graphics2D> renderRaceInfo(I18N locale, BufferedImage bi) {
		return g2d -> Graph.applyTransformed(g2d, 30, 520, g1 -> {
			Origin ori = getOrigins();
			if (ori.major() == Race.NONE) return;

			Race syn = ori.synergy();
			List<BufferedImage> icons = ori.images();

			String effects;
			if (ori.isPure()) {
				g1.drawImage(ori.major().getImage(), 0, 0, 150, 150, null);
				g1.setFont(Fonts.OPEN_SANS.deriveBold(60));
				g1.setColor(ori.major().getColor());

				String text = locale.get("str/deck_origin_pure", ori.major().getName(locale));
				Graph.drawOutlinedString(g1, text, 175, (150 + 75) / 2, 2, Color.BLACK);

				g1.setFont(Fonts.OPEN_SANS.derivePlain(36));
				g1.setColor(Color.WHITE);
				effects = "- " + ori.major().getMajor(locale)
						  + "\n\n- " + locale.get("major/pureblood")
						  + "\n\n&(#8CC4FF)- " + locale.get("pure/" + ori.major())
						  + (ori.demon() ? "\n\n&- " + Race.DEMON.getMinor(locale) : "");
			} else if (ori.major() == Race.MIXED) {
				g1.setFont(Fonts.OPEN_SANS_EXTRABOLD.deriveBold(60));
				g1.setColor(Graph.mix(Arrays.stream(ori.minor()).map(Race::getColor).toArray(Color[]::new)));

				String text = locale.get("str/deck_origin_mixed");
				Graph.drawOutlinedString(g1, text, 0, (150 + 75) / 2, 2, Color.BLACK);

				g1.setFont(Fonts.OPEN_SANS.derivePlain(36));
				g1.setColor(Color.WHITE);
				effects = "- " + locale.get("major/mixed")
						  + "\n\n" + Arrays.stream(ori.minor())
								  .filter(r -> r != Race.DEMON)
								  .map(o -> "- " + o.getMinor(locale))
								  .collect(Collectors.joining("\n\n"))
						  + (ori.demon() ? "\n\n&(#D72929)- " + Race.DEMON.getMinor(locale) : "");
			} else {
				g1.drawImage(ori.synergy().getBadge(), 0, 0, 150, 150, null);
				g1.setFont(Fonts.OPEN_SANS_EXTRABOLD.deriveBold(60));
				g1.setColor(ori.synergy().getColor());

				String text = locale.get("str/deck_origin", syn.getName(locale));
				Graph.drawOutlinedString(g1, text, 175, (150 + 75) / 2, 2, Color.BLACK);

				int majOffset = g1.getFontMetrics().stringWidth(text.substring(0, text.length() - 12));
				int minOffset = g1.getFontMetrics().stringWidth(text.substring(0, text.length() - 6));
				Graph.applyTransformed(g1, 175, 150 / 2 - 75 / 2, g2 -> {
					g2.drawImage(icons.getFirst(), majOffset + 5, 10, 75, 75, null);
					g2.drawImage(icons.get(1), minOffset + 5, 10, 75, 75, null);
				});

				g1.setFont(Fonts.OPEN_SANS.derivePlain(36));
				g1.setColor(Color.WHITE);
				effects = "- " + ori.major().getMajor(locale)
						  + "\n\n" + Arrays.stream(ori.minor())
								  .filter(r -> r != Race.DEMON)
								  .map(o -> "- " + o.getMinor(locale))
								  .collect(Collectors.joining("\n\n"))
						  + "\n\n- " + syn.getSynergy(locale)
						  + (ori.demon() ? "\n\n&(#D72929)- " + Race.DEMON.getMinor(locale) : "");
			}

			Graph.drawMultilineString(g1, effects,
					0, 190, 1140, 10, -20,
					s -> {
						JSONArray args = Utils.extractGroups(s, "&\\((#[0-9A-F]{6})\\)(.+)");

						if (!args.isEmpty()) {
							g1.setColor(Color.decode(args.getString(0)));
							return args.getString(1);
						}

						return s;
					}
			);

			String str;
			if (ori.isPure()) {
				str = "  \"" + ori.major().getDescription(locale) + "\"";
			} else if (ori.major() == Race.MIXED) {
				str = "";
			} else {
				str = "  \"" + syn.getDescription(locale) + "\"";
			}

			g1.setColor(Color.WHITE);
			Graph.drawMultilineString(g1, str,
					0, g1.getFontMetrics().getHeight() / 2 + bi.getHeight() - 520 - (int) Graph.getMultilineStringBounds(g1, str, 1100, 10).getHeight(),
					1100, 10
			);
		});
	}

	public Consumer<Graphics2D> renderCards(I18N locale) {
		return g2d -> {
			Graph.applyTransformed(g2d, 1212, 14, g -> {
				for (int i = 0; i < Math.min(getSenshi().size(), 36); i++) {
					Senshi s = getSenshi().get(i);
					g.drawImage(s.render(locale, this), 120 * (i % 9), 182 * (i / 9), 113, 175, null);
				}
			});

			Graph.applyTransformed(g2d, 1571, 768, g -> {
				for (int i = 0; i < Math.min(getEvogear().size(), 24); i++) {
					Evogear e = getEvogear().get(i);
					g.drawImage(e.render(locale, this), 120 * (i % 6), 182 * (i / 6), 113, 175, null);
				}
			});

			Graph.applyTransformed(g2d, 1185, 1314, g -> {
				for (int i = 0; i < Math.min(getFields().size(), 3); i++) {
					Field f = getFields().get(i);
					g.drawImage(f.render(locale, this), 120 * (i % 6), 0, 113, 175, null);
				}
			});

			Hero h = getHero();
			if (h != null) {
				g2d.drawImage(h.render(locale), 1237, 834, null);
			} else {
				g2d.drawImage(getFrame().getBack(this), 1252, 849, null);
			}
		};
	}

	public void setOrigin(Origin origin) {
		this.origin = origin;
	}

	public Origin getOrigins() {
		if (origin == null) {
			TreeBag<Race> races = new TreeBag<>();
			for (Senshi s : getSenshi()) {
				races.addAll(
						s.getRace().split().stream()
								.filter(r -> r != Race.NONE)
								.toList()
				);
			}

			List<Race> ori = new ArrayList<>();
			Iterator<Race> it = races.stream()
					.distinct()
					.sorted(Comparator.comparingInt(races::getCount).reversed())
					.iterator();

			int high = 0;
			int second = 0;
			boolean allSame = true;
			while (it.hasNext()) {
				if (ori.size() >= 8) break;

				Race r = it.next();
				int count = races.getCount(r);
				if (high == 0) high = count;

				if (count == high) {
					ori.add(r);
				} else {
					if (ori.size() < 2) {
						allSame = false;
						second = count;
						ori.add(r);
						continue;
					} else if (count == second) {
						allSame = true;
					}

					break;
				}
			}

			if (ori.isEmpty()) {
				origin = Origin.from(variant, Race.NONE);
			} else if (ori.size() == 1) {
				origin = Origin.from(variant, ori.getFirst());
			} else if (allSame) {
				origin = Origin.from(variant, Race.MIXED, ori.toArray(Race[]::new));
			} else {
				origin = Origin.from(variant, ori.get(0), ori.get(1));
			}
		}

		return origin;
	}

	public BaseValues getBaseValues(Hand h) {
		try {
			return new BaseValues(() -> {
				Origin origin = h == null ? getOrigins() : h.getOrigins();
				double reduction = Math.pow(0.999, -24 * getEvoWeight());
				if (getOrigins().major() == Race.BEAST) {
					reduction *= 0.66;
				}

				int base = 6000;
				if (origin.major() == Race.HUMAN) {
					if (origin.isPure()) {
						base += 1000;
					}

					base += 1000;
				}

				if (origin.synergy() == Race.DRAGON) {
					base += 1000;
				}

				int bHP = (int) Calc.clamp(base * 1.5 - base * 0.3 * reduction, 10, base);

				int mp = 5;
				SupplyChain<Integer> mpGain = new SupplyChain<>(mp)
						.add(m -> {
							if (origin.major() == Race.DEMON) {
								if (origin.isPure()) {
									m += 1;
								}

								m /= 2;
							}

							if (origin.hasMinor(Race.DIVINITY)) {
								m += getAverageMPCost();
							}

							if (h != null && h.getGame() != null) {
								if (origin.synergy() == Race.DEMIGOD) {
									m += (int) h.getGame().getCards(h.getSide()).parallelStream()
											.filter(Senshi::isFusion)
											.count();
								}

								if (h.getGame().getArena().getField().getType() == FieldType.DAY) {
									m += 1;
								}

								if (origin.synergy() == Race.FEY) {
									if (Math.ceil(h.getGame().getTurn() / 2d) % 2 == 0) {
										m = (int) Math.ceil(m * 1.5);
									} else {
										m = (int) Math.floor(m * 0.5);
									}
								}

								if (h.getGame().getArcade() == Arcade.OVERCHARGE) {
									m *= 2;
								}
							}

							return m;
						});

				SupplyChain<Integer> handCap = new SupplyChain<>(5)
						.add(c -> {
							if (origin.synergy() == Race.DRAGON) {
								c += 1;
							}

							return c;
						});

				int ls = 0;
				if (origin.major() == Race.DEMON) {
					ls += 10;
				}

				if (origin.synergy() == Race.VAMPIRE) {
					ls += 7;
				}

				return List.of(bHP, mpGain, handCap, ls);
			});
		} catch (Exception e) {
			Constants.LOGGER.error(e, e);
			return new BaseValues();
		}
	}

	public int getAverageMPCost() {
		return Calc.round(Stream.of(getSenshi(), getEvogear())
				.flatMap(List::stream)
				.mapToDouble(d -> d.getMPCost(true))
				.average().orElse(0));
	}

	public int getAverageHPCost() {
		return Calc.round(Stream.of(getSenshi(), getEvogear())
				.flatMap(List::stream)
				.mapToDouble(d -> d.getHPCost(true))
				.average().orElse(0));
	}

	public String toString(I18N locale) {
		return locale.get("str/deck_resume",
				getSenshi().size(), getEvogear().size(), getFields().size(),
				Utils.roundToString(Stream.of(getSenshi(), getEvogear())
						.flatMap(List::stream)
						.mapToInt(d -> d.getHPCost())
						.average().orElse(0), 1),
				Utils.roundToString(Stream.of(getSenshi(), getEvogear())
						.flatMap(List::stream)
						.mapToInt(d -> d.getMPCost())
						.average().orElse(0), 1)
		);
	}
}
