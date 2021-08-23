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
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.Deck;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONObject;
import groovy.lang.GroovyShell;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.persistence.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;

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

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int tier;

	@Enumerated(value = EnumType.STRING)
	private Charm charm = null;

	@Enumerated(value = EnumType.STRING)
	private Arguments argType = Arguments.NONE;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean effectOnly = false;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean parasite = false;

	private transient boolean flipped = false;
	private transient boolean available = true;
	private transient Shoukan game = null;
	private transient Account acc = null;
	private transient Pair<Integer, Champion> linkedTo = null;
	private transient int altTier = -1;
	private transient int altAtk = -1;
	private transient int altDef = -1;

	public Equipment() {
	}

	@Override
	public BufferedImage drawCard(boolean flipped) {
		BufferedImage bi = new BufferedImage(225, 350, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		if (flipped) {
			g2d.drawImage(acc.getFrame().getBack(acc), 0, 0, null);
		} else {
			FrameColor fc = acc.getFrame();
			boolean hasDesc = charm != null && charm.equals(Charm.SPELL);

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
			g2d.setBackground(fc.getSecondaryColor());

			Profile.printCenteredString(StringUtils.abbreviate(card.getName(), 18), 205, 10, 32, g2d);

			BufferedImage star = Helper.getResourceAsImage(this.getClass(), "shoukan/star.png");
			if (star != null)
				for (int i = 0; i < getTier(); i++)
					g2d.drawImage(star, (bi.getWidth() / 2) - (star.getWidth() * getTier() / 2) + star.getWidth() * i, 53, null);

			Drawable.drawAttributes(bi, getAtk(), getDef(), getMana(), getBlood(), hasDesc);

			g2d.setColor(fc.getSecondaryColor());
			g2d.setFont(Fonts.HAMMERSMITH_ONE.deriveFont(Font.PLAIN, 11));
			Profile.drawStringMultiLineNO(g2d, description, 205, 9, 277);
		}

		if (!available) {
			g2d.setColor(new Color(0, 0, 0, 150));
			g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());
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
	public void bind(Hand h) {
		this.game = h.getGame();
		this.acc = h.getAcc();
	}

	public Pair<Integer, Champion> getLinkedTo() {
		return linkedTo;
	}

	public void setLinkedTo(Pair<Integer, Champion> linkedTo) {
		this.linkedTo = linkedTo;
	}

	public int getAtk() {
		if (altAtk == -1) altAtk = atk;
		return altAtk;
	}

	public int getDef() {
		if (altDef == -1) altDef = def;
		return altDef;
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
		return mana;
	}

	public void setMana(int mana) {
		this.mana = mana;
	}

	public int getBlood() {
		return blood;
	}

	public void setBlood(int blood) {
		this.blood = blood;
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
					case MACHINE -> charm != Charm.SPELL ? tier - 1 : tier;
					case MYSTICAL -> charm == Charm.SPELL ? tier - 1 : tier;
					default -> tier;
				}, 1);
	}

	public Charm getCharm() {
		return charm;
	}

	public void setCharm(Charm charm) {
		this.charm = charm;
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

	public boolean isParasite() {
		return parasite;
	}

	public void setParasite(boolean parasite) {
		this.parasite = parasite;
	}

	public boolean hasEffect() {
		return effect != null;
	}

	public void getEffect(EffectParameters ep) {
		try {
			GroovyShell gs = new GroovyShell();
			gs.setVariable("ep", ep);
			gs.setVariable("self", this);
			gs.evaluate(effect);
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
			gs.evaluate(effect);
		} catch (Exception e) {
			Helper.logger(this.getClass()).warn("Erro ao executar efeito de " + card.getName(), e);
		}
	}

	public void setEffect(String effect) {
		this.effect = effect;
	}

	public void reset() {
		flipped = false;
		available = true;
		linkedTo = null;
		altAtk = -1;
		altDef = -1;
		altTier = -1;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Equipment champion = (Equipment) o;
		return id == champion.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public Equipment copy() {
		try {
			Equipment e = (Equipment) clone();
			e.reset();
			return e;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	public String toString() {
		return new JSONObject(card.toString()) {{
			put("equipment", new JSONObject() {{
				put("id", id);
				put("tier", tier);
				if (charm != null)
					put("charm", new JSONObject() {{
						put("id", charm);
						put("image", charm == Charm.SPELL ? "" : Helper.atob(charm.getIcon(), "png"));
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
