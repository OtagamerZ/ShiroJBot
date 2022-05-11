/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.controller;

import com.kuuhaku.Constants;
import com.kuuhaku.utils.IO;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

public abstract class Manager {
	private static final String SERVER_IP = System.getenv("SERVER_IP");
	private static final String DB_NAME = System.getenv("DB_NAME");
	private static final String DB_LOGIN = System.getenv("DB_LOGIN");
	private static final String DB_PASS = System.getenv("DB_PASS");

	private static EntityManagerFactory emf;

	public synchronized static EntityManager getEntityManager() {
		if (emf == null) {
			emf = Persistence.createEntityManagerFactory("main", Map.of(
					"javax.persistence.jdbc.user", DB_LOGIN,
					"javax.persistence.jdbc.password", DB_PASS,
					"javax.persistence.jdbc.url", "jdbc:postgresql://%s/%s?sslmode=require&useEncoding=true&characterEncoding=UTF-8".formatted(
							SERVER_IP, DB_NAME
					)
			));
			Constants.LOGGER.info("Connected to database successfully");

			File initDir = IO.getResourceAsFile("database");
			if (initDir != null && initDir.isDirectory()) {
				try (Stream<Path> ioStream = Files.walk(initDir.toPath())) {
					ioStream.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".sql"))
							.peek(p -> Constants.LOGGER.info("Applying script " + p.getFileName()))
							.map(IO::readString)
							.forEach(DAO::applyNative);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}

		return emf.createEntityManager();
	}

	public static long ping() {
		getEntityManager().close();

		long curr = System.currentTimeMillis();

		DAO.queryUnmapped("SELECT 1");

		return System.currentTimeMillis() - curr;
	}
}
