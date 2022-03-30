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

package com.kuuhaku;

import com.github.ygimenez.exception.InvalidHandlerException;
import com.github.ygimenez.model.PaginatorBuilder;
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.events.ConsoleListener;
import com.kuuhaku.events.ShiroEvents;
import com.kuuhaku.events.cron.ScheduledEvents;
import com.kuuhaku.handlers.api.Application;
import com.kuuhaku.managers.CacheManager;
import com.kuuhaku.managers.CommandManager;
import com.kuuhaku.managers.RaidManager;
import com.kuuhaku.managers.WebSocketManager;
import com.kuuhaku.model.persistent.AddedAnime;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.utils.Constants;
import com.kuuhaku.utils.ShiroInfo;
import com.kuuhaku.utils.helpers.CollectionHelper;
import com.kuuhaku.utils.helpers.MiscHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.mapdb.HTreeMap;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.imageio.ImageIO;
import javax.security.auth.login.LoginException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Executors;

public class Main implements Thread.UncaughtExceptionHandler {
	private static final ShiroInfo info = new ShiroInfo();
	private static final ShiroEvents events = new ShiroEvents();
	private static final CacheManager cacheManager = new CacheManager();
	private static final CommandManager cmdManager = new CommandManager();
	private static final RaidManager raidManager = new RaidManager();
	private static final WebSocketManager sockets = new WebSocketManager();
	private static final ShardManager shiro;

	private static boolean exiting = false;
	private static ConfigurableApplicationContext spring;

	static {
		MiscHelper.logger(Main.class).info("""
				Shiro J. Bot  Copyright (C) 2019-%s Yago Gimenez (KuuHaKu)
				This program comes with ABSOLUTELY NO WARRANTY
				This is free software, and you are welcome to redistribute it under certain conditions
				See license for more information regarding redistribution conditions
				""".formatted(LocalDate.now().getYear()));

		ShardManager shards;
		try {
			shards = DefaultShardManagerBuilder.create(ShiroInfo.TOKEN, EnumSet.allOf(GatewayIntent.class))
					.disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS)
					.setMemberCachePolicy(m -> !m.getUser().isBot())
					.setBulkDeleteSplittingEnabled(false)
					.setEventPool(Executors.newWorkStealingPool(20), true)
					.build();
		} catch (LoginException e) {
			shards = null;
			System.exit(1);
		}

		shiro = shards;
	}

	public static void main(String[] args) throws Exception {
		ImageIO.setUseCache(false);
		Thread.setDefaultUncaughtExceptionHandler(new Main());

		Executors.newSingleThreadExecutor().execute(() -> {
			shiro.getShards().stream()
					.sorted(Comparator.comparingInt(s -> s.getShardInfo().getShardId()))
					.peek(s -> {
						int id = s.getShardInfo().getShardId();

						try {
							s.awaitReady();
							MiscHelper.logger(Main.class).info("Shard " + id + " pronto!");
						} catch (InterruptedException e) {
							MiscHelper.logger(Main.class).error("Erro ao inicializar shard " + id + ": " + e);
						}
					})
					.forEach(s -> s.getPresence().setActivity(getRandomActivity()));

			shiro.addEventListener(events);
		});

		try {
			PaginatorBuilder.createPaginator()
					.setHandler(shiro)
					.shouldEventLock(true)
					.activate();
		} catch (InvalidHandlerException e) {
			MiscHelper.logger(Main.class).error(e + " | " + e.getStackTrace()[0]);
		}

		cmdManager.registerCommands();
		spring = SpringApplication.run(Application.class, args);
		finishStartUp();
	}

	private static void finishStartUp() {
		try (ConsoleListener console = new ConsoleListener()) {
			console.start();
		}

		for (Emote emote : shiro.getEmotes()) {
			cacheManager.getEmoteCache().put(":" + emote.getName() + ":", emote.getId());
		}

		for (GuildConfig guildConfig : GuildDAO.getAllGuildsWithButtons()) {
			try {
				MiscHelper.refreshButtons(guildConfig);
			} catch (RuntimeException e) {
				MiscHelper.logger(Main.class).error("Error loading role buttons for guild with ID " + guildConfig.getGuildId());
			}
		}

		ScheduledEvents.init();
		System.runFinalization();

		MiscHelper.logger(Main.class).info("<----------END OF BOOT---------->");
		MiscHelper.logger(Main.class).info("Estou pronta!");
	}

	public static ShardManager getShiro() {
		return shiro;
	}

	public static JDA getDefaultShard() {
		return shiro.getShardById(0);
	}

	public static User getSelfUser() {
		return getDefaultShard().getSelfUser();
	}

	public static ShiroInfo getInfo() {
		return info;
	}

	public static ShiroEvents getEvents() {
		return events;
	}

	public static CacheManager getCacheManager() {
		return cacheManager;
	}

	public static HTreeMap<String, byte[]> getCardCache() {
		return cacheManager.getCardCache();
	}

	public static HTreeMap<String, byte[]> getResourceCache() {
		return cacheManager.getResourceCache();
	}

	public static HTreeMap<String, String> getEmoteCache() {
		return cacheManager.getEmoteCache();
	}

	public static CommandManager getCommandManager() {
		return cmdManager;
	}

	public static RaidManager getRaidManager() {
		return raidManager;
	}

	public static void shutdown() {
		if (exiting) return;
		exiting = true;

		sockets.shutdown();
		ScheduledEvents.shutdown();
		SpringApplication.exit(spring);
		shiro.shutdown();

		System.exit(0);
	}

	public static User getUserByID(String userID) {
		if (userID == null || userID.isBlank()) return null;
		return Main.getShiro().getUserById(userID);
	}

	public static User[] getUsersByID(String... userIDs) {
		return Arrays.stream(userIDs)
				.map(Main::getUserByID)
				.toArray(User[]::new);
	}

	public static Member getMemberByID(String userID) {
		User u = getUserByID(userID);
		return u.getMutualGuilds().get(0).getMember(u);
	}

	public static Member[] getMembersByID(String... userIDs) {
		return Arrays.stream(userIDs)
				.map(Main::getMemberByID)
				.toArray(Member[]::new);
	}

	public static Role getRoleByID(String roleID) {
		return Main.getShiro().getRoleById(roleID);
	}

	public static Guild getGuildByID(String guildID) {
		return Main.getShiro().getGuildById(guildID);
	}

	public static Activity getRandomActivity() {
		List<AddedAnime> animes = AddedAnime.queryAll(AddedAnime.class, "SELECT a FROM AddedAnime a WHERE a.hidden = FALSE");
		List<Activity> activities = List.of(
				Activity.playing("Digite " + Constants.DEFAULT_PREFIX + "ajuda para ver meus comandos!"),
				Activity.competing("Shoukan ranqueado!"),
				Activity.listening(StringHelper.separate(Main.getShiro().getGuilds().size()) + " servidores, e isso ainda é só o começo!"),
				Activity.watching(CollectionHelper.getRandomEntry(animes).getName())
		);

		return CollectionHelper.getRandomEntry(activities);
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		MiscHelper.logger(this.getClass()).error(e, e);
		e.printStackTrace();
	}
}
