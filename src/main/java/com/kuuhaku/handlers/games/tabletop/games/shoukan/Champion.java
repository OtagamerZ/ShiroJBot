/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Class;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.Clan;
import com.kuuhaku.utils.Helper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.persistence.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.*;

@Entity
@Table(name = "champion")
public class Champion implements Drawable, Cloneable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@OneToOne(fetch = FetchType.EAGER)
	private Card card;

	@Enumerated(EnumType.STRING)
	private Race race;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int mana;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int atk;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int def;

	@Column(columnDefinition = "VARCHAR(140) NOT NULL DEFAULT ''")
	private String description;

	@Column(columnDefinition = "TEXT")
	private String effect = "";

	@Enumerated(EnumType.STRING)
	private Class category = null;

	@ElementCollection(fetch = FetchType.EAGER)
	private Set<String> requiredCards = new HashSet<>();

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean fusion = false;

	private transient boolean flipped = false;
	private transient boolean available = true;
	private transient Account acc = null;
	private transient Clan clan = null;
	private transient boolean defending = false;
	private transient List<Equipment> linkedTo = new ArrayList<>();
	private transient Bonus bonus = new Bonus();
	private transient Champion fakeCard = null;
	private transient int altAtk = -1;
	private transient int altDef = -1;
	private transient String altDescription = null;
	private transient String altEffect = null;
	private transient Race altRace = null;
	private transient int mAtk = 0;
	private transient int mDef = 0;
	private transient int mMana = 0;
	private transient int redAtk = 0;
	private transient int redDef = 0;
	private transient int efctAtk = 0;
	private transient int efctDef = 0;
	private transient int stun = 0;

	public Champion(Card card, Race race, int mana, int atk, int def, String description, String effect) {
		this.card = card;
		this.race = race;
		this.mana = mana;
		this.atk = atk;
		this.def = def;
		this.description = description;
		this.effect = effect;
	}

	public Champion() {
	}

	@Override
	public BufferedImage drawCard(boolean flipped) {
		BufferedImage bi = new BufferedImage(225, 350, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		if (flipped) {
			g2d.drawImage(acc.getFrame().getBack(acc, clan), 0, 0, null);
		} else {
			if (fakeCard != null)
				g2d.drawImage(fakeCard.getCard().drawCardNoBorder(), 0, 0, null);
			else
				g2d.drawImage(card.drawCardNoBorder(), 0, 0, null);
			g2d.drawImage(acc.getFrame().getFront(), 0, 0, null);
			g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 20));

			if (fakeCard != null) {
				Profile.printCenteredString(StringUtils.abbreviate(fakeCard.getCard().getName(), 16), 181, 38, 32, g2d);
				g2d.drawImage(fakeCard.getRace().getIcon(), 11, 12, null);
			} else {
				Profile.printCenteredString(StringUtils.abbreviate(card.getName(), 16), 181, 38, 32, g2d);
				g2d.drawImage(getRace().getIcon(), 11, 12, null);
			}

			g2d.setColor(Color.cyan);
			if (fakeCard != null)
				Profile.drawOutlinedText(String.valueOf(fakeCard.getMana()), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(fakeCard.getMana())), 66, g2d);
			else
				Profile.drawOutlinedText(String.valueOf(getMana()), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(getMana())), 66, g2d);

			String data = bonus.getSpecialData().optString("write");
			if (!data.isBlank()) {
				g2d.setColor(Color.yellow);
				g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 16));
				Profile.drawOutlinedText(data, 20, 66, g2d);
			}

			g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 20));

			g2d.setColor(Color.red);
			if (fakeCard != null)
				Profile.drawOutlinedText(String.valueOf(fakeCard.getAtk()), 45, 250, g2d);
			else
				Profile.drawOutlinedText(String.valueOf(getAtk()), 45, 250, g2d);

			if (bonus.getAtk() != 0)
				Profile.drawOutlinedText((bonus.getAtk() >= 0 ? "+" : "-") + Math.abs(bonus.getAtk()), 45, 225, g2d);
			for (int i = 0, slot = bonus.getAtk() != 0 ? 2 : 1; i < linkedTo.size(); i++) {
				int eAtk = linkedTo.get(i).getAtk();
				if (eAtk != 0) {
					Profile.drawOutlinedText((eAtk >= 0 ? "+" : "-") + Math.abs(eAtk), 45, 250 - (25 * slot), g2d);
					slot++;
				}
			}

			g2d.setColor(Color.green);
			if (fakeCard != null)
				Profile.drawOutlinedText(String.valueOf(fakeCard.getDef()), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(fakeCard.getDef())), 250, g2d);
			else
				Profile.drawOutlinedText(String.valueOf(getDef()), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(getDef())), 250, g2d);

			if (bonus.getDef() != 0)
				Profile.drawOutlinedText((bonus.getDef() >= 0 ? "+" : "-") + Math.abs(bonus.getDef()), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(Math.abs(bonus.getDef()))), 225, g2d);
			for (int i = 0, slot = bonus.getDef() != 0 ? 2 : 1; i < linkedTo.size(); i++) {
				int eDef = linkedTo.get(i).getDef();
				if (eDef != 0) {
					Profile.drawOutlinedText(Math.abs(eDef) + (eDef >= 0 ? "+" : "-"), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(Math.abs(eDef))), 250 - (25 * slot), g2d);
					slot++;
				}
			}

			g2d.setFont(new Font("Arial", Font.BOLD, 11));
			g2d.setColor(Color.black);
			g2d.drawString("[" + getRace().toString().toUpperCase() + (effect == null ? "" : "/EFEITO") + "]", 9, 277);

			g2d.setFont(Helper.HAMMERSMITH.deriveFont(Font.PLAIN, 11));
			Profile.drawStringMultiLineNO(g2d, fakeCard != null ? fakeCard.getDescription() : Helper.getOr(altDescription, description), 205, 9, 293);

			if (stun > 0) {
				available = false;
			}

			if (!available) {
				g2d.setColor(new Color(0, 0, 0, 150));
				g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());
			}

			if (stun > 0) {
				try {
					BufferedImage dm = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("shoukan/stunned.png")));
					g2d.drawImage(dm, 0, 0, null);
				} catch (IOException ignore) {
				}
			} else if (defending) {
				try {
					BufferedImage dm = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("shoukan/defense_mode.png")));
					g2d.drawImage(dm, 0, 0, null);
				} catch (IOException ignore) {
				}
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
	public Account getAcc() {
		return acc;
	}

	@Override
	public void setAcc(Account acc) {
		this.acc = acc;
	}

	@Override
	public Clan getClan() {
		return clan;
	}

	@Override
	public void setClan(Clan clan) {
		this.clan = clan;
	}

	public void setLinkedTo(List<Equipment> linkedTo) {
		this.linkedTo = linkedTo;
		for (Equipment e : this.linkedTo) {
			e.setLinkedTo(Pair.of(e.getLinkedTo().getLeft(), this));
		}
	}

	public List<Equipment> getLinkedTo() {
		return linkedTo;
	}

	public void addLinkedTo(Equipment linkedTo) {
		this.linkedTo.add(linkedTo);
	}

	public void removeLinkedTo(Equipment linkedTo) {
		this.linkedTo.remove(linkedTo);
	}

	public boolean isLinkedTo(String name) {
		return linkedTo.stream().anyMatch(e -> e != null && e.getCard().getId().equalsIgnoreCase(name));
	}

	public void clearLinkedTo() {
		this.linkedTo.clear();
	}

	public boolean isDefending() {
		return flipped || defending;
	}

	public void setDefending(boolean defending) {
		this.defending = defending;
	}

	public Race getRace() {
		return Helper.getOr(altRace, race);
	}

	public int getMana() {
		return mana + mMana;
	}

	public void setModMana(int mana) {
		this.mMana = mana;
	}

	public int getBaseAtk() {
		return atk;
	}

	public int getBaseDef() {
		return def;
	}

	public int getAtk() {
		if (altAtk == -1) altAtk = atk;
		return Math.max(0, altAtk - redAtk + efctAtk);
	}

	public int getDef() {
		if (altDef == -1) altDef = def;
		return Math.max(0, altDef - redDef + efctDef);
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

	public int getRedAtk() {
		return redAtk;
	}

	public void setRedAtk(int redAtk) {
		this.redAtk += redAtk;
	}

	public int getRedDef() {
		return redDef;
	}

	public void setRedDef(int redDef) {
		this.redDef += redDef;
	}

	public void setEfctAtk(int efctAtk) {
		this.efctAtk += efctAtk;
	}

	public void setEfctDef(int efctDef) {
		this.efctDef += efctDef;
	}

	public int getFinAtk() {
		return Math.max(0, getEffAtk() + getLinkedTo().stream().mapToInt(Equipment::getAtk).sum());
	}

	public int getEffAtk() {
		return Math.max(0, getAtk() + mAtk + bonus.getAtk());
	}

	public void setModAtk(int mAtk) {
		this.mAtk = mAtk;
	}

	public int getFinDef() {
		return Math.max(0, getEffDef() + getLinkedTo().stream().mapToInt(Equipment::getDef).sum());
	}

	public int getEffDef() {
		return Math.max(0, getDef() + mDef + bonus.getDef());
	}

	public void setModDef(int mDef) {
		this.mDef = mDef;
	}

	public void resetAttribs() {
		this.mAtk = 0;
		this.mDef = 0;
	}

	public String getName() {
		return fakeCard != null ? fakeCard.getCard().getName() : card.getName();
	}

	public Bonus getBonus() {
		return bonus;
	}

	public void setBonus(Bonus bonus) {
		this.bonus = bonus;
	}

	public void clearBonus() {
		this.bonus = new Bonus();
	}

	public String getDescription() {
		return description;
	}

	public void setAltDescription(String description) {
		this.altDescription = description;
	}

	public boolean hasEffect() {
		return effect != null;
	}

	public String getLiteralEffect() {
		return effect;
	}

	public boolean isFusion() {
		return fusion;
	}

	public void setFusion(boolean fusion) {
		this.fusion = fusion;
	}

	public String getRawEffect() {
		return effect;
	}

	public void setRawEffect(String effect) {
		this.effect = effect;
	}

	public void setAltEffect(String effect) {
		this.altEffect = effect;
	}

	public void setAltRace(Race race) {
		this.altRace = race;
	}

	public void getEffect(EffectParameters ep) {
		String imports = """
				//%s
				import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
				import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Phase;
				import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.ArenaField;
				import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
				import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
				import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.EffectTrigger;
				import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hand;
				import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
				import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
				import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
				import com.kuuhaku.handlers.games.tabletop.games.shoukan.SlotColumn;
				import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Charm;
				import com.kuuhaku.handlers.games.tabletop.games.shoukan.EffectOverTime;
				import com.kuuhaku.controller.postgresql.AccountDAO;
				import com.kuuhaku.controller.postgresql.CardDAO;
				import org.apache.commons.lang3.tuple.Pair;
				import com.kuuhaku.model.enums.AnimeName;
				import java.util.function.BiConsumer;
				import com.kuuhaku.utils.Helper;
				import org.json.JSONArray;
				          				
				          """.formatted(card.getName());

		try {
			Interpreter i = new Interpreter();
			i.setStrictJava(true);
			i.set("ep", ep);
			i.eval(imports + Helper.getOr(altEffect, effect));
		} catch (EvalError e) {
			Helper.logger(this.getClass()).warn(e + " | " + e.getStackTrace()[0]);
		}
	}

	public Class getCategory() {
		return category;
	}

	public Champion getFakeCard() {
		return fakeCard;
	}

	public void setFakeCard(Champion fakeCard) {
		this.fakeCard = fakeCard;
	}

	public void reset() {
		flipped = false;
		available = true;
		defending = false;
		linkedTo = new ArrayList<>();
		bonus = new Bonus();
		fakeCard = null;
		mAtk = 0;
		mDef = 0;
		mMana = 0;
		altAtk = atk;
		altDef = def;
		altDescription = null;
		altEffect = null;
		altRace = null;
		redAtk = 0;
		redDef = 0;
		efctAtk = 0;
		efctDef = 0;
		stun = 0;
	}

	public Set<String> getRequiredCards() {
		return requiredCards;
	}

	public int getStun() {
		return stun;
	}

	public void setStun(int stun) {
		this.stun = stun;
	}

	public void reduceStun() {
		this.stun--;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Champion champion = (Champion) o;
		return id == champion.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public Champion copy() {
		try {
			Champion c = (Champion) clone();
			c.linkedTo = new ArrayList<>();
			c.bonus = new Bonus();
			return c;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	public String toString() {
		return new JSONObject() {{
			put("id", id);
			put("name", card.getName());
			put("category", category);
			put("mana", mana);
			put("attack", atk);
			put("defense", def);
			put("description", description);
			put("image", Base64.getEncoder().encodeToString(Helper.getBytes(drawCard(false), "png")));
		}}.toString();
	}
}
