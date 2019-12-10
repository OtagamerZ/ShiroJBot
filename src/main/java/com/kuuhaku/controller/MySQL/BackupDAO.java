package com.kuuhaku.controller.MySQL;

import com.kuuhaku.model.CustomAnswers;
import com.kuuhaku.model.DataDump;
import com.kuuhaku.model.Member;
import com.kuuhaku.model.guildConfig;

import javax.persistence.EntityManager;
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
		Query gc = em.createQuery("SELECT g FROM guildConfig g", guildConfig.class);
		DataDump dump = new DataDump(ca.getResultList(), m.getResultList(), gc.getResultList());
		em.close();

		return dump;
	}
}
