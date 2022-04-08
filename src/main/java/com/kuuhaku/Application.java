/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku;

import com.github.ygimenez.exception.InvalidHandlerException;
import com.github.ygimenez.model.PaginatorBuilder;
import com.kuuhaku.controller.Manager;
import com.kuuhaku.listeners.GuildListener;
import com.kuuhaku.utils.Utils;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.concurrent.Executors;

public class Application implements Thread.UncaughtExceptionHandler {
	private final ShardManager shiro;

	public Application() {
		long latency = Manager.ping();
		if (latency <= 100) {
			Constants.LOGGER.info("Database latency: " + latency);
		} else if (latency <= 250) {
			Constants.LOGGER.warn("Database latency: " + latency);
		} else {
			Constants.LOGGER.error("Database latency: " + latency);
		}

		ShardManager sm = null;
		try {
			sm = DefaultShardManagerBuilder.create(Constants.BOT_TOKEN, EnumSet.allOf(GatewayIntent.class))
					.disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS)
					.setMemberCachePolicy(m -> !m.getUser().isBot())
					.addEventListeners(new GuildListener())
					.setBulkDeleteSplittingEnabled(false)
					.setEventPool(Executors.newWorkStealingPool(20), true)
					.build();
		} catch (LoginException e) {
			Constants.LOGGER.fatal("Failed to login: " + e);
			System.exit(1);
		} finally {
			shiro = sm;
		}

		Executors.newSingleThreadExecutor().execute(() ->
				shiro.getShards().stream()
						.sorted(Comparator.comparingInt(s -> s.getShardInfo().getShardId()))
						.peek(s -> {
							int id = s.getShardInfo().getShardId();

							try {
								s.awaitReady();
								Constants.LOGGER.info("Shard " + id + " ready");
							} catch (InterruptedException e) {
								Constants.LOGGER.error("Failed to initialize shard " + id + ": " + e);
							}
						})
		);

		try {
			PaginatorBuilder.createPaginator()
					.setHandler(shiro)
					.shouldEventLock(true)
					.activate();
		} catch (InvalidHandlerException e) {
			Constants.LOGGER.error("Failed to start pagination library: " + e);
			System.exit(1);
		}

		System.runFinalization();
		Constants.LOGGER.info("<----------END OF BOOT---------->");

		Main.boot.stop();
		Constants.LOGGER.info("Finished in " + Utils.toStringDuration(Main.boot.getTime()));
	}

	public ShardManager getShiro() {
		return shiro;
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		Constants.LOGGER.error(e, e);
	}
}
