package com.kuuhaku.events;

import com.kuuhaku.Main;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.RelayBlockList;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class JibrilEvents extends ListenerAdapter {
	private Thread relayThread;

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (relayThread == null) {
			relayThread = new Thread() {
				boolean done = false;

				@Override
				public void run() {
					if (event.getChannel().getId().equals(SQLite.getGuildCanalRelay(event.getGuild().getId())) && !event.getAuthor().isBot()) {
						while (!done) {
							done = doRelay(event);
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						interrupt();
					}
				}
			};
		}

		if (relayThread.isAlive()) relayThread.run();
		else relayThread.start();
	}

	private boolean doRelay(GuildMessageReceivedEvent event) {
		try {
			if (RelayBlockList.check(event.getAuthor().getId())) {
				event.getMessage().delete().queue();
				event.getAuthor().openPrivateChannel().queue(c -> c.sendMessage(":x: | Você não pode mandar mensagens no chat global (bloqueado).").queue());
				return true;
			}
			String[] msg = event.getMessage().getContentRaw().split(" ");
			for (int i = 0; i < msg.length; i++) {
				if (Helper.findURL(msg[i])) msg[i] = "`LINK BLOQUEADO`";
			}
			if (String.join(" ", msg).length() < 2048)
				Main.getRelay().relayMessage(String.join(" ", msg), event.getMember(), event.getGuild());
			else event.getChannel().sendMessage(":x: | Mensagem muito longa! (Max. 2048 letras)").queue();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
