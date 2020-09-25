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

import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Effect;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.EffectTrigger;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import org.apache.commons.lang3.StringUtils;

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

	@Column(columnDefinition = "INTEGER NOT NULL DEFAULT 0")
	private int mana;

	@Column(columnDefinition = "INTEGER NOT NULL DEFAULT 0")
	private int atk;

	@Column(columnDefinition = "INTEGER NOT NULL DEFAULT 0")
	private int def;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String description;

	@Enumerated(EnumType.STRING)
	private Effect effect;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String effectArgs;

	@Enumerated(EnumType.STRING)
	private EffectTrigger trigger;

	@ElementCollection(fetch = FetchType.EAGER)
	private Set<String> requiredCards = new HashSet<>();

	private transient boolean flipped = false;
	private transient boolean available = true;
	private transient boolean defending = false;
	private transient List<Equipment> linkedTo;

	@Override
	public BufferedImage drawCard(Account acc, boolean flipped) {
		BufferedImage bi = new BufferedImage(225, 350, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		if (flipped) {
			g2d.drawImage(acc.getFrame().getBack(), 0, 0, null);
		} else {
			g2d.drawImage(card.drawCardNoBorder(), 0, 0, null);
			g2d.drawImage(acc.getFrame().getFront(), 0, 0, null);
			g2d.setFont(Profile.FONT.deriveFont(Font.PLAIN, 20));

			Profile.printCenteredString(StringUtils.abbreviate(card.getName(), 18), 205, 10, 32, g2d);

			g2d.setColor(Color.cyan);
			Profile.drawOutlinedText(String.valueOf(mana), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(mana)), 66, g2d);

			g2d.setColor(Color.red);
			Profile.drawOutlinedText(String.valueOf(atk), 45, 250, g2d);

			for (int i = 0, slot = 1; i < linkedTo.size(); i++) {
				int eAtk = linkedTo.get(i).getAtk();
				if (eAtk > 0) {
					Profile.drawOutlinedText("+" + eAtk, 45, 250 - (25 * slot), g2d);
					slot++;
				}
			}

			g2d.setColor(Color.green);
			Profile.drawOutlinedText(String.valueOf(def), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(def)), 250, g2d);

			for (int i = 0, slot = 1; i < linkedTo.size(); i++) {
				int eDef = linkedTo.get(i).getDef();
				if (eDef > 0) {
					Profile.drawOutlinedText(eDef + "+", 178 - g2d.getFontMetrics().stringWidth(String.valueOf(eDef)), 250 - (25 * slot), g2d);
					slot++;
				}
			}

			g2d.setFont(new Font("Arial", Font.BOLD, 11));
			g2d.setColor(Color.black);
			g2d.drawString("[" + race.toString().toUpperCase() + (trigger == EffectTrigger.NONE ? "" : "/EFEITO") + "]", 13, 277);

			g2d.setFont(new Font("Arial", Font.PLAIN, 11));
			Profile.drawStringMultiLineNO(g2d, description, 199, 13, 293);
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

	public List<Equipment> getLinkedTo() {
		return linkedTo;
	}

	public void addLinkedTo(Equipment linkedTo) {
		this.linkedTo.add(linkedTo);
	}

	public void removeLinkedTo(Equipment linkedTo) {
		this.linkedTo.remove(linkedTo);
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
		return atk;
	}

	public int getDef() {
		return def;
	}

	public String getDescription() {
		return description;
	}

	public Effect getEffect() {
		return effect;
	}

	public String[] getEffectArgs() {
		return effectArgs.split(",");
	}

	public EffectTrigger getTrigger() {
		return trigger;
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
			return c;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}
