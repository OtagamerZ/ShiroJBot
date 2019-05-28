/*
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

package com.kuuhaku;

import com.kuuhaku.controller.MySQL;
import com.kuuhaku.events.JDAEvents;
import com.kuuhaku.events.generic.GenericMessageEvents;
import com.kuuhaku.events.guild.GuildEvents;
import com.kuuhaku.managers.CommandManager;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

import javax.persistence.NoResultException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class Main implements JobListener {

    private static ShiroInfo info;
    private static CommandManager cmdManager;
    private static JDA api;

    public static void main(String[] args) throws Exception {
        info = new ShiroInfo();

        cmdManager = new CommandManager();

        JDA api = new JDABuilder(AccountType.BOT).setToken(info.getToken()).build().awaitReady();
        info.setAPI(api);
        Main.api = api;

        api.addEventListener(new JDAEvents());
        api.addEventListener(new GuildEvents());
        api.addEventListener(new GenericMessageEvents());

        info.setStartTime(Instant.now().getEpochSecond());

        SQLite.connect();
        //MySQL.dumpData(null, null, null);
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
    }

    public static Game getRandomGame() {
        List<Game> games = new ArrayList<Game>() {{
            add(Game.playing("Digite !ajuda para ver meus comandos!"));
            add(Game.streaming("Na conta do meu Nii-chan sem ele saber!", "https://twitch.tv/kuuhaku_otgmz"));
            add(Game.playing("Nico nico nii!!"));
            add(Game.listening(api.getGuilds().size() + " servidores, e isso ainda é só o começo!"));
            add(Game.watching("No Game No Life pela 13ª vez, e ainda não enjoei de ver como eu atuo bem!"));
        }};

        return games.get(new Random().nextInt(games.size()));
    }

    public static ShiroInfo getInfo() {
        return info;
    }

    public static CommandManager getCommandManager() {
        return cmdManager;
    }

    public static void shutdown() throws SQLException {
        SQLite.disconnect();
        api.shutdown();
        System.out.println("Fui desligada.");
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
