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

package com.kuuhaku.controller.postgresql;

import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Class;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.AddedAnime;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.Deck;
import com.kuuhaku.model.records.CompletionState;
import com.kuuhaku.model.records.MetaData;
import com.kuuhaku.utils.Helper;
import org.apache.commons.lang3.tuple.Pair;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CardDAO {
	public static void setCardName(String o, String n) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.createQuery("UPDATE Card SET id = :new WHERE id = :old")
				.setParameter("old", o)
				.setParameter("new", n)
				.executeUpdate();
		em.getTransaction().commit();

		em.close();
	}

	public static void setCardName(String o, String n, String name) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.createQuery("UPDATE Card SET id = :new, name = :name WHERE id = :old")
				.setParameter("old", o)
				.setParameter("new", n)
				.setParameter("name", name)
				.executeUpdate();
		em.getTransaction().commit();

		em.close();
	}

	public static long getCardCount() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT COUNT(c.id) FROM Card c");
		q.setMaxResults(1);

		try {
			return (long) q.getSingleResult();
		} catch (NoResultException e) {
			return 0;
		} finally {
			em.close();
		}
	}

	public static boolean checkHash(String hash) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT 1 FROM KawaiponCard kc WHERE kc.hash = :hash");
		q.setParameter("hash", hash);

		try {
			return q.getSingleResult() != null;
		} catch (NoResultException e) {
			return false;
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static Set<AddedAnime> getValidAnime() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT a FROM AddedAnime a WHERE a.hidden = FALSE", AddedAnime.class);

		try {
			return Set.copyOf(q.getResultList());
		} catch (NoResultException e) {
			return new HashSet<>();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static Set<String> getValidAnimeNames() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT a.name FROM AddedAnime a WHERE a.hidden = FALSE");

		try {
			return Set.copyOf(q.getResultList());
		} catch (NoResultException e) {
			return new HashSet<>();
		} finally {
			em.close();
		}
	}

	public static AddedAnime verifyAnime(String name) {
		EntityManager em = Manager.getEntityManager();

		try {
			return em.find(AddedAnime.class, name);
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static Map<String, CompletionState> getCompletionState(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("SELECT x.name, x.all_normal, x.all_foil FROM \"GetCompletionState\"(:id) x");
		q.setParameter("id", id);

		try {
			List<Object[]> res = (List<Object[]>) q.getResultList();
			return res.stream()
					.collect(Collectors.toMap(
							o -> String.valueOf(o[0]),
							o -> new CompletionState((boolean) o[1], (boolean) o[2])
					));
		} finally {
			em.close();
		}
	}

	public static boolean hasCompleted(String id, String anime, boolean foil) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT COUNT(*)
				FROM Kawaipon kp
				JOIN kp.cards kc
				WHERE kc.card.anime.name = :anime
				AND kp.uid = :uid
				AND kc.foil = :foil
				""");
		q.setParameter("uid", id);
		q.setParameter("anime", anime);
		q.setParameter("foil", foil);

		long total = getTotalCards(anime);
		try {
			return total > 0 && ((Number) q.getSingleResult()).intValue() >= total;
		} catch (NoResultException e) {
			return false;
		} finally {
			em.close();
		}
	}

	public static Card getCard(String name) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Card c WHERE id = UPPER(:name) AND rarity NOT IN :blacklist", Card.class);
		q.setParameter("blacklist", Set.of(KawaiponRarity.EQUIPMENT, KawaiponRarity.FUSION, KawaiponRarity.FIELD, KawaiponRarity.ULTIMATE));
		q.setParameter("name", name);

		try {
			return (Card) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Card getCard(String name, boolean withUltimate) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT c
				FROM Card c
				WHERE id = UPPER(:name)
				AND rarity NOT IN :blacklist
				AND anime.name IN :animes
				""", Card.class);
		if (withUltimate) {
			q.setParameter("blacklist", Set.of(KawaiponRarity.EQUIPMENT, KawaiponRarity.FUSION, KawaiponRarity.FIELD));
		} else {
			q.setParameter("blacklist", Set.of(KawaiponRarity.EQUIPMENT, KawaiponRarity.FUSION, KawaiponRarity.FIELD, KawaiponRarity.ULTIMATE));
		}
		q.setParameter("name", name);
		q.setParameter("animes", getValidAnimeNames());

		try {
			return (Card) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Card getRawCard(String name) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Card c WHERE id = UPPER(:name)", Card.class);
		q.setParameter("name", name);

		try {
			return (Card) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Card getRandomCard() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Card c WHERE rarity NOT IN :blacklist ORDER BY RANDOM()", Card.class);
		q.setParameter("blacklist", Set.of(KawaiponRarity.EQUIPMENT, KawaiponRarity.FUSION, KawaiponRarity.FIELD, KawaiponRarity.ULTIMATE));
		q.setMaxResults(1);

		try {
			return (Card) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Card getUltimate(String anime) {
		EntityManager em = Manager.getEntityManager();

		try {
			return em.find(Card.class, anime);
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Card> getCards() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Card c WHERE rarity NOT IN :blacklist AND anime.name IN :animes", Card.class);
		q.setParameter("blacklist", Set.of(KawaiponRarity.EQUIPMENT, KawaiponRarity.FUSION, KawaiponRarity.FIELD, KawaiponRarity.ULTIMATE));
		q.setParameter("animes", getValidAnimeNames());

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Card> getCards(List<String> ids) {
		if (ids.isEmpty()) return new ArrayList<>();

		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Card c WHERE c.id IN :ids", Card.class);
		q.setParameter("ids", ids);

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<String> getAllCardNames() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c.id FROM Card c WHERE anime.name IN :animes AND rarity NOT IN :blacklist", String.class);
		q.setParameter("blacklist", Set.of(KawaiponRarity.EQUIPMENT, KawaiponRarity.FUSION, KawaiponRarity.FIELD, KawaiponRarity.ULTIMATE));
		q.setParameter("animes", getValidAnimeNames());

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Card> getAllCards() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Card c WHERE anime.name IN :animes AND rarity NOT IN :blacklist", Card.class);
		q.setParameter("blacklist", Set.of(KawaiponRarity.EQUIPMENT, KawaiponRarity.FUSION, KawaiponRarity.FIELD, KawaiponRarity.ULTIMATE));
		q.setParameter("animes", getValidAnimeNames());

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<String> getAllEquipmentNames() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT e.card.id FROM Equipment e", String.class);

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Equipment> getAllEquipments() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT e FROM Equipment e", Equipment.class);

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Equipment> getAllAvailableEquipments() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT e FROM Equipment e WHERE e.effectOnly = FALSE", Equipment.class);

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<String> getAllChampionNames() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c.card.id FROM Champion c WHERE (c.effect NOT LIKE '%//TODO%' OR c.effect IS NULL)", String.class);

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Champion> getAllChampions(boolean withFusion) {
		EntityManager em = Manager.getEntityManager();

		Query q;
		if (withFusion)
			q = em.createQuery("SELECT c FROM Champion c WHERE (c.effect NOT LIKE '%//TODO%' OR c.effect IS NULL)", Champion.class);
		else
			q = em.createQuery("SELECT c FROM Champion c WHERE c.fusion = FALSE AND (c.effect NOT LIKE '%//TODO%' OR c.effect IS NULL)", Champion.class);

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Champion> getAllChampionsWithEffect(boolean withFusion, int max) {
		EntityManager em = Manager.getEntityManager();

		Query q;
		if (withFusion)
			q = em.createQuery("SELECT c FROM Champion c WHERE c.mana <= :max AND c.effect NOT LIKE '%//TODO%' AND c.effect IS NOT NULL", Champion.class);
		else
			q = em.createQuery("SELECT c FROM Champion c WHERE c.mana <= :max AND c.fusion = FALSE AND c.effect NOT LIKE '%//TODO%' AND c.effect IS NOT NULL", Champion.class);

		q.setParameter("max", max);

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<String> getAllFieldNames() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT f.card.id FROM Field f", String.class);

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Field> getAllFields() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT f FROM Field f", Field.class);

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Field> getAllAvailableFields() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT f FROM Field f WHERE f.effectOnly = FALSE", Field.class);

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Card> getCardsByAnime(String anime) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Card c WHERE rarity NOT IN :blacklist AND anime.name = :anime", Card.class);
		q.setParameter("blacklist", Set.of(KawaiponRarity.EQUIPMENT, KawaiponRarity.FUSION, KawaiponRarity.FIELD, KawaiponRarity.ULTIMATE));
		q.setParameter("anime", anime);

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Boolean> getCardsByAnime(String id, String anime, boolean foil) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("SELECT x.name, x.has FROM \"GetAnimeCompletionState\"(:id, :anime, :foil) x");
		q.setParameter("id", id);
		q.setParameter("anime", anime);
		q.setParameter("foil", foil);

		try {
			List<Object[]> res = (List<Object[]>) q.getResultList();
			return res.stream()
					.collect(Collectors.toMap(o -> (String) o[0], o -> (Boolean) o[1]));
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Card> getCardsByRarity(KawaiponRarity rarity) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Card c WHERE rarity = :rarity AND anime.name IN :animes", Card.class);
		q.setParameter("rarity", rarity);
		q.setParameter("animes", getValidAnimeNames());

		try {
			return q.getResultList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Boolean> getCardsByRarity(String id, String rarity, boolean foil) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("SELECT x.name, x.has FROM \"GetRarityCompletionState\"(:id, :rarity, :foil) x");
		q.setParameter("id", id);
		q.setParameter("rarity", rarity);
		q.setParameter("foil", foil);

		try {
			List<Object[]> res = (List<Object[]>) q.getResultList();
			return res.stream()
					.collect(Collectors.toMap(o -> (String) o[0], o -> (Boolean) o[1]));
		} finally {
			em.close();
		}
	}

	public static long getTotalCards() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT COUNT(c) FROM Card c WHERE rarity NOT IN :blacklist AND anime.name IN :animes", Long.class);
		q.setParameter("blacklist", Set.of(KawaiponRarity.EQUIPMENT, KawaiponRarity.FUSION, KawaiponRarity.FIELD, KawaiponRarity.ULTIMATE));
		q.setParameter("animes", getValidAnimeNames());

		try {
			return (long) q.getSingleResult();
		} finally {
			em.close();
		}
	}

	public static long getTotalCards(String anime) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT COUNT(c) FROM Card c WHERE rarity NOT IN :blacklist AND anime.name = :anime", Long.class);
		q.setParameter("blacklist", Set.of(KawaiponRarity.EQUIPMENT, KawaiponRarity.FUSION, KawaiponRarity.FIELD, KawaiponRarity.ULTIMATE));
		q.setParameter("anime", anime);

		try {
			return (long) q.getSingleResult();
		} finally {
			em.close();
		}
	}

	public static double getCollectionProgress(String id, String anime, boolean foil) {
		EntityManager em = Manager.getEntityManager();

		Query q;
		if (foil)
			q = em.createNativeQuery("SELECT * FROM \"GetFoilCompletionState\"(:id, :anime)");
		else
			q = em.createNativeQuery("SELECT * FROM \"GetNormalCompletionState\"(:id, :anime)");
		q.setParameter("id", id);
		q.setParameter("anime", anime);

		try {
			return Helper.getOr(q.getSingleResult(), BigDecimal.valueOf(0)).setScale(2, RoundingMode.HALF_EVEN).doubleValue();
		} catch (NoResultException e) {
			return 0;
		} finally {
			em.close();
		}
	}

	public static long getTotalCards(KawaiponRarity rarity) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT COUNT(c) FROM Card c WHERE rarity = :rarity AND anime.name IN :animes", Long.class);
		q.setParameter("rarity", rarity);
		q.setParameter("animes", getValidAnimeNames());

		try {
			return (long) q.getSingleResult();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Champion> getFusions() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Champion c WHERE c.fusion = TRUE AND (c.effect NOT LIKE '%//TODO%' OR c.effect IS NULL)", Champion.class);

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Champion> getFusions(String card) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("""
				SELECT c
				FROM Champion c
				JOIN c.requiredCards req
				WHERE c.fusion = TRUE
				  AND (c.effect NOT LIKE '%//TODO%' OR c.effect IS NULL)
				  AND :card IN req
				""", Champion.class);

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	public static List<Drawable> getDrawables(List<String> ids) {
		List<Champion> c = getChampions(ids);
		List<Equipment> e = getEquipments(ids);
		List<Field> f = getFields(ids);

		return Stream.of(c, e, f)
				.flatMap(List::stream)
				.sorted(Comparator.comparing(d -> ids.indexOf(d.getCard().getId())))
				.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	public static List<Champion> getChampions(List<String> ids) {
		if (ids.isEmpty()) return new ArrayList<>();

		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Champion c WHERE card.id IN :ids AND (c.effect NOT LIKE '%//TODO%' OR c.effect IS NULL)", Champion.class);
		q.setParameter("ids", ids);

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Champion> getChampions(Class c) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Champion c WHERE c.category = :class AND (c.effect NOT LIKE '%//TODO%' OR c.effect IS NULL)", Champion.class);
		q.setParameter("class", c);

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Champion> getChampions(Race r) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Champion c WHERE c.race = :race AND (c.effect NOT LIKE '%//TODO%' OR c.effect IS NULL)", Champion.class);
		q.setParameter("race", r);

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	public static Champion peekChampion(String name) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Champion c WHERE c.card.id = UPPER(:card)", Champion.class);
		q.setParameter("card", name);

		try {
			return (Champion) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Champion peekChampion(Card c) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Champion c WHERE card = :card", Champion.class);
		q.setParameter("card", c);

		try {
			return (Champion) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Champion getChampion(int id) {
		EntityManager em = Manager.getEntityManager();

		try {
			return em.find(Champion.class, id);
		} finally {
			em.close();
		}
	}

	public static Champion getChampion(Card c) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Champion c WHERE card = :card AND (c.effect NOT LIKE '%//TODO%' OR c.effect IS NULL)", Champion.class);
		q.setParameter("card", c);

		try {
			return (Champion) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Champion getChampion(String name) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Champion c WHERE c.card.id = UPPER(:card) AND (c.effect NOT LIKE '%//TODO%' OR c.effect IS NULL)", Champion.class);
		q.setParameter("card", name);

		try {
			return (Champion) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Champion getRandomChampion() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Champion c WHERE (c.effect NOT LIKE '%//TODO%' OR c.effect IS NULL) ORDER BY RANDOM()", Champion.class);
		q.setMaxResults(1);

		try {
			return (Champion) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Champion getRandomChampion(boolean fusion) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Champion c WHERE c.fusion = :fusion AND (c.effect NOT LIKE '%//TODO%' OR c.effect IS NULL) ORDER BY RANDOM()", Champion.class);
		q.setParameter("fusion", fusion);
		q.setMaxResults(1);

		try {
			return (Champion) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Champion getRandomChampion(int mana) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Champion c WHERE c.mana = :mana AND (c.effect NOT LIKE '%//TODO%' OR c.effect IS NULL) ORDER BY RANDOM()", Champion.class);
		q.setParameter("mana", mana);
		q.setMaxResults(1);

		try {
			return (Champion) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Champion getRandomChampion(int mana, boolean fusion) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Champion c WHERE c.mana = :mana AND c.fusion = :fusion AND (c.effect NOT LIKE '%//TODO%' OR c.effect IS NULL) ORDER BY RANDOM()", Champion.class);
		q.setParameter("mana", mana);
		q.setParameter("fusion", fusion);
		q.setMaxResults(1);

		try {
			return (Champion) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Champion getRandomChampion(int mana, Race race) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Champion c WHERE c.mana = :mana AND c.race = :race AND (c.effect NOT LIKE '%//TODO%' OR c.effect IS NULL) ORDER BY RANDOM()", Champion.class);
		q.setParameter("mana", mana);
		q.setParameter("race", race);
		q.setMaxResults(1);

		try {
			return (Champion) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Champion getRandomChampion(int mana, Race race, boolean fusion) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Champion c WHERE c.mana = :mana AND c.race = :race AND c.fusion = :fusion AND (c.effect NOT LIKE '%//TODO%' OR c.effect IS NULL) ORDER BY RANDOM()", Champion.class);
		q.setParameter("mana", mana);
		q.setParameter("race", race);
		q.setParameter("fusion", fusion);
		q.setMaxResults(1);

		try {
			return (Champion) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static String[] getRandomEffect(int mana) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c.description, c.effect FROM Champion c WHERE c.fusion = FALSE AND c.effect IS NOT NULL AND c.mana = :mana AND (c.effect NOT LIKE '%//TODO%' OR c.effect IS NULL) ORDER BY RANDOM()");
		q.setParameter("mana", mana);
		q.setMaxResults(1);

		try {
			Object[] res = (Object[]) q.getSingleResult();
			return new String[]{(String) res[0], (String) res[1]};
		} catch (NoResultException e) {
			return new String[2];
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static Champion getFakeChampion() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Champion c WHERE c.card.anime.name IN :animes AND (c.effect NOT LIKE '%//TODO%' OR c.effect IS NULL) AND c.fusion = FALSE ORDER BY RANDOM()", Champion.class);
		q.setParameter("animes", getValidAnimeNames());
		q.setMaxResults(4);

		List<Champion> champs = q.getResultList();
		Champion c = new Champion(
				champs.get(0).getCard(),
				champs.get(1).getRace(),
				champs.get(1).getMana(),
				champs.get(2).getBlood(),
				champs.get(2).getAtk(),
				champs.get(2).getDef(),
				champs.get(3).getDescription(),
				champs.get(3).getRawEffect()
		);

		try {
			return c;
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Equipment> getEquipments(List<String> ids) {
		if (ids.isEmpty()) return new ArrayList<>();

		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT e FROM Equipment e WHERE card.id IN :ids", Equipment.class);
		q.setParameter("ids", ids);

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	public static Equipment getEquipment(Card c) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT e FROM Equipment e WHERE card = :card", Equipment.class);
		q.setParameter("card", c);

		try {
			return (Equipment) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Equipment getEquipment(String name) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT e FROM Equipment e WHERE card.id = UPPER(:card)", Equipment.class);
		q.setParameter("card", name);

		try {
			return (Equipment) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Equipment getRandomEquipment() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT e FROM Equipment e WHERE e.effectOnly = FALSE ORDER BY RANDOM()", Equipment.class);
		q.setMaxResults(1);

		try {
			return (Equipment) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Equipment getRandomEquipment(int tier) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT e FROM Equipment e WHERE e.tier = :tier AND e.effectOnly = FALSE ORDER BY RANDOM()", Equipment.class);
		q.setParameter("tier", tier);
		q.setMaxResults(1);

		try {
			return (Equipment) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Equipment getRandomEquipment(boolean spell) {
		EntityManager em = Manager.getEntityManager();

		Query q;
		if (spell)
			q = em.createQuery("SELECT e FROM Equipment e WHERE (e.charms LIKE '%SPELL%' OR e.charms LIKE '%CURSE%') AND e.effectOnly = FALSE ORDER BY RANDOM()", Equipment.class);
		else
			q = em.createQuery("SELECT e FROM Equipment e WHERE NOT (e.charms LIKE '%SPELL%' OR e.charms LIKE '%CURSE%') AND e.effectOnly = FALSE ORDER BY RANDOM()", Equipment.class);
		q.setMaxResults(1);

		try {
			return (Equipment) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Equipment getRandomEquipment(boolean spell, int tier) {
		EntityManager em = Manager.getEntityManager();

		Query q;
		if (spell)
			q = em.createQuery("SELECT e FROM Equipment e WHERE (e.charms LIKE '%SPELL%' OR e.charms LIKE '%CURSE%') AND e.tier = :tier AND e.effectOnly = FALSE ORDER BY RANDOM()", Equipment.class);
		else
			q = em.createQuery("SELECT e FROM Equipment e WHERE NOT (e.charms LIKE '%SPELL%' OR e.charms LIKE '%CURSE%') AND e.tier = :tier AND e.effectOnly = FALSE ORDER BY RANDOM()", Equipment.class);
		q.setParameter("tier", tier);
		q.setMaxResults(1);

		try {
			return (Equipment) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Field> getFields(List<String> ids) {
		if (ids.isEmpty()) return new ArrayList<>();

		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT f FROM Field f WHERE card.id IN :ids", Field.class);
		q.setParameter("ids", ids);

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	public static Field getField(Card c) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT f FROM Field f WHERE card = :card", Field.class);
		q.setParameter("card", c);

		try {
			return (Field) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Field getField(String name) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT f FROM Field f WHERE card.id = UPPER(:card)", Field.class);
		q.setParameter("card", name);

		try {
			return (Field) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	public static Field getRandomField() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT f FROM Field f WHERE f.effectOnly = false ORDER BY RANDOM()", Field.class);
		q.setMaxResults(1);

		try {
			return (Field) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static Map<Class, Integer> getCategoryMeta() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("SELECT * FROM \"GetCategoryMeta\"");

		try {
			List<MetaData> res = Helper.map(MetaData.class, q.getResultList());

			return res.stream()
					.map(md -> Pair.of(Class.valueOf(md.id()), md.average()))
					.collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<String> getChampionMeta() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("SELECT * FROM \"GetChampionMeta\"");

		try {
			List<MetaData> res = Helper.map(MetaData.class, q.getResultList());

			return res.stream()
					.map(md -> Collections.nCopies(md.average(), md.id()))
					.flatMap(List::stream)
					.toList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<String> getEquipmentMeta() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("SELECT * FROM \"GetEvogearMeta\"");

		try {
			List<MetaData> res = Helper.map(MetaData.class, q.getResultList());

			return res.stream()
					.map(md -> Collections.nCopies(md.average(), md.id()))
					.flatMap(List::stream)
					.toList();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<String> getFieldMeta() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("SELECT * FROM \"GetFieldMeta\"");

		try {
			List<MetaData> res = Helper.map(MetaData.class, q.getResultList());

			return res.stream()
					.map(md -> Collections.nCopies(md.average(), md.id()))
					.flatMap(List::stream)
					.toList();
		} finally {
			em.close();
		}
	}

	public static Deck getMetaDeck() {
		EntityManager em = Manager.getEntityManager();

		List<String> champs = getChampionMeta();
		List<String> evos = getEquipmentMeta();
		List<String> fields = getFieldMeta();

		try {
			return new Deck(
					champs.stream()
							.map(CardDAO::getChampion)
							.collect(Collectors.toList()),
					evos.stream()
							.map(CardDAO::getEquipment)
							.collect(Collectors.toList()),
					fields.stream()
							.map(CardDAO::getField)
							.collect(Collectors.toList())
			);
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static Set<String> getCollectedCardNames(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("SELECT kc.card_id FROM kawaiponcard kc WHERE kc.kawaipon_id = :id");
		q.setParameter("id", id);

		try {
			return Set.copyOf((List<String>) q.getResultList());
		} finally {
			em.close();
		}
	}

	public static CardType identifyType(String id) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("SELECT \"GetCardType\"(:id)");
		q.setParameter("id", id);

		try {
			return CardType.valueOf((String) q.getSingleResult());
		} finally {
			em.close();
		}
	}
}