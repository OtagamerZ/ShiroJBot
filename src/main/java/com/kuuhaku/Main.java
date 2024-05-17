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
import com.kuuhaku.managers.CacheManager;
import com.kuuhaku.managers.CommandManager;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.springframework.context.ConfigurableApplicationContext;

import javax.imageio.ImageIO;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

public class Main implements Thread.UncaughtExceptionHandler {
	private static ShiroInfo info;
	private static CommandManager cmdManager;
	private static CacheManager cacheManager;
	private static ShardManager shiroShards;
	static boolean exiting = false;
	static ConfigurableApplicationContext spring;

	private static final Main INSTANCE = new Main();

	static {
		Helper.logger(Main.class).info("""
				Shiro J. Bot  Copyright (C) 2019-%s Yago Gimenez (KuuHaKu)
				This program comes with ABSOLUTELY NO WARRANTY
				This is free software, and you are welcome to redistribute it under certain conditions
				See license for more information regarding redistribution conditions
				""".formatted(LocalDate.now().getYear()));
	}

	public static void main(String[] args) throws Exception {
		ImageIO.setUseCache(false);
		Thread.setDefaultUncaughtExceptionHandler(INSTANCE);
		info = new ShiroInfo();
		cmdManager = new CommandManager();
		cacheManager = new CacheManager();

		shiroShards = DefaultShardManagerBuilder.create(ShiroInfo.getBotToken(), EnumSet.allOf(GatewayIntent.class))
				.disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS)
				.setMemberCachePolicy(m -> !m.getUser().isBot())
				.setBulkDeleteSplittingEnabled(false)
				.setCompression(Compression.NONE)
				.setEventPool(new ForkJoinPool(
						Runtime.getRuntime().availableProcessors(),
						ForkJoinPool.defaultForkJoinWorkerThreadFactory,
						INSTANCE,
						true
				), true)
				.addEventListeners(ShiroInfo.getShiroEvents())
				.build();

		List<JDA> shards = shiroShards.getShards().stream()
				.sorted(Comparator.comparingInt(s -> s.getShardInfo().getShardId()))
				.toList();
		for (JDA shard : shards) {
			int id = shard.getShardInfo().getShardId();
			try {
				shard.awaitReady();
				shard.getPresence().setActivity(Activity.playing("Iniciando shards..."));
				Helper.logger(Main.class).info("Shard " + id + " pronto!");
			} catch (InterruptedException e) {
				Helper.logger(Main.class).error("Erro ao inicializar shard " + id + ": " + e);
			}
		}

		try {
			PaginatorBuilder.createPaginator()
					.setHandler(shiroShards)
					.shouldEventLock(true)
					.activate();
		} catch (InvalidHandlerException e) {
			Helper.logger(Main.class).error(e + " | " + e.getStackTrace()[0]);
		}

		cmdManager.registerCommands();
		info.setStartTime(System.currentTimeMillis());
		Helper.logger(Main.class).info("Criada pool de compilação: " + ShiroInfo.getCompilationPool().getCorePoolSize() + " espaços alocados");

//		info.setSockets(new WebSocketConfig());
		finishStartUp();
	}

	private static void finishStartUp() {
		try (ConsoleListener console = new ConsoleListener()) {
			console.start();
		}

		for (Emote emote : shiroShards.getEmotes()) {
			ShiroInfo.getEmoteLookup().put(":" + emote.getName() + ":", emote.getId());
		}

		for (GuildConfig guildConfig : GuildDAO.getAllGuildsWithButtons()) {
			try {
				//Helper.refreshButtons(guildConfig);
			} catch (RuntimeException e) {
				Helper.logger(Main.class).error("Error loading role buttons for guild with ID " + guildConfig.getGuildId());
			}
		}

//		CompletableFuture<Void> lst = new CompletableFuture<>();
//		Executors.newSingleThreadExecutor().execute(() -> {
//			new ScheduledEvents();
//			lst.complete(null);
//		});

		List<JDA> shards = shiroShards.getShards();
		for (JDA shard : shards) {
			shard.getPresence().setActivity(getRandomActivity());
		}

//		lst.get();
		System.runFinalization();

		Helper.logger(Main.class).info("<----------END OF BOOT---------->");
		Helper.logger(Main.class).info("Estou pronta!");
	}

	public static Activity getRandomActivity() {
		List<Activity> activities = List.of(
				Activity.playing("Digite " + ShiroInfo.getDefaultPrefix() + "ajuda para ver meus comandos!"),
				Activity.competing("Shoukan ranqueado!"),
				Activity.listening(Helper.separate(shiroShards.getGuilds().size()) + " servidores, e isso ainda é só o começo!"),
				Activity.watching("No Game No Life pela " + Helper.separate(Helper.extract(ShiroInfo.getVersion(), ".(\\d+)$", 1)) + "ª vez, e ainda não enjoei de ver como eu atuo bem!")
		);

		return Helper.getRandomEntry(activities);
	}

	public static ShiroInfo getInfo() {
		return info;
	}

	public static User getSelfUser() {
		return shiroShards.getShards().get(0).getSelfUser();
	}

	public static CommandManager getCommandManager() {
		return cmdManager;
	}

	public static void setCommandManager(CommandManager cmdManager) {
		Main.cmdManager = cmdManager;
	}

	public static CacheManager getCacheManager() {
		return cacheManager;
	}

	public static void shutdown() {
		if (exiting) return;
		exiting = true;

//		ScheduledEvents.shutdown();
//		info.getSockets().shutdown();
//		SpringApplication.exit(spring);
		shiroShards.shutdown();

		System.exit(0);
	}

	public static ShardManager getShiroShards() {
		return shiroShards;
	}

	public static JDA getDefaultShard() {
		return shiroShards.getShardById(0);
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		Helper.logger(this.getClass()).error(e, e);
		e.printStackTrace();
	}
}
