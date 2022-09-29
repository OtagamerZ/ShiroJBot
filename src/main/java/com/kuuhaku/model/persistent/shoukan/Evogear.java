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
import com.kuuhaku.exceptions.ActivationException;
import com.kuuhaku.interfaces.shoukan.EffectHolder;
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
import com.kuuhaku.util.json.JSONArray;
import jakarta.persistence.*;
import org.apache.commons.collections4.set.ListOrderedSet;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.intellij.lang.annotations.Language;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.*;
import java.util.List;

import static com.kuuhaku.model.enums.shoukan.Trigger.ON_ACTIVATE;
import static com.kuuhaku.model.enums.shoukan.Trigger.ON_SPELL_TARGET;

@Entity
@Table(name = "evogear")
public class Evogear extends DAO<Evogear> implements EffectHolder<Evogear> {
	@Transient
	public final String KLASS = getClass().getName();

	@Id
	@Column(name = "card_id", nullable = false)
	private String id;

	@OneToOne(optional = false, orphanRemoval = true)
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

	@Column(name = "charms", nullable = false)
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray charms = new JSONArray();

	@Embedded
	private CardAttributes base = new CardAttributes();

	private transient Senshi equipper = null;
	private transient CardExtra stats = new CardExtra();
	private transient Hand hand = null;
	private transient Hand leech = null;

	@Transient
	private byte state = 0b10;
	/*
	0xF
	  └ 000 0111
	         ││└ solid
	         │└─ available
	         └── flipped
	 */

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
		return spell;
	}

	public TargetType getTargetType() {
		return targetType;
	}

	public JSONArray getCharms() {
		return charms;
	}

	public boolean hasCharm(Charm charm) {
		return charms.contains(charm.name());
	}

	public CardAttributes getBase() {
		return base;
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

	public CardExtra getStats() {
		return stats;
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
		return Utils.getOr(stats.getDescription(locale), base.getDescription(locale));
	}

	@Override
	public int getMPCost() {
		return (int) Math.max(0, (base.getMana() + stats.getMana()) * getCostMult());
	}

	@Override
	public int getHPCost() {
		return (int) Math.max(0, (base.getBlood() + stats.getBlood()) * getCostMult());
	}

	@Override
	public int getSCCost() {
		return (int) Math.max(0, (base.getSacrifices() + stats.getSacrifices()) * getCostMult());
	}

	@Override
	public int getDmg() {
		int sum = base.getAtk() + stats.getAtk();

		return (int) (sum * getAttrMult());
	}

	@Override
	public int getDfs() {
		int sum = base.getDfs() + stats.getDfs();

		return (int) (sum * getAttrMult());
	}

	@Override
	public int getDodge() {
		int sum = base.getDodge() + stats.getDodge();

		return (int) (sum * getAttrMult());
	}

	@Override
	public int getBlock() {
		int sum = base.getBlock() + stats.getBlock();

		int min = 0;
		if (hand != null && hand.getOrigin().synergy() == Race.CYBORG) {
			min += 2;
		}

		return (int) ((min + sum) * getAttrMult());
	}

	private double getCostMult() {
		double mult = 1;
		if (hand != null && spell && hand.getOrigin().hasMinor(Race.MYSTICAL)) {
			mult *= 0.9 - (hand.getUserDeck().countRace(Race.MYSTICAL) * 0.01);
		}

		return mult;
	}

	private double getAttrMult() {
		double mult = 1;
		if (hand != null && !spell && hand.getOrigin().hasMinor(Race.MACHINE)) {
			mult *= 1.1 + (hand.getUserDeck().countRace(Race.MACHINE) * 0.01);
			mult *= 1 - Math.max(0, 0.06 * (hand.getOrigin().minor().length - 1));
		}

		return mult * stats.getAttrMult();
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
		return Utils.getOr(stats.getEffect(), base.getEffect());
	}

	public boolean hasEffect() {
		return !getEffect().isEmpty();
	}

	@Override
	public boolean execute(EffectParameters ep) {
		if (hand.getLockTime(Lock.EFFECT) > 0) return false;

		@Language("Groovy") String effect = Utils.getOr(stats.getEffect(), base.getEffect());
		if (!hasEffect() || !effect.contains(ep.trigger().name())) return false;

		try {
			Utils.exec(effect, Map.of(
					"ep", ep,
					"self", equipper,
					"evo", this,
					"trigger", ep.trigger(),
					"game", hand.getGame(),
					"side", hand.getSide()
			));

			return true;
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to execute " + card.getName() + " effect", e);
			return false;
		}
	}

	@Override
	public void executeAssert(Trigger trigger) {
		if (base.isLocked() || isSpell()) return;
		else if (!Utils.equalsAny(trigger, Trigger.ON_INITIALIZE, Trigger.ON_REMOVE)) return;
		else if (!hasEffect() || !getEffect().contains(trigger.name())) return;

		try {
			Utils.exec(getEffect(), Map.of(
					"ep", new EffectParameters(trigger),
					"self", equipper,
					"evo", this,
					"trigger", trigger,
					"game", hand.getGame(),
					"side", hand.getSide()
			));
		} catch (ActivationException ignored) {
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to execute " + card.getName() + " effect", e);
		}
	}

	public EffectParameters toParameters(Targeting tgt) {
		return switch (targetType) {
			case NONE -> new EffectParameters(ON_ACTIVATE);
			case ALLY -> new EffectParameters(ON_ACTIVATE, asSource(ON_ACTIVATE),
					new Target(tgt.ally(), hand.getSide(), tgt.allyPos(), ON_SPELL_TARGET, TargetType.ALLY)
			);
			case ENEMY -> new EffectParameters(ON_ACTIVATE, asSource(ON_ACTIVATE),
					new Target(tgt.enemy(), hand.getSide().getOther(), tgt.enemyPos(), ON_SPELL_TARGET, TargetType.ENEMY)
			);
			case BOTH -> new EffectParameters(ON_ACTIVATE, asSource(ON_ACTIVATE),
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
		stats = stats.clone();
		if (leech != null) {
			leech.getLeeches().remove(this);
		}

		state = 0b11;
	}

	@Override
	public BufferedImage render(I18N locale, Deck deck) {
		DeckStyling style = deck.getStyling();
		if (isFlipped()) return style.getFrame().getBack(deck);

		String desc = getDescription(locale);

		BufferedImage img = card.drawCardNoBorder(style.isUsingChrome());
		BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = out.createGraphics();
		g2d.setRenderingHints(Constants.HD_HINTS);

		g2d.setClip(style.getFrame().getBoundary());
		g2d.drawImage(img, 0, 0, null);
		g2d.setClip(null);

		g2d.drawImage(style.getFrame().getFront(!desc.isEmpty()), 0, 0, null);
		g2d.drawImage(IO.getResourceAsImage("shoukan/icons/tier_" + getTier() + ".png"), 190, 12, null);

		g2d.setFont(FONT);
		g2d.setColor(style.getFrame().getPrimaryColor());
		String name = Graph.abbreviate(g2d, getVanity().getName(), MAX_NAME_WIDTH);
		Graph.drawOutlinedString(g2d, name, 12, 30, 2, style.getFrame().getBackgroundColor());

		if (!desc.isEmpty()) {
			g2d.setColor(style.getFrame().getSecondaryColor());
			g2d.setFont(Fonts.OPEN_SANS_BOLD.deriveFont(Font.BOLD, 11));

			int y = 276;
			String tags = processTags(locale);
			if (tags != null) {
				g2d.drawString(tags, 7, 275);
				y += 11;
			}

			Graph.drawMultilineString(g2d, desc,
					7, y, 211, 3,
					parseValues(g2d, deck, this), highlightValues(g2d)
			);
		}

		drawCosts(g2d);
		drawAttributes(g2d, !desc.isEmpty());

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

				Graph.applyTransformed(g2d, 25, y - 64, g -> {
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

		if (!isAvailable()) {
			RescaleOp op = new RescaleOp(0.5f, 0, null);
			op.filter(out, out);
		}

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
				&& Objects.equals(equipper, evogear.equipper);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, card);
	}

	@Override
	public Evogear clone() throws CloneNotSupportedException {
		return (Evogear) super.clone();
	}

	@Override
	public String toString() {
		return card.getName();
	}

	public static Evogear getRandom() {
		String id = DAO.queryNative(String.class, "SELECT card_id FROM evogear WHERE tier > 0 ORDER BY RANDOM()");
		if (id == null) return null;

		return DAO.find(Evogear.class, id);
	}

	public static Evogear getRandom(String... filters) {
		XStringBuilder query = new XStringBuilder("SELECT card_id FROM evogear");
		for (String f : filters) {
			query.appendNewLine(f);
		}

		if (filters.length == 0) {
			query.appendNewLine("WHERE tier > 0");
		} else {
			query.appendNewLine("AND tier > 0");
		}

		query.appendNewLine("ORDER BY RANDOM()");

		String id = DAO.queryNative(String.class, query.toString());
		if (id == null) return null;

		return DAO.find(Evogear.class, id);
	}
}
