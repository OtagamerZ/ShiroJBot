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
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Charm;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.FrameColor;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.utils.Helper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.persistence.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
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

	private transient boolean flipped = false;
	private transient boolean available = true;
	private transient Account acc = null;
	private transient Pair<Integer, Champion> linkedTo = null;

	@Override
	public BufferedImage drawCard(Account acc, boolean flipped) {
		BufferedImage bi = new BufferedImage(225, 350, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		if (flipped) {
			g2d.drawImage(acc.getFrame().getBack(acc), 0, 0, null);
		} else {
			g2d.drawImage(card.drawCardNoBorder(), 0, 0, null);

			if (charm != null && charm.equals(Charm.SPELL)) {
				g2d.drawImage(acc.getFrame().getFrontSpell(), 0, 0, null);
				g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 20));

				Profile.printCenteredString(StringUtils.abbreviate(card.getName(), 18), 205, 10, 32, g2d);

				try {
					BufferedImage star = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("shoukan/star.png")));
					for (int i = 0; i < tier; i++)
						g2d.drawImage(star, (bi.getWidth() / 2) - (star.getWidth() * tier / 2) + star.getWidth() * i, 42, null);
				} catch (IOException ignore) {
				}

				g2d.setColor(Color.cyan);
				Profile.drawOutlinedText(String.valueOf(mana), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(mana)), 66, g2d);

				g2d.setColor(Color.black);
				g2d.setFont(Helper.HAMLIN.deriveFont(Map.of(
						TextAttribute.SIZE, 11,
						TextAttribute.WEIGHT, TextAttribute.WEIGHT_HEAVY
				)));
				Profile.drawStringMultiLineNO(g2d, description, 205, 9, 277);
			} else {
				g2d.drawImage(acc.getFrame().getFrontEquipment(), 0, 0, null);
				g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 20));

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
					if (linkedTo.getRight().getFakeCard() != null)
						g2d.drawImage(linkedTo.getRight().getFakeCard().getCard().drawCardNoBorder(), 20, 52, 60, 93, null);
					else
						g2d.drawImage(linkedTo.getRight().getCard().drawCardNoBorder(), 20, 52, 60, 93, null);
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

	public BufferedImage drawCard() {
		BufferedImage bi = new BufferedImage(225, 350, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.drawImage(card.drawCardNoBorder(), 0, 0, null);

		if (linkedTo != null) {
			g2d.drawImage(linkedTo.getRight().getCard().drawCardNoBorder(), 20, 52, 60, 93, null);
			g2d.setClip(null);
		}

		g2d.drawImage(FrameColor.PINK.getFrontEquipment(), 0, 0, null);
		g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 20));

		Profile.printCenteredString(StringUtils.abbreviate(card.getName(), 18), 205, 10, 32, g2d);

		g2d.setColor(Color.red);
		Profile.drawOutlinedText(String.valueOf(atk), 45, 316, g2d);

		g2d.setColor(Color.green);
		Profile.drawOutlinedText(String.valueOf(def), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(def)), 316, g2d);

		try {
			BufferedImage star = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("shoukan/star.png")));
			for (int i = 0; i < tier; i++)
				g2d.drawImage(star, (bi.getWidth() / 2) - (star.getWidth() * tier / 2) + star.getWidth() * i, 42, null);
		} catch (IOException ignore) {
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
		return atk;
	}

	public int getDef() {
		return def;
	}

	public int getMana() {
		return mana;
	}

	public int getTier() {
		return tier;
	}

	public Charm getCharm() {
		return charm;
	}

	public void activate(Hand you, Hand opponent, Arena arena) {
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
				import com.kuuhaku.controller.postgresql.AccountDAO;
				import com.kuuhaku.controller.postgresql.CardDAO;
				import com.kuuhaku.model.enums.AnimeName;
				import com.kuuhaku.utils.Helper;
				import org.json.JSONArray;
				          				
				          """.formatted(card.getName());

		try {
			Interpreter i = new Interpreter();
			i.setStrictJava(true);
			i.set("you", you);
			i.set("opponent", opponent);
			i.set("arena", arena);
			i.eval(imports + effect);
		} catch (EvalError e) {
			Helper.logger(this.getClass()).warn(e + " | " + e.getStackTrace()[0]);
		}
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

	public String toString(Account acc) {
		return new JSONObject() {{
			put("id", id);
			put("name", card.getName());
			put("tier", tier);
			put("attack", atk);
			put("defense", def);
			put("mana", mana);
			put("description", description);
			put("image", Base64.getEncoder().encodeToString(Helper.getBytes(drawCard(acc, false), "png")));
		}}.toString();
	}
}
