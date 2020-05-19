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

import com.github.ygimenez.method.Pages;
import com.kuuhaku.controller.Relay;
import com.kuuhaku.controller.Sweeper;
import com.kuuhaku.controller.postgresql.BackupDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.controller.sqlite.Manager;
import com.kuuhaku.events.JibrilEvents;
import com.kuuhaku.events.ScheduledEvents;
import com.kuuhaku.events.guild.GuildEvents;
import com.kuuhaku.events.guild.GuildUpdateEvents;
import com.kuuhaku.handlers.api.Application;
import com.kuuhaku.handlers.api.websocket.WebSocketConfig;
import com.kuuhaku.managers.CommandManager;
import com.kuuhaku.model.common.DataDump;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import io.socket.client.IO;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ContextException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.springframework.boot.SpringApplication;

import javax.persistence.NoResultException;
import java.net.BindException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Executors;

public class Main implements Thread.UncaughtExceptionHandler {

	private static ShiroInfo info;
	private static Relay relay;
	private static CommandManager cmdManager;
	private static JDA api;
	private static JDA jbr;
	public static String[] kill = new String[2];

	public static void main(String[] args) throws Exception {
		Helper.logger(Main.class).info("\nShiro J. Bot  Copyright (C) 2020 Yago Gimenez (KuuHaKu)\n" +
				"This program comes with ABSOLUTELY NO WARRANTY\n" +
				"This is free software, and you are welcome to redistribute it under certain conditions\n" +
				"See license for more information regarding redistribution conditions");
		Thread.setDefaultUncaughtExceptionHandler(new Main());
		info = new ShiroInfo();
		relay = new Relay();

		cmdManager = new CommandManager();

		EnumSet<GatewayIntent> intents = GatewayIntent.getIntents(GatewayIntent.DEFAULT);

		JDA api = JDABuilder.create(intents)
				.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
				.setToken(info.getBotToken())
				.setChunkingFilter(ChunkingFilter.ALL)
				.disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS)
				.setMemberCachePolicy(MemberCachePolicy.ALL)
				.setBulkDeleteSplittingEnabled(false)
				.setAudioSendFactory(new NativeAudioSendFactory())
				.build()
				.awaitReady();

		JDA jbr = JDABuilder.create(intents)
				.setToken(System.getenv("JIBRIL_TOKEN"))
				.setChunkingFilter(ChunkingFilter.NONE)
				.disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS)
				.setMemberCachePolicy(MemberCachePolicy.NONE)
				.setBulkDeleteSplittingEnabled(false)
				.build()
				.awaitReady();

		info.setAPI(api);
		Main.api = api;
		Main.jbr = jbr;

		api.getPresence().setActivity(Activity.playing("Iniciando..."));
		jbr.getPresence().setActivity(Activity.playing("Iniciando..."));

		info.setStartTime(Instant.now().getEpochSecond());
		Helper.logger(Main.class).info("Criada pool de compilação: " + info.getPool().getCorePoolSize() + " espaços alocados");

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

		SpringApplication.run(Application.class, args);

		boolean apiOnline = false;
		int tries = 0;
		while (!apiOnline) {
			try {
				info.setServer(new WebSocketConfig(8000 + tries));
				info.setClient(IO.socket("http://localhost/")).connect();
				apiOnline = true;
			} catch (URISyntaxException | BindException e) {
				Helper.logger(Main.class).error("Erro ao conectar client: " + e + " | " + e.getStackTrace()[0]);
				try {
					Thread.sleep(4000);
					tries++;
				} catch (InterruptedException ignore) {
				}
			}
		}
		finishStartUp();
	}

	private static void finishStartUp() {
		api.getPresence().setActivity(getRandomActivity());
		jbr.getPresence().setActivity(Activity.listening("as mensagens de " + relay.getRelayMap().size() + " servidores!"));
		getInfo().setWinner(ExceedDAO.getWinner());
		api.getGuilds().forEach(g -> {
			try {
				GuildDAO.getGuildById(g.getId());
			} catch (NoResultException e) {
				GuildDAO.addGuildToDB(g);
				Helper.logger(Main.class).info("Guild adicionada ao banco: " + g.getName());
			}
		});
		api.addEventListener(Main.getInfo().getShiroEvents());
		api.addEventListener(new GuildEvents());
		api.addEventListener(new GuildUpdateEvents());
		jbr.addEventListener(new JibrilEvents());

		Pages.activate(api);

		GuildDAO.getAllGuilds().forEach(Helper::refreshButtons);

		Helper.logger(Main.class).info("<----------END OF BOOT---------->");
		Helper.logger(Main.class).info("Estou pronta!");
	}

	public static Activity getRandomActivity() {
		List<Activity> activities = new ArrayList<Activity>() {{
			add(Activity.playing("Digite " + info.getDefaultPrefix() + "ajuda para ver meus comandos!"));
			add(Activity.streaming("Na conta do meu Nii-chan sem ele saber!", "https://twitch.tv/kuuhaku_otgmz"));
			add(Activity.playing("Nico nico nii!!"));
			add(Activity.listening(api.getGuilds().size() + " servidores, e isso ainda é só o começo!"));
			add(Activity.watching("No Game No Life pela 13ª vez, e ainda não enjoei de ver como eu atuo bem!"));
		}};

		return activities.get(Helper.rng(activities.size()));
	}

	public static ShiroInfo getInfo() {
		return info;
	}

	public static CommandManager getCommandManager() {
		return cmdManager;
	}

	public static void shutdown() {
		int sweeper = Sweeper.mark();
		TextChannel chn = api.getTextChannelById(kill[0]);
		assert chn != null;
		Message msg = chn.retrieveMessageById(kill[1]).complete();

		Helper.logger(Main.class).info(sweeper + " entradas dispensáveis encontradas!");
		msg.editMessage(msg.getContentRaw() + "\n:white_check_mark: -> " + sweeper + " entradas dispensáveis encontradas!").queue();

		BackupDAO.dumpData(new DataDump(com.kuuhaku.controller.sqlite.BackupDAO.getCADump(), com.kuuhaku.controller.sqlite.BackupDAO.getGuildDump(), com.kuuhaku.controller.sqlite.BackupDAO.getAppUserDump(), com.kuuhaku.controller.sqlite.BackupDAO.getKawaigotchiDump(), com.kuuhaku.controller.sqlite.BackupDAO.getPoliticalStateDump()));
		Helper.logger(Main.class).info("Respostas/Guilds/Usuários/Kawaigotchis/Exceeds salvos com sucesso!");
		msg.editMessage(msg.getContentRaw() + "\n:white_check_mark: -> Respostas/Guilds/Usuários/Kawaigotchis salvos com sucesso!").queue();

		BackupDAO.dumpData(new DataDump(com.kuuhaku.controller.sqlite.BackupDAO.getMemberDump()));
		Helper.logger(Main.class).info("Membros salvos com sucesso!");
		msg.editMessage(msg.getContentRaw() + "\n:white_check_mark: -> Membros salvos com sucesso!").queue();

		Sweeper.sweep();
		Manager.disconnect();
		jbr.shutdown();
		msg.editMessage(msg.getContentRaw() + "\n:white_check_mark: -> Fui desligada!").queue();

		api.shutdown();
		Helper.logger(Main.class).info("Fui desligada.");
	}

	public static Relay getRelay() {
		return relay;
	}

	public static JDA getJibril() {
		return jbr;
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
