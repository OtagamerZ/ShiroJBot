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

import com.kuuhaku.controller.MySQL;
import com.kuuhaku.controller.Relay;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.events.JDAEvents;
import com.kuuhaku.events.JibrilEvents;
import com.kuuhaku.events.ScheduledEvents;
import com.kuuhaku.events.guild.GuildEvents;
import com.kuuhaku.events.guild.GuildUpdateEvents;
import com.kuuhaku.managers.CommandManager;
import com.kuuhaku.model.DataDump;
import com.kuuhaku.model.Profile;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import com.kuuhaku.utils.ShiroInfo;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;

import javax.persistence.NoResultException;
import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


public class Main {

	private static ShiroInfo info;
	private static Relay relay;
	private static CommandManager cmdManager;
	private static JDA api;
	private static JDA jbr;

	public static void main(String[] args) throws Exception {
		info = new ShiroInfo();
		relay = new Relay();

		cmdManager = new CommandManager();

		JDA api = new JDABuilder(AccountType.BOT).setToken(info.getBotToken()).build().awaitReady();
		JDA jbr = new JDABuilder(AccountType.BOT).setToken(System.getenv("JIBRIL_TOKEN")).build().awaitReady();
		info.setAPI(api);
		Main.api = api;
		Main.jbr = jbr;

		api.getPresence().setGame(Game.playing("Iniciando..."));
		jbr.getPresence().setGame(Game.listening("as mensagens de " + relay.getRelayMap().size() + " servidores!"));

		api.addEventListener(new JDAEvents());
		api.addEventListener(new GuildEvents());
		api.addEventListener(new GuildUpdateEvents());
		jbr.addEventListener(new JibrilEvents());

		info.setStartTime(Instant.now().getEpochSecond());

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		ge.registerFont(Profile.FONT);

		SQLite.connect();
		if (SQLite.restoreData(MySQL.getData()))
			Helper.log(Main.class, LogLevel.INFO, "Dados recuperados com sucesso!");
		else Helper.log(Main.class, LogLevel.ERROR, "Erro ao recuperar dados.");

		new ScheduledEvents();

		AudioSourceManagers.registerRemoteSources(getInfo().getApm());
		AudioSourceManagers.registerLocalSource(getInfo().getApm());

		finishStartUp();
	}

	private static void finishStartUp() {
		api.getPresence().setGame(getRandomGame());
		Main.getInfo().getAPI().getGuilds().forEach(g -> {
			try {
				SQLite.getGuildById(g.getId());
			} catch (NoResultException e) {
				SQLite.addGuildToDB(g);
				Helper.log(Main.class, LogLevel.INFO, "Guild adicionada ao banco: " + g.getName());
			}
		});
		Helper.log(Main.class, LogLevel.INFO, "<----------END OF BOOT---------->");
		Helper.log(Main.class, LogLevel.INFO, "Estou pronta!");
		getInfo().setReady(true);
	}

	public static Game getRandomGame() {
		List<Game> games = new ArrayList<Game>() {{
			add(Game.playing("Digite " + info.getDefaultPrefix() + "ajuda para ver meus comandos!"));
			add(Game.streaming("Na conta do meu Nii-chan sem ele saber!", "https://twitch.tv/kuuhaku_otgmz"));
			add(Game.playing("Nico nico nii!!"));
			add(Game.listening(api.getGuilds().size() + " servidores, e isso ainda é só o começo!"));
			add(Game.watching("No Game No Life pela 13ª vez, e ainda não enjoei de ver como eu atuo bem!"));
		}};

		return games.get(Helper.rng(games.size()));
	}

	public static ShiroInfo getInfo() {
		return info;
	}

	public static CommandManager getCommandManager() {
		return cmdManager;
	}

	public static void shutdown() {
		MySQL.dumpData(new DataDump(SQLite.getCADump(), SQLite.getGuildDump()));
		Helper.log(Main.class, LogLevel.INFO, "Respostas/Guilds salvos com sucesso!");
		MySQL.dumpData(new DataDump(SQLite.getMemberDump()));
		Helper.log(Main.class, LogLevel.INFO, "Membros salvos com sucesso!");
		SQLite.disconnect();
		api.shutdown();
		Helper.log(Main.class, LogLevel.INFO, "Fui desligada.");
	}

	public static Relay getRelay() {
		return relay;
	}

	public static JDA getJibril() {
		return jbr;
	}
}
