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

import com.kuuhaku.handlers.games.tabletop.framework.enums.Neighbor;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Class;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.*;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.records.CardLink;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.records.FusionMaterial;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.records.Source;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONArray;
import com.kuuhaku.utils.UniqueList;
import groovy.lang.GroovyShell;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

import javax.persistence.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.EffectTrigger.*;

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
	private int blood;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int atk;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int def;

	@Column(columnDefinition = "VARCHAR(140) NOT NULL DEFAULT ''")
	private String description;

	@Column(columnDefinition = "TEXT")
	private String effect = "";

	@Column(columnDefinition = "VARCHAR(255)")
	private String tags = null;

	@Enumerated(EnumType.STRING)
	private Class category = null;

	@ElementCollection(fetch = FetchType.EAGER)
	@JoinColumn(nullable = false, name = "champion_id")
	private Set<String> requiredCards = new HashSet<>();

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean fusion = false;

	private transient Hero hero = null;
	private transient Shoukan game = null;
	private transient Account acc = null;
	private transient Bonus bonus = new Bonus();
	private transient Champion fakeCard = null;
	private transient Champion nemesis = null;
	private transient BiConsumer<Side, Shoukan> onDuelEnd = null;
	private transient List<CardLink> linkedTo = new UniqueList<>(CardLink::getIndex);
	private transient AtomicInteger index = new AtomicInteger(-2);
	private transient Side side = null;

	private transient String altImage = null;
	private transient String altDescription = null;
	private transient String altEffect = null;
	private transient Set<String> curses = new HashSet<>();
	private transient Race altRace = null;

	private transient boolean flipped = false;
	private transient boolean available = true;
	private transient boolean defending = false;
	private transient boolean sealed = false;
	private transient int altAtk = -1;
	private transient int altDef = -1;
	private transient int mAtk = 0;
	private transient int mDef = 0;
	private transient double mDodge = 0;
	private transient double mBlock = 0;
	private transient boolean gravelocked = false;

	private transient int stasis = 0;
	private transient int stun = 0;
	private transient int sleep = 0;
	private transient boolean triggerLock = false;

	public Champion(Card card, Race race, int mana, int blood, int atk, int def, String description, String effect) {
		this.card = card;
		this.race = race;
		this.mana = mana;
		this.blood = blood;
		this.atk = atk;
		this.def = def;
		this.description = description;
		this.effect = effect;
	}

	public Champion() {
	}

	@Override
	public BufferedImage drawCard(boolean flipped) {
		boolean debug = game != null && game.getRules().debug();

		BufferedImage bi = new BufferedImage(225, 350, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Champion c = Helper.getOr(fakeCard, this);
		if (flipped) {
			g2d.drawImage(acc.getFrame().getBack(acc), 0, 0, null);
		} else {
			FrameColor fc = acc.getFrame();

			g2d.setClip(new Polygon(
					new int[]{13, 212, 223, 223, 212, 13, 2, 2},
					new int[]{2, 2, 13, 337, 348, 348, 337, 13},
					8
			));
			if (altImage == null) {
				g2d.drawImage(c.getCard().drawCardNoBorder(acc), 0, 0, null);
			} else {
				g2d.drawImage(Helper.btoa(altImage), 0, 0, null);
			}
			g2d.setClip(null);

			g2d.drawImage(fc.getFront(true), 0, 0, null);
			g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 20));
			g2d.setColor(fc.getPrimaryColor());
			g2d.setBackground(fc.getBackgroundColor());

			if (debug) {
				Profile.printCenteredString(toString(), 181, 38, 32, g2d);
			} else {
				Profile.printCenteredString(StringUtils.abbreviate(c.getCard().getName(), 15), 181, 38, 32, g2d);
				g2d.drawImage(c.getRace().getIcon(), 11, 12, 23, 23, null);
			}

			if (bonus.getWrite() != null) {
				g2d.setBackground(Color.black);
				g2d.setColor(Color.yellow);
				g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 16));
				Profile.printCenteredString(bonus.getWrite(), 205, 10, 57, g2d);
				g2d.setBackground(fc.getBackgroundColor());
			}

			if (isCursed()) {
				g2d.setBackground(Color.white);
				g2d.setColor(Color.black);
				g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 20));
				Profile.drawOutlinedText(String.valueOf(curses.size()), 188, 193, g2d);

				g2d.drawImage(Charm.CURSE.getIcon(), 135, 188, null);
			}

			drawAttributes(bi, c.getFinAtk(), c.getFinDef(), c.getMana(), c.getBlood(), true);

			g2d.setFont(new Font("Arial", Font.BOLD, 11));
			g2d.setColor(fc.getSecondaryColor());
			g2d.setFont(Fonts.HAMMERSMITH_ONE.deriveFont(Font.PLAIN, 11));
			if (debug) {
				Profile.drawStringMultiLineNO(g2d, getLinkedTo().toString(), 205, 9, 277);
			} else {
				g2d.drawString("[" + c.getRace().toString().toUpperCase(Locale.ROOT) + (c.hasEffect() ? "/EFEITO" : "") + "]", 9, 277);
				Profile.drawStringMultiLineNO(g2d, c.getDescription(), 205, 9, 293);
			}

			if (isStunned() || isSleeping() || (hero != null && hero.getQuest() != null)) {
				available = false;
			}

			if (!available) {
				g2d.setColor(new Color(0, 0, 0, 150));
				g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());
			}

			if (isDuelling()) {
				g2d.drawImage(Helper.getResourceAsImage(this.getClass(), "shoukan/duel.png"), 0, 0, null);
			} else if (isStasis()) {
				g2d.drawImage(Helper.getResourceAsImage(this.getClass(), "shoukan/stasis.png"), 0, 0, null);
			} else if (isStunned()) {
				g2d.drawImage(Helper.getResourceAsImage(this.getClass(), "shoukan/stun.png"), 0, 0, null);
			} else if (isSleeping()) {
				g2d.drawImage(Helper.getResourceAsImage(this.getClass(), "shoukan/sleep.png"), 0, 0, null);
			} else if (isDefending()) {
				g2d.drawImage(Helper.getResourceAsImage(this.getClass(), "shoukan/defense_mode.png"), 0, 0, null);
			} else if (hero != null) {
				if (hero.getQuest() != null) {
					if (!hero.hasArrived()) {
						g2d.drawImage(Helper.getResourceAsImage(this.getClass(), "shoukan/expedition.png"), 0, 0, null);
					} else {
						g2d.drawImage(Helper.getResourceAsImage(this.getClass(), "shoukan/arrived.png"), 0, 0, null);
					}
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
	public void bind(Hand h) {
		this.game = h.getGame();
		this.acc = h.getAcc();
		this.side = h.getSide();
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
	public Side getSide() {
		return side;
	}

	@Override
	public void setSide(Side side) {
		this.side = side;
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
		setFlipped(flipped, true);
	}

	public void setFlipped(boolean flipped, boolean trigger) {
		boolean wasFlipped = this.flipped;
		this.flipped = flipped;

		if (!this.flipped) {
			if (wasFlipped) defending = true;

			if (trigger) {
				if (wasFlipped) {
					game.applyEffect(game.getCurrentSide() == side ? ON_SUMMON : ON_FLIP, this, side, getIndex(), new Source(this, side, getIndex()));
				}

				game.applyEffect(ON_SWITCH, this, side, getIndex(), new Source(this, side, getIndex()));
			}
		}
	}

	@Override
	public boolean isAvailable() {
		return available;
	}

	@Override
	public void setAvailable(boolean available) {
		this.available = available;
	}

	public List<CardLink> getLinkedTo() {
		return linkedTo;
	}

	public void link(Equipment link) {
		this.linkedTo.add(new CardLink(link.getIndexReference(), link, this));
		link.link(this);

		game.applyEffect(ON_EQUIP, this, side, getIndex(), new Source(this, side, getIndex()));
	}

	public void link(Equipment link, boolean fake) {
		this.linkedTo.add(new CardLink(fake ? new AtomicInteger(-1) : link.getIndexReference(), link, this));
		link.link(this);

		game.applyEffect(ON_EQUIP, this, side, getIndex(), new Source(this, side, getIndex()));
	}

	public void unlink(Equipment link) {
		int i = -1;
		for (int j = 0; j < linkedTo.size(); j++) {
			CardLink cl = linkedTo.get(j);
			if (cl.getIndex() == link.getIndex() && cl.linked().equals(link)) {
				i = j;
				break;
			}
		}

		if (i > -1) {
			linkedTo.remove(i);
		}

		link.unlink();
	}

	public boolean isLinkedTo(String name) {
		return linkedTo.stream().anyMatch(cl -> cl.linked().getCard().getId().equalsIgnoreCase(name));
	}

	public boolean isDefending() {
		return !isDuelling() && (flipped || defending || isStasis() || isStunned() || isSleeping());
	}

	public void setDefending(boolean defending) {
		setDefending(defending, true);
	}

	public void setDefending(boolean defending, boolean trigger) {
		this.defending = defending;

		if (trigger) {
			game.applyEffect(ON_SWITCH, this, side, getIndex(), new Source(this, side, getIndex()));
		}
	}

	public boolean isSealed() {
		return sealed;
	}

	public void setSealed(boolean sealed) {
		this.sealed = sealed;
		if (sealed) {
			bonus.getFlags().clear();
			bonus.getPFlags().clear();
		}
	}

	public Race getRace() {
		return Helper.getOr(altRace, race);
	}

	public int getMana() {
		return Math.max(0, mana + bonus.getMana());
	}

	public int getMana(int fusion) {
		return getMana() + (this.fusion ? fusion : 0);
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

	public int getBaseAtk() {
		return atk;
	}

	public int getBaseDef() {
		return def;
	}

	public int getBaseStats() {
		return atk + def;
	}

	public int getAtk() {
		if (altAtk == -1) altAtk = atk;
		float fBonus = 1;
		float cBonus = 1;
		float hBonus = 1;

		if (game != null) {
			Side s = game.getSideById(acc.getUid());
			Triple<Race, Boolean, Race> combos = game.getCombos().getOrDefault(s, Triple.of(Race.NONE, false, Race.NONE));

			if (linkedTo.stream().noneMatch(sl -> sl.asEquipment().getCharms().contains(Charm.LINK)) && bonus.getCharm() != Charm.LINK) {
				Field f = game.getArena().getField();
				if (f != null) {
					boolean capped = hero != null && hero.getPerks().contains(Perk.REAPER);
					fBonus = f.getModifiers().getFloat(getRace().name());
					if (capped) fBonus = Math.min(fBonus, 0);

					if (combos.getLeft() == Race.ELF) {
						if (fBonus < 0) fBonus /= 2;
						else if (fBonus > 0) fBonus *= 1.25;
					}

					fBonus++;
				}
			}

			if (combos.getRight() == Race.UNDEAD)
				cBonus += game.getArena().getGraveyard().get(s).size() / 100f;

			if (hero != null) {
				for (Perk perk : hero.getPerks()) {
					hBonus *= switch (perk) {
						case MASOCHIST -> 1 + (1 - Helper.prcnt(hero.getHitpoints(), hero.getMaxHp())) / 2;
						case COWARD -> 1 - (1 - Helper.prcnt(hero.getHitpoints(), hero.getMaxHp())) / 2;
						default -> 1;
					};
				}
			}
		}

		float extraFac = 1f;
		if (!hasEffect() && !fusion) {
			extraFac *= 1.1f;
		}

		return Helper.roundTrunc(Math.max(0, Math.round((altAtk + bonus.getAtk()) * fBonus * cBonus * hBonus * extraFac)), 25);
	}

	public int getDef() {
		if (altDef == -1) altDef = def;
		float fBonus = 1;
		float cBonus = 1;
		float hBonus = 1;

		if (game != null) {
			Side s = game.getSideById(acc.getUid());
			Triple<Race, Boolean, Race> combos = game.getCombos().getOrDefault(s, Triple.of(Race.NONE, false, Race.NONE));

			if (linkedTo.stream().noneMatch(sl -> sl.asEquipment().getCharms().contains(Charm.LINK)) && bonus.getCharm() != Charm.LINK) {
				Field f = game.getArena().getField();
				if (f != null) {
					boolean capped = hero != null && hero.getPerks().contains(Perk.REAPER);
					fBonus = f.getModifiers().getFloat(getRace().name());
					if (capped) fBonus = Math.min(fBonus, 1);

					if (combos.getLeft() == Race.ELF) {
						if (fBonus < 0) fBonus /= 2;
						else if (fBonus > 0) fBonus *= 1.25;
					}

					fBonus++;
				}
			}

			if (combos.getRight() == Race.SPIRIT)
				cBonus += game.getArena().getGraveyard().get(s).size() / 100f;

			if (hero != null) {
				for (Perk perk : hero.getPerks()) {
					hBonus *= switch (perk) {
						case MASOCHIST -> 1 - (1 - Helper.prcnt(hero.getHitpoints(), hero.getMaxHp())) / 2;
						case COWARD -> 1 + (1 - Helper.prcnt(hero.getHitpoints(), hero.getMaxHp())) / 2;
						default -> 1;
					};
				}
			}
		}

		float extraFac = 1f;
		if (!hasEffect() && !fusion) {
			extraFac *= 1.1f;
		}

		return Helper.roundTrunc(Math.max(0, Math.round((altDef + bonus.getDef()) * fBonus * cBonus * hBonus * extraFac)), 25);
	}

	public void setAtk(int atk) {
		bonus.setAtk(atk);
	}

	public void setDef(int def) {
		bonus.setDef(def);
	}

	public void setDodge(int dodge) {
		bonus.setDodge(dodge);
	}

	public void addAtk(int atk) {
		bonus.addAtk(atk);
	}

	public void addDef(int def) {
		bonus.addDef(def);
	}

	public void addDodge(int dodge) {
		bonus.addDodge(dodge);
	}

	public void removeAtk(int atk) {
		bonus.removeAtk(atk);
	}

	public void removeDef(int def) {
		bonus.removeDef(def);
	}

	public void removeDodge(int dodge) {
		bonus.removeDodge(dodge);
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

	public void setModAtk(int mAtk) {
		this.mAtk = mAtk;
	}

	public void addModAtk(int mAtk) {
		this.mAtk += mAtk;
	}

	public void removeModAtk(int mAtk) {
		this.mAtk -= mAtk;
	}

	public void setModDef(int mDef) {
		this.mDef = mDef;
	}

	public void addModDef(int mDef) {
		this.mDef += mDef;
	}

	public void removeModDef(int mDef) {
		this.mDef -= mDef;
	}

	public int getEffAtk() {
		return Math.max(0, getAtk() + mAtk);
	}

	public int getEffDef() {
		return Math.max(0, getDef() + mDef);
	}

	public int getFinAtk() {
		return Math.max(0, getEffAtk() + getLinkedTo().stream()
				.map(CardLink::asEquipment)
				.mapToInt(Equipment::getAtk)
				.sum()
		);
	}

	public int getPenAtk() {
		return (int) Math.round(getFinAtk() * linkedTo.stream()
				.map(CardLink::asEquipment)
				.filter(e -> e.getCharms().contains(Charm.PIERCING))
				.mapToDouble(e -> 0.075 * e.getTier())
				.sum()
		);
	}

	public int getBldAtk() {
		return (int) Math.round(getFinAtk() * linkedTo.stream()
				.map(CardLink::asEquipment)
				.filter(e -> e.getCharms().contains(Charm.BLEEDING))
				.mapToDouble(e -> 0.05 * e.getTier())
				.sum()
		);
	}

	public int getManaDrain() {
		return linkedTo.stream()
				.map(CardLink::asEquipment)
				.filter(e -> e.getCharms().contains(Charm.DRAIN))
				.mapToInt(e -> (int) Helper.getFibonacci(e.getTier()))
				.sum();
	}

	public int getFinDef() {
		return Math.max(0, getEffDef() + getLinkedTo().stream()
				.map(CardLink::asEquipment)
				.mapToInt(Equipment::getDef)
				.sum()
		);
	}

	public int getFinAttr() {
		return isDefending() ? getFinDef() : getFinAtk();
	}

	public int getDodge(boolean ignoreAdaptive) {
		if (isStasis() || isStunned() || isSleeping()) return 0;

		double heroMod = 1;
		int extra = isDuelling() ? 50 : 0;
		if (hero != null) {
			extra += hero.getDodge();

			if (game != null) {
				if (hero.getPerks().contains(Perk.NIGHTCAT) && game.getArena().getField() != null) {
					heroMod = game.getArena().getField().isDay() ? 0.5 : 2;
				}

				if (!ignoreAdaptive && hero.getPerks().contains(Perk.ADAPTIVE)) {
					if (!isDefending())
						extra += getBlock(true) * 2;
					else
						heroMod = 0;
				}
			}
		}

		if (game != null && acc != null) {
			extra += game.getHands().get(game.getSideById(acc.getUid())).getMitigation() * 75;
		}

		int agiEquips = (int) Math.round(getLinkedTo().stream()
				.map(CardLink::asEquipment)
				.filter(e -> e.getCharms().contains(Charm.AGILITY))
				.mapToDouble(e -> 7.5 * e.getTier())
				.sum()
		);
		double d = Helper.clamp((bonus.getDodge() + mDodge + agiEquips + extra) * heroMod, 0, 100);
		double parFac = !isAvailable() ? 0.75 : 1;
		return (int) Helper.roundTrunc(d * parFac * 100, 5) / 100;
	}

	public int getDodge() {
		return getDodge(false);
	}

	public double getModDodge() {
		return mDodge;
	}

	public void setModDodge(double dodge) {
		this.mDodge = dodge;
	}

	public void addModDodge(double dodge) {
		this.mDodge += dodge;
	}

	public void removeModDodge(double dodge) {
		this.mDodge -= dodge;
	}

	public int getBlock(boolean ignoreAdaptive) {
		if (isStasis() || isStunned() || isSleeping()) return 0;

		double heroMod = 1;
		int extra = 0;
		if (hero != null) {
			extra += hero.getBlock();

			if (game != null) {
				if (!ignoreAdaptive && hero.getPerks().contains(Perk.ADAPTIVE)) {
					if (isDefending())
						extra += getDodge(true) / 2;
					else
						heroMod = 0;
				}
			}
		}

		int blockEquips = getLinkedTo().stream()
				.map(CardLink::asEquipment)
				.filter(e -> e.getCharms().contains(Charm.FORTIFY))
				.mapToInt(e -> 5 * e.getTier())
				.sum();
		double d = Helper.clamp((bonus.getBlock() + mBlock + blockEquips + extra) * heroMod * (isDefending() ? 2 : 1), 0, 100);
		return (int) d;
	}

	public int getBlock() {
		return getBlock(false);
	}

	public double getModBlock() {
		return mBlock;
	}

	public void setModBlock(double block) {
		this.mBlock = block;
	}

	public void resetAttribs() {
		this.mAtk = 0;
		this.mDef = 0;
		this.mDodge = 0;
		this.mBlock = 0;
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

	public String getAltImage() {
		return altImage;
	}

	public void setAltImage(String altImage) {
		this.altImage = altImage;
	}

	public String getDescription() {
		return sealed ? "Carta selada." : Helper.getOr(altDescription, description);
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setAltDescription(String description) {
		this.altDescription = description;
	}

	public boolean hasEffect() {
		return Helper.getOr(altEffect, effect) != null && !sealed;
	}

	public boolean isFusion() {
		return fusion;
	}

	public void setFusion(boolean fusion) {
		this.fusion = fusion;
	}

	public String getRawEffect() {
		return sealed ? null : Helper.getOr(altEffect, effect);
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
		String effect = getRawEffect();
		if (triggerLock || effect == null || !effect.contains(ep.getTrigger().name())) return;

		Hand other = ep.getHands().get(ep.getOtherSide());
		try {
			triggerLock = true;

			if (hero != null) {
				other.setHeroDefense(true);
			}

			GroovyShell gs = new GroovyShell();
			gs.setVariable("ep", ep);
			gs.setVariable("self", this);
			gs.evaluate(effect);
		} catch (Exception e) {
			Helper.logger(this.getClass()).warn("Erro ao executar efeito de " + card.getName(), e);
		} finally {
			other.setHeroDefense(false);
		}
	}

	public boolean isCursed() {
		return !curses.isEmpty();
	}

	public Set<String> getRawCurses() {
		return curses;
	}

	public void setRawCurses(Set<String> curse) {
		this.curses = curse;
	}

	public void addCurse(String curse) {
		this.curses.add(curse);
	}

	public void getCurse(EffectParameters ep) {
		if (triggerLock) return;

		triggerLock = true;

		GroovyShell gs = new GroovyShell();
		gs.setVariable("ep", ep);
		gs.setVariable("self", this);

		for (String curse : curses) {
			if (curse == null || !curse.contains(ep.getTrigger().name())) continue;

			try {
				gs.evaluate(curse);
			} catch (Exception e) {
				Helper.logger(this.getClass()).warn("Erro ao executar maldição de " + card.getName(), e);
			}
		}
	}

	public void triggerSpells(EffectParameters ep) {
		for (CardLink cl : List.copyOf(linkedTo)) {
			if (cl.isFake()) continue;

			Equipment e = cl.asEquipment();
			if (!e.hasEffect()) continue;

			e.getEffect(ep);
		}
	}

	public Class getCategory() {
		return category;
	}

	public void setCategory(Class category) {
		this.category = category;
	}

	public Set<String> getTags() {
		if (tags == null) return Set.of();

		return new JSONArray(tags).stream()
				.map(String::valueOf)
				.collect(Collectors.toSet());
	}

	public Champion getFakeCard() {
		return fakeCard;
	}

	public void setFakeCard(Champion fakeCard) {
		this.fakeCard = fakeCard;
	}

	public Set<String> getRequiredCards() {
		return requiredCards;
	}

	public List<FusionMaterial> canFuse(List<String> champ, List<String> equip, String field) {
		Set<String> rem = new HashSet<>(requiredCards);
		List<FusionMaterial> out = new ArrayList<>();

		for (String req : requiredCards) {
			if (req.contains(",")) {
				String[] optArgs = req.split(";");
				int reqCombo = Integer.parseInt(optArgs[0]);
				String[] opts = optArgs[1].split(",");

				Set<String> found = new HashSet<>();
				for (String opt : opts) {
					if (champ.contains(opt)) {
						if (found.add(opt)) {
							out.add(new FusionMaterial(opt, champ.indexOf(opt), false));

							if (found.size() == reqCombo) break;
						}
					} else if (equip.contains(opt)) {
						if (found.add(opt)) {
							out.add(new FusionMaterial(opt, equip.indexOf(opt), true));

							if (found.size() == reqCombo) break;
						}
					} else if (field.equals(opt)) {
						if (found.add(opt) && found.size() == reqCombo) break;
					}
				}

				if (found.size() != reqCombo) return List.of();
				else rem.remove(req);
			} else {
				if (champ.contains(req)) {
					out.add(new FusionMaterial(req, champ.indexOf(req), false));
					rem.remove(req);
				} else if (equip.contains(req)) {
					out.add(new FusionMaterial(req, equip.indexOf(req), true));
					rem.remove(req);
				} else if (field.equals(req)) {
					rem.remove(req);
				} else return List.of();
			}
		}

		if (rem.isEmpty()) {
			out.sort(Comparator.comparingInt(fm -> fm.equipment() ? 0 : 1));

			return out;
		} else return List.of();
	}

	public List<FusionMaterial> canFuse(List<String> cards) {
		Set<String> rem = new HashSet<>(requiredCards);
		List<FusionMaterial> out = new ArrayList<>();

		for (String req : requiredCards) {
			if (req.contains(",")) {
				String[] optArgs = req.split(";");
				int reqCombo = Integer.parseInt(optArgs[0]);
				String[] opts = optArgs[1].split(",");

				Set<String> found = new HashSet<>();
				for (String opt : opts) {
					if (cards.contains(opt)) {
						if (found.add(opt)) {
							out.add(new FusionMaterial(opt, cards.indexOf(opt), false));

							if (found.size() == reqCombo) break;
						}
					}
				}

				if (found.size() != reqCombo) return List.of();
				else rem.remove(req);
			} else {
				if (cards.contains(req)) {
					out.add(new FusionMaterial(req, cards.indexOf(req), false));
					rem.remove(req);
				} else return List.of();
			}
		}

		if (rem.isEmpty()) {
			out.sort(Comparator.comparingInt(fm -> fm.equipment() ? 0 : 1));

			return out;
		} else return List.of();
	}

	public void setRequiredCards(Set<String> requiredCards) {
		this.requiredCards = requiredCards;
	}

	public void duel(Champion nemesis, BiConsumer<Side, Shoukan> onDuelEnd) {
		if (this.nemesis != null) return;

		this.flipped = false;
		this.defending = false;
		this.nemesis = nemesis;
		this.onDuelEnd = onDuelEnd;
		nemesis.duel(this, onDuelEnd);
	}

	public Champion getNemesis() {
		return nemesis;
	}

	public boolean isDuelling() {
		if (acc != null && game != null) {
			Side s = game.getSideById(acc.getUid());

			if (nemesis != null && (nemesis.getIndex() == -2 || !Objects.equals(game.getSlot(s.getOther(), nemesis.getIndex()).getTop(), nemesis))) {
				if (getIndex() != -2)
					onDuelEnd.accept(s, game);

				if (nemesis != null) {
					nemesis.nemesis = null;
					nemesis.onDuelEnd = null;
				}
				nemesis = null;
				onDuelEnd = null;
			}
		}

		return nemesis != null;
	}

	public boolean isStasis() {
		return !isDuelling() && stasis > 0;
	}

	public int getStasis() {
		return stasis;
	}

	public void setStasis(int stasis) {
		this.stasis = Math.max(stasis, this.stasis);
		if (getStasis() > 0) defending = true;
	}

	public void reduceStasis() {
		int dec = 1;
		if (game.getCombos().get(side).getLeft() == Race.CREATURE) {
			Field f = game.getArena().getField();
			if (f != null && f.isDay()) dec *= 2;
		}

		this.stasis = Math.max(stasis - dec, 0);
	}

	public void reduceStasis(int val) {
		this.stasis = Math.max(stasis - val, 0);
	}

	public boolean isStunned() {
		return !isStasis() && stun > 0;
	}

	public int getStun() {
		return stun;
	}

	public void setStun(int stun) {
		if (bonus.getFlags().contains(Flag.NOSTUN)) {
			this.stun = 0;
			return;
		}

		this.stun = Math.max(stun, this.stun);
		if (getStun() > 0) defending = true;
	}

	public void reduceStun() {
		int dec = 1;
		if (game.getCombos().get(side).getLeft() == Race.CREATURE) {
			Field f = game.getArena().getField();
			if (f != null && f.isDay()) dec *= 2;
		}

		this.stun = Math.max(stun - dec, 0);
	}

	public void reduceStun(int val) {
		this.stun = Math.max(stun - val, 0);
	}

	public boolean isSleeping() {
		return !isStunned() && sleep > 0;
	}

	public int getSleep() {
		return sleep;
	}

	public void setSleep(int sleep) {
		if (bonus.getFlags().contains(Flag.NOSLEEP)) {
			this.sleep = 0;
			return;
		}

		this.sleep = Math.max(sleep, this.sleep);
		if (getSleep() > 0) defending = true;
	}

	public void reduceSleep() {
		int dec = 1;
		if (game.getCombos().get(side).getLeft() == Race.CREATURE) {
			Field f = game.getArena().getField();
			if (f != null && f.isDay()) dec *= 2;
		}

		this.sleep = Math.max(sleep - dec, 0);
	}

	public void reduceSleep(int val) {
		this.sleep = Math.max(sleep - val, 0);
	}

	public boolean isTriggerLocked() {
		return triggerLock;
	}

	public void lockTrigger() {
		this.triggerLock = true;
	}

	public void unlockTrigger() {
		this.triggerLock = false;
		for (CardLink link : linkedTo) {
			link.asEquipment().unlockTrigger();
		}
	}

	public Status getStatus() {
		if (isFlipped())
			return Status.FLIPPED;
		if (isStasis())
			return Status.STASIS;
		if (isStunned())
			return Status.STUNNED;
		if (isSleeping())
			return Status.SLEEPING;
		if (!isAvailable())
			return Status.UNAVAILABLE;
		if (isDefending())
			return Status.DEFENDING;

		return Status.NONE;
	}

	public Set<Charm> getCharms() {
		Set<Charm> charms = EnumSet.noneOf(Charm.class);
		if (bonus.getCharm() != null)
			charms.add(bonus.getCharm());

		charms.addAll(linkedTo.stream()
				.map(CardLink::asEquipment)
				.map(Equipment::getCharms)
				.flatMap(List::stream)
				.toList()
		);

		return charms;
	}

	public boolean isBuffed() {
		if (game == null) return false;
		else if (getCharms().contains(Charm.LINK)) return false;
		else if (hero != null && hero.getPerks().contains(Perk.REAPER)) return false;

		Field f = game.getArena().getField();
		if (f != null) {
			Champion c = Helper.getOr(fakeCard, this);
			return f.getModifiers().getFloat(c.getRace().name()) > 0;
		}

		return false;
	}

	public boolean isNerfed() {
		if (game == null) return false;
		else if (getCharms().contains(Charm.LINK)) return false;

		Field f = game.getArena().getField();
		if (f != null) {
			Champion c = Helper.getOr(fakeCard, this);
			return f.getModifiers().getFloat(c.getRace().name()) < 0;
		}

		return false;
	}

	public boolean canGoToGrave() {
		return !fusion && !gravelocked;
	}

	public void setGravelocked(boolean gravelocked) {
		this.gravelocked = gravelocked;
	}

	public Hero getHero() {
		return hero;
	}

	public void setHero(Hero hero) {
		if (hero == null) return;

		this.hero = hero;
	}

	public Champion getAdjacent(Neighbor direction) {
		int index = getIndex();
		if (!Helper.between(index, 0, 5)) return null;

		return switch (direction) {
			case LEFT -> index > 0 ? game.getArena().getSlots().get(side).get(index - 1).getTop() : null;
			case RIGHT -> index < 4 ? game.getArena().getSlots().get(side).get(index + 1).getTop() : null;
			default -> null;
		};
	}

	@Override
	public void reset() {
		flipped = false;
		available = true;
		defending = false;
		linkedTo = new UniqueList<>(CardLink::getIndex);
		bonus = new Bonus();
		fakeCard = null;
		nemesis = null;
		onDuelEnd = null;
		index = new AtomicInteger(-2);
		altAtk = -1;
		altDef = -1;
		altImage = null;
		altDescription = null;
		altEffect = null;
		curses = new HashSet<>();
		altRace = null;
		mAtk = 0;
		mDef = 0;
		mDodge = 0;
		mBlock = 0;
		stasis = 0;
		stun = 0;
		sleep = 0;
		triggerLock = false;
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
			Champion c = (Champion) super.clone();
			c.linkedTo = new UniqueList<>(CardLink::getIndex);
			c.index = new AtomicInteger(-2);
			c.curses = new HashSet<>();
			c.bonus = bonus.clone();
			c.nemesis = null;
			c.onDuelEnd = null;
			return c;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	@Override
	public Champion deepCopy() {
		try {
			Champion c = (Champion) super.clone();
			c.linkedTo = new UniqueList<>(CardLink::getIndex);
			for (CardLink cl : linkedTo) {
				c.linkedTo.add(new CardLink(
						new AtomicInteger(cl.getIndex()),
						cl.linked(),
						c
				));
			}

			c.index = new AtomicInteger(index.get());
			c.curses = new HashSet<>(curses);
			c.bonus = bonus.clone();
			if (nemesis != null) {
				c.nemesis = nemesis.deepCopy();
				c.onDuelEnd = onDuelEnd;
			}
			return c;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	public String getBase64() {
		return Helper.atob(drawCard(false), "png");
	}

	@Override
	public String toString() {
		return "Champion@%x".formatted(super.hashCode());
	}
}