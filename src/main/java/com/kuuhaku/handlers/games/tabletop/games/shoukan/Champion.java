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
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Charm;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Class;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
	private int blood;

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
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Set<String> requiredCards = new HashSet<>();

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean fusion = false;

	private transient boolean flipped = false;
	private transient boolean available = true;
	private transient boolean defending = false;
	private transient boolean sealed = false;
	private transient Shoukan game = null;
	private transient Account acc = null;
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
	private transient int redAtk = 0;
	private transient int redDef = 0;
	private transient int efctMana = 0;
	private transient int efctBlood = 0;
	private transient int[] efctAtk = new int[6];
	private transient int[] efctDef = new int[6];
	private transient int stasis = 0;
	private transient int stun = 0;
	private transient int sleep = 0;
	private transient double dodge = 0;
	private transient double mDodge = 0;
	private transient boolean gravelocked = false;

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

		if (flipped) {
			g2d.drawImage(acc.getFrame().getBack(acc), 0, 0, null);
		} else {
			if (fakeCard != null) {
				boolean useFoil = acc.isUsingFoil() && CardDAO.hasCompleted(acc.getUid(), fakeCard.getCard().getAnime().getName(), true);

				g2d.drawImage(fakeCard.getCard().drawCardNoBorder(useFoil), 0, 0, null);
			} else {
				boolean useFoil = acc.isUsingFoil() && CardDAO.hasCompleted(acc.getUid(), card.getAnime().getName(), true);

				g2d.drawImage(card.drawCardNoBorder(useFoil), 0, 0, null);
			}
			g2d.drawImage(acc.getFrame().getFront(), 0, 0, null);
			g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 20));

			if (fakeCard != null) {
				Profile.printCenteredString(StringUtils.abbreviate(fakeCard.getCard().getName(), 15), 181, 38, 32, g2d);
				g2d.drawImage(fakeCard.getRace().getIcon(), 11, 12, 23, 23, null);
			} else {
				Profile.printCenteredString(StringUtils.abbreviate(card.getName(), 15), 181, 38, 32, g2d);
				g2d.drawImage(getRace().getIcon(), 11, 12, 23, 23, null);
			}

			boolean drawnMana = false;
			if (getMana() > 0 || (fakeCard != null && fakeCard.getMana() > 0)) {
				g2d.drawImage(Helper.getResourceAsImage(this.getClass(), "shoukan/mana.png"), 184, 47, null);

				g2d.setColor(new Color(0, 165, 255));
				if (fakeCard != null)
					Profile.drawOutlinedText(String.valueOf(fakeCard.getMana()), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(fakeCard.getMana())), 67, g2d);
				else
					Profile.drawOutlinedText(String.valueOf(getMana()), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(getMana())), 67, g2d);

				drawnMana = true;
			}

			if (getBlood() > 0 || (fakeCard != null && fakeCard.getBlood() > 0)) {
				g2d.drawImage(Helper.getResourceAsImage(this.getClass(), "shoukan/blood.png"), 184, 47 + (drawnMana ? 23 : 0), null);

				g2d.setColor(new Color(255, 51, 0));
				if (fakeCard != null)
					Profile.drawOutlinedText(String.valueOf(fakeCard.getBlood()), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(fakeCard.getBlood())), 67 + (drawnMana ? 22 : 0), g2d);
				else
					Profile.drawOutlinedText(String.valueOf(getBlood()), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(getBlood())), 67 + (drawnMana ? 22 : 0), g2d);
			}

			String data = bonus.getSpecialData().getString("write");
			if (!data.isBlank()) {
				g2d.setColor(Color.yellow);
				g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 16));
				Profile.drawOutlinedText(data, 20, 66, g2d);
			}

			g2d.setFont(Fonts.DOREKING.deriveFont(Font.PLAIN, 20));

			g2d.setColor(Color.red);
			if (fakeCard != null)
				Profile.drawOutlinedText(String.valueOf(fakeCard.getFinAtk()), 45, 250, g2d);
			else
				Profile.drawOutlinedText(String.valueOf(getFinAtk()), 45, 250, g2d);

			g2d.setColor(Color.green);
			if (fakeCard != null)
				Profile.drawOutlinedText(String.valueOf(fakeCard.getFinDef()), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(fakeCard.getFinDef())), 250, g2d);
			else
				Profile.drawOutlinedText(String.valueOf(getFinDef()), 178 - g2d.getFontMetrics().stringWidth(String.valueOf(getFinDef())), 250, g2d);

			g2d.setFont(new Font("Arial", Font.BOLD, 11));
			g2d.setColor(Color.black);
			g2d.drawString("[" + getRace().toString().toUpperCase(Locale.ROOT) + (effect == null ? "" : "/EFEITO") + "]", 9, 277);

			g2d.setFont(Fonts.HAMMERSMITH_ONE.deriveFont(Font.PLAIN, 11));
			Profile.drawStringMultiLineNO(g2d, fakeCard != null ? fakeCard.getDescription() : Helper.getOr(altDescription, description), 205, 9, 293);

			if (isStasis() || isStunned() || isSleeping()) {
				available = false;
			}

			if (!available) {
				g2d.setColor(new Color(0, 0, 0, 150));
				g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());
			}

			if (isStasis()) {
				try {
					BufferedImage dm = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("shoukan/stasis.png")));
					g2d.drawImage(dm, 0, 0, null);
				} catch (IOException ignore) {
				}
			} else if (isStunned()) {
				try {
					BufferedImage dm = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("shoukan/stun.png")));
					g2d.drawImage(dm, 0, 0, null);
				} catch (IOException ignore) {
				}
			} else if (isSleeping()) {
				try {
					BufferedImage dm = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("shoukan/sleep.png")));
					g2d.drawImage(dm, 0, 0, null);
				} catch (IOException ignore) {
				}
			} else if (isDefending()) {
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

	public void setLinkedTo(List<Equipment> linkedTo) {
		this.linkedTo = Helper.getOr(linkedTo, new ArrayList<>());
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
		return flipped || defending || getStun() > 0 || getStasis() > 0;
	}

	public void setDefending(boolean defending) {
		this.defending = defending;
	}

	public boolean isSealed() {
		return sealed;
	}

	public void setSealed(boolean sealed) {
		this.sealed = sealed;
	}

	public Race getRace() {
		return Helper.getOr(altRace, race);
	}

	public int getMana() {
		return mana + efctMana;
	}

	public void setMana(int mana) {
		this.mana = mana;
	}

	public void setEfctMana(int mana) {
		this.efctMana = mana;
	}

	public void addEfctMana(int mana) {
		this.efctMana += mana;
	}

	public void removeEfctMana(int mana) {
		this.efctMana -= mana;
	}

	public int getBlood() {
		return blood + efctBlood;
	}

	public void setEfctBlood(int blood) {
		this.efctBlood = blood;
	}

	public void addEfctBlood(int blood) {
		this.efctBlood += blood;
	}

	public void removeEfctBlood(int blood) {
		this.efctBlood -= blood;
	}

	public void setBlood(int blood) {
		this.blood = blood;
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

		if (game != null) {
			Side s = game.getSideById(acc.getUid());
			Pair<Race, Race> combos = game.getCombos().getOrDefault(s, Pair.of(Race.NONE, Race.NONE));

			if (linkedTo.stream().noneMatch(e -> e.getCharm() == Charm.SOULLINK || bonus.getSpecialData().getEnum(Charm.class, "charm") == Charm.SOULLINK)) {
				Field f = game.getArena().getField();
				if (f != null) {
					fBonus = f.getModifiers().getFloat(getRace().name(), 1);

					if (combos.getLeft() == Race.ELF) {
						if (fBonus < 1) fBonus /= 2;
						else if (fBonus > 1) fBonus *= 1.25;
					}
				}
			}

			if (combos.getLeft() == Race.UNDEAD) {
				cBonus += game.getArena().getGraveyard().get(s).size() / 100f;
			} else if (combos.getRight() == Race.UNDEAD)
				cBonus += game.getArena().getGraveyard().get(s).size() / 200f;
		}

		return Helper.roundTrunc(Math.max(0, Math.round((altAtk - redAtk + getEfctAtk()) * fBonus * cBonus * (Helper.getOr(altEffect, effect) == null && !fusion ? 1.1f : 1))), 25);
	}

	public int getDef() {
		if (altDef == -1) altDef = def;
		float fBonus = 1;
		float cBonus = 1;

		if (game != null) {
			if (linkedTo.stream().noneMatch(e -> e.getCharm() == Charm.SOULLINK || bonus.getSpecialData().getEnum(Charm.class, "charm") == Charm.SOULLINK)) {
				Field f = game.getArena().getField();
				if (f != null)
					fBonus = f.getModifiers().getFloat(getRace().name(), 1);
			}

			Side s = game.getSideById(acc.getUid());
			Pair<Race, Race> combos = game.getCombos().getOrDefault(s, Pair.of(Race.NONE, Race.NONE));
			if (combos.getLeft() == Race.SPIRIT) {
				cBonus += game.getArena().getGraveyard().get(s).size() / 50f;
			} else if (combos.getRight() == Race.SPIRIT)
				cBonus += game.getArena().getGraveyard().get(s).size() / 100f;
		}

		return Helper.roundTrunc(Math.max(0, Math.round((altDef - redDef + getEfctDef()) * fBonus * cBonus * (Helper.getOr(altEffect, effect) == null && !fusion ? 1.1f : 1))), 25);
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

	public int getRedAtk() {
		return redAtk;
	}

	public void setRedAtk(int redAtk) {
		this.redAtk = redAtk;
	}

	public void addRedAtk(int redAtk) {
		this.redAtk += redAtk;
	}

	public void removeRedAtk(int redAtk) {
		this.redAtk -= redAtk;
	}

	public int getRedDef() {
		return redDef;
	}

	public void setRedDef(int redDef) {
		this.redDef = redDef;
	}

	public void addRedDef(int redDef) {
		this.redDef += redDef;
	}

	public void removeRedDef(int redDef) {
		this.redDef -= redDef;
	}

	public int getEfctAtk() {
		return Arrays.stream(efctAtk).sum();
	}

	public void addEfctAtk(int efctAtk) {
		this.efctAtk[5] += efctAtk;
	}

	public void removeEfctAtk(int efctAtk) {
		this.efctAtk[5] -= efctAtk;
	}

	public void setEfctAtk(int index, int efctAtk) {
		this.efctAtk[index] = efctAtk;
	}

	public void addEfctAtk(int index, int efctAtk) {
		this.efctAtk[index] += efctAtk;
	}

	public void removeEfctAtk(int index, int efctAtk) {
		this.efctAtk[index] -= efctAtk;
	}

	public int getEfctDef() {
		return Arrays.stream(efctDef).sum();
	}

	public void addEfctDef(int efctDef) {
		this.efctDef[5] += efctDef;
	}

	public void removeEfctDef(int efctDef) {
		this.efctDef[5] -= efctDef;
	}

	public void setEfctDef(int index, int efctDef) {
		this.efctDef[index] = efctDef;
	}

	public void addEfctDef(int index, int efctDef) {
		this.efctDef[index] += efctDef;
	}

	public void removeEfctDef(int index, int efctDef) {
		this.efctDef[index] -= efctDef;
	}

	public int getEffAtk() {
		return Math.max(0, getAtk() + mAtk + bonus.getAtk());
	}

	public int getEffDef() {
		return Math.max(0, getDef() + mDef + bonus.getDef());
	}

	public int getFinAtk() {
		return Math.max(0, getEffAtk() + getLinkedTo().stream().mapToInt(Equipment::getAtk).sum());
	}

	public int getFinDef() {
		return Math.max(0, getEffDef() + getLinkedTo().stream().mapToInt(Equipment::getDef).sum());
	}

	public double getDodge() {
		return Helper.clamp(dodge + mDodge + getLinkedTo().stream().filter(e -> e.getCharm() == Charm.AGILITY).count() * 15, 0, 100);
	}

	public void setDodge(double dodge) {
		this.dodge = dodge;
	}

	public void addDodge(double dodge) {
		this.dodge += dodge;
	}

	public void removeDodge(double dodge) {
		this.dodge -= dodge;
	}

	public double getModDodge() {
		return mDodge;
	}

	public void setModDodge(double dodge) {
		this.mDodge = dodge;
	}

	public void resetAttribs() {
		this.mAtk = 0;
		this.mDef = 0;
		this.mDodge = 0;
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
		return sealed ? "Carta selada." : description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setAltDescription(String description) {
		this.altDescription = description;
	}

	public boolean hasEffect() {
		return effect != null && !sealed;
	}

	public boolean isFusion() {
		return fusion;
	}

	public void setFusion(boolean fusion) {
		this.fusion = fusion;
	}

	public String getRawEffect() {
		return sealed ? null : effect;
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
		String imports = EffectParameters.IMPORTS.formatted(card.getName());

		try {
			Interpreter i = new Interpreter();
			i.setStrictJava(true);
			i.set("ep", ep);
			i.set("self", this);
			i.eval(imports + Helper.getOr(altEffect, effect));
		} catch (EvalError e) {
			Helper.logger(this.getClass()).warn(e + " | " + e.getStackTrace()[0]);
		}
	}

	public void activateParasites(EffectParameters ep) {
		for (Equipment e : linkedTo) {
			if (e.getCharm() != Charm.SPELL || !e.isParasite()) continue;

			e.getEffect(ep);
		}
	}

	public Class getCategory() {
		return category;
	}

	public void setCategory(Class category) {
		this.category = category;
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
		altAtk = -1;
		altDef = -1;
		altDescription = null;
		altEffect = null;
		altRace = null;
		mAtk = 0;
		mDef = 0;
		redAtk = 0;
		redDef = 0;
		efctMana = 0;
		efctBlood = 0;
		efctAtk = new int[6];
		efctDef = new int[6];
		stasis = 0;
		stun = 0;
		sleep = 0;
		dodge = 0;
		mDodge = 0;
	}

	public Set<String> getRequiredCards() {
		return requiredCards;
	}

	public Map<String, Pair<Integer, Boolean>> canFuse(List<String> champ, List<String> equip, String field) {
		Set<String> rem = new HashSet<>(requiredCards);
		Map<String, Pair<Integer, Boolean>> out = new HashMap<>();

		for (String req : requiredCards) {
			if (req.contains(",")) {
				String[] optArgs = req.split(";");
				int reqCombo = Integer.parseInt(optArgs[0]);
				String[] opts = optArgs[1].split(",");

				Set<String> found = new HashSet<>();
				for (String opt : opts) {
					if (champ.contains(opt)) {
						if (found.add(opt)) {
							out.put(opt, Pair.of(champ.indexOf(opt), false));

							if (found.size() == reqCombo) break;
						}
					} else if (equip.contains(opt)) {
						if (found.add(opt)) {
							out.put(opt, Pair.of(equip.indexOf(opt), true));

							if (found.size() == reqCombo) break;
						}
					}
				}

				if (found.size() != reqCombo) return Map.of();
				else rem.remove(req);
			} else {
				if (champ.contains(req)) {
					out.put(req, Pair.of(champ.indexOf(req), false));
					rem.remove(req);
				} else if (equip.contains(req)) {
					out.put(req, Pair.of(equip.indexOf(req), true));
					rem.remove(req);
				} else if (req.equals(field)) {
					rem.remove(req);
				} else return Map.of();
			}
		}

		if (rem.isEmpty()) return out;
		else return Map.of();
	}

	public void setRequiredCards(Set<String> requiredCards) {
		this.requiredCards = requiredCards;
	}

	public boolean isStasis() {
		return stasis > 0;
	}

	public int getStasis() {
		return stasis;
	}

	public void setStasis(int stasis) {
		this.stasis = stasis;
		if (getStasis() > 0) defending = true;
	}

	public void reduceStasis() {
		this.stasis = Math.max(stasis - 1, 0);
	}

	public boolean isStunned() {
		return !isStasis() && stun > 0;
	}

	public int getStun() {
		return stun;
	}

	public void setStun(int stun) {
		this.stun = stun;
		if (getStun() > 0) defending = true;
	}

	public void reduceStun() {
		this.stun = Math.max(stun - 1, 0);
	}

	public boolean isSleeping() {
		return !isStunned() && sleep > 0;
	}

	public int getSleep() {
		return sleep;
	}

	public void setSleep(int sleep) {
		this.sleep = sleep;
		if (getSleep() > 0) defending = true;
	}

	public void reduceSleep() {
		this.sleep = Math.max(sleep - 1, 0);
	}

	public boolean isBuffed() {
		if (game == null) return false;

		boolean slink = getBonus().getSpecialData().getEnum(Charm.class, "charm") == Charm.SOULLINK || linkedTo.stream().anyMatch(e -> e.getCharm() == Charm.SOULLINK);
		if (slink) return false;

		Field f = game.getArena().getField();
		if (f != null)
			return f.getModifiers().getFloat(getRace().name(), 1) > 1;

		return false;
	}

	public boolean isNerfed() {
		if (game == null) return false;

		boolean slink = getBonus().getSpecialData().getEnum(Charm.class, "charm") == Charm.SOULLINK || linkedTo.stream().anyMatch(e -> e.getCharm() == Charm.SOULLINK);
		if (slink) return false;

		Field f = game.getArena().getField();
		if (f != null)
			return f.getModifiers().getFloat(getRace().name(), 1) < 1;

		return false;
	}

	public boolean isGravelocked() {
		return fusion || gravelocked;
	}

	public void setGravelocked(boolean gravelocked) {
		this.gravelocked = gravelocked;
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
			c.reset();
			return c;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	public String toString() {
		return new JSONObject(card.toString()) {{
			put("champion", new JSONObject() {{
				put("id", id);
				put("category", category);
				put("mana", mana);
				put("attack", atk);
				put("defense", def);
				put("description", description);
			}});
		}}.toString();
	}

	public String getBase64() {
		return Helper.atob(drawCard(false), "png");
	}
}
