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
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.Dunhun;
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.common.Delta;
import com.kuuhaku.model.common.dunhun.ActorModifiers;
import com.kuuhaku.model.common.dunhun.Equipment;
import com.kuuhaku.model.common.dunhun.SelfEffect;
import com.kuuhaku.model.common.shoukan.RegDeg;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.ContinueMode;
import com.kuuhaku.model.enums.dunhun.RarityClass;
import com.kuuhaku.model.enums.dunhun.Team;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.converter.JSONObjectConverter;
import com.kuuhaku.model.persistent.shoukan.CombatCardAttributes;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.dunhun.Attributes;
import com.kuuhaku.model.records.dunhun.CombatContext;
import com.kuuhaku.model.records.shoukan.Origin;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Graph;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONArray;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.*;
import kotlin.Pair;
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
	public static final long MAX_IMG_SIZE = 4 * 1024 * 1024;

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
	private transient final Map<Consumable, Integer> spentConsumables = new HashMap<>();
	private transient Equipment equipCache;
	private transient List<Skill> skillCache;
	private transient Senshi senshiCache;
	private transient Dunhun game;
	private transient I18N locale;
	private transient Deck deck;
	private transient Team team;
	private transient int ap;
	private transient int mindControl;
	private transient boolean flee;
	private transient ContinueMode contMode = ContinueMode.CONTINUE;

	public Hero() {
	}

	public Hero(Account account, String name, Race race) {
		this.id = name;
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
		File parent = new File(Constants.CARDS_ROOT + "../heroes");
		if (!parent.exists()) parent.mkdir();


		File f = new File(parent, id + ".png");
		img = Graph.scaleAndCenterImage(Graph.toColorSpace(img, BufferedImage.TYPE_INT_ARGB), 225, 350);
		Main.getCacheManager().getResourceCache().invalidate("H:" + id);

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
		int max = getMaxHp();

		if (hp.get() == null || hp.get() > max) hp.set(max);
		return hp.get();
	}

	@Override
	public int getMaxHp() {
		double flat = 300 + modifiers.getMaxHp().get() + stats.getLevel() * 20;

		return (int) Math.max(1, flat * modifiers.getHpMult().get() * (1 + getAttributes().vit() * 0.15));
	}

	@Override
	public int getHpDelta() {
		if (hp.previous() == null) return 0;

		return hp.get() - hp.previous();
	}

	@Override
	public void setHp(int value, boolean bypass) {
		hp.set(Calc.clamp(value, 0, getMaxHp()), bypass);
	}

	@Override
	public int getAp() {
		return ap;
	}

	public void mindControl(int time) {
		mindControl = Math.max(mindControl, time);
	}

	public boolean isMindControlled() {
		return mindControl > 0;
	}

	public void decMindControl() {
		if (mindControl > 0) mindControl--;
	}

	public void clearMindControl() {
		mindControl = 0;
	}

	@Override
	public int getMaxAp() {
		return (int) Calc.clamp(2 + stats.getLevel() / 8d + modifiers.getMaxAp().get(), 1, getApCap());
	}

	public int getApCap() {
		return (int) (5 + modifiers.getMaxAp().get());
	}

	@Override
	public void modAp(int value) {
		ap = Calc.clamp(ap + value, 0, getMaxAp());
	}

	@Override
	public int getInitiative() {
		return stats.getLevel() / 3 + (int) modifiers.getInitiative().get();
	}

	@Override
	public double getCritical() {
		double crit = getEquipment().getWeaponList().stream()
				.filter(Gear::isWeapon)
				.mapToDouble(Gear::getCritical)
				.average().orElse(0);

		return Calc.clamp(crit * (1 + modifiers.getCritical().get() + getAttributes().dex() / 20d), 0, 100);
	}

	@Override
	public int getAggroScore() {
		int flat = getSenshi().getDmg() / 10 + getSenshi().getDfs() / 20 + getHp() / 200;
		return (int) Math.max(1, flat * (1 + modifiers.getAggroMult().get()) * stats.getLevel() / 2);
	}

	@Override
	public boolean hasFleed() {
		return flee;
	}

	@Override
	public void setFleed(boolean flee) {
		this.flee = flee;

		if (game != null && flee) {
			game.getChannel().sendMessage(game.getLocale().get("str/actor_flee", getName()));
		}
	}

	@Override
	public RarityClass getRarityClass() {
		return RarityClass.UNIQUE;
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
		Attributes total = getStats().getAttributes();
		for (Gear g : getEquipment()) {
			total = total.merge(g.getAttributes(this));
		}

		return total;
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
				.filter(g -> stats.getLevel() >= g.getBasetype().getStats().reqLevel())
				.collect(Collectors.toMap(Gear::getId, Function.identity()));

		Equipment equip = new Equipment((gs, i) -> {
			if (i < 0) {
				return gear.get(equipment.getInt(gs.name()));
			}

			return gear.get(equipment.getJSONArray(gs.name()).getInt(i));
		});

		return equipCache = equip;
	}

	public JSONArray getWeaponTags() {
		JSONArray out = new JSONArray();
		Equipment equips = getEquipment();

		int weapons = 0;
		boolean unarmed = true;
		for (Gear g : equips.getWeapons().getEntries()) {
			if (g.isWeapon()) {
				if (!g.getTags().contains("UNARMED")) unarmed = false;
				weapons++;
			}

			out.addAll(g.getTags());
		}

		if (unarmed) {
			out.add("UNARMED");
		}

		if (weapons >= 2) {
			out.add("DUAL_WIELD");
		}

		return out;
	}

	public void setEquipment(Equipment equipment) {
		equipCache = equipment;
	}

	@Override
	public void trigger(Trigger trigger, Actor target) {
		for (Gear g : getEquipment()) {
			if (g.getEffects().isEmpty()) continue;

			for (SelfEffect e : g.getEffects()) {
				if (!Utils.equalsAny(trigger, e.getTriggers())) continue;

				try {
					e.lock();
					e.getEffect().accept(e, new CombatContext(trigger, this, target));
				} finally {
					e.unlock();
				}
			}
		}
	}

	@Override
	public void shiftInto(Actor a) {
		if (a == null) {
			senshiCache = null;
			skillCache = null;
		} else {
			senshiCache = a.getSenshi().copy();
			skillCache = a.getSkills().stream()
					.map(Skill::clone)
					.collect(Collectors.toCollection(ArrayList::new));
		}
	}

	public int getInventoryCapacity() {
		return 50 + getAttributes().str() * 5;
	}

	public List<Gear> getInventory() {
		List<Integer> ids = DAO.queryAllNative(Integer.class, """
				SELECT g.id
				FROM gear g
				INNER JOIN hero h ON h.id = g.owner_id
				WHERE h.id = ?1
				  AND NOT jsonb_path_exists(h.equipment, '$.* ? (@ == $val)', cast('{"val": ' || g.id || '}' AS JSONB))
				""", id);

		return DAO.queryAll(Gear.class, "SELECT g FROM Gear g WHERE g.id IN ?1 ORDER BY g.id DESC", ids);
	}

	public Gear getInvGear(int id) {
		return DAO.query(Gear.class, "SELECT g FROM Gear g WHERE g.id = ?1 AND g.owner.id = ?2", id, this.id);
	}

	@Override
	public List<Skill> getSkills() {
		if (skillCache != null) return skillCache;

		Attributes attrs = getAttributes();
		return skillCache = DAO.queryAll(Skill.class, "SELECT s FROM Skill s WHERE s.id IN ?1 OR s.reqRace = ?2", stats.getSkills(), getRace())
				.stream()
				.filter(s -> attrs.has(s.getRequirements()) && (
						(s.getReqRace() == null && (s.isFree() || getStats().getUnlockedSkills().contains(s.getId())))
						|| s.getReqRace() == getRace()
				))
				.sorted(Comparator.comparingInt(s -> stats.getSkills().indexOf(s.getId())))
				.collect(ArrayList::new, List::add, List::addAll);
	}

	public void setSkills(List<Skill> skills) {
		skillCache = skills;
	}

	public List<Skill> getAllSkills() {
		return DAO.queryAll(Skill.class, """
						SELECT s
						FROM Skill s
						WHERE s.reqRace IS NULL
						  AND s.requirements.attributes != -1
						ORDER BY s.id
						""").stream()
				.sorted(Comparator
						.<Skill>comparingInt(s -> s.getRequirements().count())
						.thenComparing(Skill::getId)
				)
				.toList();
	}

	public int getConsumableCount(String id) {
		return stats.getConsumables().getInt(id.toUpperCase());
	}

	public void addConsumable(Consumable item, int amount) {
		addConsumable(item.getId(), amount);
	}

	public void addConsumable(String id, int amount) {
		stats.getConsumables().compute(id.toUpperCase(), (k, v) -> {
			if (v == null) return amount;

			return ((Number) v).intValue() + amount;
		});
	}

	public void consume(Consumable item) {
		consume(item.getId());
	}

	public void consume(String id) {
		consume(id, 1);
	}

	public void consume(Consumable item, int amount) {
		consume(item.getId(), amount);
	}

	public void consume(String id, int amount) {
		if (amount <= 0) return;

		int rem = stats.getConsumables().getInt(id.toUpperCase());

		if (rem - amount == 0) {
			stats.getConsumables().remove(id.toUpperCase());
		} else {
			stats.getConsumables().put(id.toUpperCase(), rem - amount);
		}
	}

	public Map<Consumable, Integer> getConsumables() {
		return stats.getConsumables().entrySet().parallelStream()
				.filter(e -> ((Number) e.getValue()).intValue() > 0)
				.map(e -> new Pair<>(DAO.find(Consumable.class, e.getKey()), ((Number) e.getValue()).intValue()))
				.filter(p -> p.getFirst() != null)
				.map(p -> new Pair<>(p.getFirst(), p.getSecond() - spentConsumables.getOrDefault(p.getFirst(), 0)))
				.filter(p -> p.getSecond() > 0)
				.collect(Collectors.toMap(
						Pair::getFirst, Pair::getSecond, Integer::sum,
						() -> new TreeMap<>(Comparator.comparing(Consumable::getId))
				));
	}

	public Map<Consumable, Integer> getSpentConsumables() {
		return spentConsumables;
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
	public Dunhun getGame() {
		return game;
	}

	@Override
	public void setGame(Dunhun game) {
		this.game = game;
		this.locale = game.getLocale();
	}

	public I18N getLocale() {
		return locale;
	}

	public void setLocale(I18N locale) {
		this.locale = locale;
	}

	public ContinueMode getContMode() {
		return contMode;
	}

	public void setContMode(ContinueMode contMode) {
		this.contMode = contMode;
	}

	@Override
	public Senshi asSenshi(I18N locale) {
		return asSenshi(locale, true);
	}

	public Senshi asSenshi(I18N locale, boolean load) {
		if (senshiCache != null) return senshiCache;

		senshiCache = new Senshi(this, locale);
		CombatCardAttributes base = senshiCache.getBase();

		modifiers.clear(this);
		int dmg = 100;
		int def = 100;
		int wDmg = 0;

		Attributes total = getAttributes();
		Equipment equip = getEquipment();

		boolean check = true;
		while (check) {
			check = false;

			for (Gear g : equip) {
				if (!total.has(g.getBasetype().getStats().requirements())) {
					equip.unequip(g);
					total = total.reduce(g.getAttributes(this));

					check = true;
				}
			}
		}

		for (Gear g : equip) {
			if (g == null) continue;

			if (load) {
				g.load(locale, this);
			}

			if (!g.isWeapon()) {
				dmg += g.getDmg();
				def += g.getDfs();
			} else {
				double mult = 1;
				if (g.getTags().contains("LIGHT")) {
					mult *= 1 + total.dex() * 0.05f;
				}

				if (g.getTags().contains("HEAVY")) {
					mult *= 1 + total.str() * 0.05f;
				}

				if (g.getTags().contains("OFFHAND")) {
					dmg += (int) (g.getDmg() * mult);
				} else {
					int d = (int) (g.getDmg() * mult);
					if (wDmg > 0) {
						wDmg = (int) ((wDmg + d) * 0.6);
					} else {
						wDmg += d;
					}
				}

				def += (int) (g.getDfs() * mult);
			}
		}

		base.setAtk((int) ((dmg + wDmg) * (1 + (total.str() + total.dex()) * 0.01)));
		base.setDfs((int) (def * (1 + total.vit() * 0.1 + total.str() * 0.05)));

		int effCost = (int) Utils.regex(base.getEffect(), "%EFFECT%").results().count();
		base.setMana(1 + (base.getAtk() + base.getDfs()) / 750 + effCost);
		base.setSacrifices((base.getAtk() + base.getDfs()) / 3000);

		base.getTags().add("HERO");

		return senshiCache;
	}

	@Override
	public Senshi getSenshi() {
		if (senshiCache != null) return senshiCache;
		if (locale != null) return asSenshi(locale, false);
		return asSenshi(game.getLocale(), false);
	}

	@Override
	public BufferedImage render(I18N locale) {
		if (deck == null) {
			deck = account.getDeck();
			deck.setOrigin(Origin.from(false, Race.NONE));
		}

		setLocale(locale);
		return getSenshi().render(locale, deck);
	}

	@Override
	public void beforeSave() {
		if (equipCache != null) {
			equipment = new JSONObject(equipCache.toString());
			equipCache = null;
		}

		if (skillCache != null) {
			stats.setSkills(skillCache.stream()
					.filter(Objects::nonNull)
					.filter(s -> s.getReqRace() == null)
					.map(Skill::getId)
					.collect(Collectors.toCollection(JSONArray::new))
			);
			skillCache = null;
		}
	}

	@Override
	public void beforeDelete() {
		List<Integer> ids = DAO.queryAllNative(Integer.class, """
				SELECT g.id
				FROM gear g
				INNER JOIN hero h ON h.id = g.owner_id
				WHERE h.id = ?1
				ORDER BY g.id DESC
				""", id);

		DAO.applyNative(GearAffix.class, "DELETE FROM gear_affix WHERE gear_id IN ?1", ids);
		DAO.applyNative(Gear.class, "DELETE FROM gear WHERE id IN ?1", ids);
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
		clone.team = team;
		clone.game = game;

		return clone;
	}
}
