package com.kuuhaku.events;

import com.kuuhaku.Main;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.model.RelayBlockList;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.persistence.NoResultException;

public class JibrilEvents extends ListenerAdapter {

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (Main.getRelay().getRelayMap().containsValue(event.getChannel().getId()) && !event.getAuthor().isBot()) {
            if (RelayBlockList.check(event.getAuthor().getId())) {
                event.getMessage().delete().queue();
                event.getAuthor().openPrivateChannel().queue(c -> c.sendMessage(":x: | Você não pode mandar mensagens no chat global (bloqueado).").queue());
                return;
            }
            String[] msg = event.getMessage().getContentRaw().split(" ");
            for (int i = 0; i < msg.length; i++) {
                try {
                    if (Helper.findURL(msg[i]) && !MySQL.getTagById(event.getAuthor().getId()).isVerified())
                        msg[i] = "`LINK BLOQUEADO`";
                } catch (NoResultException e) {
                    if (Helper.findURL(msg[i])) msg[i] = "`LINK BLOQUEADO`";
                }
            }
            if (String.join(" ", msg).length() < 2048) {
                try {
                    if (MySQL.getTagById(event.getAuthor().getId()).isVerified() && event.getMessage().getAttachments().size() > 0) {
                        try {
                            Main.getRelay().relayMessage(String.join(" ", msg), event.getMember(), event.getGuild(), event.getMessage().getAttachments().get(0).getUrl());
                        } catch (Exception e) {
                            Main.getRelay().relayMessage(String.join(" ", msg), event.getMember(), event.getGuild(), null);
                        }
                    }
                } catch (NoResultException e) {
                    Main.getRelay().relayMessage(String.join(" ", msg), event.getMember(), event.getGuild(), null);
                }
            } else event.getChannel().sendMessage(":x: | Mensagem muito longa! (Max. 2048 letras)").queue();
        }
    }
}
