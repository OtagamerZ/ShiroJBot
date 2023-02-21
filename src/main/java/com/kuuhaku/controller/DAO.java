package com.kuuhaku.controller;

import com.kuuhaku.interfaces.Blacklistable;
import com.kuuhaku.interfaces.DAOListener;
import com.kuuhaku.interfaces.annotations.WhenNull;
import com.kuuhaku.util.Utils;
import jakarta.persistence.*;
import org.intellij.lang.annotations.Language;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class DAO<T extends DAO<T>> implements DAOListener {
	public static <T extends DAO<T>, ID> T find(@Nonnull Class<T> klass, @Nonnull ID id) {
		EntityManager em = Manager.getEntityManager();

		try {
			T t = em.find(klass, id);
			if (t instanceof Blacklistable lock) {
				if (lock.isBlacklisted()) {
					t = null;
				}
			}

			if (t == null) {
				for (Constructor<?> method : klass.getConstructors()) {
					if (method.isAnnotationPresent(WhenNull.class)) {
						Class<?>[] params = method.getParameterTypes();
						if (params.length > 0 && params[0] == id.getClass()) {
							try {
								t = klass.cast(method.newInstance(id));
								t.save();
								break;
							} catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
								throw new IllegalStateException("Failed to instantiate class", e);
							}
						}
					}
				}
			}

			return t;
		} finally {
			em.close();
		}
	}

	public static <T extends DAO<T>> T query(@Nonnull Class<T> klass, @Nonnull @Language("JPAQL") String query, @Nonnull Object... params) {
		EntityManager em = Manager.getEntityManager();

		try {
			TypedQuery<T> q = em.createQuery(query, klass);
			q.setMaxResults(1);
			for (int i = 0; i < params.length; i++) {
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
		} finally {
			em.close();
		}
	}

	public static <T> T queryNative(@Nonnull Class<T> klass, @Nonnull @Language("PostgreSQL") String query, @Nonnull Object... params) {
		EntityManager em = Manager.getEntityManager();

		try {
			Query q = em.createNativeQuery(query);
			q.setMaxResults(1);
			for (int i = 0; i < params.length; i++) {
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
		} finally {
			em.close();
		}
	}

	public static Object[] queryUnmapped(@Nonnull @Language("PostgreSQL") String query, @Nonnull Object... params) {
		EntityManager em = Manager.getEntityManager();

		try {
			Query q = em.createNativeQuery(query);
			q.setMaxResults(1);
			for (int i = 0; i < params.length; i++) {
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
		} finally {
			em.close();
		}
	}

	public static <T extends DAO<T>> List<T> findAll(@Nonnull Class<T> klass) {
		EntityManager em = Manager.getEntityManager();

		try {
			TypedQuery<T> q = em.createQuery("SELECT o FROM " + klass.getSimpleName() + " o", klass);

			if (klass.isInstance(Blacklistable.class)) {
				return q.getResultStream()
						.filter(o -> !((Blacklistable) o).isBlacklisted())
						.toList();
			} else {
				return q.getResultList();
			}
		} finally {
			em.close();
		}
	}

	public static <T extends DAO<T>> List<T> queryAll(@Nonnull Class<T> klass, @Nonnull @Language("JPAQL") String query, @Nonnull Object... params) {
		EntityManager em = Manager.getEntityManager();

		try {
			TypedQuery<T> q = em.createQuery(query, klass);
			for (int i = 0; i < params.length; i++) {
				q.setParameter(i + 1, params[i]);
			}

			if (klass.isInstance(Blacklistable.class)) {
				return q.getResultStream()
						.filter(o -> !((Blacklistable) o).isBlacklisted())
						.toList();
			} else {
				return q.getResultList();
			}
		} finally {
			em.close();
		}
	}

	public static <T> List<T> queryAllNative(@Nonnull Class<T> klass, @Nonnull @Language("PostgreSQL") String query, @Nonnull Object... params) {
		EntityManager em = Manager.getEntityManager();

		try {
			Query q = em.createNativeQuery(query);
			for (int i = 0; i < params.length; i++) {
				q.setParameter(i + 1, params[i]);
			}

			if (klass.isInstance(Blacklistable.class)) {
				return ((Stream<?>) q.getResultStream())
						.map(klass::cast)
						.filter(o -> !((Blacklistable) o).isBlacklisted())
						.toList();
			} else if (Number.class.isAssignableFrom(klass)) {
				return ((Stream<?>) q.getResultStream())
						.map(o -> Utils.fromNumber(klass, (Number) o))
						.map(klass::cast)
						.toList();
			} else {
				return ((Stream<?>) q.getResultStream())
						.map(klass::cast)
						.toList();
			}
		} finally {
			em.close();
		}
	}

	public static List<Object[]> queryAllUnmapped(@Nonnull @Language("PostgreSQL") String query, @Nonnull Object... params) {
		EntityManager em = Manager.getEntityManager();

		try {
			Query q = em.createNativeQuery(query);
			for (int i = 0; i < params.length; i++) {
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
		} finally {
			em.close();
		}
	}

	public static <T extends DAO<?>, ID> void apply(@Nonnull Class<T> klass, @Nonnull ID id, @Nonnull Consumer<T> consumer) {
		EntityManager em = Manager.getEntityManager();

		try {
			em.getTransaction().begin();

			T obj = em.find(klass, id);
			if (obj == null) return;
			else if (obj instanceof Blacklistable lock) {
				if (lock.isBlacklisted()) return;
			}

			consumer.accept(obj);
			em.merge(obj);

			em.getTransaction().commit();
		} finally {
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}

			em.close();
		}
	}

	public static void apply(@Nonnull @Language("JPAQL") String query, @Nonnull Object... params) {
		EntityManager em = Manager.getEntityManager();

		try {
			em.getTransaction().begin();

			Query q = em.createQuery(query);
			for (int i = 0; i < params.length; i++) {
				q.setParameter(i + 1, params[i]);
			}
			q.executeUpdate();

			em.getTransaction().commit();
		} finally {
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}

			em.close();
		}
	}

	public static void applyNative(@Nonnull @Language("PostgreSQL") String query, @Nonnull Object... params) {
		EntityManager em = Manager.getEntityManager();

		try {
			em.getTransaction().begin();

			Query q = em.createNativeQuery(query);
			for (int i = 0; i < params.length; i++) {
				q.setParameter(i + 1, params[i]);
			}
			q.executeUpdate();

			em.getTransaction().commit();
		} finally {
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}

			em.close();
		}
	}

	public static <T extends DAO<T>> List<T> queryBuilder(@Nonnull Class<T> klass, @Nonnull @Language("JPAQL") String query, Function<TypedQuery<T>, List<T>> processor, @Nonnull Object... params) {
		EntityManager em = Manager.getEntityManager();

		try {
			TypedQuery<T> q = em.createQuery(query, klass);
			for (int i = 0; i < params.length; i++) {
				q.setParameter(i + 1, params[i]);
			}

			if (klass.isInstance(Blacklistable.class)) {
				return processor.apply(q).stream()
						.filter(o -> !((Blacklistable) o).isBlacklisted())
						.toList();
			} else {
				return processor.apply(q);
			}
		} finally {
			em.close();
		}
	}

	public static <T> List<T> nativeQueryBuilder(@Nonnull Class<T> klass, @Nonnull @Language("PostgreSQL") String query, Function<Query, List<T>> processor, @Nonnull Object... params) {
		EntityManager em = Manager.getEntityManager();

		try {
			Query q = em.createNativeQuery(query);
			for (int i = 0; i < params.length; i++) {
				q.setParameter(i + 1, params[i]);
			}

			if (klass.isInstance(Blacklistable.class)) {
				return processor.apply(q).stream()
						.map(klass::cast)
						.filter(o -> !((Blacklistable) o).isBlacklisted())
						.toList();
			} else if (Number.class.isAssignableFrom(klass)) {
				return processor.apply(q).stream()
						.map(o -> Utils.fromNumber(klass, (Number) o))
						.map(klass::cast)
						.toList();
			} else {
				return processor.apply(q).stream()
						.map(klass::cast)
						.toList();
			}
		} finally {
			em.close();
		}
	}

	public static List<Object[]> unmappedQueryBuilder(@Nonnull @Language("PostgreSQL") String query, Function<Query, List<Object>> processor, @Nonnull Object... params) {
		EntityManager em = Manager.getEntityManager();

		try {
			Query q = em.createNativeQuery(query);
			for (int i = 0; i < params.length; i++) {
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
		} finally {
			em.close();
		}
	}

	public final void save() {
		EntityManager em = Manager.getEntityManager();

		beforeSave();
		try {
			if (this instanceof Blacklistable lock) {
				if (lock.isBlacklisted()) return;
			}

			em.getTransaction().begin();
			try {
				if (em.contains(this)) {
					em.merge(this);
				}

				em.persist(this);
			} catch (EntityExistsException e) {
				em.merge(this);
			}
			em.getTransaction().commit();
		} finally {
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}

			afterSave();
			em.close();
		}
	}

	@SuppressWarnings("unchecked")
	public final T refresh() {
		EntityManager em = Manager.getEntityManager();

		beforeRefresh();
		try {
			Field[] fields = getClass().getDeclaredFields();
			for (Field field : fields) {
				if (getClass().isAnnotationPresent(Id.class)) {
					return (T) find(getClass(), field.get(field));
				}
			}

			return (T) this;
		} catch (IllegalAccessException e) {
			return (T) this;
		} finally {
			afterRefresh();
			em.close();
		}
	}

	public final void delete() {
		EntityManager em = Manager.getEntityManager();

		beforeDelete();
		try {
			DAO<?> ent;
			if (!em.contains(this)) {
				Object key = Manager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(this);
				ent = em.find(getClass(), key);

				if (ent == null) return;
			} else {
				ent = this;
			}

			em.getTransaction().begin();
			em.remove(ent);
			em.getTransaction().commit();
		} finally {
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}

			afterDelete();
			em.close();
		}
	}
}