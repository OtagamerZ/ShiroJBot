package com.kuuhaku.events;

import com.kuuhaku.Main;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.RelayBlockList;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.imageio.ImageIO;
import javax.persistence.NoResultException;
import java.io.ByteArrayOutputStream;

public class JibrilEvents extends ListenerAdapter {

	@Override//removeGuildFromDB
	public void onGuildJoin(GuildJoinEvent event) {
		Main.getInfo().getDevelopers().forEach(d -> Main.getInfo().getUserByID(d).openPrivateChannel().queue(c -> {
			String msg = "Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".";
			c.sendMessage(msg).queue();
		}));
		Helper.log(this.getClass(), LogLevel.INFO, "Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".");
	}

	@Override
	public void onGuildLeave(GuildLeaveEvent event) {
		Main.getInfo().getDevelopers().forEach(d -> Main.getInfo().getUserByID(d).openPrivateChannel().queue(c -> {
			String msg = "Acabei de sair do servidor \"" + event.getGuild().getName() + "\".";
			c.sendMessage(msg).queue();
		}));
		Helper.log(this.getClass(), LogLevel.INFO, "Acabei de sair do servidor \"" + event.getGuild().getName() + "\".");
	}

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
			if (String.join(" ", msg).length() < 2000) {
				try {
					event.getAuthor().openPrivateChannel().queue(c -> c.getMessageById(c.getLatestMessageId()).queue(m -> {
						String s = ":warning: | Cuidado, mensagens de SPAM podem fazer com que você seja bloqueado do chat global (isto é só um aviso!\nEsta mensagem não aparecerá novamente enquanto for a última mensagem.";
						if (!m.getContentRaw().equals(s)) c.sendMessage(s).queue();
					}));

					if (MySQL.getTagById(event.getAuthor().getId()).isVerified() && event.getMessage().getAttachments().size() > 0) {
						try {
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							ImageIO.write(ImageIO.read(Helper.getImage(event.getMessage().getAttachments().get(0).getUrl())), "png", baos);
							if (SQLite.getGuildById(event.getGuild().getId()).isLiteMode()) Main.getRelay().relayLite(event.getMessage(), String.join(" ", msg), event.getMember(), event.getGuild(), baos);
							else Main.getRelay().relayMessage(event.getMessage(), String.join(" ", msg), event.getMember(), event.getGuild(), baos);
						} catch (Exception e) {
							if (SQLite.getGuildById(event.getGuild().getId()).isLiteMode()) Main.getRelay().relayLite(event.getMessage(), String.join(" ", msg), event.getMember(), event.getGuild(), null);
							else Main.getRelay().relayMessage(event.getMessage(), String.join(" ", msg), event.getMember(), event.getGuild(), null);
						}
						return;
					}
					if (SQLite.getGuildById(event.getGuild().getId()).isLiteMode()) Main.getRelay().relayLite(event.getMessage(), String.join(" ", msg), event.getMember(), event.getGuild(), null);
					else Main.getRelay().relayMessage(event.getMessage(), String.join(" ", msg), event.getMember(), event.getGuild(), null);
				} catch (NoResultException e) {
					Main.getRelay().relayMessage(event.getMessage(), String.join(" ", msg), event.getMember(), event.getGuild(), null);
				} catch (ErrorResponseException ex) {
					if (Helper.compareWithValues(ex.getErrorCode(), 10008, 50007)) {
						event.getChannel().sendMessage(":x: | " + event.getAuthor().getAsMention() + ", você está com a opção \"Permitir mensagens diretas de membros do servidor.\" desligada, você não poderá mandar mensagens no chat global enquanto ela estiver desligada.\nPara habilitá-la, vá em `Configurações de privacidade -> Mensagens diretas` no menu do canto superior esquerdo, ao lado do nome do servidor.").queue();
						if (event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) event.getMessage().delete().queue();
					}
				}
			} else {
				event.getChannel().sendMessage(":x: | Mensagem muito longa! (Max. 2000 caractéres)").queue();
				if (event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) event.getMessage().delete().queue();
			}
		}
	}
}
