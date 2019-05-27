package com.kuuhaku;

import com.kuuhaku.events.generic.GenericMessageEvents;
import com.kuuhaku.events.guild.GuildEvents;
import com.kuuhaku.utils.SQLite;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import com.kuuhaku.events.JDAEvents;
import com.kuuhaku.managers.CommandManager;
import com.kuuhaku.utils.ShiroInfo;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;



public class Main {

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
        finishStartUp();
    }

    private static void finishStartUp() {
        api.getPresence().setGame(getRandomGame());
    }

    private static Game getRandomGame() {
        ArrayList<Game> games = new ArrayList<>();

        games.add(Game.playing("Digite !ajuda para ver meus comandos!"));
        games.add(Game.streaming("Na conta do meu Nii-chan sem ele saber!", "https://twitch.tv/kuuhaku_otgmz"));
        games.add(Game.playing("Nico nico nii!!"));
        games.add(Game.listening(api.getGuilds().size() + " servidores, e isso ainda é só o começo!"));
        games.add(Game.watching("No Game No Life pela 13ª vez, e ainda não enjoei de ver como eu atuo bem!"));

        return games.get((int) (Math.random() * games.size()));
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

}
