/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

import bsh.EvalError;
import bsh.Interpreter;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Arguments;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Charm;
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

import javax.imageio.ImageIO;
import javax.persistence.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
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
	private transient int altAtk = -1;
	private transient int altDef = -1;

	public Equipment() {
	}

	@Override
	public BufferedImage drawCard(boolean flipped) {
		boolean useFoil = acc.isUsingFoil() && CardDAO.hasCompleted(acc.getUid(), card.getAnime().getName(), true);

		BufferedImage bi = new BufferedImage(225, 350, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		if (flipped) {
			g2d.drawImage(acc.getFrame().getBack(acc), 0, 0, null);
		} else {
			g2d.drawImage(card.drawCardNoBorder(useFoil), 0, 0, null);

			if (charm != null && charm.equals(Charm.SPELL)) {
				g2d.drawImage(acc.getFrame().getFrontSpell(), 0, 0, null);
				g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 20));

				Profile.printCenteredString(StringUtils.abbreviate(card.getName(), 18), 205, 10, 32, g2d);

				try {
					BufferedImage star = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("shoukan/star.png")));
					for (int i = 0; i < tier; i++)
						g2d.drawImage(star, (bi.getWidth() / 2) - (star.getWidth() * tier / 2) + star.getWidth() * i, 42, null);
				} catch (IOException ignore) {
				}

				if (getMana() > 0) {
					g2d.drawImage(Helper.getResourceAsImage(this.getClass(), "shoukan/mana.png"), 184, 47, null);

					g2d.setColor(new Color(0, 165, 255));
					Profile.drawOutlinedText(String.valueOf(getMana()), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(getMana())), 67, g2d);
				}

				g2d.setColor(Color.black);
				g2d.setFont(Fonts.HAMMERSMITH_ONE.deriveFont(Font.PLAIN, 11));
				Profile.drawStringMultiLineNO(g2d, description, 205, 9, 277);
			} else {
				g2d.drawImage(acc.getFrame().getFrontEquipment(), 0, 0, null);
				g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 20));

				Profile.printCenteredString(StringUtils.abbreviate(card.getName(), 18), 205, 10, 32, g2d);

				if (charm != null)
					g2d.drawImage(charm.getIcon(), 135, 58, null);
				try {
					BufferedImage star = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("shoukan/star.png")));
					for (int i = 0; i < tier; i++)
						g2d.drawImage(star, (bi.getWidth() / 2) - (star.getWidth() * tier / 2) + star.getWidth() * i, 42, null);
				} catch (IOException ignore) {
				}

				g2d.setColor(Color.red);
				Profile.drawOutlinedText(String.valueOf(atk), 45, 316, g2d);

				g2d.setColor(Color.green);
				Profile.drawOutlinedText(String.valueOf(def), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(def)), 316, g2d);

				if (linkedTo != null) {
					if (linkedTo.getRight().getFakeCard() != null) {
						boolean linkedFoil = acc.isUsingFoil() && CardDAO.hasCompleted(acc.getUid(), linkedTo.getRight().getFakeCard().getCard().getAnime().getName(), true);

						g2d.drawImage(linkedTo.getRight().getFakeCard().getCard().drawCardNoBorder(linkedFoil), 20, 52, 60, 93, null);
					} else {
						boolean linkedFoil = acc.isUsingFoil() && CardDAO.hasCompleted(acc.getUid(), linkedTo.getRight().getCard().getAnime().getName(), true);

						g2d.drawImage(linkedTo.getRight().getCard().drawCardNoBorder(linkedFoil), 20, 52, 60, 93, null);
					}
					g2d.setClip(null);
				}
			}
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
		this.altAtk = Math.max(-1, altAtk);
	}

	public int getAltDef() {
		return altDef;
	}

	public void setAltDef(int altDef) {
		this.altDef = Math.max(-1, altDef);
	}

	public int getMana() {
		return mana;
	}

	public void setMana(int mana) {
		this.mana = mana;
	}

	public int getTier() {
		return tier;
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
		String imports = EffectParameters.IMPORTS.formatted(card.getName());

		if (effect.startsWith("//ENGINE: GROOVY"))
			try {
				GroovyShell gs = new GroovyShell();
				gs.setVariable("ep", ep);
				gs.setVariable("self", this);
				gs.evaluate(effect);
			} catch (Exception e) {
				Helper.logger(this.getClass()).warn(e + " | " + e.getStackTrace()[0]);
			}
		else
			try {
				Interpreter i = new Interpreter();
				i.setStrictJava(true);
				i.set("ep", ep);
				i.set("self", this);
				i.eval(imports + effect);
			} catch (Exception e) {
				Helper.logger(this.getClass()).warn(e + " | " + e.getStackTrace()[0]);
			}
	}

	public void activate(Hand you, Hand opponent, Shoukan game, int allyPos, int enemyPos) {
		String imports = EffectParameters.IMPORTS.formatted(card.getName());

		if (effect.startsWith("//ENGINE: GROOVY"))
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
				Helper.logger(this.getClass()).warn(e + " | " + e.getStackTrace()[0]);
			}
		else
			try {
				Interpreter i = new Interpreter();
				i.setStrictJava(true);
				i.set("you", you);
				i.set("opponent", opponent);
				i.set("game", game);
				i.set("allyPos", allyPos);
				i.set("enemyPos", enemyPos);
				i.set("self", this);
				i.eval(imports + effect);
			} catch (EvalError e) {
				Helper.logger(this.getClass()).warn(e + " | " + e.getStackTrace()[0]);
			}
	}

	public void setEffect(String effect) {
		this.effect = effect;
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
			return (Equipment) clone();
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
