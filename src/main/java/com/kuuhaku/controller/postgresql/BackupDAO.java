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

import com.kuuhaku.handlers.games.disboard.model.PoliticalState;
import com.kuuhaku.handlers.games.kawaigotchi.Kawaigotchi;
import com.kuuhaku.model.common.DataDump;
import com.kuuhaku.model.persistent.Backup;
import com.kuuhaku.model.persistent.CustomAnswers;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.model.persistent.Member;
import com.kuuhaku.utils.ExceedEnums;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.Guild;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

public class BackupDAO {
	public static void dumpData(DataDump data) {
		EntityManager em = Manager.getEntityManager();
		em.getTransaction().begin();

		List<CustomAnswers> caDump = data.getCaDump();
		List<GuildConfig> gcDump = data.getGcDump();
		List<Member> mDump = data.getmDump();
		List<Kawaigotchi> kgDump = data.getKgDump();
		List<PoliticalState> psDump = data.getPsDump();

		for (int i = 0; i < caDump.size(); i++) {
			em.merge(data.getCaDump().get(i));
			saveChunk(em, i, caDump.size(), "respostas");
		}

		for (int i = 0; i < gcDump.size(); i++) {
			em.merge(data.getGcDump().get(i));
			saveChunk(em, i, gcDump.size(), "configurações");
		}

		for (int i = 0; i < mDump.size(); i++) {
			em.merge(data.getmDump().get(i));
			saveChunk(em, i, mDump.size(), "membros");
		}

		for (int i = 0; i < kgDump.size(); i++) {
			em.merge(data.getKgDump().get(i));
			saveChunk(em, i, kgDump.size(), "kgotchis");
		}

		for (int i = 0; i < psDump.size(); i++) {
			em.merge(data.getPsDump().get(i));
			saveChunk(em, i, psDump.size(), "estados");
		}

		em.getTransaction().commit();
		em.close();
	}

	private static void saveChunk(EntityManager em, int i, int size, String name) {
		if (i % 20 == 0) {
			em.flush();
			em.clear();
		}
		if (i % 1000 == 0) {
			em.getTransaction().commit();
			em.clear();
			em.getTransaction().begin();
			Helper.logger(BackupDAO.class).info("Salvo chunk de " + name + " (" + (i / 1000) + "/" + (size / 1000) + ")");
		}
	}

	@SuppressWarnings("unchecked")
	public static DataDump getData() {
		EntityManager em = Manager.getEntityManager();

		Query ca = em.createQuery("SELECT c FROM CustomAnswers c", CustomAnswers.class);
		Query m = em.createQuery("SELECT m FROM Member m", Member.class);
		Query gc = em.createQuery("SELECT g FROM GuildConfig g", GuildConfig.class);
		Query kg = em.createQuery("SELECT k FROM Kawaigotchi k", Kawaigotchi.class);
		List<PoliticalState> ps = new ArrayList<>();

		for (ExceedEnums ex : ExceedEnums.values()) {
			ps.add(PStateDAO.getPoliticalState(ex));
		}

		DataDump dump = new DataDump(ca.getResultList(), m.getResultList(), gc.getResultList(), kg.getResultList(), ps);
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
