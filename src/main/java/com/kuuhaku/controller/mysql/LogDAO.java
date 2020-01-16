package com.kuuhaku.controller.mysql;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Query;
import java.util.List;

public class LogDAO {
	public static void saveLog(com.kuuhaku.model.Log log) {
		EntityManager em = Manager.getEntityManager();

		em.getTransaction().begin();
		em.merge(log);
		em.getTransaction().commit();

		em.close();
	}

	@SuppressWarnings("unchecked")
	public static List<UsageView> getUses() {
		EntityManager em = Manager.getEntityManager();

		Query q = em.createQuery("SELECT guild, COUNT(guild) AS uses FROM UsageView u GROUP BY guild ORDER BY uses DESC", Object.class);

		return (List<UsageView>) q.getResultList();
	}

	@Entity
	public static class UsageView {
		@Id
		private String guild;
		private int uses;

		public String getGuild() {
			return guild;
		}

		public void setGuild(String guild) {
			this.guild = guild;
		}

		public int getUses() {
			return uses;
		}

		public void setUses(int uses) {
			this.uses = uses;
		}
	}
}
