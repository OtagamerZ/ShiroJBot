/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.controller;

import com.kuuhaku.interfaces.AutoMake;
import com.kuuhaku.interfaces.Blacklistable;
import com.kuuhaku.interfaces.DAOListener;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.*;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class DAO<T extends DAO<T>> implements DAOListener {
	@SuppressWarnings("unchecked")
	public static <T extends DAO<T>, ID> T find(@NotNull Class<T> klass, @NotNull ID id) {
		EntityManager em = Manager.getEntityManager();

		try {
			Map<String, Object> ids = new HashMap<>();
			for (Field f : klass.getDeclaredFields()) {
				if (f.isAnnotationPresent(Id.class)) {
					ids.put(f.getName(), id);
					break;
				} else if (f.isAnnotationPresent(EmbeddedId.class)) {
					for (Field ef : f.getType().getDeclaredFields()) {
						if (ef.isAnnotationPresent(Column.class)) {
							ef.setAccessible(true);
							ids.put(ef.getName(), ef.get(id));
							break;
						}
					}
				}
			}

			if (ids.isEmpty()) {
				throw new NoSuchFieldException("Class' ID not found");
			}

			T t = em.find(klass, id);
			if (t == null && AutoMake.class.isAssignableFrom(klass)) {
				t = (T) ((AutoMake<?>) klass.getConstructor().newInstance()).make(new JSONObject(ids));
				t.save();
			}

			return t;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static <T extends DAO<T>> T query(@NotNull Class<T> klass, @NotNull @Language("JPAQL") String query, @NotNull Object... params) {
		EntityManager em = Manager.getEntityManager();

		TypedQuery<T> q = em.createQuery(query, klass);
		q.setMaxResults(1);

		int paramSize = Objects.requireNonNull(params).length;
		for (int i = 0; i < paramSize; i++) {
			q.setParameter(i + 1, params[i]);
		}

		T t;
		try {
			t = q.getSingleResult();
			if (t instanceof Blacklistable lock) {
				if (lock.isBlacklisted()) {
					t = null;
				}
			}
		} catch (NoResultException e) {
			t = null;
		}

		return t;
	}

	public static <T> T queryNative(@NotNull Class<T> klass, @NotNull @Language("PostgreSQL") String query, @NotNull Object... params) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery(query);
		q.setMaxResults(1);

		int paramSize = Objects.requireNonNull(params).length;
		for (int i = 0; i < paramSize; i++) {
			q.setParameter(i + 1, params[i]);
		}

		T t;
		try {
			if (Number.class.isAssignableFrom(klass)) {
				t = klass.cast(Utils.fromNumber(klass, (Number) q.getSingleResult()));
			} else {
				t = klass.cast(q.getSingleResult());
				if (t instanceof Blacklistable lock) {
					if (lock.isBlacklisted()) {
						t = null;
					}
				}
			}
		} catch (NoResultException e) {
			t = null;
		}

		return t;
	}

	public static Object[] queryUnmapped(@NotNull @Language("PostgreSQL") String query, @NotNull Object... params) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery(query);
		q.setMaxResults(1);

		int paramSize = Objects.requireNonNull(params).length;
		for (int i = 0; i < paramSize; i++) {
			q.setParameter(i + 1, params[i]);
		}

		try {
			Object obj = q.getSingleResult();
			if (obj.getClass().isArray()) {
				return (Object[]) obj;
			} else {
				return new Object[]{obj};
			}
		} catch (NoResultException e) {
			return null;
		}
	}

	public static <T extends DAO<T>> List<T> findAll(@NotNull Class<T> klass) {
		EntityManager em = Manager.getEntityManager();

		TypedQuery<T> q = em.createQuery("SELECT o FROM " + klass.getSimpleName() + " o", klass);

		if (klass.isInstance(Blacklistable.class)) {
			return q.getResultStream()
					.filter(o -> !((Blacklistable) o).isBlacklisted())
					.toList();
		} else {
			return q.getResultList();
		}
	}

	public static <T extends DAO<T>> List<T> queryAll(@NotNull Class<T> klass, @NotNull @Language("JPAQL") String query, @NotNull Object... params) {
		EntityManager em = Manager.getEntityManager();

		TypedQuery<T> q = em.createQuery(query, klass);

		int paramSize = Objects.requireNonNull(params).length;
		for (int i = 0; i < paramSize; i++) {
			q.setParameter(i + 1, params[i]);
		}

		if (klass.isInstance(Blacklistable.class)) {
			return q.getResultStream()
					.filter(o -> !((Blacklistable) o).isBlacklisted())
					.toList();
		} else {
			return q.getResultList();
		}
	}

	public static <T> List<T> queryAllNative(@NotNull Class<T> klass, @NotNull @Language("PostgreSQL") String query, @NotNull Object... params) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery(query);

		int paramSize = Objects.requireNonNull(params).length;
		for (int i = 0; i < paramSize; i++) {
			q.setParameter(i + 1, params[i]);
		}

		if (klass.isInstance(Blacklistable.class)) {
			return ((Stream<?>) q.getResultStream())
					.map(klass::cast)
					.filter(o -> !((Blacklistable) o).isBlacklisted())
					.toList();
		} else if (Number.class.isAssignableFrom(klass)) {
			return ((Stream<?>) q.getResultStream())
					.map(o -> klass.cast(Utils.fromNumber(klass, (Number) o)))
					.toList();
		} else {
			return ((Stream<?>) q.getResultStream())
					.map(klass::cast)
					.toList();
		}
	}

	public static List<Object[]> queryAllUnmapped(@NotNull @Language("PostgreSQL") String query, @NotNull Object... params) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery(query);

		int paramSize = Objects.requireNonNull(params).length;
		for (int i = 0; i < paramSize; i++) {
			q.setParameter(i + 1, params[i]);
		}

		return ((Stream<?>) q.getResultStream())
				.map(o -> {
					if (o.getClass().isArray()) {
						return (Object[]) o;
					} else {
						return new Object[]{o};
					}
				}).toList();
	}

	public static <T extends DAO<?>, ID> void apply(@NotNull Class<T> klass, @NotNull ID id, @NotNull Consumer<T> consumer) {
		EntityManager em = Manager.getEntityManager();

		T obj = em.find(klass, id);
		if (obj == null) return;
		else if (obj instanceof Blacklistable lock) {
			if (lock.isBlacklisted()) return;
		}

		transaction(em, () -> {
			consumer.accept(obj);
			em.flush();
		});
	}

	public static void apply(@NotNull @Language("JPAQL") String query, @NotNull Object... params) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery(query);

		int paramSize = Objects.requireNonNull(params).length;
		for (int i = 0; i < paramSize; i++) {
			q.setParameter(i + 1, params[i]);
		}

		transaction(em, q::executeUpdate);
	}

	public static void applyNative(@NotNull @Language("PostgreSQL") String query, @NotNull Object... params) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery(query);

		int paramSize = Objects.requireNonNull(params).length;
		for (int i = 0; i < paramSize; i++) {
			q.setParameter(i + 1, params[i]);
		}

		transaction(em, q::executeUpdate);
	}

	public static <T extends DAO<T>> List<T> queryBuilder(@NotNull Class<T> klass, @NotNull @Language("JPAQL") String query, Function<TypedQuery<T>, List<T>> processor, @NotNull Object... params) {
		EntityManager em = Manager.getEntityManager();

		TypedQuery<T> q = em.createQuery(query, klass);

		int paramSize = Objects.requireNonNull(params).length;
		for (int i = 0; i < paramSize; i++) {
			q.setParameter(i + 1, params[i]);
		}

		if (klass.isInstance(Blacklistable.class)) {
			return processor.apply(q).stream()
					.filter(o -> !((Blacklistable) o).isBlacklisted())
					.toList();
		} else {
			return processor.apply(q);
		}
	}

	public static <T> List<T> nativeQueryBuilder(@NotNull Class<T> klass, @NotNull @Language("PostgreSQL") String query, Function<Query, List<T>> processor, @NotNull Object... params) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery(query);

		int paramSize = Objects.requireNonNull(params).length;
		for (int i = 0; i < paramSize; i++) {
			q.setParameter(i + 1, params[i]);
		}

		if (klass.isInstance(Blacklistable.class)) {
			return processor.apply(q).stream()
					.map(klass::cast)
					.filter(o -> !((Blacklistable) o).isBlacklisted())
					.toList();
		} else if (Number.class.isAssignableFrom(klass)) {
			return processor.apply(q).stream()
					.map(o -> klass.cast(Utils.fromNumber(klass, (Number) o)))
					.toList();
		} else {
			return processor.apply(q).stream()
					.map(klass::cast)
					.toList();
		}
	}

	public static List<Object[]> unmappedQueryBuilder(@NotNull @Language("PostgreSQL") String query, Function<Query, List<Object>> processor, @NotNull Object... params) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createNativeQuery(query);

		int paramSize = Objects.requireNonNull(params).length;
		for (int i = 0; i < paramSize; i++) {
			q.setParameter(i + 1, params[i]);
		}

		return processor.apply(q).stream()
				.map(o -> {
					if (o.getClass().isArray()) {
						return (Object[]) o;
					} else {
						return new Object[]{o};
					}
				}).toList();
	}

	public final void save() {
		EntityManager em = Manager.getEntityManager();

		beforeSave();
		try {
			if (this instanceof Blacklistable lock) {
				if (lock.isBlacklisted()) return;
			}

			transaction(em, () -> {
				if (!em.contains(this)) em.merge(this);
				else em.flush();
			});
		} finally {
			afterSave();
		}
	}

	@SuppressWarnings("unchecked")
	public final T refresh() {
		EntityManager em = Manager.getEntityManager();

		try {
			beforeRefresh();
			if (em.contains(this)) {
				em.refresh(this);
				return (T) this;
			} else {
				Object key = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(this);
				return (T) Utils.getOr(em.find(getClass(), key), this);
			}
		} finally {
			afterRefresh();
		}
	}

	public final void delete() {
		EntityManager em = Manager.getEntityManager();

		beforeDelete();
		try {
			DAO<?> ent;
			if (em.contains(this)) {
				ent = this;
			} else {
				Object key = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(this);
				ent = em.find(getClass(), key);

				if (ent == null) {
					throw new EntityNotFoundException("Could not delete entity of class " + getClass().getSimpleName() + " [" + key + "]");
				}
			}

			transaction(em, () -> em.remove(ent));
		} finally {
			afterDelete();
		}
	}

	private static void transaction(EntityManager em, Runnable op) {
		EntityTransaction tx = em.getTransaction();
		boolean transOpen = tx.isActive();

		if (transOpen) {
			op.run();
		} else {
			try {
				tx.begin();
				op.run();
				tx.commit();
			} finally {
				if (tx.isActive()) {
					tx.rollback();
				}
			}
		}
	}
}
