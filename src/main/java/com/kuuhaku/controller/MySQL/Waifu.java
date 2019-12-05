package com.kuuhaku.controller.MySQL;

import com.kuuhaku.model.Member;
import net.dv8tion.jda.api.entities.User;

import javax.persistence.EntityManager;

public class Waifu {
	public static void saveMemberWaifu(Member m, User u) {
		EntityManager em = Manager.getEntityManager();

		m.marry(u);

		em.getTransaction().begin();
		em.merge(m);
		em.getTransaction().commit();

		em.close();
	}

	public static void removeMemberWaifu(Member m) {
		EntityManager em = Manager.getEntityManager();

		m.divorce();

		em.getTransaction().begin();
		em.merge(m);
		em.getTransaction().commit();

		em.close();
	}
}
