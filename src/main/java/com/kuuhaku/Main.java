package com.kuuhaku;/*
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

import com.kuuhaku.controller.MySQL.BackupDAO;
import com.kuuhaku.controller.MySQL.ExceedDAO;
import com.kuuhaku.controller.Relay;
import com.kuuhaku.controller.SQLite.GuildDAO;
import com.kuuhaku.controller.SQLite.Manager;
import com.kuuhaku.events.JibrilEvents;
import com.kuuhaku.events.ScheduledEvents;
import com.kuuhaku.events.TetEvents;
import com.kuuhaku.events.guild.GuildEvents;
import com.kuuhaku.events.guild.GuildUpdateEvents;
import com.kuuhaku.managers.CommandManager;
import com.kuuhaku.managers.RPGCommandManager;
import com.kuuhaku.model.DataDump;
import com.kuuhaku.model.Profile;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import javax.persistence.NoResultException;
import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


public class Main implements Thread.UncaughtExceptionHandler {

	private static ShiroInfo info;
	private static Relay relay;
	private static CommandManager cmdManager;
	private static RPGCommandManager rpgCmdManager;
	private static JDA api;
	private static JDA jbr;
	private static JDA tet;
	private static String[] arguments;

	public static void main(String[] args) throws Exception {
		Thread.setDefaultUncaughtExceptionHandler(new Main());
		info = new ShiroInfo();
		relay = new Relay();

		cmdManager = new CommandManager();
		rpgCmdManager = new RPGCommandManager();

		JDA api = new JDABuilder(AccountType.BOT).setToken(info.getBotToken()).build().awaitReady();
		JDA jbr = new JDABuilder(AccountType.BOT).setToken(System.getenv("JIBRIL_TOKEN")).build().awaitReady();
		JDA tet = new JDABuilder(AccountType.BOT).setToken(System.getenv("TET_TOKEN")).build().awaitReady();
		info.setAPI(api);
		Main.api = api;
		Main.jbr = jbr;
		Main.tet = tet;

		api.getPresence().setActivity(Activity.playing("Iniciando..."));
		jbr.getPresence().setActivity(Activity.playing("Iniciando..."));
		tet.getPresence().setActivity(Activity.playing("Iniciando..."));

		info.setStartTime(Instant.now().getEpochSecond());
		Helper.logger(Main.class).info("Criada pool de compilação: " + info.getPool().getCorePoolSize() + " espaços alocados");

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		ge.registerFont(Profile.FONT);

		Manager.connect();
		if (com.kuuhaku.controller.SQLite.BackupDAO.restoreData(BackupDAO.getData()))
			Helper.logger(Main.class).info("Dados recuperados com sucesso!");
		else Helper.logger(Main.class).error("Erro ao recuperar dados.");

		new ScheduledEvents();

		AudioSourceManagers.registerRemoteSources(getInfo().getApm());
		AudioSourceManagers.registerLocalSource(getInfo().getApm());

		finishStartUp();
		arguments = args;
	}

	private static void finishStartUp() {
		api.getPresence().setActivity(getRandomActivity());
		jbr.getPresence().setActivity(Activity.listening("as mensagens de " + relay.getRelayMap().size() + " servidores!"));
		tet.getPresence().setActivity(Activity.playing(" em diversos mundos espalhados em " + tet.getGuilds().size() + " servidores!"));
		getInfo().setWinner(ExceedDAO.getWinner());
		Main.getInfo().getAPI().getGuilds().forEach(g -> {
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
		tet.addEventListener(new TetEvents());
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

	public static String[] getArgs() {
		return arguments;
	}

	public static CommandManager getCommandManager() {
		return cmdManager;
	}

	public static RPGCommandManager getRPGCommandManager() {
		return rpgCmdManager;
	}

	public static void shutdown() {
		BackupDAO.dumpData(new DataDump(com.kuuhaku.controller.SQLite.BackupDAO.getCADump(), com.kuuhaku.controller.SQLite.BackupDAO.getGuildDump()));
		Helper.logger(Main.class).info("Respostas/Guilds salvos com sucesso!");
		BackupDAO.dumpData(new DataDump(com.kuuhaku.controller.SQLite.BackupDAO.getMemberDump()));
		Helper.logger(Main.class).info("Membros salvos com sucesso!");
		Manager.disconnect();
		tet.shutdown();
		jbr.shutdown();
		api.shutdown();
		Helper.logger(Main.class).info("Fui desligada.");
	}

	public static Relay getRelay() {
		return relay;
	}

	public static JDA getJibril() {
		return jbr;
	}

	public static JDA getTet() {
		return tet;
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		if (e.getClass().getCanonicalName().equals(ErrorResponseException.class.getCanonicalName())) {
			return;
		}
		Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
	}
}
