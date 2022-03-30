package com.kuuhaku.controller.postgresql;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Evogear;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Class;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.persistent.Deck;
import com.kuuhaku.model.records.MetaData;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DrawableDAO extends DAO {
	public static List<Champion> getChampions() {
		return queryAll(Champion.class, "SELECT c FROM Champion c");
	}

	public static List<Champion> getChampions(boolean fusion) {
		return queryAll(Champion.class, "SELECT c FROM Champion c WHERE c.fusion = :fusion", fusion);
	}

	public static List<Champion> getChampions(List<String> ids) {
		return queryAll(Champion.class, "SELECT c FROM Champion c WHERE c.card.id IN :idsn",ids);
	}

	public static Champion getChampion(String card) {
		return query(Champion.class, "SELECT c FROM Champion c WHERE c.card.id = UPPER(:card)", card);
	}

	public static Champion getRandomChampion() {
		return queryNative(Champion.class, "SELECT c FROM Champion c ORDER BY RANDOM() LIMIT 1");
	}

	public static Champion getRandomChampion(boolean fusion) {
		return queryNative(Champion.class, "SELECT c FROM Champion c WHERE c.fusion = :fusion ORDER BY RANDOM() LIMIT 1", fusion);
	}

	public static List<Evogear> getEvogears() {
		return queryAll(Evogear.class, "SELECT e FROM Evogear e");
	}

	public static List<Evogear> getEvogears(boolean effOnly) {
		return queryAll(Evogear.class, "SELECT e FROM Evogear e WHERE e.effectOnly = :effOnly", effOnly);
	}

	public static List<Evogear> getEvogears(List<String> ids) {
		return queryAll(Evogear.class, "SELECT e FROM Evogear e WHERE e.card.id IN :ids", ids);
	}

	public static Evogear getEvogear(String card) {
		return query(Evogear.class, "SELECT e FROM Evogear e WHERE e.card.id = UPPER(:card)", card);
	}

	public static Evogear getRandomEvogear() {
		return queryNative(Evogear.class, "SELECT e FROM Evogear e ORDER BY RANDOM() LIMIT 1");
	}

	public static Evogear getRandomEvogear(boolean effOnly) {
		return queryNative(Evogear.class, "SELECT e FROM Evogear e WHERE e.effectOnly = :effOnly ORDER BY RANDOM() LIMIT 1", effOnly);
	}

	public static List<Field> getFields() {
		return queryAll(Field.class, "SELECT f FROM Field f");
	}

	public static List<Field> getFields(boolean effOnly) {
		return queryAll(Field.class, "SELECT f FROM Field f WHERE f.effectOnly = :effOnly", effOnly);
	}

	public static List<Field> getFields(List<String> ids) {
		return queryAll(Field.class, "SELECT f FROM Field f WHERE f.card.id IN :ids", ids);
	}

	public static Field getField(String card) {
		return query(Field.class, "SELECT f FROM Field f WHERE f.card.id = UPPER(:card)", card);
	}

	public static Field getRandomField() {
		return queryNative(Field.class, "SELECT f FROM Field f ORDER BY RANDOM() LIMIT 1");
	}

	public static Field getRandomField(boolean effOnly) {
		return queryNative(Field.class, "SELECT f FROM Field f WHERE f.effectOnly = :effOnly ORDER BY RANDOM() LIMIT 1", effOnly);
	}

	public static List<Drawable> getDrawables(List<String> ids) {
		List<Champion> c = getChampions(ids);
		List<Evogear> e = getEvogears(ids);
		List<Field> f = getFields(ids);

		return Stream.of(c, e, f)
				.flatMap(List::stream)
				.sorted(Comparator.comparing(d -> ids.indexOf(d.getCard().getId())))
				.collect(Collectors.toList());
	}

	public static Map<Class, Integer> getCategoryMeta() {
		Map<Class, Integer> meta = new HashMap<>();
		List<MetaData> data = queryAllNative(MetaData.class, "SELECT md.id, md.average FROM \"GetCategoryMeta\" md");
		for (MetaData md : data) {
			meta.put(Class.valueOf(md.id()), md.average());
		}
		
		return meta;
	}

	public static List<String> getChampionMeta() {
		List<String> meta = new ArrayList<>();
		List<MetaData> data = queryAllNative(MetaData.class, "SELECT md.id, md.average FROM \"GetChampionMeta\" md");
		for (MetaData md : data) {
			meta.addAll(Collections.nCopies(md.average(), md.id()));
		}

		return meta;
	}

	public static List<String> getEquipmentMeta() {
		List<String> meta = new ArrayList<>();
		List<MetaData> data = queryAllNative(MetaData.class, "SELECT md.id, md.average FROM \"GetEvogearMeta\" md");
		for (MetaData md : data) {
			meta.addAll(Collections.nCopies(md.average(), md.id()));
		}

		return meta;
	}

	public static List<String> getFieldMeta() {
		List<String> meta = new ArrayList<>();
		List<MetaData> data = queryAllNative(MetaData.class, "SELECT md.id, md.average FROM \"GetFieldMeta\" md");
		for (MetaData md : data) {
			meta.addAll(Collections.nCopies(md.average(), md.id()));
		}

		return meta;
	}

	public static Deck getMetaDeck() {
		EntityManager em = Manager.getEntityManager();

		List<String> champs = getChampionMeta();
		List<String> evos = getEquipmentMeta();
		List<String> fields = getFieldMeta();

		try {
			return new Deck(
					champs.stream()
							.map(Champion::getChampion)
							.collect(Collectors.toList()),
					evos.stream()
							.map(Evogear::getEvogear)
							.collect(Collectors.toList()),
					fields.stream()
							.map(Field::getField)
							.collect(Collectors.toList())
			);
		} finally {
			em.close();
		}
	}
}
