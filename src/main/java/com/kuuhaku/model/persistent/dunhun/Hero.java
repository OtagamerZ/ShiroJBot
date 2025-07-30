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
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.common.dunhun.Equipment;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.converter.JSONObjectConverter;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.dunhun.Attributes;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Graph;
import com.ygimenez.json.JSONArray;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.*;
import org.apache.commons.text.WordUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Entity
@Table(name = "hero", schema = "dunhun")
public class Hero extends Actor<Hero> {
	public static final long MAX_IMG_SIZE = 4 * 1024 * 1024;

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

	public Hero() {
	}

	public Hero(Account account, String name, Race race) {
		super(name);
		this.account = account;
		stats.setRace(race);
	}

	@Override
	public String getName(I18N locale) {
		return WordUtils.capitalizeFully(getId().replace("_", " "));
	}

	@Override
	public Race getRace() {
		return stats.getRace();
	}

	public boolean setImage(BufferedImage img) {
		File parent = new File(Constants.CARDS_ROOT + "../heroes");
		if (!parent.exists()) parent.mkdir();

		File f = new File(parent, getId() + ".png");
		img = Graph.scaleAndCenterImage(Graph.toColorSpace(img, BufferedImage.TYPE_INT_ARGB), 225, 350);
		Main.getCacheManager().getResourceCache().invalidate("H:" + getId());

		try {
			ImageIO.write(img, "png", f);
			return true;
		} catch (IOException e) {
			Constants.LOGGER.error(e, e);
			return false;
		}
	}

	@Override
	public int getMaxHp() {
		double flat = 280 + getModifiers().getMaxHp().get() + stats.getRaceBonus().hp() + stats.getLevel() * 20;

		return (int) Math.max(1, flat * getModifiers().getHpMult().get() * (1 + getAttributes().vit() * 0.1));
	}

	@Override
	public int getMaxAp() {
		return (int) Calc.clamp(2 + stats.getLevel() / 8d + getModifiers().getMaxAp().get(), 1, getApCap());
	}

	public int getApCap() {
		return (int) (5 + getModifiers().getMaxAp().get());
	}

	@Override
	public int getInitiative() {
		return stats.getLevel() / 3 + (int) getModifiers().getInitiative().get();
	}

	@Override
	public double getCritical() {
		double crit = getEquipment().getWeaponList().stream()
				.filter(Gear::isWeapon)
				.mapToDouble(Gear::getCritical)
				.average().orElse(0) + stats.getRaceBonus().critical();

		return Calc.clamp(crit * (1 + getModifiers().getCritical().get()), 0, 100);
	}

	@Override
	public int getAggroScore() {
		int flat = getSenshi().getDmg() / 10 + getSenshi().getDfs() / 20 + getHp() / 200;
		return (int) Math.max(1, flat * (1 + getModifiers().getAggroMult().get()) * stats.getLevel() / 2);
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

	public JSONObject getEquipmentRefs() {
		return equipment;
	}

	public int getInventoryCapacity() {
		return 50;
	}

	public List<Gear> getInventory() {
		List<Integer> ids = DAO.queryAllNative(Integer.class, """
				SELECT g.id
				FROM gear g
				INNER JOIN hero h ON h.id = g.owner_id
				WHERE h.id = ?1
				  AND NOT jsonb_path_exists(h.equipment, '$.* ? (@ == $val)', cast('{"val": ' || g.id || '}' AS JSONB))
				""", getId());

		return DAO.queryAll(Gear.class, "SELECT g FROM Gear g WHERE g.id IN ?1 ORDER BY g.id DESC", ids);
	}

	public Gear getInvGear(int id) {
		return DAO.query(Gear.class, "SELECT g FROM Gear g WHERE g.id = ?1 AND g.owner.id = ?2", id, getId());
	}

	public List<Skill> getAllSkills() {
		return DAO.queryAll(Skill.class, """
						SELECT s
						FROM Skill s
						WHERE s.requirements.attributes.attributes != -1
						ORDER BY s.id
						""").stream()
				.sorted(Comparator
						.<Skill>comparingInt(s -> s.getRequirements().attributes().count())
						.thenComparing(Skill::getId)
				)
				.toList();
	}

	public int getConsumableCount(String id) {
		return stats.getConsumables().getInt(id.toUpperCase());
	}

	public void setConsumableCount(String id, int count) {
		if (count <= 0) {
			stats.getConsumables().remove(id.toUpperCase());
		} else {
			stats.getConsumables().put(id.toUpperCase(), count);
		}
	}

	public void modConsumableCount(Consumable item, int amount) {
		modConsumableCount(item.getId(), amount);
	}

	public void modConsumableCount(String id, int amount) {
		int total = getConsumableCount(id) + amount;
		setConsumableCount(id, total);
	}

	public TreeSet<Consumable> getConsumables() {
		return stats.getConsumables().entrySet().parallelStream()
				.map(e -> {
					Consumable item = DAO.find(Consumable.class, e.getKey());
					if (item != null) {
						item.add(((Number) e.getValue()).intValue());
					}

					return item;
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Consumable::getId))));
	}

	@Override
	public void beforeSave() {
		Equipment equipCache = getCache().peekEquipment();
		if (equipCache != null) {
			equipment = new JSONObject(equipCache.toString());
		}

		List<Skill> skillCache = getCache().peekSkills();
		if (skillCache != null) {
			stats.setSkills(skillCache.stream()
					.filter(Objects::nonNull)
					.map(Skill::getId)
					.collect(Collectors.toCollection(JSONArray::new))
			);
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
				""", getId());

		DAO.applyNative(GearAffix.class, "DELETE FROM gear_affix WHERE gear_id IN ?1", ids);
		DAO.applyNative(Gear.class, "DELETE FROM gear WHERE id IN ?1", ids);
	}

	@Override
	public Actor<?> fork() {
		Hero clone = new Hero(account, getId(), stats.getRace());
		clone.stats = stats;
		clone.equipment = new JSONObject(equipment);
		clone.getModifiers().copyFrom(getModifiers());
		clone.getBinding().bind(getBinding());
		clone.setHp(getHp());
		clone.setAp(getAp());

		return clone;
	}
}
