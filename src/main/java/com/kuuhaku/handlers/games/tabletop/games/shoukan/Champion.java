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
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.utils.Helper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import javax.persistence.*;
import java.awt.*;
import java.awt.font.TextAttribute;
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

	@Column(columnDefinition = "VARCHAR(130) NOT NULL DEFAULT ''")
	private String description;

	@Column(columnDefinition = "TEXT")
	private String effect = "";

	@ElementCollection(fetch = FetchType.EAGER)
	private Set<String> requiredCards = new HashSet<>();

	private transient boolean flipped = false;
	private transient boolean available = true;
	private transient boolean defending = false;
	private transient List<Equipment> linkedTo = new ArrayList<>();
	private transient Bonus bonus = new Bonus();
	private transient Champion fakeCard = null;
	private transient int altAtk = -1;
	private transient int altDef = -1;
	private transient int mAtk = 0;
	private transient int mDef = 0;


	@Override
	public BufferedImage drawCard(Account acc, boolean flipped) {
		BufferedImage bi = new BufferedImage(225, 350, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		if (flipped) {
			g2d.drawImage(acc.getFrame().getBack(acc), 0, 0, null);
		} else {
			if (fakeCard != null)
				g2d.drawImage(fakeCard.getCard().drawCardNoBorder(), 0, 0, null);
			else
				g2d.drawImage(card.drawCardNoBorder(), 0, 0, null);
			g2d.drawImage(acc.getFrame().getFront(), 0, 0, null);
			g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 20));

			Profile.printCenteredString(StringUtils.abbreviate(card.getName(), 18), 205, 10, 32, g2d);

			g2d.setColor(Color.cyan);
			Profile.drawOutlinedText(String.valueOf(mana), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(mana)), 66, g2d);

			String data = bonus.getSpecialData().optString("write");
			if (!data.isBlank()) {
				g2d.setColor(Color.yellow);
				g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 16));
				Profile.drawOutlinedText(data, 45, 66, g2d);
			}

			g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 20));

			g2d.setColor(Color.red);
			if (fakeCard != null)
				Profile.drawOutlinedText(String.valueOf(fakeCard.getAtk()), 45, 250, g2d);
			else
				Profile.drawOutlinedText(String.valueOf(atk), 45, 250, g2d);

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
				Profile.drawOutlinedText(String.valueOf(fakeCard.getDef()), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(def)), 250, g2d);
			else
				Profile.drawOutlinedText(String.valueOf(def), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(def)), 250, g2d);

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
			g2d.drawString("[" + race.toString().toUpperCase() + (effect == null ? "" : "/EFEITO") + "]", 9, 277);

			g2d.setFont(Helper.HAMLIN.deriveFont(Map.of(
					TextAttribute.SIZE, 11,
					TextAttribute.WEIGHT, TextAttribute.WEIGHT_HEAVY
			)));
			Profile.drawStringMultiLineNO(g2d, fakeCard != null ? fakeCard.getDescription() : description, 205, 9, 293);
		}

		if (!available) {
			g2d.setColor(new Color(0, 0, 0, 150));
			g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		}

		if (defending) {
			try {
				BufferedImage dm = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("shoukan/defense_mode.png")));
				g2d.drawImage(dm, 0, 0, null);
			} catch (IOException ignore) {
			}
		}

		g2d.dispose();

		return bi;
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

	public void setLinkedTo(List<Equipment> linkedTo) {
		this.linkedTo = linkedTo;
		this.linkedTo.forEach(e -> e.setLinkedTo(Pair.of(e.getLinkedTo().getLeft(), card)));
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
		return linkedTo.stream().anyMatch(e -> e != null && e.getCard().getName().equalsIgnoreCase(name));
	}

	public void clearLinkedTo() {
		this.linkedTo.clear();
	}

	public boolean isDefending() {
		return defending;
	}

	public void setDefending(boolean defending) {
		this.defending = defending;
	}

	public Race getRace() {
		return race;
	}

	public int getMana() {
		return mana;
	}

	public int getAtk() {
		return Math.max(atk, altAtk) + bonus.getAtk();
	}

	public int getDef() {
		return Math.max(def, altDef) + bonus.getDef();
	}

	public void setAltAtk(int altAtk) {
		this.altAtk = altAtk;
	}

	public void setAltDef(int altDef) {
		this.altDef = altDef;
	}

	public int getEAtk() {
		return Math.max(0, getAtk() + mAtk + bonus.getAtk());
	}

	public void setMAtk(int mAtk) {
		this.mAtk = mAtk;
	}

	public int getEDef() {
		return Math.max(0, getDef() + mDef + bonus.getDef());
	}

	public void setMDef(int mDef) {
		this.mDef = mDef;
	}

	public void resetAttribs() {
		this.mAtk = 0;
		this.mDef = 0;
	}

	public Bonus getBonus() {
		return bonus;
	}

	public void clearBonus() {
		this.bonus = new Bonus();
	}

	public String getDescription() {
		return description;
	}

	public boolean hasEffect() {
		return effect != null;
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
				              import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
				              import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
				              import com.kuuhaku.handlers.games.tabletop.games.shoukan.SlotColumn;
				              import com.kuuhaku.controller.postgresql.CardDAO;
				              import com.kuuhaku.model.enums.AnimeName;
				              				
				              """.formatted(card.getName());

		try {
			Interpreter i = new Interpreter();
			i.setStrictJava(true);
			i.set("ep", ep);
			i.eval(imports + effect);
		} catch (EvalError e) {
			Helper.logger(this.getClass()).warn(e + " | " + e.getStackTrace()[0]);
		}
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
		altAtk = -1;
		altDef = -1;
	}

	public Set<String> getRequiredCards() {
		return requiredCards;
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
	public Drawable copy() {
		try {
			Champion c = (Champion) clone();
			c.linkedTo = new ArrayList<>();
			c.bonus = new Bonus();
			return c;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}
