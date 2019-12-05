package com.kuuhaku.events;

import com.kuuhaku.Main;
import com.kuuhaku.controller.MySQL.Tag;
import com.kuuhaku.controller.SQLiteOld;
import com.kuuhaku.model.Member;
import com.kuuhaku.model.RelayBlockList;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.persistence.NoResultException;
import java.io.ByteArrayOutputStream;
import java.util.Objects;

public class JibrilEvents extends ListenerAdapter {

	@Override//removeGuildFromDB
	public void onGuildJoin(@NotNull GuildJoinEvent event) {
		try {
			Helper.sendPM(Objects.requireNonNull(event.getGuild().getOwner()).getUser(), "Obrigada por me adicionar ao seu servidor, utilize `s!settings crelay #CANAL` para definir o canal que usarei para transmitir as mensagens globais!\n\nDúvidas? Pergunte-me diretamente e um de meus desenvolvedores responderá assim que possível!");
		} catch (Exception err) {
			TextChannel dch = event.getGuild().getDefaultChannel();
			if (dch != null) {
				if (dch.canTalk()) {
					dch.sendMessage("Obrigada por me adicionar ao seu servidor, utilize `s!settings crelay #CANAL` para definir o canal que usarei para transmitir as mensagens globais!\n\nDúvidas? Pergunte-me diretamente e um de meus desenvolvedores responderá assim que possível!").queue();
				}
			}
		}

		Main.getInfo().getDevelopers().forEach(d -> Objects.requireNonNull(Main.getJibril().getUserById(d)).openPrivateChannel().queue(c -> {
			String msg = "Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".";
			c.sendMessage(msg).queue();
		}));
		Helper.logger(this.getClass()).info("Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".");
	}

	@Override
	public void onGuildLeave(GuildLeaveEvent event) {
		Main.getInfo().getDevelopers().forEach(d -> Objects.requireNonNull(Main.getJibril().getUserById(d)).openPrivateChannel().queue(c -> {
			String msg = "Acabei de sair do servidor \"" + event.getGuild().getName() + "\".";
			c.sendMessage(msg).queue();
		}));
		Helper.logger(this.getClass()).info("Acabei de sair do servidor \"" + event.getGuild().getName() + "\".");
	}

	@Override
	public void onPrivateMessageReceived(@NotNull PrivateMessageReceivedEvent event) {
		Main.getInfo().getShiroEvents().onPrivateMessageReceived(event);
	}

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		try {
			if (event.getMessage().getContentRaw().startsWith(SQLiteOld.getGuildPrefix(event.getGuild().getId()))) return;

			if (Main.getRelay().getRelayMap().containsValue(event.getChannel().getId()) && !event.getAuthor().isBot()) {
				Member mb;
				try {
					mb = SQLiteOld.getMemberById(event.getAuthor().getId() + event.getGuild().getId());
				} catch (NoResultException e) {
					assert event.getMember() != null;
					SQLiteOld.addMemberToDB(event.getMember());
					mb = SQLiteOld.getMemberById(event.getAuthor().getId() + event.getGuild().getId());
				}
				if (mb.getMid() == null) SQLiteOld.saveMemberMid(mb, event.getAuthor());

				if (event.getMessage().getContentRaw().trim().equals("<@" + Main.getJibril().getSelfUser().getId() + ">")) {
					event.getChannel().sendMessage("Oi? Ah, você quer saber meus comandos né?\nBem, eu não sou uma bot de comandos, eu apenas gerencio o chat global, que pode ser definido pelos moderadores desse servidor usando `" + SQLiteOld.getGuildPrefix(event.getGuild().getId()) + "settings crelay #CANAL`!").queue();
					return;
				}

				if (!mb.isRulesSent())
					try {
						Member finalMb = mb;
						event.getAuthor().openPrivateChannel().queue(c -> c.sendMessage(introMsg()).queue(s1 ->
								c.sendMessage(rulesMsg()).queue(s2 ->
										c.sendMessage(finalMsg()).queue(s3 -> {
											finalMb.setRulesSent(true);
											SQLiteOld.updateMemberSettings(finalMb);
											com.kuuhaku.controller.MySQL.Member.saveMemberToBD(finalMb);
										}))));
					} catch (ErrorResponseException ignore) {
					}
				if (RelayBlockList.check(event.getAuthor().getId())) {
					if (!SQLiteOld.getGuildById(event.getGuild().getId()).isLiteMode())
						event.getMessage().delete().queue();
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
						if (Helper.findURL(msg[i]) && !Tag.getTagById(event.getAuthor().getId()).isVerified())
							msg[i] = "`LINK BLOQUEADO`";
						if (Helper.findMentions(msg[i]))
							msg[i] = "`EVERYONE/HERE BLOQUEADO`";
					} catch (NoResultException e) {
						if (Helper.findURL(msg[i])) msg[i] = "`LINK BLOQUEADO`";
					}
				}
				if (String.join(" ", msg).length() < 2000) {
					net.dv8tion.jda.api.entities.Member m = event.getMember();
					assert m != null;
					try {
						if (Tag.getTagById(event.getAuthor().getId()).isVerified() && event.getMessage().getAttachments().size() > 0) {
							try {
								ByteArrayOutputStream baos = new ByteArrayOutputStream();
								ImageIO.write(ImageIO.read(Helper.getImage(event.getMessage().getAttachments().get(0).getUrl())), "png", baos);
								Main.getRelay().relayMessage(event.getMessage(), String.join(" ", msg), m, event.getGuild(), baos);
							} catch (Exception e) {
								Main.getRelay().relayMessage(event.getMessage(), String.join(" ", msg), m, event.getGuild(), null);
							}
							return;
						}
						Main.getRelay().relayMessage(event.getMessage(), String.join(" ", msg), m, event.getGuild(), null);
					} catch (NoResultException e) {
						Main.getRelay().relayMessage(event.getMessage(), String.join(" ", msg), m, event.getGuild(), null);
					}
				}
			}
		} catch (ErrorResponseException e) {
			Helper.logger(this.getClass()).error(e.getErrorCode() + ": " + e + " | " + e.getStackTrace()[0]);
		}
	}

	private static String introMsg() {
		return "__**Olá, sou Jibril, a gerenciadora do chat global!**__\n" +
				"Pera, o que? Você não sabe o que é o chat global?? Bem, vou te explicar!\n\n" +
				"O chat global (ou relay) é uma criação de meu mestre KuuHaKu, ele une todos os servidores em que estou em um único canal de texto. " +
				"Assim, todos os servidores participantes terão um fluxo de mensagens a todo momento, quebrando aquele \"gelo\" que muitos servidores pequenos possuem\n";
	}

	private static String rulesMsg() {
		return "__**Mas existem regras, viu?**__\n" +
				"Como todo chat, para mantermos um ambiente saudável e amigável são necessárias regras.\n\n" +
				"O chat global possue suas próprias regras, além daquelas do servidor atual, que são:\n" +
				"1 - SPAM ou flood é proibido, pois além de ser desnecessário faz com que eu fique lenta;\n" +
				"2 - Links e imagens são bloqueadas, você não será punido por elas pois elas não serão enviadas;\n" +
				"3 - Avatares indecentes serão bloqueados 3 vezes antes de te causar um bloqueio no chat global;\n" +
				"4 - Os bloqueios são temporários, todos serão desbloqueados às 00:00h e 12:00h. Mas o terceiro bloqueio é permanente, você NÃO será desbloqueado de um permanente.\n";
	}

	private static String finalMsg() {
		return "__**E é isso, seja bem-vindo(a) ao grande chat global!**__\n\n" +
				"Se tiver dúvidas, denúncias ou sugestões, basta me enviar uma mensagem neste canal privado, ou usar os comando `bug` (feedback) ou `report` (denúncia).";
	}
}
