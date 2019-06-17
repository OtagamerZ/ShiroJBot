package com.kuuhaku.events;

import com.kuuhaku.Main;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.RelayBlockList;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class JibrilEvents extends ListenerAdapter {
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (event.getChannel().getId().equals(SQLite.getGuildCanalRelay(event.getGuild().getId()))) {
			if (RelayBlockList.check(event.getAuthor().getId())) {
				event.getMessage().delete().queue();
				event.getAuthor().openPrivateChannel().queue(c -> c.sendMessage(":x: | Você não pode mandar mensagens no chat global (bloqueado).").queue());
				return;
			}
			String[] msg = event.getMessage().getContentRaw().split(" ");
			for (int i = 0; i < msg.length; i++) {
				if (Helper.findURL(msg[i])) msg[i] = "`LINK BLOQUEADO`";
			}
			if (String.join(" ", msg).length() < 2048)
				Main.getRelay().relayMessage(String.join(" ", msg), event.getMember(), event.getGuild());
			else event.getChannel().sendMessage(":x: | Mensagem muito longa! (Max. 2048 letras)").queue();
		}
	}
}
