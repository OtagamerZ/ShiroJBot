package com.kuuhaku.events;

import com.kuuhaku.Main;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.Member;
import com.kuuhaku.model.RelayBlockList;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.imageio.ImageIO;
import javax.persistence.NoResultException;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class JibrilEvents extends ListenerAdapter {

	@Override//removeGuildFromDB
	public void onGuildJoin(GuildJoinEvent event) {
		Main.getInfo().getDevelopers().forEach(d -> Main.getJibril().getUserById(d).openPrivateChannel().queue(c -> {
			String msg = "Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".";
			c.sendMessage(msg).queue();
		}));
		Helper.log(this.getClass(), LogLevel.INFO, "Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".");
	}

	@Override
	public void onGuildLeave(GuildLeaveEvent event) {
		Main.getInfo().getDevelopers().forEach(d -> Main.getJibril().getUserById(d).openPrivateChannel().queue(c -> {
			String msg = "Acabei de sair do servidor \"" + event.getGuild().getName() + "\".";
			c.sendMessage(msg).queue();
		}));
		Helper.log(this.getClass(), LogLevel.INFO, "Acabei de sair do servidor \"" + event.getGuild().getName() + "\".");
	}

	@Override
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
		if (event.getAuthor() == Main.getJibril().getUserById(Main.getInfo().getNiiChan())) return;
		EmbedBuilder eb = new EmbedBuilder();

		eb.setAuthor(event.getAuthor().getName(), event.getAuthor().getAvatarUrl());
		eb.setFooter(LocalDateTime.now().atOffset(ZoneOffset.ofHours(-3)).format(DateTimeFormatter.ofPattern("HH:mm / dd/MMM/yyyy")), null);
		Main.getJibril().getUserById(Main.getInfo().getNiiChan()).openPrivateChannel().queue(c -> c.sendMessage(event.getMessage()).embed(eb.build()).queue());
	}

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (event.getMessage().getContentRaw().startsWith(SQLite.getGuildPrefix(event.getGuild().getId()))) return;

		Member mb = SQLite.getMemberById(event.getAuthor().getId() + event.getGuild().getId());

		if (mb.getMid() == null) SQLite.saveMemberMid(mb, event.getAuthor());

		if (Main.getRelay().getRelayMap().containsValue(event.getChannel().getId()) && !event.getAuthor().isBot()) {
			/*if (!mb.isRulesSent())
				event.getAuthor().openPrivateChannel().queue(c -> {
					try {
						c.sendMessage(introMsg()).queue(s1 ->
								c.sendMessage(rulesMsg()).queue(s2 ->
										c.sendMessage(finalMsg()).queue(s3 -> {
											mb.setRulesSent(true);
											SQLite.updateMemberSettings(mb);
										})));
					} catch (ErrorResponseException ignore) {
					}
				});*/
			if (RelayBlockList.check(event.getAuthor().getId())) {
				if (!SQLite.getGuildById(event.getGuild().getId()).isLiteMode()) event.getMessage().delete().queue();
				event.getAuthor().openPrivateChannel().queue(c -> {
					try {
						String s = ":x: | Você não pode mandar mensagens no chat global (bloqueado).";
						c.getHistory().retrievePast(20).queue(h -> {
							if (h.stream().noneMatch(m -> m.getContentRaw().equalsIgnoreCase(s)))
								c.sendMessage(s).queue();
						});
					} catch (ErrorResponseException ignore) {
					}
				});
				return;
			}
			String[] msg = event.getMessage().getContentRaw().split(" ");
			for (int i = 0; i < msg.length; i++) {
				try {
					if (Helper.findURL(msg[i]) && !MySQL.getTagById(event.getAuthor().getId()).isVerified())
						msg[i] = "`LINK BLOQUEADO`";
					if (Helper.findMentions(msg[i]))
						msg[i] = "`EVERYONE/HERE BLOQUEADO`";
				} catch (NoResultException e) {
					if (Helper.findURL(msg[i])) msg[i] = "`LINK BLOQUEADO`";
				}
			}
			if (String.join(" ", msg).length() < 2000) {
				try {
					if (MySQL.getTagById(event.getAuthor().getId()).isVerified() && event.getMessage().getAttachments().size() > 0) {
						try {
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							ImageIO.write(ImageIO.read(Helper.getImage(event.getMessage().getAttachments().get(0).getUrl())), "png", baos);
							Main.getRelay().relayMessage(event.getMessage(), String.join(" ", msg), event.getMember(), event.getGuild(), baos);
						} catch (Exception e) {
							Main.getRelay().relayMessage(event.getMessage(), String.join(" ", msg), event.getMember(), event.getGuild(), null);
						}
						return;
					}
					Main.getRelay().relayMessage(event.getMessage(), String.join(" ", msg), event.getMember(), event.getGuild(), null);
				} catch (NoResultException e) {
					Main.getRelay().relayMessage(event.getMessage(), String.join(" ", msg), event.getMember(), event.getGuild(), null);
				}
			}
		}
	}

	private static String introMsg() {
		return "__**Olá, sou Jibril, a gerenciadora do chat global!**__\n" +
				"Pera, o que? Você não sabe o que é o chat global?? Bem, vou te explicar!\n\n" +
				"O chat global (ou relay) é uma criação original de meu mestre KuuHaKu, ele une todos os servidores em que estou em um único canal de texto. " +
				"Assim, todos os servidores participantes terão um fluxo de mensagens a todo momento, quebrando aquele \"gelo\" que muitos servidores pequenos possuem";
	}

	private static String rulesMsg() {
		return "__**Mas existem regras, viu?**__\n" +
				"Como todo chat, para mantermos um ambiente saudável e amigável são necessárias regras.\n\n" +
				"O chat global possue suas próprias regras, além daquelas do servidor atual, que são:\n" +
				"1 - SPAM ou flood é proibido, pois além de ser desnecessário faz com que eu fique lenta;" +
				"2 - Links e imagens são bloqueadas, você não será punido por elas pois elas não serão enviadas;" +
				"3 - Avatares indecentes serão bloqueados 3 vezes antes de te causar um bloqueio no chat global;" +
				"4 - Os bloqueios são temporários, todos serão desbloqueados às 00:00h e 12:00h. Mas o terceiro bloqueio é permanente, você NÃO será desbloqueado de um permanente.";
	}

	private static String finalMsg() {
		return "__**E é isso, seja bem-vindo(a) ao grande chat global!**__\n" +
				"Se tiver dúvidas, denúncias ou sugestões, basta me enviar uma mensagem neste canal privado, ou usar os comando `bug` (feedback) ou `report` (denúncia).";
	}
}
