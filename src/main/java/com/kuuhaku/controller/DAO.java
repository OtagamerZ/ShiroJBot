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

import com.antkorwin.xsync.XSync;
import com.kuuhaku.Constants;
import com.kuuhaku.interfaces.AutoMake;
import com.kuuhaku.interfaces.Blacklistable;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.*;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.query.NativeQuery;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

@MappedSuperclass
public abstract class DAO<T extends DAO<T>> {
	private static final XSync<Object> MUTEX = new XSync<>();

	public static <T extends DAO<T>, ID> T find(@NotNull Class<T> klass, @NotNull ID id) {
		return MUTEX.evaluate(id, () -> Manager.getFactory().callInTransaction(em -> {
			try {
				Map<String, Object> ids = new HashMap<>();
				for (Field f : FieldUtils.getAllFields(klass)) {
					if (f.isAnnotationPresent(Id.class)) {
						ids.put(f.getName(), id);
						break;
					} else if (f.isAnnotationPresent(EmbeddedId.class)) {
						for (Field ef : f.getType().getDeclaredFields()) {
							if (ef.isAnnotationPresent(Column.class)) {
								ef.setAccessible(true);
								ids.put(f.getName() + "." + ef.getName(), ef.get(id));
								ef.setAccessible(false);
							}
						}
					}
				}

				if (ids.isEmpty()) {
					throw new NoSuchFieldException("Class' ID not found");
				}

				T t = em.find(klass, id);
				if (t == null && AutoMake.class.isAssignableFrom(klass)) {
					try {
						t = klass.cast(((AutoMake<?>) klass.getConstructor().newInstance()).make(new JSONObject(ids)));
						t.save();
					} catch (Exception e) {
						Constants.LOGGER.error(new JSONObject(ids));
						throw e;
					}
				}

				return t;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}));
	}

	@SuppressWarnings("SqlSourceToSinkFlow")
	public static <T extends DAO<T>> T query(@NotNull Class<T> klass, @NotNull @Language("JPAQL") String query, @NotNull Object... params) {
		return Manager.getFactory().callInTransaction(em -> {
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
		});
	}

	public static <T> T queryNative(@NotNull Class<T> klass, @NotNull @Language("PostgreSQL") String query, @NotNull Object... params) {
		return Manager.getFactory().callInTransaction(em -> {
			Query q = prepareQuery(query, em, null, true, params);

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
		});
	}

	public static Object[] queryUnmapped(@NotNull @Language("PostgreSQL") String query, @NotNull Object... params) {
		return Manager.getFactory().callInTransaction(em -> {
			Query q = prepareQuery(query, em, null, true, params);

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
		});
	}

	public static <T extends DAO<T>> List<T> findAll(@NotNull Class<T> klass) {
		return Manager.getFactory().callInTransaction(em -> {
			TypedQuery<T> q = em.createQuery("SELECT o FROM " + klass.getSimpleName() + " o", klass);

			if (klass.isInstance(Blacklistable.class)) {
				return q.getResultStream()
						.filter(o -> !((Blacklistable) o).isBlacklisted())
						.toList();
			} else {
				return q.getResultList();
			}
		});
	}

	@SuppressWarnings("SqlSourceToSinkFlow")
	public static <T extends DAO<T>> List<T> queryAll(@NotNull Class<T> klass, @NotNull @Language("JPAQL") String query, @NotNull Object... params) {
		return Manager.getFactory().callInTransaction(em -> {
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
		});
	}

	public static <T> List<T> queryAllNative(@NotNull Class<T> klass, @NotNull @Language("PostgreSQL") String query, @NotNull Object... params) {
		return Manager.getFactory().callInTransaction(em -> {
			Query q = prepareQuery(query, em, null, false, params);

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
		});
	}

	public static List<Object[]> queryAllUnmapped(@NotNull @Language("PostgreSQL") String query, @NotNull Object... params) {
		return Manager.getFactory().callInTransaction(em -> {
			Query q = prepareQuery(query, em, null, false, params);

			return ((Stream<?>) q.getResultStream())
					.map(o -> {
						if (o.getClass().isArray()) {
							return (Object[]) o;
						} else {
							return new Object[]{o};
						}
					}).toList();
		});
	}

	public static <T extends DAO<?>, ID> void apply(@NotNull Class<T> klass, @NotNull ID id, @NotNull Consumer<T> consumer) {
		Manager.getFactory().runInTransaction(em -> {
			T obj = em.find(klass, id);
			if (obj == null) return;
			else if (obj instanceof Blacklistable lock) {
				if (lock.isBlacklisted()) return;
			}

			consumer.accept(obj);
			em.merge(obj);
		});
	}

	@SuppressWarnings("SqlSourceToSinkFlow")
	public static void apply(@NotNull @Language("JPAQL") String query, @NotNull Object... params) {
		Manager.getFactory().runInTransaction(em -> {
			Query q = em.createQuery(query);

			int paramSize = Objects.requireNonNull(params).length;
			for (int i = 0; i < paramSize; i++) {
				q.setParameter(i + 1, params[i]);
			}

			q.executeUpdate();
		});
	}

	public static <T extends DAO<T>> void applyNative(Class<T> klass, @NotNull @Language("PostgreSQL") String query, @NotNull Object... params) {
		Manager.getFactory().runInTransaction(em ->
				prepareQuery(query, em, klass, false, params).executeUpdate()
		);
	}

	@SuppressWarnings("SqlSourceToSinkFlow")
	public static <T extends DAO<T>> List<T> queryBuilder(@NotNull Class<T> klass, @NotNull @Language("JPAQL") String query, Function<TypedQuery<T>, List<T>> processor, @NotNull Object... params) {
		return Manager.getFactory().callInTransaction(em -> {
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
		});
	}

	public static <T> List<T> nativeQueryBuilder(@NotNull Class<T> klass, @NotNull @Language("PostgreSQL") String query, Function<Query, List<T>> processor, @NotNull Object... params) {
		return Manager.getFactory().callInTransaction(em -> {
			Query q = prepareQuery(query, em, null, false, params);

			if (klass.isInstance(Blacklistable.class)) {
				return processor.apply(q).stream()
						.filter(o -> !((Blacklistable) o).isBlacklisted())
						.toList();
			} else if (Number.class.isAssignableFrom(klass)) {
				return processor.apply(q).stream()
						.map(o -> klass.cast(Utils.fromNumber(klass, (Number) o)))
						.toList();
			} else {
				return processor.apply(q);
			}
		});
	}

	public static List<Object[]> unmappedQueryBuilder(@NotNull @Language("PostgreSQL") String query, Function<Query, List<Object>> processor, @NotNull Object... params) {
		return Manager.getFactory().callInTransaction(em -> {
			Query q = prepareQuery(query, em, null, false, params);

			return processor.apply(q).stream()
					.map(o -> {
						if (o.getClass().isArray()) {
							return (Object[]) o;
						} else {
							return new Object[]{o};
						}
					}).toList();
		});
	}

	public static <T extends DAO<T>> void insertBatch(Collection<T> entries) {
		Manager.getFactory().runInTransaction(em -> {
			int i = 0;
			for (DAO<?> entry : entries) {
				if (i > 0 && i % 1000 == 0) {
					em.flush();
					em.clear();
				}

				em.persist(entry);
				i++;
			}
		});
	}

	public static <T extends DAO<T>> void deleteBatch(Collection<T> entries) {
		Manager.getFactory().runInTransaction(em -> {
			for (DAO<?> entry : entries) {
				DAO<?> ent = entry;
				if (!em.contains(entry)) {
					Object key = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entry);
					ent = em.find(entry.getClass(), key);

					if (ent == null) {
						throw new EntityNotFoundException("Could not delete entity of class " + entry.getClass().getSimpleName() + " [" + key + "]");
					}
				}

				em.remove(ent);
			}
		});
	}

	@SuppressWarnings("unchecked")
	public final T save() {
		return Manager.getFactory().callInTransaction(em -> {
			if (this instanceof Blacklistable lock) {
				if (lock.isBlacklisted()) return (T) this;
			}

			try {
				beforeSave();
				return em.merge((T) this);
			} finally {
				afterSave();
			}
		});
	}

	@SuppressWarnings("unchecked")
	public void apply(@NotNull Consumer<T> consumer) {
		Manager.getFactory().runInTransaction(em -> {
			Object key = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(this);

			T t = (T) em.find(getClass(), key);
			if (t == null) return;
			else if (t instanceof Blacklistable lock) {
				if (lock.isBlacklisted()) return;
			}

			consumer.accept(t);
			consumer.accept((T) this);
			em.merge(t);
		});
	}

	@SuppressWarnings("unchecked")
	public final T refresh() {
		return Manager.getFactory().callInTransaction(em -> {
			if (em.contains(this)) {
				em.refresh(this);
				return (T) this;
			} else {
				Object key = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(this);
				return (T) Utils.getOr(em.find(getClass(), key), this);
			}
		});
	}

	public final void delete() {
		Manager.getFactory().runInTransaction(em -> {
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

			try {
				beforeDelete();
				em.remove(ent);
			} finally {
				afterDelete();
			}
		});
	}

	private static @NotNull Query prepareQuery(@Language("PostgreSQL") @NotNull String query, EntityManager em, Class<?> klass, boolean singleResult, Object... params) {
		Query q = em.createNativeQuery(query);
		if (klass != null) {
			q.unwrap(NativeQuery.class).addSynchronizedEntityClass(klass);
		} else {
			q.unwrap(NativeQuery.class).addSynchronizedQuerySpace("");
		}

		if (singleResult) {
			q.setMaxResults(1);
		}

		int paramSize = Objects.requireNonNull(params).length;
		for (int i = 0; i < paramSize; i++) {
			q.setParameter(i + 1, params[i]);
		}

		return q;
	}

	public void beforeSave() {
	}

	public void afterSave() {
	}

	public void beforeDelete() {
	}

	public void afterDelete() {
	}
}
