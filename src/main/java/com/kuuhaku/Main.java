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

package com.kuuhaku;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.ygimenez.exception.InvalidHandlerException;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Paginator;
import com.github.ygimenez.model.PaginatorBuilder;
import com.kuuhaku.controller.Relay;
import com.kuuhaku.controller.postgresql.BackupDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.controller.sqlite.Manager;
import com.kuuhaku.events.ConsoleListener;
import com.kuuhaku.events.JibrilEvents;
import com.kuuhaku.events.ScheduledEvents;
import com.kuuhaku.events.TwitchEvents;
import com.kuuhaku.handlers.api.Application;
import com.kuuhaku.handlers.api.websocket.WebSocketConfig;
import com.kuuhaku.managers.CommandManager;
import com.kuuhaku.managers.TwitchCommandManager;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ContextException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.imageio.ImageIO;
import javax.persistence.NoResultException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Executors;

public class Main implements Thread.UncaughtExceptionHandler {

	private static ShiroInfo info;
	private static Relay relay;
	private static CommandManager cmdManager;
	private static TwitchCommandManager tCmdManager;
	private static ShardManager shiroShards;
	private static JDA jbr;
	private static JDA tet;
	private static TwitchClient twitch;
	private static TwitchEvents twitchManager;
	public static boolean exiting = false;
	public static ConfigurableApplicationContext spring;

	public static void main(String[] args) throws Exception {
		//Locale.setDefault(new Locale("pt", "BR"));
		Helper.logger(Main.class).info("""
				Shiro J. Bot  Copyright (C) 2020 Yago Gimenez (KuuHaKu)
				This program comes with ABSOLUTELY NO WARRANTY 
				This is free software, and you are welcome to redistribute it under certain conditions
				See license for more information regarding redistribution conditions
				""");
		Thread.setDefaultUncaughtExceptionHandler(new Main());
		ImageIO.setUseCache(false);
		info = new ShiroInfo();
		relay = new Relay();
		cmdManager = new CommandManager();
		tCmdManager = new TwitchCommandManager();

		EnumSet<GatewayIntent> intents = EnumSet.allOf(GatewayIntent.class);

		shiroShards = DefaultShardManagerBuilder.create(info.getBotToken(), intents)
				.disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS)
				.setMemberCachePolicy(m -> !m.getUser().isBot())
				.setBulkDeleteSplittingEnabled(false)
				.setAudioSendFactory(new NativeAudioSendFactory())
				.setEventPool(Executors.newCachedThreadPool(), true)
				.build();

		jbr = JDABuilder.createLight(System.getenv("JIBRIL_TOKEN"))
				.setMaxReconnectDelay(32)
				.setEventPool(Executors.newCachedThreadPool(), true)
				.build()
				.awaitReady();

		shiroShards.setActivity(Activity.playing("Iniciando..."));
		jbr.getPresence().setActivity(Activity.playing("Iniciando..."));

		info.setStartTime(Instant.now().getEpochSecond());
		Helper.logger(Main.class).info("Criada pool de compilação: " + info.getCompilationPool().getCorePoolSize() + " espaços alocados");

		Manager.connect();
		if (com.kuuhaku.controller.sqlite.BackupDAO.restoreData(BackupDAO.getData()))
			Helper.logger(Main.class).info("Dados recuperados com sucesso!");
		else {
			Helper.logger(Main.class).error("Erro ao recuperar dados.");
			return;
		}

		Helper.logger(Main.class).info("Campanhas recuperadas com sucesso!");

		Executors.newSingleThreadExecutor().execute(ScheduledEvents::new);

		AudioSourceManagers.registerRemoteSources(getInfo().getApm());
		AudioSourceManagers.registerLocalSource(getInfo().getApm());

		spring = SpringApplication.run(Application.class, args);

		if (System.getenv().containsKey("TWITCH_TOKEN")) {
			OAuth2Credential cred = new OAuth2Credential("twitch", System.getenv("TWITCH_TOKEN"));
			twitch = TwitchClientBuilder.builder()
					.withEnableHelix(true)
					.withEnableChat(true)
					.withDefaultAuthToken(cred)
					.withChatAccount(cred)
					.build();

			twitchManager = new TwitchEvents(twitch);
			twitch.getChat().joinChannel("kuuhaku_otgmz");
			twitch.getClientHelper().enableStreamEventListener("kuuhaku_otgmz");
			twitch.getClientHelper().enableFollowEventListener("kuuhaku_otgmz");
		}

		info.setSockets(new WebSocketConfig());
		finishStartUp();
	}

	private static void finishStartUp() {
		shiroShards.setActivity(getRandomActivity());
		jbr.getPresence().setActivity(Activity.listening("as mensagens de " + relay.getRelayMap().size() + " servidores!"));
		getInfo().setWinner(ExceedDAO.getWinner());
		for (Guild g : shiroShards.getGuilds()) {
			try {
				GuildDAO.getGuildById(g.getId());
			} catch (NoResultException e) {
				GuildDAO.addGuildToDB(g);
				Helper.logger(Main.class).info("Guild adicionada ao banco: " + g.getName());
			}
		}

		ConsoleListener console = new ConsoleListener();

		for (Emote emote : shiroShards.getEmotes()) {
			ShiroInfo.getEmoteCache().put(":" + emote.getName() + ":", emote.getId());
		}

		try {
			Paginator p = PaginatorBuilder.createPaginator()
					.setHandler(shiroShards)
					.shouldRemoveOnReact(true)
					.build();

			Pages.activate(p);
		} catch (InvalidHandlerException e) {
			Helper.logger(Main.class).error(e + " | " + e.getStackTrace()[0]);
		}

		console.start();

		for (GuildConfig guildConfig : GuildDAO.getAllGuildsWithButtons()) {
			Helper.refreshButtons(guildConfig);
		}

		System.gc();
		Helper.logger(Main.class).info("<----------END OF BOOT---------->");
		Helper.logger(Main.class).info("Estou pronta!");

		shiroShards.addEventListener(info.getShiroEvents());
		jbr.addEventListener(new JibrilEvents());
	}

	public static Activity getRandomActivity() {
		List<Activity> activities = new ArrayList<>() {{
			add(Activity.playing("Digite " + info.getDefaultPrefix() + "ajuda para ver meus comandos!"));
			add(Activity.playing("Nico nico nii!!"));
			add(Activity.listening(shiroShards.getGuilds().size() + " servidores, e isso ainda é só o começo!"));
			add(Activity.watching("No Game No Life pela 30ª vez, e ainda não enjoei de ver como eu atuo bem!"));
		}};

		return activities.get(Helper.rng(activities.size(), true));
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

	public static TwitchCommandManager getTwitchCommandManager() {
		return tCmdManager;
	}

	public static TwitchClient getTwitch() {
		return twitch;
	}

	public static TwitchEvents getTwitchManager() {
		return twitchManager;
	}

	public static boolean shutdown() {
		if (exiting) return false;
		exiting = true;

		SpringApplication.exit(spring);
		jbr.shutdownNow();
		shiroShards.shutdown();
		return true;
	}

	public static Relay getRelay() {
		return relay;
	}

	public static ShardManager getShiroShards() {
		return shiroShards;
	}

	public static JDA getJibril() {
		return jbr;
	}

	public static JDA getTet() {
		return tet;
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		if (e.getClass().getCanonicalName().equals(ContextException.class.getCanonicalName())) {
			return;
		}
		e.printStackTrace();
		Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
	}
}
