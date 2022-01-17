/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.handlers.games.tabletop.games.shoukan;

import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Arguments;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Charm;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.FrameColor;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.records.CardLink;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.Deck;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONArray;
import com.kuuhaku.utils.JSONObject;
import groovy.lang.GroovyShell;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Entity
@Table(name = "equipment")
public class Equipment implements Drawable, Cloneable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@OneToOne(fetch = FetchType.EAGER)
	private Card card;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int atk;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int def;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int mana;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int blood;

	@Column(columnDefinition = "VARCHAR(175) NOT NULL DEFAULT ''")
	private String description;

	@Column(columnDefinition = "TEXT")
	private String effect = "";

	@Column(columnDefinition = "TEXT")
	private String finalization = "";

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int tier;

	@Column(columnDefinition = "VARCHAR(255)")
	private String charms = null;

	@Column(columnDefinition = "VARCHAR(255)")
	private String tags = null;

	@Enumerated(value = EnumType.STRING)
	private Arguments argType = Arguments.NONE;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean effectOnly = false;

	private transient Shoukan game = null;
	private transient Account acc = null;
	private transient Bonus bonus = new Bonus();
	private transient CardLink linkedTo = null;
	private transient AtomicInteger index = new AtomicInteger(-2);

	private transient String altDescription = null;
	private transient String altEffect = null;

	private transient boolean flipped = false;
	private transient boolean available = true;
	private transient int altTier = -1;
	private transient int altAtk = -1;
	private transient int altDef = -1;

	public Equipment() {
	}

	@Override
	public BufferedImage drawCard(boolean flipped) {
		BufferedImage bi = new BufferedImage(225, 350, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if (flipped) {
			g2d.drawImage(acc.getFrame().getBack(acc), 0, 0, null);
		} else {
			FrameColor fc = acc.getFrame();
			boolean hasDesc = description != null && !description.isBlank();

			g2d.setClip(new Polygon(
					new int[]{13, 212, 223, 223, 212, 13, 2, 2},
					new int[]{2, 2, 13, 337, 348, 348, 337, 13},
					8
			));
			g2d.drawImage(card.drawCardNoBorder(acc), 0, 0, null);
			g2d.setClip(null);

			g2d.drawImage(fc.getFront(hasDesc), 0, 0, null);
			g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 20));
			g2d.setColor(fc.getPrimaryColor());
			g2d.setBackground(fc.getBackgroundColor());

			Profile.printCenteredString(StringUtils.abbreviate(card.getName(), 18), 205, 10, 32, g2d);

			BufferedImage star = Helper.getResourceAsImage(this.getClass(), "shoukan/star.png");
			if (star != null)
				for (int i = 0; i < getTier(); i++)
					g2d.drawImage(star, (bi.getWidth() / 2) - (24 * getTier() / 2) + 24 * i, 41, 24, 24, null);

			List<Charm> charms = getCharms();
			if (!charms.isEmpty()) {
				g2d.drawImage(Charm.getIcon(charms), 135, hasDesc ? 188 : 255, null);
			}

			Drawable.drawAttributes(bi, getAtk(), getDef(), getMana(), getBlood(), 0, 0, hasDesc);

			if (linkedTo != null) {
				Champion c = Helper.getOr(linkedTo.asChampion().getFakeCard(), linkedTo.asChampion());
				BufferedImage linked = c.isFlipped()
						? Helper.getResourceAsImage(this.getClass(), "kawaipon/missing.jpg")
						: c.getCard().drawCardNoBorder(acc);

				g2d.drawImage(linked, 20, 52, 60, 93, null);
			}

			g2d.setColor(fc.getSecondaryColor());
			g2d.setFont(Fonts.HAMMERSMITH_ONE.deriveFont(Font.PLAIN, 11));
			Profile.drawStringMultiLineNO(g2d, getDescription(), 205, 9, 277);

			if (!available) {
				g2d.setColor(new Color(0, 0, 0, 150));
				g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());
			}
		}

		g2d.dispose();

		return bi;
	}

	public int getId() {
		return id;
	}

	@Override
	public Card getCard() {
		return card;
	}

	@Override
	public void bind(Hand h) {
		this.game = h.getGame();
		this.acc = h.getAcc();
	}

	@Override
	public Shoukan getGame() {
		return game;
	}

	@Override
	public void setGame(Shoukan game) {
		this.game = game;
	}

	@Override
	public Account getAcc() {
		return acc;
	}

	@Override
	public void setAcc(Account acc) {
		this.acc = acc;
	}

	@Override
	public int getIndex() {
		return index.get();
	}

	@Override
	public AtomicInteger getIndexReference() {
		return index;
	}

	@Override
	public void setIndex(int index) {
		this.index.set(index);
	}

	@Override
	public boolean isFlipped() {
		return flipped;
	}

	@Override
	public void setFlipped(boolean flipped) {
		this.flipped = flipped;
	}

	@Override
	public boolean isAvailable() {
		return available;
	}

	@Override
	public void setAvailable(boolean available) {
		this.available = available;
	}

	public Bonus getBonus() {
		return bonus;
	}

	public CardLink getLinkedTo() {
		return linkedTo;
	}

	public void link(Champion link) {
		this.linkedTo = new CardLink(link.getIndexReference(), link, this);
	}

	public void unlink() {
		this.linkedTo = null;
	}

	public int getBaseAtk() {
		return atk;
	}

	public int getBaseDef() {
		return def;
	}

	public int getAtk() {
		if (altAtk == -1) altAtk = atk;
		return altAtk + bonus.getAtk();
	}

	public int getDef() {
		if (altDef == -1) altDef = def;
		return altDef + bonus.getDef();
	}

	public void addAtk(int atk) {
		bonus.addAtk(atk);
	}

	public void addDef(int def) {
		bonus.addDef(def);
	}

	public void removeAtk(int atk) {
		bonus.removeAtk(atk);
	}

	public void removeDef(int def) {
		bonus.removeDef(def);
	}

	public int getAltAtk() {
		return altAtk;
	}

	public void setAltAtk(int altAtk) {
		this.altAtk = altAtk;
	}

	public int getAltDef() {
		return altDef;
	}

	public void setAltDef(int altDef) {
		this.altDef = altDef;
	}

	public int getMana() {
		return mana + bonus.getMana();
	}

	public void setMana(int mana) {
		this.bonus.setMana(mana);
	}

	public int getBlood() {
		return blood + bonus.getBlood();
	}

	public void setBlood(int blood) {
		this.bonus.setBlood(blood);
	}

	public int getTier() {
		if (altTier == -1) altTier = tier;
		return altTier;
	}

	public int getAltTier() {
		return altTier;
	}

	public void setAltTier(int altTier) {
		this.altTier = altTier;
	}

	public int getWeight(Deck d) {
		return Math.max(
				switch (d.getCombo().getLeft()) {
					case MACHINE -> !hasEffect() ? tier - 1 : tier;
					case MYSTICAL -> hasEffect() ? tier - 1 : tier;
					default -> tier;
				}, 1);
	}

	public List<Charm> getCharms() {
		if (charms == null) return List.of();

		return new JSONArray(charms).stream()
				.map(o -> Charm.valueOf(String.valueOf(o)))
				.collect(Collectors.toList());
	}

	public void setCharms(String charms) {
		this.charms = charms;
	}

	public Set<String> getTags() {
		if (tags == null) return Set.of();

		return new JSONArray(tags).stream()
				.map(String::valueOf)
				.collect(Collectors.toSet());
	}

	public void setTags(List<String> tags) {
		this.tags = new JSONArray(tags).toString();
	}

	public Arguments getArgType() {
		return argType;
	}

	public void setArgType(Arguments argType) {
		this.argType = argType;
	}

	public boolean isEffectOnly() {
		return effectOnly;
	}

	public void setEffectOnly(boolean effectOnly) {
		this.effectOnly = effectOnly;
	}

	public String getDescription() {
		return Helper.getOr(altDescription, description);
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setAltDescription(String altDescription) {
		this.altDescription = altDescription;
	}

	public boolean hasEffect() {
		return Helper.getOr(altEffect, effect) != null;
	}

	public String getRawEffect() {
		return Helper.getOr(altEffect, effect);
	}

	public void setRawEffect(String effect) {
		this.effect = effect;
	}

	public void setAltEffect(String altEffect) {
		this.altEffect = altEffect;
	}

	public void getEffect(EffectParameters ep) {
		try {
			GroovyShell gs = new GroovyShell();
			gs.setVariable("ep", ep);
			gs.setVariable("self", this);
			gs.evaluate(Helper.getOr(altEffect, effect));
		} catch (Exception e) {
			Helper.logger(this.getClass()).warn("Erro ao executar efeito de " + card.getName(), e);
		}
	}

	public void activate(Hand you, Hand opponent, Shoukan game, int allyPos, int enemyPos) {
		try {
			GroovyShell gs = new GroovyShell();
			gs.setVariable("you", you);
			gs.setVariable("opponent", opponent);
			gs.setVariable("game", game);
			gs.setVariable("allyPos", allyPos);
			gs.setVariable("enemyPos", enemyPos);
			gs.setVariable("self", this);
			gs.evaluate(Helper.getOr(altEffect, effect));
		} catch (Exception e) {
			Helper.logger(this.getClass()).warn("Erro ao executar efeito de " + card.getName(), e);
		}
	}

	public void finalizeEffect(EffectParameters ep) {
		try {
			GroovyShell gs = new GroovyShell();
			gs.setVariable("ep", ep);
			gs.setVariable("self", this);
			gs.evaluate(Helper.getOr(finalization, ""));
		} catch (Exception e) {
			Helper.logger(this.getClass()).warn("Erro ao executar efeito de " + card.getName(), e);
		}
	}

	public boolean canGoToGrave() {
		return !effectOnly;
	}

	@Override
	public void reset() {
		flipped = false;
		available = true;
		bonus = new Bonus();
		linkedTo = null;
		index = new AtomicInteger(-2);
		altAtk = -1;
		altDef = -1;
		altTier = -1;
		altDescription = null;
		altEffect = null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Equipment equipment = (Equipment) o;
		return id == equipment.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public Equipment copy() {
		try {
			Equipment e = (Equipment) super.clone();
			e.index = new AtomicInteger(-2);
			e.bonus = bonus.clone();
			if (linkedTo != null)
				e.linkedTo = new CardLink(linkedTo.index(), linkedTo.linked(), e);
			return e;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	@Override
	public Equipment deepCopy() {
		return copy();
	}

	public String toString() {
		return new JSONObject(card.toString()) {{
			put("equipment", new JSONObject() {{
				put("id", id);
				put("tier", tier);
				if (charms != null)
					put("charm", new JSONObject() {{
						put("id", charms);
						put("image", hasEffect() ? "" : Helper.atob(Charm.getIcon(getCharms()), "png"));
					}});
				put("attack", atk);
				put("defense", def);
				put("mana", mana);
				put("description", description);
				put("effectType", argType);
			}});
		}}.toString();
	}

	public String getBase64() {
		return Helper.atob(drawCard(false), "png");
	}
}
