/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.util.IO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceConfiguration;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public abstract class Manager {
	private static final String SERVER_IP = System.getenv("SERVER_IP");
	private static final String DB_NAME = System.getenv("DB_NAME");
	private static final String DB_LOGIN = System.getenv("DB_LOGIN");
	private static final String DB_PASS = System.getenv("DB_PASS");

	private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("main", Map.of(
			PersistenceConfiguration.JDBC_USER, DB_LOGIN,
			PersistenceConfiguration.JDBC_PASSWORD, DB_PASS,
			PersistenceConfiguration.CACHE_MODE, "ALL",
			PersistenceConfiguration.JDBC_URL, "jdbc:postgresql://%s/%s?currentSchema=shiro,dunhun,kawaipon&sslmode=require&useEncoding=true&characterEncoding=UTF-8".formatted(
					SERVER_IP, DB_NAME
			)
	));

	static {
		String db = DAO.queryNative(String.class, "SELECT current_database()");
		String schema = DAO.queryNative(String.class, "SELECT current_schema()");
		Constants.LOGGER.info("Connected to database {}, schema {} successfully", db, schema);

		File initDir = IO.getResourceAsFile("database");
		if (initDir != null && initDir.isDirectory()) {
			Set<String> scripts = new HashSet<>();
			emf.runInTransaction(em -> {
				try (Stream<Path> ioStream = Files.walk(initDir.toPath())) {
					ioStream.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".sql"))
							.sorted(Comparator.comparing(Path::toString).thenComparing(Path::getNameCount))
							.peek(s -> scripts.add(FilenameUtils.removeExtension(s.getFileName().toString())))
							.map(IO::readString)
							.filter(Objects::nonNull)
							.forEach(sql -> DAO.applyNative(null, sql));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});

			Constants.LOGGER.info("Applied {} scripts: {}", scripts.size(), scripts);
		}
	}

	public static EntityManagerFactory getFactory() {
		return emf;
	}

	public static EntityManager getEntityManager() {
		return emf.createEntityManager();
	}

	public static long ping() {
		long curr = System.currentTimeMillis();

		DAO.queryUnmapped("SELECT 1");

		return System.currentTimeMillis() - curr;
	}
}
