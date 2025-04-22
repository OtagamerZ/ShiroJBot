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
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.Shoukan;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.interfaces.shoukan.EffectHolder;
import com.kuuhaku.model.common.BondedList;
import com.kuuhaku.model.common.CachedScriptManager;
import com.kuuhaku.model.common.XList;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.common.shoukan.CardExtra;
import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.common.shoukan.TagBundle;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.*;
import com.kuuhaku.model.persistent.converter.JSONObjectConverter;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.user.StashedCard;
import com.kuuhaku.model.records.shoukan.EffectParameters;
import com.kuuhaku.util.*;
import com.kuuhaku.util.Graph;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.*;
import kotlin.Pair;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;
import org.hibernate.type.SqlTypes;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

import static com.kuuhaku.model.enums.shoukan.Trigger.*;
import static com.kuuhaku.model.enums.shoukan.Trigger.ON_REMOVE;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "field", schema = "kawaipon")
public class Field extends DAO<Field> implements EffectHolder<Field> {
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

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "modifiers", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONObjectConverter.class)
	private JSONObject modifiers = new JSONObject();

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private FieldType type = FieldType.NONE;

	@Column(name = "effect_only", nullable = false)
	private boolean effectOnly = false;

	@Embedded
	private CardAttributes base = new CardAttributes();

	private transient Hand originalHand = null;
	private transient Hand hand = null;
	private final transient CachedScriptManager cachedEffect = new CachedScriptManager();
	private transient CardExtra stats = new CardExtra();
	private transient StashedCard stashRef = null;
	private transient BondedList<?> currentStack;
	private transient Trigger currentTrigger = null;

	@Transient
	private byte state = 0b1;
	/*
	0xF
	  └ 000 1111
	        │││└─ available
	        ││└── bamboozled
	        │└─── ethereal
	        └──── manipulated
	 */

	public Field() {
	}

	public Field(String id, Card card, JSONObject modifiers, FieldType type, boolean effectOnly, CardAttributes base, CardExtra stats, StashedCard stashRef) {
		this.id = id;
		this.card = card;
		this.modifiers = modifiers;
		this.type = type;
		this.effectOnly = effectOnly;
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

	public JSONObject getModifiers() {
		if (hand != null && getGame() != null && !wasBamboozled()) {
			if (Utils.equalsAny(Race.PIXIE, hand.getOrigins().synergy(), hand.getOther().getOrigins().synergy())) {
				int mods = modifiers.size();
				modifiers.clear();

				for (int i = 0; i < mods; i++) {
					Race r = Utils.getRandomEntry(hand.getGame().getRng(), Race.validValues());
					double mod = Calc.rng(-0.5, 0.5, hand.getGame().getRng());

					modifiers.put(r.name(), mod);
				}

				setBamboozled(true);
			}
		}

		JSONObject mods = modifiers;
		if (stashRef != null) {
			mods = new JSONObject();
			for (Map.Entry<String, Object> e : modifiers.entrySet()) {
				mods.put(e.getKey(), ((Double) e.getValue()) * (1 + stashRef.getQuality() / 100));
			}
		}

		return mods;
	}

	public void setModifiers(JSONObject modifiers) {
		this.modifiers = modifiers;
	}

	public FieldType getType() {
		return type;
	}

	public void setType(FieldType type) {
		this.type = type;
	}

	public boolean isEffectOnly() {
		return effectOnly;
	}

	@Override
	public boolean hasCharm(Charm charm) {
		return false;
	}

	@Override
	public CardAttributes getBase() {
		return base;
	}

	@Override
	public CardExtra getStats() {
		return stats;
	}

	@Override
	public TagBundle getTagBundle() {
		TagBundle out = new TagBundle();

		for (Object tag : base.getTags()) {
			out.add("tag", ((String) tag).toLowerCase());
		}

		return out;
	}

	@Override
	public Hand getOriginalHand() {
		return originalHand;
	}

	@Override
	public Hand getHand() {
		return hand;
	}

	@Override
	public void setHand(Hand hand) {
		if (originalHand == null) {
			this.originalHand = hand;
		}

		this.hand = hand;
	}

	@Override
	public boolean isAvailable() {
		return Bit32.on(state, 0);
	}

	@Override
	public void setAvailable(boolean available) {
		state = (byte) Bit32.set(state, 0, available);
	}

	public boolean wasBamboozled() {
		return Bit32.on(state, 1);
	}

	public void setBamboozled(boolean available) {
		state = (byte) Bit32.set(state, 1, available);
	}

	public boolean isEthereal() {
		return Bit32.on(state, 2);
	}

	public void setEthereal(boolean ethereal) {
		state = (byte) Bit32.set(state, 2, ethereal);
	}

	@Override
	public boolean isManipulated() {
		return Bit32.on(state, 3);
	}

	@Override
	public void setManipulated(boolean manipulated) {
		state = (byte) Bit32.set(state, 3, manipulated);
	}

	public String getEffect() {
		return Utils.getOr(stats.getEffect(), base.getEffect());
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
			if (hand.getLockTime(Lock.EFFECT) > 0) return false;
		}

		if (!getEffect().contains(ep.trigger().name())) {
			return false;
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
					.withConst("game", getGame())
					.withConst("data", stats.getData())
					.withVar("ep", ep.forSide(getSide()))
					.withVar("side", getSide())
					.withVar("trigger", ep.trigger());

			if (stats.getSource() instanceof Senshi) {
				csm.withVar("me", stats.getSource());
			}

			csm.run();

			if (ep.trigger() != ON_TICK) {
				hasFlag(Flag.EMPOWERED, true);
			}

			return true;
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
		else if (!getEffect().contains(trigger.name())) return;

		if (trigger == ON_REMOVE) {
			getGame().unbind(this);
		}

		try {
			CachedScriptManager csm = getCSM();
			csm.assertOwner(getSource(), () -> parseDescription(hand, getGame().getLocale()))
					.forScript(getEffect())
					.withConst("game", getGame())
					.withConst("data", stats.getData())
					.withVar("ep", new EffectParameters(trigger, getSide()))
					.withVar("side", getSide())
					.withVar("trigger", trigger);

			if (stats.getSource() instanceof Senshi) {
				csm.withVar("me", stats.getSource());
			}

			csm.run();
		} catch (Exception ignored) {
		}
	}

	public boolean isActive() {
		return getGame().getArena().getField().equals(this);
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
		currentStack = stack;
	}

	@Override
	public void reset() {
		state = 0b1;
	}

	@Override
	public BufferedImage render(I18N locale) {
		BufferedImage out = new BufferedImage(SIZE.width, SIZE.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = out.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);

		Deck deck = originalHand.getUserDeck();
		Graph.applyTransformed(g2d, 15, 15, g1 -> {
			if (isFlipped()) {
				g1.drawImage(deck.getFrame().getBack(deck), 0, 0, null);

				if (!isAvailable() || isManipulated()) {
					RescaleOp op = new RescaleOp(0.5f, 0, null);
					op.filter(out, out);
				}
			} else {
				String desc = getDescription(locale);
				BufferedImage img = getVanity().drawCardNoBorder(Utils.getOr(() -> stashRef.isChrome(), false));

				g1.setClip(deck.getFrame().getBoundary());
				g1.drawImage(img, 0, 0, null);
				g1.setClip(null);

				g1.drawImage(deck.getFrame().getFront(!desc.isBlank()), 0, 0, null);

				drawName(g1);
				drawDescription(g1, hand, locale);

				if (type != FieldType.NONE) {
					BufferedImage icon = type.getIcon();
					assert icon != null;

					g1.drawImage(icon, 200 - icon.getWidth(), 55, null);
				}

				g1.setFont(FONT);
				FontMetrics m = g1.getFontMetrics();

				List<Pair<Race, Double>> mods = getModifiers().entrySet().stream()
						.map(e -> new Pair<>(Race.valueOf(e.getKey()), ((Number) e.getValue()).doubleValue()))
						.sorted(Comparator.comparing(Pair::getSecond))
						.toList();

				int y = !desc.isBlank() ? 213 : 279;
				for (Pair<Race, Double> mod : mods) {
					if (mod.getSecond() == 0) continue;

					BufferedImage icon = mod.getFirst().getIcon();
					g1.drawImage(icon, 23, y, null);
					g1.setColor(mod.getFirst().getColor());
					Graph.drawOutlinedString(g1, Utils.sign((int) (mod.getSecond() * 100)) + "%",
							23 + icon.getWidth() + 5, y - 4 + (icon.getHeight() + m.getHeight()) / 2,
							BORDER_WIDTH, Color.BLACK
					);

					y -= 25;
				}

				if (!isAvailable() || isManipulated()) {
					RescaleOp op = new RescaleOp(0.5f, 0, null);
					op.filter(out, out);
				}

				if (hand != null) {
					boolean legacy = hand.getUserDeck().getFrame().isLegacy();
					String path = "shoukan/frames/state/" + (legacy ? "old" : "new");

					if (isEthereal()) {
						BufferedImage ovr = IO.getResourceAsImage(path + "/ethereal.png");
						g1.drawImage(ovr, 0, 0, null);
					}

					if (isManipulated()) {
						BufferedImage ovr = IO.getResourceAsImage("shoukan/states/locked.png");
						g1.drawImage(ovr, 15, 15, null);
					}
				}
			}
		});

		g2d.dispose();

		return out;
	}

	public BufferedImage renderBackground() {
		byte[] bytes = Main.getCacheManager().computeResource("field-" + id, (k, v) -> {
			if (v != null && v.length > 0) return v;

			BufferedImage bi = IO.getResourceAsImage("shoukan/arenas/" + id + ".jpg");
			if (bi == null) {
				bi = IO.getResourceAsImage("shoukan/arenas/DEFAULT.jpg");
			}

			Graphics2D g2d = bi.createGraphics();
			g2d.setRenderingHints(Constants.SD_HINTS);

			BufferedImage aux = IO.getImage(Shoukan.SKIN_PATH + "middle.png");
			g2d.drawImage(aux, bi.getWidth() / 2 - aux.getWidth() / 2, bi.getHeight() / 2 - aux.getHeight() / 2, null);

			aux = IO.getResourceAsImage("shoukan/overlay/middle.png");
			g2d.drawImage(aux, bi.getWidth() / 2 - aux.getWidth() / 2, bi.getHeight() / 2 - aux.getHeight() / 2, null);

			g2d.dispose();

			return IO.getBytes(bi);
		});

		return IO.imageFromBytes(bytes);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Field field = (Field) o;
		return SERIAL == field.SERIAL
			   && Objects.equals(id, field.id)
			   && Objects.equals(card, field.card);
	}

	@Override
	public int hashCode() {
		return Objects.hash(SERIAL, id, card);
	}

	@Override
	public Field fork() throws CloneNotSupportedException {
		Field clone = new Field(id, card, modifiers.clone(), type, effectOnly, base.clone(), stats.clone(), stashRef);
		clone.hand = hand;
		clone.state = (byte) (state & 0b111);

		return clone;
	}

	@Override
	public String toString() {
		return card.getName();
	}

	public static Field getRandom(RandomGenerator rng) {
		List<String> ids = DAO.queryAllNative(String.class, "SELECT card_id FROM field WHERE NOT effect ORDER BY card_id");
		if (ids.isEmpty()) return null;

		return DAO.find(Field.class, Utils.getRandomEntry(rng, ids));
	}

	public static Field getRandom(RandomGenerator rng, String... filters) {
		XStringBuilder query = new XStringBuilder("SELECT card_id FROM field");
		for (String f : filters) {
			query.appendNewLine(f);
		}

		if (filters.length == 0) {
			query.appendNewLine("WHERE NOT effect");
		} else {
			query.appendNewLine("AND NOT effect");
		}

		query.appendNewLine("ORDER BY card_id");

		List<String> ids = DAO.queryAllNative(String.class, query.toString());
		if (ids.isEmpty()) return null;

		return DAO.find(Field.class, Utils.getRandomEntry(rng, ids));
	}

	public static XList<Field> getByTag(RandomGenerator rng, String... tags) {
		List<String> ids = DAO.queryAllNative(String.class, "SELECT by_tag('field', ?1)", (Object) tags);

		return new XList<>(DAO.queryAll(Field.class, "SELECT f FROM Field f WHERE f.id IN ?1 ORDER BY f.id", ids), rng);
	}
}
