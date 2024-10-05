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
import com.kuuhaku.model.common.BondedList;
import com.kuuhaku.model.common.CachedScriptManager;
import com.kuuhaku.model.common.XList;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.common.shoukan.CardExtra;
import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.common.shoukan.SlotColumn;
import com.kuuhaku.model.common.shoukan.TagBundle;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.*;
import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.shoukan.EffectParameters;
import com.kuuhaku.model.records.shoukan.Target;
import com.kuuhaku.model.records.shoukan.Targeting;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.*;
import com.ygimenez.json.JSONArray;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import org.apache.commons.collections4.set.ListOrderedSet;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

import static com.kuuhaku.model.enums.shoukan.Trigger.*;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "evogear", schema = "kawaipon")
public class Evogear extends DAO<Evogear> implements EffectHolder<Evogear> {
	@Transient
	public final String KLASS = getClass().getName();
	@Transient
	public final long SERIAL = ThreadLocalRandom.current().nextLong();

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
	private final transient CachedScriptManager cachedEffect = new CachedScriptManager();
	private transient StashedCard stashRef = null;
	private transient BondedList<?> currentStack;
	private transient Trigger currentTrigger = null;

	@Transient
	private short state = 0b1;
	/*
	0xF F
	  │ └ 000 1111
	  │       │││└─ available
	  │       ││└── flipped
	  │       │└─── ethereal
	  │       └──── manipulated
	  └ cooldown (0 - 15)
	 */

	public Evogear() {
	}

	public Evogear(String id, Card card, int tier, boolean spell, TargetType targetType, JSONArray charms, CardAttributes base, CardExtra stats, StashedCard stashRef) {
		this.id = id;
		this.card = card;
		this.tier = tier;
		this.spell = spell;
		this.targetType = targetType;
		this.charms = charms;
		this.base = base;
		this.stats = stats;
		this.stashRef = stashRef;
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
		int sum = tier;
		if (hand != null) {
			if (hand.getOrigins().isPure(Race.MACHINE)) {
				sum += 1;
			}
		}

		return sum + (int) stats.getTier().get();
	}

	public boolean isSpell() {
		return spell && equipper == null;
	}

	public boolean isParasite() {
		return base.getTags().contains("PARASITE") || base.getTags().contains("STRATAGEM");
	}

	public TargetType getTargetType() {
		if (stats.getSource() instanceof Senshi s) {
			return s.getTargetType();
		}

		return targetType;
	}

	public JSONArray getCharms() {
		return charms;
	}

	@Override
	public boolean hasCharm(Charm charm) {
		return getCharms().contains(charm.name());
	}

	@Override
	public SlotColumn getSlot() {
		if (equipper != null) {
			return equipper.getSlot();
		}

		return new SlotColumn(getGame(), hand.getSide(), -1);
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
	public TagBundle getTagBundle() {
		TagBundle out = new TagBundle();
		if (hasEffect()) {
			if (isSpell()) {
				out.add("tag", "spell");
			} else {
				out.add("tag", "effect");
			}
		}
		for (Object tag : base.getTags()) {
			out.add("tag", ((String) tag).toLowerCase());
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
	public String getDescription(I18N locale) {
		EffectHolder<?> source = getSource();
		String out = Utils.getOr(source.getStats().getDescription(locale), source.getBase().getDescription(locale));
		if (hand != null) {
			if (hand.getOrigins().major() == Race.DEMON) {
				out = out.replace("$mp", "($hp/($bhp*0.08))");
			}
		}

		return out;
	}

	@Override
	public int getMPCost(boolean ignoreRace) {
		int cost = Math.max(0, Calc.round((base.getMana() + stats.getMana().get()) * getCostMult()));
		if (hand != null && !ignoreRace) {
			if (hand.getOrigins().synergy() == Race.CELESTIAL) {
				cost = hand.getUserDeck().getAverageMPCost();
			}

			if (hand.getOrigins().major() == Race.DEMON) {
				cost = 0;
			}
		}

		return cost;
	}

	@Override
	public int getHPCost(boolean ignoreRace) {
		int cost = Math.max(0, Calc.round((base.getBlood() + stats.getBlood().get()) * getCostMult()));
		if (hand != null) {
			if (!ignoreRace) {
				if (hand.getOrigins().synergy() == Race.CELESTIAL) {
					cost = hand.getUserDeck().getAverageHPCost();
				}
			}

			int mp = getMPCost(true);

			if (hand.getOrigins().major() == Race.DEMON) {
				cost += (int) (hand.getBase().hp() * 0.08 * mp);
			}
		}

		return cost;
	}

	@Override
	public int getSCCost() {
		return Math.max(0, Calc.round((base.getSacrifices() + stats.getSacrifices().get()) * getCostMult()));
	}

	@Override
	public int getDmg() {
		int sum = base.getAtk() + (int) stats.getAtk().get();

		if (hand != null && hand.getOrigins().synergy() == Race.CYBERBEAST) {
			sum += getCards(getSide()).stream().mapToInt(Senshi::getParry).sum();
		}

		return Calc.round(sum * getAttrMult() * stats.getAtkMult().get());
	}

	@Override
	public int getDfs() {
		int sum = base.getDfs() + (int) stats.getDfs().get();

		return Calc.round(sum * getAttrMult() * stats.getDfsMult().get());
	}

	@Override
	public int getDodge() {
		int sum = base.getDodge() + (int) stats.getDodge().get();

		int min = 0;
		if (hand != null && hand.getOrigins().synergy() == Race.GEIST) {
			min += 10;
		}

		return Utils.clamp(min + sum, min, 100);
	}

	@Override
	public int getParry() {
		int sum = base.getParry() + (int) stats.getParry().get();

		int min = 0;
		if (hand != null && hand.getOrigins().synergy() == Race.CYBORG) {
			min += 10;
		}

		return Utils.clamp(min + sum, min, 100);
	}

	@Override
	public double getCostMult() {
		double mult = stats.getCostMult().get();
		if (hand != null) {
			if ((!spell && hand.getOrigins().hasMinor(Race.MACHINE)) || (spell && hand.getOrigins().hasMinor(Race.MYSTICAL))) {
				mult *= 0.8;
			}

			if (hand.getOrigins().synergy() == Race.DULLAHAN) {
				mult *= 2;
			}
		}

		return mult;
	}

	@Override
	public double getAttrMult() {
		double mult = stats.getAttrMult().get();
		if (hand != null) {
			if (!spell && hand.getOrigins().hasMinor(Race.MACHINE)) {
				mult *= 1.14 + (hand.getUserDeck().countRace(Race.MACHINE) * 0.02);
			}

			if (getGame() != null && getGame().getArcade() == Arcade.OVERCHARGE) {
				mult *= 1.5;
			}

			if (hand.getOrigins().synergy() == Race.FABLED) {
				mult *= getPower();
			}
		}

		if (!isSpell() && stashRef != null) {
			mult *= 1 + stashRef.getQuality() / 100;
		}

		return mult;
	}

	@Override
	public double getPower() {
		double mult = stats.getPower().get() * (hasFlag(Flag.EMPOWERED) ? 1.5 : 1);
		if (hand != null) {
			if (spell) {
				if (hand.getOrigins().hasMinor(Race.MYSTICAL)) {
					mult *= 1.14 + (hand.getUserDeck().countRace(Race.MYSTICAL) * 0.02);
				}

				if (hand.getOrigins().isPure(Race.MYSTICAL)) {
					mult *= 1.5;
				}
			}

			if (hand.getOrigins().major() == Race.MIXED) {
				mult *= 1 - 0.07 * hand.getOrigins().minor().length;
			}

			if (getGame() != null && getGame().getArcade() == Arcade.OVERCHARGE) {
				mult *= 1.75;
			}
		}

		if (isSpell() && stashRef != null) {
			mult *= 1 + stashRef.getQuality() / 100;
		}

		return mult;
	}

	@Override
	public boolean isAvailable() {
		return Bit32.on(state, 0);
	}

	@Override
	public void setAvailable(boolean available) {
		state = (short) Bit32.set(state, 0, available);
	}

	@Override
	public boolean isFlipped() {
		if (equipper != null) {
			return equipper.isFlipped() || Bit32.on(state, 1);
		}

		return Bit32.on(state, 1);
	}

	@Override
	public void setFlipped(boolean flipped) {
		state = (short) Bit32.set(state, 1, flipped);
	}

	@Override
	public boolean isEthereal() {
		return Bit32.on(state, 2);
	}

	@Override
	public void setEthereal(boolean ethereal) {
		state = (short) Bit32.set(state, 2, ethereal);
	}

	@Override
	public boolean isManipulated() {
		return Bit32.on(state, 3);
	}

	@Override
	public void setManipulated(boolean manipulated) {
		state = (short) Bit32.set(state, 3, manipulated);
	}

	@Override
	public int getCooldown() {
		return Bit32.get(state, 1, 4);
	}

	@Override
	public void setCooldown(int time) {
		short curr = (short) Bit32.get(state, 1, 4);
		state = (short) Bit32.set(state, 1, Math.max(curr, time), 4);
	}

	public void reduceCooldown(int time) {
		short curr = (short) Bit32.get(state, 1, 4);
		state = (short) Bit32.set(state, 1, Math.max(0, curr - time), 4);
	}

	public String getEffect() {
		EffectHolder<?> source = getSource();
		return Utils.getOr(source.getStats().getEffect(), source.getBase().getEffect());
	}

	@Override
	public boolean hasEffect() {
		return !getEffect().isEmpty() && !hasFlag(Flag.NO_EFFECT);
	}

	@Override
	public Trigger getCurrentTrigger() {
		return currentTrigger;
	}

	@Override
	public CachedScriptManager getCSM() {
		return cachedEffect;
	}

	@Override
	public boolean execute(EffectParameters ep) {
		if (!hasEffect()) return false;
		else if (!hasTrueEffect()) {
			if (!isSpell() && hand.getLockTime(Lock.EFFECT) > 0) return false;
		}

		if (!getEffect().contains(ep.trigger().name())) {
			if (!isSpell() || !Utils.equalsAny(ep.trigger(), ON_ACTIVATE, ON_TRAP)) {
				return false;
			}
		}

		Shoukan game = getGame();
		if (base.isLocked(ep.trigger()) || ep.trigger() == NONE) {
			return false;
		}

		try {
			base.lock(ep.trigger());
			if (getSlot().getIndex() > -1 && ep.trigger() != ON_TICK) {
				execute(new EffectParameters(ON_TICK, getSide(), asSource(ON_TICK)));
			}

			currentTrigger = ep.trigger();
			CachedScriptManager csm = getCSM();
			csm.assertOwner(getSource(), () -> parseDescription(hand, getGame().getLocale()))
					.forScript(getEffect())
					.withConst("evo", this)
					.withConst("game", getGame())
					.withConst("data", stats.getData())
					.withVar("ep", ep.forSide(getSide()))
					.withVar("side", getSide())
					.withVar("trigger", ep.trigger());

			if (!isSpell()) {
				if (stats.getSource() instanceof Senshi) {
					csm.withVar("me", stats.getSource());
				}

				if (stats.getSource() instanceof Senshi s && equipper == null) {
					csm.withVar("self", s);
				} else {
					csm.withVar("self", equipper);
				}
			}

			csm.run();

			if (isSpell()) {
				hand.getData().put("last_spell", this);
				hand.getData().put("last_evogear", this);
				getGame().trigger(ON_SPELL, getSide());

				if (hand.getOrigins().isPure(Race.MYSTICAL)) {
					hand.modMP(1);
				}
			}

			if (ep.trigger() != ON_TICK) {
				hasFlag(Flag.EMPOWERED, true);
			}

			return true;
		} catch (TargetException e) {
			if (targetType != TargetType.NONE && ep.trigger() == Trigger.ON_ACTIVATE) {
				if (Arrays.stream(ep.targets()).allMatch(t -> t.skip().get())) {
					setAvailable(false);
					return false;
				}

				game.getChannel().sendMessage(game.getString("error/target", game.getString("str/target_" + targetType))).queue();
			}

			return false;
		} catch (ActivationException e) {
			game.getChannel().sendMessage(game.getString("error/spell", game.getString(e.getMessage()))).queue();
			return false;
		} catch (Exception e) {
			Drawable<?> source = Utils.getOr(stats.getSource(), this);

			game.getChannel().sendMessage(game.getString("error/effect")).queue();
			Constants.LOGGER.warn("Failed to execute {} effect\n{}", this, "/* " + source + " */\n" + getEffect(), e);
			return false;
		} finally {
			currentTrigger = null;
			base.unlock(ep.trigger());
		}
	}

	@Override
	public void executeAssert(Trigger trigger) {
		if (!Utils.equalsAny(trigger, ON_INITIALIZE, ON_REMOVE) || !hasEffect()) return;
		else if (!getEffect().contains(trigger.name()) && !getTags().contains("STRATAGEM")) return;

		if (trigger == ON_REMOVE) {
			getGame().unbind(this);
		}

		try {
			CachedScriptManager csm = getCSM();
			csm.assertOwner(getSource(), () -> parseDescription(hand, getGame().getLocale()))
					.forScript(getEffect())
					.withConst("evo", this)
					.withConst("game", getGame())
					.withConst("data", stats.getData())
					.withVar("ep", new EffectParameters(trigger, getSide()))
					.withVar("side", getSide())
					.withVar("trigger", trigger);

			if (!isSpell()) {
				if (stats.getSource() instanceof Senshi) {
					csm.withVar("me", stats.getSource());
				}

				if (stats.getSource() instanceof Senshi s && equipper == null) {
					csm.withVar("self", s);
				} else {
					csm.withVar("self", equipper);
				}
			}

			csm.run();
		} catch (Exception ignored) {
		}
	}

	public EffectParameters toParameters(Targeting tgt) {
		return switch (targetType) {
			case NONE -> new EffectParameters(ON_ACTIVATE, getSide());
			case ALLY -> new EffectParameters(ON_ACTIVATE, getSide(), asSource(ON_ACTIVATE),
					new Target(tgt.ally(), tgt.allyPos(), ON_SPELL_TARGET, TargetType.ALLY)
			);
			case ENEMY -> new EffectParameters(ON_ACTIVATE, getSide(), asSource(ON_ACTIVATE),
					new Target(tgt.enemy(), tgt.enemyPos(), ON_SPELL_TARGET, TargetType.ENEMY)
			);
			case BOTH -> new EffectParameters(ON_ACTIVATE, getSide(), asSource(ON_ACTIVATE),
					new Target(tgt.ally(), tgt.allyPos(), ON_SPELL_TARGET, TargetType.ALLY),
					new Target(tgt.enemy(), tgt.enemyPos(), ON_SPELL_TARGET, TargetType.ENEMY)
			);
		};
	}

	@Override
	public StashedCard getStashRef() {
		return stashRef;
	}

	@Override
	public void setStashRef(StashedCard sc) {
		stashRef = sc;
	}

	@Override
	public BondedList<?> getCurrentStack() {
		return currentStack;
	}

	@Override
	public void setCurrentStack(BondedList<?> stack) {
		if (getTags().contains("STRATAGEM") && getGame().getArcade() != Arcade.CARDMASTER) {
			executeAssert(ON_INITIALIZE);
			getGame().getChannel().sendMessage(getGame().getString("str/stratagem_use", this)).queue();
			return;
		}

		currentStack = stack;
	}

	@Override
	public void reset() {
		equipper = null;
		stats.clear();
		base.unlockAll();
		state = 0b1;
	}

	@Override
	public BufferedImage render(I18N locale, Deck deck) {
		if (hand == null) {
			hand = new Hand(deck);
		}

		BufferedImage out = new BufferedImage(SIZE.width, SIZE.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = out.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);

		DeckStyling style = deck.getStyling();
		Graph.applyTransformed(g2d, 15, 15, g1 -> {
			if (isFlipped()) {
				g1.drawImage(style.getFrame().getBack(deck), 0, 0, null);
				parseDescription(hand, getGame().getLocale());

				if (!isAvailable() || isManipulated()) {
					RescaleOp op = new RescaleOp(0.5f, 0, null);
					op.filter(out, out);
				}
			} else {
				String desc = getDescription(locale);
				BufferedImage img = card.drawCardNoBorder(Utils.getOr(() -> stashRef.isChrome(), false));

				g1.setClip(style.getFrame().getBoundary());
				g1.drawImage(img, 0, 0, null);
				g1.setClip(null);

				g1.drawImage(style.getFrame().getFront(!desc.isEmpty()), 0, 0, null);
				g1.drawImage(IO.getResourceAsImage("shoukan/icons/tier_" + tier + ".png"), 190, 12, null);

				g1.setFont(FONT);
				g1.setColor(style.getFrame().getPrimaryColor());
				String name = Graph.abbreviate(g1, getVanity().getName(), MAX_NAME_WIDTH);
				Graph.drawOutlinedString(g1, name, 12, 30, 2, style.getFrame().getBackgroundColor());

				if (!stats.getWrite().isBlank()) {
					g1.setColor(Color.ORANGE);
					g1.setFont(Fonts.NOTO_SANS_EXTRABOLD.deriveBold(15f));

					String str = stats.getWrite();
					FontMetrics fm = g1.getFontMetrics();
					Graph.drawOutlinedString(g1, str,
							225 / 2 - fm.stringWidth(str) / 2, 39 + fm.getHeight() / 2,
							2, Color.BLACK
					);
				}

				drawDescription(g1, hand, locale);
				drawCosts(g1);
				drawAttributes(g1, !desc.isEmpty());

				if (!getCharms().isEmpty()) {
					List<BufferedImage> icons = getCharms().stream()
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
								g.drawImage(icons.getFirst(), 0, 0, null);
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

				if (!isAvailable() || isManipulated()) {
					RescaleOp op = new RescaleOp(0.5f, 0, null);
					op.filter(out, out);
				}

				if (hand != null) {
					boolean legacy = hand.getUserDeck().getStyling().getFrame().isLegacy();
					String path = "shoukan/frames/state/" + (legacy ? "old" : "new");

					if (hasFlag(Flag.EMPOWERED)) {
						BufferedImage ovr = IO.getResourceAsImage(path + "/empowered.png");
						g2d.drawImage(ovr, 0, 0, null);
					}

					if (isEthereal()) {
						BufferedImage ovr = IO.getResourceAsImage(path + "/ethereal.png");
						g2d.drawImage(ovr, 0, 0, null);
					}

					if (isManipulated()) {
						BufferedImage ovr = IO.getResourceAsImage("shoukan/states/locked.png");
						g2d.drawImage(ovr, 15, 15, null);
					}
				}

				int t = getTier();
				if (t != tier) {
					String str = Utils.sign(t - tier);

					g1.setColor(Color.ORANGE);
					g1.setFont(Drawable.FONT.deriveFont(Drawable.FONT.getSize() * 5f));

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
		return Objects.equals(id, evogear.id) && Objects.equals(card, evogear.card) && SERIAL == evogear.SERIAL;
	}

	public int posHash() {
		return Objects.hash(id, equipper);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, card, SERIAL);
	}

	@Override
	public Evogear fork() throws CloneNotSupportedException {
		Evogear clone = new Evogear(id, card, tier, spell, targetType, charms.clone(), base.clone(), stats.clone(), stashRef);
		clone.stats = stats.clone();
		clone.hand = hand;
		clone.state = (byte) (state & (0b111 | 0xF0));
		clone.stashRef = stashRef;

		return clone;
	}

	@Override
	public String toString() {
		if ((isFlipped() || getBase().getTags().contains("SECRET")) && hand != null) {
			return getGame().getString("str/" + (isSpell() ? "a_spell" : "an_equipment"));
		}

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
		List<String> ids = DAO.queryAllNative(String.class, "SELECT by_tag('evogear', ?1)", (Object) tags);

		return new XList<>(DAO.queryAll(Evogear.class, "SELECT e FROM Evogear e WHERE e.id IN ?1 ORDER BY e.id", ids), rng);
	}
}
