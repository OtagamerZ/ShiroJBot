/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2024  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent.dunhun;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.Dunhun;
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.common.Delta;
import com.kuuhaku.model.common.dunhun.ActorModifiers;
import com.kuuhaku.model.common.dunhun.Equipment;
import com.kuuhaku.model.common.shoukan.RegDeg;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.Team;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.converter.JSONObjectConverter;
import com.kuuhaku.model.persistent.shoukan.CardAttributes;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.dunhun.Attributes;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Graph;
import com.ygimenez.json.JSONArray;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.TreeBag;
import org.apache.commons.text.WordUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Entity
@Table(name = "hero", schema = "dunhun")
public class Hero extends DAO<Hero> implements Actor {
	@Transient
	public final long SERIAL = ThreadLocalRandom.current().nextLong();

	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@Embedded
	private HeroStats stats = new HeroStats();

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "account_uid")
	@Fetch(FetchMode.JOIN)
	private Account account;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "equipment", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONObjectConverter.class)
	private JSONObject equipment = new JSONObject();

	private transient final ActorModifiers modifiers = new ActorModifiers();
	private transient final RegDeg regDeg = new RegDeg(null);
	private transient final Delta<Integer> hp = new Delta<>();
	private transient Equipment equipCache;
	private transient List<Skill> skillCache;
	private transient Bag<Consumable> consumableCache;
	private transient Senshi senshiCache;
	private transient Dunhun game;
	private transient Deck deck;
	private transient Team team;
	private transient int ap;
	private transient boolean flee;

	public Hero() {
	}

	public Hero(Account account, String name, Race race) {
		this.id = name.toUpperCase();
		this.account = account;
		stats.setRace(race);
	}

	@Override
	public String getId() {
		return id;
	}

	public String getName() {
		return WordUtils.capitalizeFully(id.replace("_", " "));
	}

	@Override
	public String getName(I18N locale) {
		return getName();
	}

	@Override
	public Race getRace() {
		return stats.getRace();
	}

	public boolean setImage(BufferedImage img) {
		String hash = HexFormat.of().formatHex(DigestUtils.getMd5Digest().digest(("H:" + id).getBytes()));
		File parent = new File(System.getenv("CARDS_PATH") + "../heroes");
		if (!parent.exists()) parent.mkdir();

		File f = new File(parent, hash + ".png");
		img = Graph.scaleAndCenterImage(Graph.toColorSpace(img, BufferedImage.TYPE_INT_ARGB), 225, 350);

		try {
			ImageIO.write(img, "png", f);
			return true;
		} catch (IOException e) {
			Constants.LOGGER.error(e, e);
			return false;
		}
	}

	@Override
	public int getHp() {
		if (hp.get() == null) hp.set(getMaxHp());
		return hp.get();
	}

	@Override
	public int getMaxHp() {
		return (int) ((500 + modifiers.getMaxHp().get()) * (1 + modifiers.getHpMult().get()) * (1 + getAttributes().vit() / 10d));
	}

	@Override
	public int getHpDelta() {
		if (hp.previous() == null) return 0;

		return hp.get() - hp.previous();
	}

	@Override
	public void modHp(int value) {
		if (getHp() == 0) return;

		if (value < 0 && senshiCache != null) {
			value = -value;

			if (senshiCache.isDefending()) {
				value = (int) -Math.max(value / 10f, (2.5 * Math.pow(value, 2)) / (senshiCache.getDfs() + 2.5 * value));
			} else {
				value = (int) -Math.max(value / 5f, (5 * Math.pow(value, 2)) / (senshiCache.getDfs() + 5 * value));
			}

			if (senshiCache.isSleeping()) {
				senshiCache.reduceSleep(999);
			}
		}

		hp.set(Calc.clamp(getHp() + value, 0, getMaxHp()));
	}

	@Override
	public void revive(int value) {
		if (getHp() > 0) return;

		hp.set(Calc.clamp(value, 0, getMaxHp()));
		if (senshiCache != null) {
			senshiCache.setAvailable(true);
		}
	}

	@Override
	public int getAp() {
		return ap;
	}

	@Override
	public int getMaxAp() {
		return Calc.clamp(1 + (int) modifiers.getMaxAp().get() + stats.getLevel() / 5, 1, 5 + getAttributes().dex() / 10);
	}

	@Override
	public void modAp(int value) {
		ap = Calc.clamp(ap + value, 0, getMaxAp());
	}

	@Override
	public int getInitiative() {
		return getAttributes().dex() / 3 + (int) modifiers.getInitiative().get();
	}

	@Override
	public double getCritical() {
		double crit = getEquipment().getWeaponList().stream()
				.mapToDouble(Gear::getCritical)
				.average().orElse(0);

		return (int) (crit * (1 + modifiers.getCritical().get() + getAttributes().dex() * 0.02));
	}

	@Override
	public int getAggroScore() {
		int aggro = 1;
		if (senshiCache != null) {
			aggro = senshiCache.getDmg() / 10 + senshiCache.getDfs() / 20;
		}

		return (int) (aggro * (1 + modifiers.getAggroMult().get()) * stats.getLevel() / 2);
	}

	@Override
	public boolean hasFleed() {
		return flee;
	}

	@Override
	public void setFleed(boolean flee) {
		this.flee = flee;
	}

	@Override
	public ActorModifiers getModifiers() {
		return modifiers;
	}

	@Override
	public RegDeg getRegDeg() {
		return regDeg;
	}

	public Attributes getAttributes() {
		return getStats().getAttributes().merge(getModifiers().getAttributes());
	}

	public HeroStats getStats() {
		return stats;
	}

	public Account getAccount() {
		return account;
	}

	public Equipment getEquipment() {
		if (equipCache != null) return equipCache;

		List<Integer> ids = new ArrayList<>();
		for (Object o : equipment.values()) {
			if (o instanceof Number n) {
				ids.add(n.intValue());
			} else if (o instanceof Collection<?> c) {
				for (Object n : c) {
					ids.add(((Number) n).intValue());
				}
			}
		}

		Map<Integer, Gear> gear = DAO.queryAll(Gear.class, "SELECT g FROM Gear g WHERE g.id IN ?1", ids)
				.parallelStream()
				.collect(Collectors.toMap(Gear::getId, Function.identity()));

		return equipCache = new Equipment((gs, i) -> {
			if (i < 0) {
				return gear.get(equipment.getInt(gs.name()));
			}

			return gear.get(equipment.getJSONArray(gs.name()).getInt(i));
		});
	}

	public List<Gear> getInventory() {
		List<Integer> ids = DAO.queryAllNative(Integer.class, """
				SELECT g.id
				FROM gear g
				INNER JOIN hero h ON h.id = g.owner_id
				WHERE h.id = ?1
				  AND NOT jsonb_path_exists(h.equipment, '$.* ? (@ == $val)', cast('{"val": ' || g.id || '}' AS JSONB))
				""", id);

		return DAO.queryAll(Gear.class, "SELECT g FROM Gear g WHERE g.id IN ?1", ids);
	}

	public Gear getInvGear(int id) {
		return DAO.query(Gear.class, "SELECT g FROM Gear g WHERE g.id = ?1 AND g.owner.id = ?2", id, this.id);
	}

	@Override
	public List<Skill> getSkills() {
		if (skillCache != null) return skillCache;

		return skillCache = DAO.queryAll(Skill.class, "SELECT s FROM Skill s WHERE s.id IN ?1", stats.getSkills());
	}

	public Bag<Consumable> getConsumables() {
		if (consumableCache != null) return consumableCache;

		Map<String, Consumable> cons = DAO.queryAll(Consumable.class, "SELECT c FROM Consumable c WHERE c.id IN ?1", stats.getConsumables())
				.stream()
				.collect(Collectors.toMap(Consumable::getId, Function.identity()));

		TreeBag<Consumable> out = new TreeBag<>();
		for (Object id : stats.getConsumables()) {
			if (cons.containsKey(String.valueOf(id))) {
				out.add(cons.get(String.valueOf(id)));
			}
		}

		return consumableCache = out;
	}

	public Consumable getConsumable(String id) {
		return getConsumables().stream()
				.filter(cons -> cons.getId().equals(id))
				.findFirst()
				.orElse(null);
	}

	@Override
	public Team getTeam() {
		return team;
	}

	@Override
	public void setTeam(Team team) {
		this.team = team;
	}

	@Override
	public void setGame(Dunhun game) {
		this.game = game;
	}

	@Override
	public Senshi asSenshi(I18N locale) {
		if (senshiCache != null) return senshiCache;

		senshiCache = new Senshi(this, locale);
		CardAttributes base = senshiCache.getBase();

		modifiers.clear();
		int dmg = 100;
		int def = 100;

		for (Gear g : getEquipment()) {
			if (g == null) continue;

			g.load(locale, this);
			def += g.getDfs();
			if (g.getBasetype().getStats().wpnType() == null) {
				dmg += g.getDmg();
			}
		}

		dmg += getEquipment().getWeaponList().stream()
				.mapToInt(g -> (int) (g.getDmg() * 0.6))
				.sum();

		Attributes a = getAttributes();
		base.setAtk((int) (dmg * (1 + a.str() * 0.075 + a.dex() * 0.05)));
		base.setDfs((int) (def * (1 + a.str() * 0.06 + a.dex() * 0.03)));
		base.setDodge(Math.max(0, a.dex() / 2 - a.vit() / 5));
		base.setParry(Math.max(0, a.dex() / 5));

		senshiCache.getStats().getPower().set(0.05 * a.wis());

		base.setMana(1 + (base.getAtk() + base.getDfs()) / 750);
		base.setSacrifices((base.getAtk() + base.getDfs()) / 3000);

		base.getTags().add("HERO");

		return senshiCache;
	}

	@Override
	public BufferedImage render(I18N locale) {
		if (deck == null) deck = account.getDeck();
		return asSenshi(locale).render(locale, deck);
	}

	@Override
	public void beforeSave() {
		if (equipCache != null) {
			equipment = new JSONObject(equipCache.toString());
			equipCache = null;
		}

		if (skillCache != null) {
			stats.setSkills(skillCache.stream()
					.map(Skill::getId)
					.collect(Collectors.toCollection(JSONArray::new))
			);
			skillCache = null;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Hero hero = (Hero) o;
		return SERIAL == hero.SERIAL && Objects.equals(id, hero.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(SERIAL, id);
	}

	@Override
	public Actor fork() {
		Hero clone = new Hero(account, id, stats.getRace());
		clone.stats = stats;
		clone.equipment = new JSONObject(equipment);
		clone.equipCache = equipCache;
		clone.skillCache = skillCache;
		clone.consumableCache = new TreeBag<>();
		clone.team = team;
		clone.game = game;

		return clone;
	}
}
