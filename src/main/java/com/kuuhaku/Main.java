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
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import javax.persistence.NoResultException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


public class Main implements JobListener {

	private static ShiroInfo info;
	private static Relay relay;
	private static CommandManager cmdManager;
	private static JDA api;
	private static JDA jbr;
	private static JobDetail backup;
	private static Scheduler sched;

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
		jbr.getPresence().setGame(Game.listening("as mensagens de " + relay.getRelayArray().size() + " servidores!"));

		api.addEventListener(new JDAEvents());
		api.addEventListener(new GuildEvents());
		api.addEventListener(new GuildUpdateEvents());
		jbr.addEventListener(new JibrilEvents());

		info.setStartTime(Instant.now().getEpochSecond());

		SQLite.connect();
		if (SQLite.restoreData(MySQL.getData())) System.out.println("Dados recuperados com sucesso!");
		else System.out.println("Erro ao recuperar dados.");

		try {
			if (backup == null) {
				backup = JobBuilder.newJob(ScheduledEvents.class).withIdentity("backup", "1").build();
			}
			Trigger cron = TriggerBuilder.newTrigger().withIdentity("backup", "1").withSchedule(CronScheduleBuilder.cronSchedule("0 0 0/1 ? * * *")).build();
			SchedulerFactory sf = new StdSchedulerFactory();
			try {
				sched = sf.getScheduler();
				sched.scheduleJob(backup, cron);
			} catch (Exception ignore) {
			} finally {
				sched.start();
				System.out.println("Cronograma inicializado com sucesso!");
			}
		} catch (SchedulerException e) {
			System.out.println("Erro ao inicializar cronograma: " + e);
		}

		finishStartUp();
	}

	private static void finishStartUp() {
		api.getPresence().setGame(getRandomGame());
		Main.getInfo().getAPI().getGuilds().forEach(g -> {
			try {
				SQLite.getGuildById(g.getId());
			} catch (NoResultException e) {
				SQLite.addGuildToDB(g);
				System.out.println("Guild adicionada ao banco: " + g.getName());
			}
		});
		Helper.cls();
		System.out.println("Estou pronta!");
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

	public static void shutdown() throws SQLException {
		MySQL.dumpData(new DataDump(SQLite.getCADump(), SQLite.getMemberDump(), SQLite.getGuildDump()));
		SQLite.disconnect();
		api.shutdown();
		System.out.println("Fui desligada.");
	}

	public static Relay getRelay() {
		return relay;
	}

	public static JDA getJibril() {
		return jbr;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public void jobToBeExecuted(JobExecutionContext context) {

	}

	@Override
	public void jobExecutionVetoed(JobExecutionContext context) {

	}

	@Override
	public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
		System.out.println("Programação executada em " + context.getFireTime() + ".\nPróxima execução em " + context.getNextFireTime());
	}
}
