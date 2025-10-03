package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.persistent.dunhun.Gear;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.dunhun.Skill;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.FrameSkin;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.persistent.user.Account;
import com.ygimenez.json.JSONArray;
import com.ygimenez.json.JSONObject;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ActorCache {
	private final Actor<?> actor;

	private Equipment equipment;
	private List<Skill> skills;
	private Deck deck;
	private Senshi senshi;

	public ActorCache(Actor<?> actor) {
		this.actor = actor;
	}

	public Equipment peekEquipment() {
		return equipment;
	}

	public Equipment getEquipment() {
		if (equipment == null) {
			if (actor instanceof Hero h) {
				JSONObject refs = h.getEquipmentRefs();

				List<Integer> ids = new ArrayList<>();
				for (Object o : refs.values()) {
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
						.filter(g -> h.getStats().getLevel() >= g.getBasetype().getStats().requirements().level())
						.collect(Collectors.toMap(Gear::getId, Function.identity()));

				equipment = new Equipment((gs, i) -> {
					if (i < 0) {
						return gear.get(refs.getInt(gs.name()));
					}

					return gear.get(refs.getJSONArray(gs.name()).getInt(i));
				});
			} else {
				equipment = new Equipment();
			}
		}

		return equipment;
	}

	public void setEquipment(Equipment equipment) {
		this.equipment = equipment;
	}

	public List<Skill> peekSkills() {
		return skills;
	}

	public List<Skill> getSkills() {
		if (skills == null) {
			JSONArray ids;
			if (actor instanceof Hero h) {
				ids = h.getStats().getSkills();
			} else if (actor instanceof MonsterBase<?> m) {
				ids = m.getStats().getSkills();
			} else {
				ids = new JSONArray();
			}

			skills = DAO.queryAll(Skill.class, "SELECT s FROM Skill s WHERE s.id IN ?1", ids).stream()
					.sorted(Comparator.comparingInt(s -> ids.indexOf(s.getId())))
					.collect(ArrayList::new, List::add, List::addAll);
		}

		return skills;
	}

	public void setSkills(List<Skill> skills) {
		this.skills = skills;
	}

	public Deck getDeck() {
		if (deck == null) {
			if (actor instanceof Hero h) {
				Account acc = h.getAccount();

				deck = new Deck(acc);
				deck.getStyling().setFrame(acc.getDeck().getFrame());
			} else {
				deck = new Deck();
				deck.getStyling().setFrame(DAO.find(FrameSkin.class, "GLITCH"));
			}
		}

		return deck;
	}

	public Senshi getSenshi() {
		if (senshi == null) {
			actor.createSenshi();
		}

		return senshi;
	}

	public void setSenshi(Senshi senshi) {
		this.senshi = senshi;
	}

	public void reset() {
		equipment = null;
		skills = null;
		deck = null;
		senshi = null;
	}
}
