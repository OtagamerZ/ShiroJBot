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
import com.kuuhaku.model.common.DataDump;
import com.kuuhaku.model.persistent.Member;
import com.kuuhaku.utils.Helper;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BackupDAO {
	private static final ExecutorService backupThread = Executors.newSingleThreadExecutor();

	public static void dumpData(DataDump data, boolean thenExit) {
		List<Member> mDump = data.getmDump();

		Future<Boolean> act = backupThread.submit(() -> {
			EntityManager em = Manager.getEntityManager();
			em.getTransaction().begin();

			for (int i = 0; i < mDump.size(); i++) {
				em.merge(mDump.get(i));
				saveChunk(em, i, mDump.size(), "membros");
			}
			if (mDump.size() > 0) Helper.logger(Main.class).info(mDump.size() + " membros salvos com sucesso!");

			em.getTransaction().commit();
			em.close();

			return true;
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

		Query m = em.createQuery("SELECT m FROM Member m", Member.class);

		DataDump dump = new DataDump(m.getResultList());
		em.close();

		return dump;
	}
}
