package com.rdx.events.guild;

import com.rdx.utils.SQLite;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.sql.SQLException;

public class GuildEvents extends ListenerAdapter {

    @Override//removeGuildFromDB
    public void onGuildJoin(GuildJoinEvent event) {
        try {
            SQLite.addGuildToDB(event.getGuild());
        } catch (SQLException err) { err.printStackTrace(); }
        System.out.println("Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".");
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        try {
            SQLite.removeGuildFromDB(event.getGuild());
        } catch (SQLException err) { err.printStackTrace(); }
        System.out.println("Acabei de sair do servidor \"" + event.getGuild().getName() + "\".");
    }
}