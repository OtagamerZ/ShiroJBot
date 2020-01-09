package com.kuuhaku.controller.mysql;

import com.kuuhaku.model.*;
import net.dv8tion.jda.api.entities.Guild;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

public class BackupDAO {
	public static void dumpData(DataDump data) {
		EntityManager em = Manager.getEntityManager();
		em.getTransaction().begin();
		data.getCaDump().forEach(em::merge);
		data.getGcDump().forEach(em::merge);

		for (int i = 0; i < data.getmDump().size(); i++) {
			em.merge(data.getmDump().get(i));
			if (i % 20 == 0) {
				em.flush();
				em.clear();
			}
			if (i % 1000 == 0) {
				em.getTransaction().commit();
				em.clear();
				em.getTransaction().begin();
			}
		}

		em.getTransaction().commit();
		em.close();
	}

	@SuppressWarnings("unchecked")
	public static DataDump getData() {
		EntityManager em = Manager.getEntityManager();

		Query ca = em.createQuery("SELECT c FROM CustomAnswers c", CustomAnswers.class);
		Query m = em.createQuery("SELECT m FROM Member m", Member.class);
		Query gc = em.createQuery("SELECT g FROM GuildConfig g", GuildConfig.class);
		DataDump dump = new DataDump(ca.getResultList(), m.getResultList(), gc.getResultList());
		em.close();

		return dump;
	}

	public static Backup getGuildBackup(Guild g) {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT b FROM Backup b WHERE b.guild LIKE :id", Backup.class);
		q.setParameter("id", g.getId());

		try {
			return (Backup) q.getSingleResult();
		} catch (NoResultException e) {
			return new Backup();
		} finally {
			em.close();
		}
	}

	public static void saveBackup(Backup b) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(b);
		em.getTransaction().commit();

		em.close();
	}
}
