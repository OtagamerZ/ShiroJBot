package com.kuuhaku.controller;

import com.kuuhaku.interfaces.Blacklistable;
import com.kuuhaku.interfaces.annotations.WhenNull;
import com.kuuhaku.utils.Utils;
import org.intellij.lang.annotations.Language;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public abstract class DAO {
	public static <T extends DAO, ID> T find(@Nonnull Class<T> klass, @Nonnull ID id) {
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

	public static <T extends DAO> T query(@Nonnull Class<T> klass, @Nonnull @Language("JPAQL") String query, @Nonnull Object... params) {
		EntityManager em = Manager.getEntityManager();

		try {
			TypedQuery<T> q = em.createQuery(query, klass);
			q.setMaxResults(1);
			for (int i = 0; i < params.length; i++) {
				q.setParameter(i+1, params[i]);
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
				q.setParameter(i+1, params[i]);
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
				q.setParameter(i+1, params[i]);
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

	public static <T extends DAO> List<T> findAll(@Nonnull Class<T> klass) {
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

	public static <T extends DAO> List<T> queryAll(@Nonnull Class<T> klass, @Nonnull @Language("JPAQL") String query, @Nonnull Object... params) {
		EntityManager em = Manager.getEntityManager();

		try {
			TypedQuery<T> q = em.createQuery(query, klass);
			for (int i = 0; i < params.length; i++) {
				q.setParameter(i+1, params[i]);
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
				q.setParameter(i+1, params[i]);
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
				q.setParameter(i+1, params[i]);
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

	public static <T extends DAO, ID> void apply(@Nonnull Class<T> klass, @Nonnull ID id, @Nonnull Consumer<T> consumer) {
		EntityManager em = Manager.getEntityManager();

		try {
			em.getTransaction().begin();

			T obj = em.find(klass, id, LockModeType.PESSIMISTIC_WRITE);
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
			q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
			for (int i = 0; i < params.length; i++) {
				q.setParameter(i+1, params[i]);
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

			Query q = em.createQuery(query);
			q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
			for (int i = 0; i < params.length; i++) {
				q.setParameter(i+1, params[i]);
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

	public final void save() {
		EntityManager em = Manager.getEntityManager();

		try {
			if (this instanceof Blacklistable lock) {
				if (lock.isBlacklisted()) return;
			}

			em.getTransaction().begin();
			em.merge(this);
			em.getTransaction().commit();
		} finally {
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}

			em.close();
		}
	}

	public final void delete() {
		EntityManager em = Manager.getEntityManager();

		try {
			em.getTransaction().begin();
			em.remove(em.contains(this) ? em : em.merge(this));
			em.getTransaction().commit();
		} finally {
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}

			em.close();
		}
	}
}
