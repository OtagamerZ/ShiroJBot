package com.kuuhaku.controller.SQLite;

import com.kuuhaku.model.CustomAnswers;
import com.kuuhaku.model.DataDump;
import com.kuuhaku.model.Member;
import com.kuuhaku.model.guildConfig;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

public class BackupDAO {
	public static boolean restoreData(DataDump data) {
		EntityManager em = Manager.getEntityManager();

		try {
			em.getTransaction().begin();
			data.getCaDump().forEach(em::merge);
			data.getmDump().forEach(em::merge);
			data.getGcDump().forEach(em::merge);
			em.getTransaction().commit();

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public static List<CustomAnswers> getCADump() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT c FROM CustomAnswers c", CustomAnswers.class);
		List<CustomAnswers> ca = q.getResultList();
		ca.removeIf(CustomAnswers::isMarkForDelete);

		return ca;
	}

	@SuppressWarnings("unchecked")
	public static List<Member> getMemberDump() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT m FROM Member m", Member.class);

		return q.getResultList();
	}

	@SuppressWarnings("unchecked")
	public static List<guildConfig> getGuildDump() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT g FROM guildConfig g", guildConfig.class);

		return q.getResultList();
	}
}
