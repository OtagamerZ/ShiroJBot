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

package com.kuuhaku.controller.postgresql;

import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Class;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.AddedAnime;
import com.kuuhaku.model.persistent.Card;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CardDAO {
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

		try {
			return ((Number) q.getSingleResult()).intValue() == totalCards(anime);
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

		Query q;
		if (withUltimate) {
			q = em.createQuery("SELECT c FROM Card c WHERE id = UPPER(:name) AND rarity NOT IN :blacklist AND anime.name IN :animes", Card.class);
			q.setParameter("blacklist", Set.of(KawaiponRarity.EQUIPMENT, KawaiponRarity.FUSION, KawaiponRarity.FIELD));
		} else {
			q = em.createQuery("SELECT c FROM Card c WHERE id = UPPER(:name) AND rarity NOT IN :blacklist AND anime.name IN :animes", Card.class);
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
	public static List<String> getAllChampionNames() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c.card.id FROM Champion c", String.class);

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
			q = em.createQuery("SELECT c FROM Champion c", Champion.class);
		else
			q = em.createQuery("SELECT c FROM Champion c WHERE c.fusion = FALSE", Champion.class);

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
	public static List<Card> getAllCardsByAnime(String anime) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Card c WHERE anime.name = :anime", Card.class);
		q.setParameter("anime", anime);

		try {
			return q.getResultList();
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

	public static long totalCards() {
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

	public static long totalCards(String anime) {
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

	public static long totalCards(KawaiponRarity rarity) {
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

		Query q = em.createQuery("SELECT c FROM Champion c WHERE c.fusion = TRUE", Champion.class);

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Champion> getChampions(List<String> ids) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Champion c WHERE card.id IN :ids", Champion.class);
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

		Query q = em.createQuery("SELECT c FROM Champion c WHERE c.category = :class", Champion.class);
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

		Query q = em.createQuery("SELECT c FROM Champion c WHERE c.race = :race", Champion.class);
		q.setParameter("race", r);

		try {
			return q.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		} finally {
			em.close();
		}
	}

	public static Champion getChampion(Card c) {
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

	public static Champion getChampion(String name) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Champion c WHERE card.id = UPPER(:card)", Champion.class);
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

		Query q = em.createQuery("SELECT c FROM Champion c WHERE c.card.anime.name IN :animes ORDER BY RANDOM()", Champion.class);
		q.setParameter("animes", getValidAnimeNames());
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

		Query q = em.createQuery("SELECT c FROM Champion c WHERE c.card.anime.name IN :animes AND c.mana = :mana ORDER BY RANDOM()", Champion.class);
		q.setParameter("animes", getValidAnimeNames());
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

	@SuppressWarnings("unchecked")
	public static Champion getFakeChampion() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM Champion c WHERE c.card.anime.name IN :animes ORDER BY RANDOM()", Champion.class);
		q.setParameter("animes", getValidAnimeNames());
		q.setMaxResults(4);

		List<Champion> champs = q.getResultList();
		Champion c = new Champion(
				champs.get(0).getCard(),
				champs.get(1).getRace(),
				champs.get(2).getMana(),
				champs.get(2).getAtk(),
				champs.get(2).getDef(),
				champs.get(3).getDescription(),
				champs.get(3).getLiteralEffect()
		);

		try {
			return c;
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

	public static double getMeta(Class cat) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery("""
				SELECT SUM(x.total) / COUNT(1) * 0.75
				FROM (
						SELECT COUNT(1) AS total
						     , c.category
						FROM kawaipon_champion kc
						         INNER JOIN champion c on c.id = kc.champions_id
						GROUP BY kc.kawaipon_id, c.category
					) x
				WHERE x.category = :cat
				""");
		q.setParameter("cat", cat.name());

		try {
			return ((Number) q.getSingleResult()).doubleValue();
		} finally {
			em.close();
		}
	}

	public static CardType identifyType(String name) {
		if (getField(name) != null) return CardType.FIELD;
		else if (getEquipment(name) != null) return CardType.EVOGEAR;
		else return CardType.KAWAIPON;
	}
}
