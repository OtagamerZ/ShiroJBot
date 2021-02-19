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

import com.kuuhaku.Main;
import com.kuuhaku.handlers.games.disboard.model.PoliticalState;
import com.kuuhaku.model.common.DataDump;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.persistent.CustomAnswer;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.model.persistent.Member;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.NContract;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackupDAO {
	private static final ExecutorService backupQueue = Executors.newFixedThreadPool(5);

	public static void dumpData(DataDump data, boolean thenExit) {
		List<CustomAnswer> caDump = data.getCaDump();
		List<GuildConfig> gcDump = data.getGcDump();
		List<Member> mDump = data.getmDump();
		List<PoliticalState> psDump = data.getPsDump();

		NContract<Boolean> act = new NContract<>(4);
		act.setAction(bols -> bols.stream().allMatch(Boolean::booleanValue));

		backupQueue.execute(() -> {
			EntityManager em = Manager.getEntityManager();
			em.getTransaction().begin();

			for (int i = 0; i < caDump.size(); i++) {
				em.merge(caDump.get(i));
				saveChunk(em, i, caDump.size(), "respostas");
			}
			if (caDump.size() > 0) Helper.logger(Main.class).info(caDump.size() + " respostas salvas com sucesso!");

			em.getTransaction().commit();
			em.close();
			act.addSignature(0, true);
		});

		backupQueue.execute(() -> {
			EntityManager em = Manager.getEntityManager();
			em.getTransaction().begin();

			for (int i = 0; i < gcDump.size(); i++) {
				em.merge(gcDump.get(i));
				saveChunk(em, i, gcDump.size(), "configurações");
			}
			if (gcDump.size() > 0) Helper.logger(Main.class).info(gcDump.size() + " configurações salvas com sucesso!");

			em.getTransaction().commit();
			em.close();
			act.addSignature(1, true);
		});

		backupQueue.execute(() -> {
			EntityManager em = Manager.getEntityManager();
			em.getTransaction().begin();

			for (int i = 0; i < mDump.size(); i++) {
				em.merge(mDump.get(i));
				saveChunk(em, i, mDump.size(), "membros");
			}
			if (mDump.size() > 0) Helper.logger(Main.class).info(mDump.size() + " membros salvos com sucesso!");

			em.getTransaction().commit();
			em.close();
			act.addSignature(2, true);
		});

		backupQueue.execute(() -> {
			EntityManager em = Manager.getEntityManager();
			em.getTransaction().begin();

			for (int i = 0; i < psDump.size(); i++) {
				em.merge(psDump.get(i));
				saveChunk(em, i, psDump.size(), "estados");
			}
			if (psDump.size() > 0) Helper.logger(Main.class).info(psDump.size() + " estados salvos com sucesso!");

			em.getTransaction().commit();
			em.close();
			act.addSignature(3, true);
		});

		try {
			if (act.get() && thenExit) {
				if (Main.shutdown()) System.exit(0);
			}
		} catch (InterruptedException | ExecutionException e) {
			Helper.logger(BackupDAO.class).error(e + " | " + e.getStackTrace()[0]);
		}
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
			Helper.logger(BackupDAO.class).debug("Salvo chunk de " + name + " (" + (i / 1000) + "/" + (size / 1000) + ")");
		}
	}

	@SuppressWarnings("unchecked")
	public static DataDump getData() {
		EntityManager em = Manager.getEntityManager();

		Query ca = em.createQuery("SELECT c FROM CustomAnswer c", CustomAnswer.class);
		Query m = em.createQuery("SELECT m FROM Member m", Member.class);
		Query gc = em.createQuery("SELECT g FROM GuildConfig g", GuildConfig.class);
		List<PoliticalState> ps = new ArrayList<>();

		for (ExceedEnum ex : ExceedEnum.values()) {
			ps.add(PStateDAO.getPoliticalState(ex));
		}

		DataDump dump = new DataDump(ca.getResultList(), m.getResultList(), gc.getResultList(), ps);
		em.close();

		return dump;
	}
}
