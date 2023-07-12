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
import com.kuuhaku.exceptions.ActivationException;
import com.kuuhaku.exceptions.TargetException;
import com.kuuhaku.game.Shoukan;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.interfaces.shoukan.EffectHolder;
import com.kuuhaku.interfaces.shoukan.Proxy;
import com.kuuhaku.model.common.CachedScriptManager;
import com.kuuhaku.model.common.XList;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.common.shoukan.CardExtra;
import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.*;
import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.records.shoukan.EffectParameters;
import com.kuuhaku.model.records.shoukan.Target;
import com.kuuhaku.model.records.shoukan.Targeting;
import com.kuuhaku.util.*;
import com.ygimenez.json.JSONArray;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.*;
import org.apache.commons.collections4.set.ListOrderedSet;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.List;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

import static com.kuuhaku.model.enums.shoukan.Trigger.*;

@Entity
@Table(name = "evogear")
public class Evogear extends DAO<Evogear> implements EffectHolder<Evogear> {
	@Transient
	public final String KLASS = getClass().getName();
	public transient long SERIAL = ThreadLocalRandom.current().nextLong();

	@Id
	@Column(name = "card_id", nullable = false)
	private String id;

	@OneToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "card_id")
	@Fetch(FetchMode.JOIN)
	@MapsId("id")
	private Card card;

	@Column(name = "tier", nullable = false)
	private int tier;

	@Column(name = "spell", nullable = false)
	private boolean spell;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", nullable = false)
	private TargetType targetType = TargetType.NONE;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "charms", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray charms = new JSONArray();

	@Embedded
	private CardAttributes base = new CardAttributes();

	private transient Senshi equipper = null;
	private transient CardExtra stats = new CardExtra();
	private transient Hand hand = null;
	private transient Hand leech = null;
	private transient CachedScriptManager<Evogear> cachedEffect = new CachedScriptManager<>();

	@Transient
	private byte state = 0b10;
	/*
	0xF
	  └ 000 0111
	         ││└ solid
	         │└─ available
	         └── flipped
	 */

	public Evogear() {
	}

	public Evogear(String id, Card card, int tier, boolean spell, TargetType type, JSONArray charms, CardAttributes base) {
		this.id = id;
		this.card = card;
		this.tier = tier;
		this.spell = spell;
		this.targetType = type;
		this.charms = charms;
		this.base = base;
	}

	@Override
	public long getSerial() {
		return SERIAL;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Card getCard() {
		return card;
	}

	public int getTier() {
		return tier + stats.getTier();
	}

	public boolean isSpell() {
		return spell && equipper == null;
	}

	public TargetType getTargetType() {
		return targetType;
	}

	public JSONArray getCharms() {
		return charms;
	}

	@Override
	public boolean hasCharm(Charm charm) {
		return charms.contains(charm.name());
	}

	@Override
	public CardAttributes getBase() {
		return base;
	}

	@Override
	public CardExtra getStats() {
		return stats;
	}

	public Senshi getEquipper() {
		return equipper;
	}

	public void setEquipper(Senshi equipper) {
		this.equipper = equipper;
	}

	@Override
	public ListOrderedSet<String> getCurses() {
		return stats.getCurses();
	}

	@Override
	public List<String> getTags() {
		List<String> out = new ArrayList<>();
		if (hasEffect()) {
			if (isSpell()) {
				out.add("tag/spell");
			} else {
				out.add("tag/effect");
			}
		}
		for (Object tag : base.getTags()) {
			out.add("tag/" + ((String) tag).toLowerCase());
		}

		return out;
	}

	@Override
	public Hand getHand() {
		return hand;
	}

	@Override
	public void setHand(Hand hand) {
		this.hand = hand;

		if (this instanceof Proxy<?> p) {
			p.getOriginal().setHand(hand);
		}
	}

	@Override
	public Hand getLeech() {
		return leech;
	}

	@Override
	public void setLeech(Hand leech) {
		if (this.leech != null) {
			if (leech == null) {
				this.leech.getLeeches().remove(this);
			} else {
				return;
			}
		}

		this.leech = leech;
		if (this.leech != null) {
			this.leech.getLeeches().add(this);
		}
	}

	@Override
	public String getDescription(I18N locale) {
		EffectHolder<?> source = (EffectHolder<?>) Utils.getOr(stats.getSource(), this);

		return Utils.getOr(source.getStats().getDescription(locale), source.getBase().getDescription(locale));
	}

	@Override
	public int getMPCost() {
		return Math.max(0, Calc.round((base.getMana() + stats.getMana()) * getCostMult()));
	}

	@Override
	public int getHPCost() {
		return Math.max(0, Calc.round((base.getBlood() + stats.getBlood()) * getCostMult()));
	}

	@Override
	public int getSCCost() {
		return Math.max(0, Calc.round((base.getSacrifices() + stats.getSacrifices()) * getCostMult()));
	}

	@Override
	public int getDmg() {
		int sum = base.getAtk() + stats.getAtk();

		return Calc.round(sum * getAttrMult());
	}

	@Override
	public int getDfs() {
		int sum = base.getDfs() + stats.getDfs();

		return Calc.round(sum * getAttrMult());
	}

	@Override
	public int getDodge() {
		int sum = base.getDodge() + stats.getDodge();

		return Utils.clamp(sum, 0, 100);
	}

	@Override
	public int getBlock() {
		int sum = base.getBlock() + stats.getBlock();

		int min = 0;
		if (hand != null && hand.getOrigin().synergy() == Race.CYBORG) {
			min += 2;
		}

		return Utils.clamp(min + sum, min, 100);
	}

	@Override
	public double getCostMult() {
		double mult = stats.getCostMult();
		if (hand != null && ((!spell && hand.getOrigin().hasMinor(Race.MACHINE)) || (spell && hand.getOrigin().hasMinor(Race.MYSTICAL)))) {
			mult *= 0.8;
		}

		return mult;
	}

	@Override
	public double getAttrMult() {
		double mult = stats.getAttrMult();
		if (hand != null) {
			if (!spell && hand.getOrigin().hasMinor(Race.MACHINE)) {
				mult *= 1.14 + (hand.getUserDeck().countRace(Race.MACHINE) * 0.02);
			}

			if (getGame() != null && getGame().getArcade() == Arcade.OVERCHARGE) {
				mult *= 1.5;
			}
		}

		return mult;
	}

	@Override
	public double getPower() {
		double mult = stats.getPower();
		if (hand != null) {
			if (spell && hand.getOrigin().hasMinor(Race.MYSTICAL)) {
				mult *= 1.14 + (hand.getUserDeck().countRace(Race.MYSTICAL) * 0.02);
			}

			if (hand.getOrigin().major() == Race.NONE) {
				mult *= 1 - Math.max(0, 0.07 * (hand.getOrigin().minor().length - 1));
			}

			if (getGame() != null && getGame().getArcade() == Arcade.OVERCHARGE) {
				mult *= 1.75;
			}
		}

		return mult;
	}

	@Override
	public boolean isSolid() {
		return Bit.on(state, 0);
	}

	@Override
	public void setSolid(boolean solid) {
		state = (byte) Bit.set(state, 0, solid);
	}

	@Override
	public boolean isAvailable() {
		return Bit.on(state, 1);
	}

	@Override
	public void setAvailable(boolean available) {
		state = (byte) Bit.set(state, 1, available);
	}

	@Override
	public boolean isFlipped() {
		if (equipper != null) {
			return equipper.isFlipped() || Bit.on(state, 2);
		}

		return Bit.on(state, 2);
	}

	@Override
	public void setFlipped(boolean flipped) {
		state = (byte) Bit.set(state, 2, flipped);
	}

	public String getEffect() {
		EffectHolder<?> source = (EffectHolder<?>) Utils.getOr(stats.getSource(), this);

		return Utils.getOr(source.getStats().getEffect(), source.getBase().getEffect());
	}

	public boolean hasEffect() {
		return !getEffect().isEmpty();
	}

	@Override
	public CachedScriptManager<Evogear> getCSM() {
		return cachedEffect;
	}

	@Override
	public boolean execute(EffectParameters ep) {
		if (ep.trigger() == NONE || !hasEffect() || (!isSpell() && hand.getLockTime(Lock.EFFECT) > 0)) return false;
		else if (!getEffect().contains(ep.trigger().name())) {
			if (!isSpell() || !Utils.equalsAny(ep.trigger(), ON_ACTIVATE, ON_TRAP)) {
				return false;
			}
		}

		Shoukan game = getGame();
		try {
			cachedEffect.forScript(getEffect())
					.withConst("evo", this)
					.withConst("game", getGame())
					.withConst("data", stats.getData())
					.withVar("ep", ep)
					.withVar("side", hand.getSide())
					.withVar("props", extractValues(getGame().getLocale()))
					.withVar("trigger", ep.trigger());

			if (!isSpell()) {
				cachedEffect.withVar("self", equipper);
			}

			cachedEffect.run();

			stats.popFlag(Flag.EMPOWERED);
			if (ep.trigger() != ON_TICK) {
				game.trigger(ON_EFFECT, hand.getSide());
			}

			return true;
		} catch (TargetException e) {
			if (targetType != TargetType.NONE && ep.trigger() == Trigger.ON_ACTIVATE) {
				if (Arrays.stream(ep.targets()).allMatch(t -> t.skip().get())) {
					setAvailable(false);
					return false;
				}

				game.getChannel().sendMessage(game.getLocale().get("error/target", game.getLocale().get("str/target_" + targetType))).queue();
			}

			return false;
		} catch (ActivationException e) {
			game.getChannel().sendMessage(game.getLocale().get("error/spell", game.getString(e.getMessage()))).queue();
			return false;
		} catch (Exception e) {
			Drawable<?> source = Utils.getOr(stats.getSource(), this);

			game.getChannel().sendMessage(game.getLocale().get("error/effect")).queue();
			Constants.LOGGER.warn("Failed to execute " + this + " effect\n" + ("/* " + source + " */\n" + getEffect()), e);
			return false;
		}
	}

	@Override
	public void executeAssert(Trigger trigger) {
		if (!Utils.equalsAny(trigger, Trigger.ON_INITIALIZE, Trigger.ON_REMOVE)) return;
		else if (!hasEffect() || !getEffect().contains(trigger.name())) return;

		try {
			Utils.exec(getEffect(), Map.of(
					"evo", this,
					"game", getGame(),
					"data", stats.getData(),
					"ep", new EffectParameters(trigger, getSide()),
					"side", hand.getSide(),
					"props", extractValues(getGame().getLocale()),
					"self", equipper,
					"trigger", trigger
			));
		} catch (Exception ignored) {
		}
	}

	public EffectParameters toParameters(Targeting tgt) {
		return switch (targetType) {
			case NONE -> new EffectParameters(ON_ACTIVATE, getSide());
			case ALLY -> new EffectParameters(ON_ACTIVATE, getSide(), asSource(ON_ACTIVATE),
					new Target(tgt.ally(), hand.getSide(), tgt.allyPos(), ON_SPELL_TARGET, TargetType.ALLY)
			);
			case ENEMY -> new EffectParameters(ON_ACTIVATE, getSide(), asSource(ON_ACTIVATE),
					new Target(tgt.enemy(), hand.getSide().getOther(), tgt.enemyPos(), ON_SPELL_TARGET, TargetType.ENEMY)
			);
			case BOTH -> new EffectParameters(ON_ACTIVATE, getSide(), asSource(ON_ACTIVATE),
					new Target(tgt.ally(), hand.getSide(), tgt.allyPos(), ON_SPELL_TARGET, TargetType.ALLY),
					new Target(tgt.enemy(), hand.getSide().getOther(), tgt.enemyPos(), ON_SPELL_TARGET, TargetType.ENEMY)
			);
		};
	}

	@Override
	public boolean keepOnDestroy() {
		return tier > 0;
	}

	@Override
	public void reset() {
		equipper = null;
		stats.clear();
		if (leech != null) {
			leech.getLeeches().remove(this);
		}
		cachedEffect = new CachedScriptManager<>();

		state = 0b11;
	}

	@Override
	public BufferedImage render(I18N locale, Deck deck) {
		BufferedImage out = new BufferedImage(SIZE.width, SIZE.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = out.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);

		DeckStyling style = deck.getStyling();
		Graph.applyTransformed(g2d, 15, 15, g1 -> {
			if (isFlipped()) {
				g1.drawImage(style.getFrame().getBack(deck), 15, 15, null);
			} else {
				String desc = getDescription(locale);
				BufferedImage img = card.drawCardNoBorder(style.isUsingChrome());

				g1.setClip(style.getFrame().getBoundary());
				g1.drawImage(img, 0, 0, null);
				g1.setClip(null);

				g1.drawImage(style.getFrame().getFront(!desc.isEmpty()), 0, 0, null);
				g1.drawImage(IO.getResourceAsImage("shoukan/icons/tier_" + tier + ".png"), 190, 12, null);

				g1.setFont(FONT);
				g1.setColor(style.getFrame().getPrimaryColor());
				String name = Graph.abbreviate(g1, getVanity().getName(), MAX_NAME_WIDTH);
				Graph.drawOutlinedString(g1, name, 12, 30, 2, style.getFrame().getBackgroundColor());

				if (!desc.isEmpty()) {
					g1.setColor(style.getFrame().getSecondaryColor());
					g1.setFont(Fonts.OPEN_SANS_BOLD.deriveFont(Font.BOLD, 11));

					int y = 276;
					String tags = processTags(locale);
					if (tags != null) {
						g1.drawString(tags, 7, 275);
						y += 11;
					}

					JSONObject values = extractValues(locale);
					Graph.drawMultilineString(g1, desc,
							7, y, 211, 3,
							parseValues(g1, deck.getStyling(), values),
							highlightValues(g1, style.getFrame().isLegacy())
					);
				}

				drawCosts(g1);
				drawAttributes(g1, !desc.isEmpty());

				if (!getCharms().isEmpty()) {
					List<BufferedImage> icons = charms.stream()
							.map(String::valueOf)
							.map(Charm::valueOf)
							.map(Charm::getIcon)
							.filter(Objects::nonNull)
							.limit(2)
							.toList();

					if (!icons.isEmpty()) {
						int y = !desc.isBlank() ? 253 : 319;
						if (getDmg() != 0) y -= 28;
						if (getDfs() != 0) y -= 28;
						if (getCooldown() != 0) y -= 28;

						Graph.applyTransformed(g1, 25, y - 64, g -> {
							if (icons.size() == 1) {
								g.drawImage(icons.get(0), 0, 0, null);
							} else {
								BufferedImage mask = IO.getResourceAsImage("shoukan/charm/mask.png");
								assert mask != null;

								for (int i = 0; i < icons.size(); i++) {
									BufferedImage icon = icons.get(i);
									Graph.applyMask(icon, mask, i, true);
									g.drawImage(icon, 0, 0, null);
								}
								g.drawImage(IO.getResourceAsImage("shoukan/charm/div.png"), 0, 0, null);
							}
						});
					}
				}

				if (hand != null) {
					if (stats.hasFlag(Flag.EMPOWERED)) {
						boolean legacy = hand.getUserDeck().getStyling().getFrame().isLegacy();
						BufferedImage emp = IO.getResourceAsImage("shoukan/frames/" + (legacy ? "old" : "new") + "/empowered.png");

						g2d.drawImage(emp, 0, 0, null);
					}
				}

				if (!isAvailable()) {
					RescaleOp op = new RescaleOp(0.5f, 0, null);
					op.filter(out, out);
				}

				int t = getTier();
				if (t != tier) {
					String str = Utils.sign(t - tier);

					g1.setColor(Color.ORANGE);
					g1.setFont(Drawable.FONT.deriveFont(Font.PLAIN, Drawable.FONT.getSize() * 5f));

					FontMetrics fm = g1.getFontMetrics();
					Graph.drawOutlinedString(g1, str,
							225 / 2 - fm.stringWidth(str) / 2, (225 / 2 + fm.getHeight() / 2),
							6, Color.BLACK
					);
				}
			}
		});

		g2d.dispose();

		return out;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Evogear evogear = (Evogear) o;
		return Objects.equals(id, evogear.id)
			   && Objects.equals(card, evogear.card)
			   && Objects.equals(equipper, evogear.equipper)
			   && SERIAL == evogear.SERIAL;
	}

	public int posHash() {
		return Objects.hash(id, equipper);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, card);
	}

	@Override
	public Evogear fork() throws CloneNotSupportedException {
		Evogear clone = new Evogear(id, card, tier, spell, targetType, charms.clone(), base.clone());
		clone.stats = stats.clone();
		clone.hand = hand;

		return clone;
	}

	@Override
	public String toString() {
		return card.getName();
	}

	public static Evogear getRandom(RandomGenerator rng) {
		List<String> ids = DAO.queryAllNative(String.class, "SELECT card_id FROM evogear WHERE tier > 0 ORDER BY card_id");
		if (ids.isEmpty()) return null;

		return DAO.find(Evogear.class, Utils.getRandomEntry(rng, ids));
	}

	public static Evogear getRandom(RandomGenerator rng, String... filters) {
		XStringBuilder query = new XStringBuilder("SELECT card_id FROM evogear");
		for (String f : filters) {
			query.appendNewLine(f);
		}

		if (filters.length == 0) {
			query.appendNewLine("WHERE tier > 0");
		} else {
			query.appendNewLine("AND tier > 0");
		}

		query.appendNewLine("ORDER BY card_id");

		List<String> ids = DAO.queryAllNative(String.class, query.toString());
		if (ids.isEmpty()) return null;

		return DAO.find(Evogear.class, Utils.getRandomEntry(rng, ids));
	}

	public static XList<Evogear> getByTag(RandomGenerator rng, String... tags) {
		List<String> ids = DAO.queryAllNative(String.class, "SELECT by_tag('evogear', ?1)", (Object[]) tags);

		return new XList<>(DAO.queryAll(Evogear.class, "SELECT e FROM Evogear e WHERE e.id IN ?1 ORDER BY e.id", ids), rng);
	}
}
