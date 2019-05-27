package com.kuuhaku.events;

import net.dv8tion.jda.core.events.ReadyEvent;
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
	
	/*@Override
	public void onReconnect(ReconnectedEvent event) {
		MainANT.getInfo().getLogChannel().sendMessage(DiscordHelper.getCustomEmoteMention(MainANT.getInfo().getGuild(), "kawaii") + " | Fui desparalizada!").queue();
	}*/
	
	/*@Override
	public void onShutdown(ShutdownEvent event) {
		//com.kuuhaku.MainANT.getInfo().getLogChannel().sendMessage(DiscordHelper.getCustomEmoteMention(com.kuuhaku.MainANT.getInfo().getGuild(), "choro") + " | Nunca vos esquecerei... Faleci! " + DiscordHelper.getCustomEmoteMention(com.kuuhaku.MainANT.getInfo().getGuild(), "bruh")).queue();
	}
	
	@Override
	public void onDisconnect(DisconnectEvent event) {
		com.kuuhaku.MainANT.getInfo().getLogChannel().sendMessage(DiscordHelper.getCustomEmoteMention(com.kuuhaku.MainANT.getInfo().getGuild(), "kms") + " | Fui paraliz-... " + DiscordHelper.getCustomEmoteMention(com.kuuhaku.MainANT.getInfo().getGuild(), "yeetus")).queue();
	}*/

}
