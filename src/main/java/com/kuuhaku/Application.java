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
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.PaginatorBuilder;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.controller.Manager;
import com.kuuhaku.listener.GuildListener;
import com.kuuhaku.model.common.websocket.CommonSocket;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.util.API;
import com.kuuhaku.util.Utils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.AllowedMentions;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import static net.dv8tion.jda.api.entities.Message.MentionType.EVERYONE;
import static net.dv8tion.jda.api.entities.Message.MentionType.HERE;

public class Application implements Thread.UncaughtExceptionHandler {
	private final ShardManager shiro;

	public Application() {
		long latency = Manager.ping();
		if (latency <= 100) {
			Constants.LOGGER.info("Database latency: " + latency + "ms");
		} else if (latency <= 250) {
			Constants.LOGGER.warn("Database latency: " + latency + "ms");
		} else {
			Constants.LOGGER.error("Database latency: " + latency + "ms");
		}

		ShardManager sm = null;
		try {
			sm = DefaultShardManagerBuilder.create(Constants.BOT_TOKEN, EnumSet.allOf(GatewayIntent.class))
					.disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS)
					.setMemberCachePolicy(MemberCachePolicy.ONLINE
							.and(MemberCachePolicy.OWNER)
							.and(m -> !m.getUser().isBot()))
					.addEventListeners(new GuildListener())
					.setBulkDeleteSplittingEnabled(false)
					.setEventPool(new ForkJoinPool(
							Runtime.getRuntime().availableProcessors(),
							ForkJoinPool.defaultForkJoinWorkerThreadFactory,
							this,
							true
					), true)
					.build();
		} catch (LoginException e) {
			Constants.LOGGER.fatal("Failed to login: " + e);
			System.exit(1);
		} finally {
			shiro = sm;
		}

		CompletableFuture.runAsync(() ->
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
						.forEach(this::setRandomAction)
		);

		AllowedMentions.setDefaultMentions(EnumSet.complementOf(EnumSet.of(EVERYONE, HERE)));

		try {
			PaginatorBuilder.createPaginator()
					.setHandler(shiro)
					.shouldEventLock(true)
					.activate();
		} catch (InvalidHandlerException e) {
			Constants.LOGGER.error("Failed to start pagination library: " + e);
			System.exit(1);
		}

		API.connectSocket(CommonSocket.class, Constants.SOCKET_ROOT);

		System.runFinalization();
		Constants.LOGGER.info("<----------END OF BOOT---------->");

		Main.boot.stop();
		Constants.LOGGER.info("Finished in " + Utils.toStringDuration(I18N.EN, Main.boot.getTime()));
	}

	public ShardManager getShiro() {
		return shiro;
	}

	public User getUserById(String id) {
		return Utils.getOr(shiro.getUserById(id), Pages.subGet(shiro.retrieveUserById(id)));
	}

	public JDA getMainShard() {
		return shiro.getShards().get(0);
	}

	public String getId() {
		return getMainShard().getSelfUser().getId();
	}

	public void setRandomAction(JDA jda) {
		final List<Activity> activities = List.of(
				Activity.watching(Utils.separate(shiro.getGuildCache().size()) + " servidores, e estou apenas começando!"),
				Activity.competing("Shoukan ranqueado!"),
				Activity.watching(DAO.queryNative(String.class, """
						SELECT c.name||' pela '||(SELECT COUNT(1) FROM card x WHERE x.anime_id = c.anime_id)||'ª vez!'
						FROM card c
						WHERE c.rarity = 'ULTIMATE'
						ORDER BY RANDOM()
						""")),
				Activity.playing("com minhas cartas Kawaipon!"),
				Activity.of(Activity.ActivityType.DEFAULT, "Use s!help para ver os meus comandos!")
		);

		jda.getPresence().setActivity(Utils.getRandomEntry(activities));
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		Constants.LOGGER.error(e, e);
	}
}
