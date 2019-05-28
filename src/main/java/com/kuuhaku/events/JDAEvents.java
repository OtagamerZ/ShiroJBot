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

package com.kuuhaku.events;

import com.kuuhaku.Main;
import com.kuuhaku.command.Reactions.HugReaction;
import com.kuuhaku.model.guildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.controller.SQLite;
import net.dv8tion.jda.client.events.relationship.FriendRequestReceivedEvent;
import net.dv8tion.jda.client.events.relationship.FriendRequestSentEvent;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class JDAEvents extends ListenerAdapter {

    @Override
    public void onReady(ReadyEvent event) {
        try {
            System.out.println("Estou pronta!");
        } catch (Exception e) {
            System.out.println("Erro ao inicializar bot: " + e);
        }
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        Message message = event.getChannel().getMessageById(event.getMessageId()).complete();
        if (message.getAuthor() == Main.getInfo().getSelfUser()) {
            if (message.getContentRaw().contains("abraçou")) {
                User author = message.getMentionedUsers().get(0);
                MessageChannel channel = message.getChannel();

                new HugReaction(true).execute(author, null, null, null, message, channel, null, null, null);
            }
        }
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        guildConfig gc = new guildConfig();
        gc.setGuildId(event.getGuild().getId());
        SQLite.addGuildToDB(event.getGuild());
        try {
            Helper.sendPM(event.getGuild().getOwner().getUser(), "Obrigada por me adicionar ao seu servidor!");
        } catch (Exception err) {
            TextChannel dch = event.getGuild().getDefaultChannel();
            if (dch != null) {
                if (dch.canTalk()) {
                    dch.sendMessage("Obrigada por me adicionar ao seu servidor!").queue();
                }
            }
        }
    }

    /*@Override
	public void onReconnect(ReconnectedEvent event) {
		MainANT.getInfo().getLogChannel().sendMessage(DiscordHelper.getCustomEmoteMention(MainANT.getInfo().getGuild(), "kawaii") + " | Fui desparalizada!").queue();
	}*/

    @Override
    public void onShutdown(ShutdownEvent event) {
        //com.kuuhaku.MainANT.getInfo().getLogChannel().sendMessage(DiscordHelper.getCustomEmoteMention(com.kuuhaku.MainANT.getInfo().getGuild(), "choro") + " | Nunca vos esquecerei... Faleci! " + DiscordHelper.getCustomEmoteMention(com.kuuhaku.MainANT.getInfo().getGuild(), "bruh")).queue();
        System.out.println();
    }
	
	/*@Override
	public void onDisconnect(DisconnectEvent event) {
		com.kuuhaku.MainANT.getInfo().getLogChannel().sendMessage(DiscordHelper.getCustomEmoteMention(com.kuuhaku.MainANT.getInfo().getGuild(), "kms") + " | Fui paraliz-... " + DiscordHelper.getCustomEmoteMention(com.kuuhaku.MainANT.getInfo().getGuild(), "yeetus")).queue();
	}*/
}
