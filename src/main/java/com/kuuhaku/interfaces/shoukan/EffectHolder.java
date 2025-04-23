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

package com.kuuhaku.interfaces.shoukan;

import com.kuuhaku.ExpressionListener;
import com.kuuhaku.Tag;
import com.kuuhaku.generated.ShoukanExprLexer;
import com.kuuhaku.generated.ShoukanExprParser;
import com.kuuhaku.model.common.CachedScriptManager;
import com.kuuhaku.model.common.shoukan.*;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.*;
import com.kuuhaku.model.persistent.shoukan.CardAttributes;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.records.shoukan.EffectParameters;
import com.kuuhaku.model.common.shoukan.PropValue;
import com.kuuhaku.model.records.shoukan.Source;
import com.kuuhaku.model.records.shoukan.Target;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import kotlin.Pair;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public interface EffectHolder<T extends Drawable<T>> extends Drawable<T> {
	Pair<Integer, Color> EMPTY = new Pair<>(-1, Color.BLACK);

	Map<String, Pair<Integer, Color>> COLORS = Map.ofEntries(
			Map.entry("php", new Pair<>(0, new Color(0x85C720))),
			Map.entry("bhp", new Pair<>(1, new Color(0xC78C20))),
			Map.entry("pmp", new Pair<>(2, new Color(0x3F9EFF))),
			Map.entry("pdg", new Pair<>(3, new Color(0x9A1313))),
			Map.entry("prg", new Pair<>(4, new Color(0x7ABCFF))),

			Map.entry("hp", new Pair<>(5, new Color(0xFF0000))),
			Map.entry("mp", new Pair<>(6, new Color(0x3F9EFE))),
			Map.entry("atk", new Pair<>(7, new Color(0xFF0000))),
			Map.entry("dfs", new Pair<>(8, new Color(0x00C500))),
			Map.entry("ddg", new Pair<>(9, new Color(0xFFC800))),
			Map.entry("pry", new Pair<>(10, new Color(0xA9A9A9))),
			Map.entry("cd", new Pair<>(11, new Color(0x48BAFF))),

			Map.entry("ally", new Pair<>(12, Color.BLACK)),
			Map.entry("enemy", new Pair<>(13, Color.BLACK)),
			Map.entry("enemy_ignore", new Pair<>(14, Color.BLACK)),
			Map.entry("b", EMPTY),
			Map.entry("n", EMPTY)
	);

	String DC1 = "\u200B";
	String DC2 = "\u200C";
	String[] ICONS = {
			"hp.png", "base_hp.png", "mp.png",
			"degen.png", "regen.png", "blood.png",
			"mana.png", "attack.png", "defense.png",
			"dodge.png", "parry.png", "cooldown.png",
			"ally_target.png", "enemy_target.png", "enemy_target_ignore.png"
	};

	CardAttributes getBase();

	CardExtra getStats();

	@Override
	default int getMPCost() {
		return getMPCost(false);
	}

	default int getMPCost(boolean ignoreRace) {
		return 0;
	}

	@Override
	default int getHPCost() {
		return getHPCost(false);
	}

	default int getHPCost(boolean ignoreRace) {
		return 0;
	}

	default EffectHolder<?> getSource() {
		return (EffectHolder<?>) Utils.getOr(getStats().getSource(), this);
	}

	default String getDescription(I18N locale) {
		EffectHolder<?> source = getSource();
		String out = Utils.getOr(source.getStats().getDescription(locale), source.getBase().getDescription(locale));
		if (getHand() != null) {
			if (getHand().getOrigins().major() == Race.DEMON) {
				out = out.replace("$mp", "($hp/($bhp*0.08))");
			}
		}

		return out;
	}

	default void trigger(Trigger trigger, Source source, Target... targets) {
		if (getGame() != null) {
			getGame().trigger(trigger, source, targets);
		}
	}

	default void trigger(Trigger trigger, Side side) {
		if (getGame() != null) {
			getGame().trigger(trigger, side);
		}
	}

	default Flags getFlags() {
		return getStats().getFlags();
	}

	default boolean hasFlag(Flag flag) {
		return hasFlag(flag, false);
	}

	default boolean hasFlag(Flag flag, boolean pop) {
		if (pop) {
			return getFlags().pop(flag);
		} else {
			return getFlags().has(flag);
		}
	}

	default void setFlag(Flag flag) {
		setFlag(null, flag);
	}

	default void setFlag(Drawable<?> source, Flag flag) {
		getStats().setFlag(source, flag, true, true);
		if (getGame() != null) {
			trigger(Trigger.ON_FLAG_ALTER, asSource(Trigger.ON_FLAG_ALTER));
		}
	}

	default void setFlag(Flag flag, boolean value) {
		setFlag(null, flag, value);
	}

	default void setFlag(Drawable<?> source, Flag flag, boolean value) {
		getStats().setFlag(source, flag, value, false);
		if (getGame() != null) {
			trigger(Trigger.ON_FLAG_ALTER, asSource(Trigger.ON_FLAG_ALTER));
		}
	}

	boolean hasCharm(Charm charm);

	default boolean isPassive() {
		return this instanceof Evogear e && e.isSpell() && e.getTags().contains("PASSIVE");
	}

	default boolean isFixed() {
		return getBase().getTags().contains("FIXED");
	}

	boolean hasEffect();

	default boolean hasTrueEffect() {
		return hasTrueEffect(false);
	}

	default boolean hasTrueEffect(boolean pop) {
		return hasEffect() && (getBase().getTags().contains("TRUE_EFFECT") || hasFlag(Flag.EMPOWERED) || hasFlag(Flag.TRUE_EFFECT, pop));
	}

	Trigger getCurrentTrigger();

	boolean execute(EffectParameters ep);

	default void executeAssert(Trigger trigger) {
	}

	CachedScriptManager getCSM();

	default String parseDescription(@Nullable Hand h, I18N locale) {
		boolean display = h == null;
		boolean demon = !display && h.getOrigins().major() == Race.DEMON;

		int equips;
		EffectHolder<?> source = this;
		if (this instanceof Evogear e && e.getEquipper() != null) {
			source = e.getEquipper();
			equips = e.getEquipper().getEquipments().size();
		} else {
			equips = 1;
		}

		Map<String, Object> values = Map.ofEntries(
				Map.entry("php", display ? 6000 : h.getHP()),
				Map.entry("bhp", display ? 6000 : h.getBase().hp()),
				Map.entry("pmp", display ? 5 : h.getMP()),
				Map.entry("pdg", display ? 0 : Math.max(0, -h.getRegDeg().peek())),
				Map.entry("prg", display ? 0 : Math.max(0, h.getRegDeg().peek())),
				Map.entry("mp", demon ? source.getHPCost() : source.getMPCost()),
				Map.entry("hp", source.getHPCost()),
				Map.entry("atk", source.getDmg()),
				Map.entry("dfs", source.getDfs()),
				Map.entry("ddg", source.getDodge()),
				Map.entry("pry", source.getParry()),
				Map.entry("data", getStats().getData())
		);

		String desc = getDescription(locale);
		Matcher pat = Utils.regex(desc, "(?:\\{=([^{}]*?)}|\\{(\\w+)})(%)?");
		Map<String, Integer> counter = new HashMap<>();
		int hash = Objects.hash(desc, values);

		CachedScriptManager csm = getCSM();
		boolean stale = csm.getPropHash().intValue() != hash;
		if (stale) {
			csm.getStoredProps().clear();
		}

		Props props = csm.getStoredProps();
		try {
			return pat.replaceAll(m -> {
				boolean tag = m.group(2) != null;
				boolean prcnt = m.group(3) != null;
				String str = tag ? m.group(2) : m.group(1);

				String out = "";
				if (!tag) {
					LinkedHashSet<Object> types = new LinkedHashSet<>(Utils.extractGroups(str, "\\$(\\w+)"));
					if (types.isEmpty()) {
						types.add("untyped");
					}

					String main = types.stream()
							.map(String::valueOf)
							.findFirst().orElse(null);

					if (stale) {
						String val = String.valueOf(Utils.exec("import static java.lang.Math.*\n\n" + str.replace("$", ""), values));

						for (Object type : types) {
							props.compute(String.valueOf(type), (k, v) -> {
								double power = getPower();
								if (!display && h.getOrigins().synergy() == Race.FABLED) {
									power = 1;
								}

								int value = Calc.round(NumberUtils.toDouble(val) * power / equips);

								if (v == null) {
									return PropValue.from(value);
								} else {
									return v.add(value);
								}
							});
						}

						if (demon && props.containsKey("hp")) {
							props.put("mp", props.get("hp"));
						}
					}

					int it = counter.compute(main, (k, v) -> Utils.getOr(v, 0) + 1) - 1;
					out = String.valueOf(props.get(main).getAt(it));

					if (prcnt) {
						out += "%";
					}

					out += types.stream()
							.map(String::valueOf)
							.filter(COLORS::containsKey)
							.map(t -> display ? Tag.valueOf(t.toUpperCase()).toString() : "§" + Character.toString(0x2801 + COLORS.get(t).getFirst()))
							.collect(Collectors.joining());
				} else {
					Pair<Integer, Color> idx = COLORS.get(str);

					if (idx != null) {
						if (idx.getFirst() != -1) {
							if (display) {
								out = Tag.valueOf(str.toUpperCase()).toString();
							} else {
								out = "£" + Character.toString(0x2801 + idx.getFirst());
							}
						} else {
							if (!display) {
								out = switch (str) {
									case "b" -> DC1;
									case "n" -> DC2;
									default -> "";
								};
							}
						}
					}
				}

				return Matcher.quoteReplacement(out);
			});
		} finally {
			csm.getPropHash().set(hash);
		}
	}

	default TriConsumer<String, Integer, Integer> highlightValues(Graphics2D g2d, boolean legacy) {
		FrameSkin frame = getHand() == null ? FrameSkin.PINK : getHand().getUserDeck().getFrame();
		AtomicInteger lastVal = new AtomicInteger();
		AtomicInteger line = new AtomicInteger();
		TagBundle tags = getTagBundle();

		return (str, x, y) -> {
			if (lastVal.get() != y) {
				line.getAndIncrement();
				lastVal.set(y);
			}

			if (!legacy && line.get() == (tags.isEmpty() ? 7 : 6)) {
				x += 10;
			}

			g2d.setFont(Fonts.OPEN_SANS.deriveBold(10));
			g2d.setColor(frame.getSecondaryColor());

			FontMetrics fm = g2d.getFontMetrics();
			try {
				if (str.contains(DC1)) {
					g2d.setFont(Fonts.OPEN_SANS_BOLD.derivePlain(10));

					if (Calc.luminance(g2d.getColor()) < 0.2) {
						Graph.drawOutlinedString(g2d, str, x, y, 1.5f, Color.WHITE);
					} else {
						Graph.drawOutlinedString(g2d, str, x, y, 1.5f, Color.BLACK);
					}

					return;
				} else if (str.contains(DC2)) {
					Graph.drawOutlinedString(g2d, str, x, y, 0.125f, g2d.getColor());
					return;
				}
			} finally {
				g2d.setFont(Fonts.OPEN_SANS.deriveBold(10));
			}

			if (str.contains("§") || str.contains("£")) {
				Collection<Pair<Integer, Color>> cols = COLORS.values();
				Color color = Utils.extractGroups(str, "([⠁-⣿])").parallelStream()
						.map(o -> String.valueOf(o).charAt(0))
						.map(c -> cols.parallelStream()
								.filter(p -> p.getFirst() == c - 0x2801)
								.map(Pair::getSecond)
								.findAny().orElse(null)
						)
						.filter(Objects::nonNull)
						.collect(Collectors.collectingAndThen(Collectors.toList(), c -> {
							Color out = Graph.mix(c);
							if (c.size() > 1) {
								out = out.brighter();
							}

							return out;
						}));

				boolean tag = str.contains("£");
				boolean after = false;
				for (String s : str.split("[§£](?=[⠁-⣿])|(?<=[⠁-⣿])")) {
					if (s.isEmpty()) continue;

					char code = s.charAt(0);
					if (Utils.between(code, 0x2801, 0x2900)) {
						String path = "shoukan/icons/" + ICONS[code - 0x2801];

						BufferedImage icon = IO.getResourceAsImage(path);
						if (icon != null) {
							int size = g2d.getFont().getSize();
							g2d.drawImage(icon, x + 2, y - size + 1, size, size, null);
							x += size + 2;
						}

						after = true;
					} else {
						if (!after && !tag) {
							if (getFlags().has(Flag.HIDE_STATS)) {
								s = s.replaceAll("\\d", "?");
							}

							g2d.setColor(color);
							Graph.drawOutlinedString(g2d, s, x, y, 1.5f, Color.BLACK);
						} else {
							g2d.setColor(frame.getSecondaryColor());
						}

						g2d.drawString(s, x, y);
						x += fm.stringWidth(s);
					}
				}
			} else {
				g2d.drawString(str, x, y);
			}
		};
	}

	default void drawDescription(Graphics2D g2d, Hand h, I18N locale) {
		FrameSkin frame = getHand() == null ? FrameSkin.PINK : getHand().getUserDeck().getFrame();

		g2d.setFont(Fonts.OPEN_SANS_BOLD.deriveBold(11));
		g2d.setColor(frame.getSecondaryColor());

		String desc = parseDescription(h, locale);
		if (!desc.isBlank()) {
			int y = 276;
			String tags = processTags(locale);
			if (tags != null) {
				g2d.drawString(tags, 7, 275);
				y += 11;
			}

			g2d.setFont(Fonts.OPEN_SANS.deriveBold(10));
			renderText(g2d, desc, y, highlightValues(g2d, frame.isLegacy()));
		}
	}

	private static void renderText(Graphics2D g2d, String text, int y, TriConsumer<String, Integer, Integer> renderer) {
		AtomicInteger yOffset = new AtomicInteger(y);
		text.lines().forEach(l -> {
			String[] words = l.split("(?<=\\S )|(?=\\{=)|(?<=}%?)(?=[^%{ ])|(?<=[}])|(?<=\\(\\d)");
			int offset = 0;
			for (String s : words) {
				FontMetrics m = g2d.getFontMetrics();

				if (offset + m.stringWidth(s) <= 211) {
					renderer.accept(s, 7 + offset, yOffset.get());
					offset += m.stringWidth(s);
				} else {
					yOffset.updateAndGet(o -> o + m.getHeight() - 3);
					renderer.accept(s, 7, yOffset.get());
					offset = m.stringWidth(s);
				}
			}

			yOffset.updateAndGet(o -> o + g2d.getFontMetrics().getHeight() - 3);
		});
	}

	default String getReadableDescription(I18N locale) {
		String desc = getDescription(locale);
		if (desc != null) {
			Matcher pat = Utils.regex(desc, "\\{=(\\S+?)}|([A-Za-z]+?)?\\{([a-z_]+?)}");

			return parse(pat)
					.replaceAll("\\(([^()]|\\(([^()]|\\(([^()]|\\(([^()])*\\))*\\))*\\))*\\)", "**$0**")
					.replaceAll("\\({2}([^()]+?)\\){2}", "($1)");
		}

		return "";
	}

	private static String parse(Matcher matcher) {
		return matcher.replaceAll(m -> {
			if (m.group(1) != null) {
				ShoukanExprLexer lex = new ShoukanExprLexer(CharStreams.fromString(m.group(1)));

				CommonTokenStream cts = new CommonTokenStream(lex);
				ShoukanExprParser parser = new ShoukanExprParser(cts);
				ShoukanExprParser.LineContext tree = parser.line();

				ParseTreeWalker walker = new ParseTreeWalker();
				ExpressionListener listener = new ExpressionListener();

				walker.walk(listener, tree);

				return Matcher.quoteReplacement("(" + listener.getOutput().toString() + ")");
			} else if (m.group(3) != null) {
				if (m.group(2) != null) {
					return Matcher.quoteReplacement("__" + m.group(2) + "__" + Tag.valueOf(m.group(3).toUpperCase()));
				}

				return Matcher.quoteReplacement(Tag.valueOf(m.group(3).toUpperCase()).toString());
			}

			return null;
		});
	}
}
