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
import com.kuuhaku.command.Command;
import com.kuuhaku.command.commands.reactions.*;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.guildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.Music;
import de.androidpit.colorthief.ColorThief;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class JDAEvents extends ListenerAdapter {

	@Override
	public void onReady(@NotNull ReadyEvent event) {
		try {
			Helper.logger(this.getClass()).info("Estou pronta!");
		} catch (Exception e) {
			Helper.logger(this.getClass()).error("Erro ao inicializar bot: " + e);
		}
	}

	@Override
	public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
		if (Objects.requireNonNull(event.getMember()).getUser().isBot()) return;
		Message message = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
		if (Main.getInfo().getPolls().containsKey(message.getId())) {

			if (event.getReactionEmote().getName().equals("\uD83D\uDC4D"))
				Main.getInfo().getPolls().get(message.getId())[0]--;
			else if (event.getReactionEmote().getName().equals("\uD83D\uDC4E"))
				Main.getInfo().getPolls().get(message.getId())[1]--;
		}
	}

	@Override
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		if (event.getUser().isBot()) return;

		try {
			Message message = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
			if (Main.getInfo().getPolls().containsKey(message.getId())) {

				if (event.getReactionEmote().getName().equals("\uD83D\uDC4D"))
					Main.getInfo().getPolls().get(message.getId())[0]++;
				else if (event.getReactionEmote().getName().equals("\uD83D\uDC4E"))
					Main.getInfo().getPolls().get(message.getId())[1]++;
				else if (event.getReactionEmote().getName().equals("\u274C") && Objects.requireNonNull(message.getEmbeds().get(0).getTitle()).equals(":notepad_spiral: Enquete criada por " + Objects.requireNonNull(event.getMember()).getEffectiveName())) {
					Main.getInfo().getPolls().remove(message.getId());
					message.delete().queue();
				}
			}

			if (message.getAuthor() == Main.getInfo().getSelfUser() && message.getMentionedUsers().size() > 0) {
				if (event.getUser() == message.getMentionedUsers().get(1)) {
					if (message.getContentRaw().contains("abraçou")) {
						User author = message.getMentionedUsers().get(0);
						MessageChannel channel = message.getChannel();

						new HugReaction(true).execute(author, null, null, null, message, channel, null, null, null);
					} else if (message.getContentRaw().contains("beijou")) {
						User author = message.getMentionedUsers().get(0);
						MessageChannel channel = message.getChannel();

						new KissReaction(true).execute(author, null, null, null, message, channel, null, null, null);
					} else if (message.getContentRaw().contains("fez cafuné em")) {
						User author = message.getMentionedUsers().get(0);
						MessageChannel channel = message.getChannel();

						new PatReaction(true).execute(author, null, null, null, message, channel, null, null, null);
					} else if (message.getContentRaw().contains("encarou")) {
						User author = message.getMentionedUsers().get(0);
						MessageChannel channel = message.getChannel();

						new StareReaction(true).execute(author, null, null, null, message, channel, null, null, null);
					} else if (message.getContentRaw().contains("deu um tapa em")) {
						User author = message.getMentionedUsers().get(0);
						MessageChannel channel = message.getChannel();

						new SlapReaction(true).execute(author, null, null, null, message, channel, null, null, null);
					} else if (message.getContentRaw().contains("socou")) {
						User author = message.getMentionedUsers().get(0);
						MessageChannel channel = message.getChannel();

						new PunchReaction(true).execute(author, null, null, null, message, channel, null, null, null);
					} else if (message.getContentRaw().contains("mordeu")) {
						User author = message.getMentionedUsers().get(0);
						MessageChannel channel = message.getChannel();

						new BiteReaction(true).execute(author, null, null, null, message, channel, null, null, null);
					}
				}
			}

			if (message.getAuthor() == Main.getInfo().getSelfUser() && message.getEmbeds().size() > 0 && Objects.requireNonNull(Objects.requireNonNull(message.getEmbeds().get(0).getFooter()).getText()).startsWith("Link: https://www.youtube.com/watch?v=") && event.getReactionEmote().getName().equals(Helper.ACCEPT)) {
				Music.loadAndPlay(event.getMember(), event.getTextChannel(), message.getEmbeds().get(0).getUrl());
				if (event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) {
					message.delete().queue();
				}
			}
		} catch (NullPointerException ignore) {
		}
	}

	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		SQLite.addGuildToDB(event.getGuild());
		try {
			Helper.sendPM(Objects.requireNonNull(event.getGuild().getOwner()).getUser(), "Obrigada por me adicionar ao seu servidor, utilize `s!ajuda` para ver meus comandos!\n\nDúvidas? Pergunte-me diretamente e um de meus desenvolvedores responderá assim que possível!");
		} catch (Exception err) {
			TextChannel dch = event.getGuild().getDefaultChannel();
			if (dch != null) {
				if (dch.canTalk()) {
					dch.sendMessage("Obrigada por me adicionar ao seu servidor, utilize `s!ajuda` para ver meus comandos!\n\nDúvidas? Pergunte-me diretamente e um de meus desenvolvedores responderá assim que possível!").queue();
				}
			}
		}

		Main.getInfo().getDevelopers().forEach(d -> Main.getInfo().getUserByID(d).openPrivateChannel().queue(c -> {
			String msg = "Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".";
			c.sendMessage(msg).queue();
		}));
		Helper.logger(this.getClass()).info("Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".");
	}

	@Override
	public void onGuildLeave(GuildLeaveEvent event) {
		Main.getInfo().getDevelopers().forEach(d -> Main.getInfo().getUserByID(d).openPrivateChannel().queue(c -> {
			String msg = "Acabei de sair do servidor \"" + event.getGuild().getName() + "\".";
			c.sendMessage(msg).queue();
		}));
		Helper.logger(this.getClass()).info("Acabei de sair do servidor \"" + event.getGuild().getName() + "\".");
	}

    /*@Override
	public void onReconnect(ReconnectedEvent event) {
		MainANT.getInfo().getLogChannel().sendMessage(DiscordHelper.getCustomEmoteMention(MainANT.getInfo().getGuild(), "kawaii") + " | Fui desparalizada!").queue();
	}*/
	
	/*@Override
	public void onDisconnect(DisconnectEvent event) {
		com.kuuhaku.MainANT.getInfo().getLogChannel().sendMessage(DiscordHelper.getCustomEmoteMention(com.kuuhaku.MainANT.getInfo().getGuild(), "kms") + " | Fui paraliz-... " + DiscordHelper.getCustomEmoteMention(com.kuuhaku.MainANT.getInfo().getGuild(), "yeetus")).queue();
	}*/

	@Override
	public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
		try {
			guildConfig gc = SQLite.getGuildById(event.getGuild().getId());

			if (!gc.getMsgBoasVindas().equals("")) {
				if (gc.isAntiRaid() && ((ChronoUnit.MILLIS.between(event.getUser().getTimeCreated().toLocalDateTime(), OffsetDateTime.now().atZoneSameInstant(ZoneOffset.UTC)) / 1000) / 60) < 10) {
					Helper.logToChannel(event.getUser(), false, null, "Um usuário foi expulso automaticamente por ter uma conta muito recente.\n`(data de criação: " + event.getUser().getTimeCreated().format(DateTimeFormatter.ofPattern("dd/MM/yyyy - hh:mm:ss")) + "h)`", event.getGuild());
					event.getGuild().kick(event.getMember()).queue();
					return;
				}
				URL url = new URL(Objects.requireNonNull(event.getUser().getAvatarUrl()));
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setRequestProperty("User-Agent", "Mozilla/5.0");
				BufferedImage image = ImageIO.read(con.getInputStream());

				EmbedBuilder eb = new EmbedBuilder();

				eb.setAuthor(event.getUser().getAsTag(), event.getUser().getAvatarUrl(), event.getUser().getAvatarUrl());
				eb.setColor(new Color(ColorThief.getColor(image)[0], ColorThief.getColor(image)[1], ColorThief.getColor(image)[2]));
				eb.setDescription(gc.getMsgBoasVindas().replace("%user%", event.getUser().getName()).replace("%guild%", event.getGuild().getName()));
				eb.setThumbnail(event.getUser().getAvatarUrl());
				eb.setFooter("ID do usuário: " + event.getUser().getId(), event.getGuild().getIconUrl());
				switch ((int) (Math.random() * 5)) {
					case 0:
						eb.setTitle("Opa, parece que temos um novo membro?");
						break;
					case 1:
						eb.setTitle("Mais um membro para nosso lindo servidor!");
						break;
					case 2:
						eb.setTitle("Um novo jogador entrou na partida, pressione start 2P!");
						break;
					case 3:
						eb.setTitle("Agora podemos iniciar a teamfight, um novo membro veio nos ajudar!");
						break;
					case 4:
						eb.setTitle("Bem-vindo ao nosso servidor, puxe uma cadeira e fique à vontade!");
						break;
				}

				Objects.requireNonNull(event.getGuild().getTextChannelById(gc.getCanalBV())).sendMessage(event.getUser().getAsMention()).embed(eb.build()).queue();
				Helper.logToChannel(event.getUser(), false, null, "Um usuário entrou no servidor", event.getGuild());
			} else if (gc.isAntiRaid() && ((ChronoUnit.MILLIS.between(event.getUser().getTimeCreated().toLocalDateTime(), OffsetDateTime.now().atZoneSameInstant(ZoneOffset.UTC)) / 1000) / 60) < 10) {
				Helper.logToChannel(event.getUser(), false, null, "Um foi bloqueado de entrar no servidor", event.getGuild());
				event.getGuild().kick(event.getMember()).queue();
			}
		} catch (Exception ignore) {
		}
	}

	@Override
	public void onGuildMemberLeave(@NotNull GuildMemberLeaveEvent event) {
		try {
			guildConfig gc = SQLite.getGuildById(event.getGuild().getId());

			if (!gc.getMsgAdeus().equals("")) {
				URL url = new URL(Objects.requireNonNull(event.getUser().getAvatarUrl()));
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setRequestProperty("User-Agent", "Mozilla/5.0");
				BufferedImage image = ImageIO.read(con.getInputStream());

				int rmsg = (int) (Math.random() * 5);

				EmbedBuilder eb = new EmbedBuilder();

				eb.setAuthor(event.getUser().getAsTag(), event.getUser().getAvatarUrl(), event.getUser().getAvatarUrl());
				eb.setColor(new Color(ColorThief.getColor(image)[0], ColorThief.getColor(image)[1], ColorThief.getColor(image)[2]));
				eb.setThumbnail(event.getUser().getAvatarUrl());
				eb.setDescription(gc.getMsgAdeus().replace("%user%", event.getUser().getName()).replace("%guild%", event.getGuild().getName()));
				eb.setFooter("ID do usuário: " + event.getUser().getId() + "\n\nServidor gerenciado por " + Objects.requireNonNull(event.getGuild().getOwner()).getEffectiveName(), event.getGuild().getOwner().getUser().getAvatarUrl());
				switch (rmsg) {
					case 0:
						eb.setTitle("Nãããoo...um membro deixou este servidor!");
						break;
					case 1:
						eb.setTitle("O quê? Temos um membro a menos neste servidor!");
						break;
					case 2:
						eb.setTitle("Alguém saiu do servidor, deve ter acabado a pilha, só pode!");
						break;
					case 3:
						eb.setTitle("Bem, alguém não está mais neste servidor, que pena!");
						break;
					case 4:
						eb.setTitle("Saíram do servidor bem no meio de uma teamfight, da pra acreditar?");
						break;
				}

				Objects.requireNonNull(event.getGuild().getTextChannelById(gc.getCanalAdeus())).sendMessage(eb.build()).queue();
				Helper.logToChannel(event.getUser(), false, null, "Um usuário saiu do servidor", event.getGuild());
			}
		} catch (Exception ignore) {
		}
	}

	@Override
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
		if (event.getAuthor().isBot()) return;
		if (Main.getInfo().getDevelopers().contains(event.getAuthor().getId())) {
			String msg = event.getMessage().getContentRaw();
			String[] args = msg.split(" ");
			if (args.length < 2) return;
			String msgNoArgs = msg.replaceFirst(args[0] + " " + args[1], "");

			switch (args[0].toLowerCase()) {
				case "send":
				case "s":
					Main.getInfo().getUserByID(args[1]).openPrivateChannel().queue(c ->
							c.sendMessage(msgNoArgs).queue());

					Main.getInfo().getDevelopers().forEach(d -> {
						if (!d.equals(event.getAuthor().getId())) {
							Main.getInfo().getUserByID(d).openPrivateChannel().queue(c ->
									c.sendMessage(event.getAuthor().getName() + " respondeu:\n> " + msgNoArgs).queue());
						}
					});
					break;
			}
		} else {
			try {
				event.getAuthor().openPrivateChannel().queue(c ->
						c.sendMessage("Mensagem enviada, aguardando resposta...").queue());
			} catch (Exception e) {
				return;
			}

			EmbedBuilder eb = new EmbedBuilder();

			eb.setAuthor(event.getAuthor().getAsTag(), event.getAuthor().getAvatarUrl());
			eb.setFooter(event.getAuthor().getId() + " - " + LocalDateTime.now().atOffset(ZoneOffset.ofHours(-3)).format(DateTimeFormatter.ofPattern("HH:mm | dd/MMM/yyyy")), null);
			Main.getInfo().getDevelopers().forEach(d ->
					Main.getInfo().getUserByID(d).openPrivateChannel().queue(c -> c.sendMessage(event.getMessage()).embed(eb.build()).queue()));
		}
	}

	public static boolean checkPermissions(@NotNull GuildMessageReceivedEvent event, User author, Member member, Message message, MessageChannel channel, Guild guild, String prefix, String rawMsgNoPrefix, String[] args, Command command) {
		if (Helper.hasPermission(member, command.getCategory().getPrivilegeLevel())) {
			command.execute(author, member, rawMsgNoPrefix, args, message, channel, guild, event, prefix);
			Helper.spawnAd(channel);
			return true;
		}

		try {
			channel.sendMessage(":x: | Você não tem permissão para executar este comando!").queue();
			Helper.spawnAd(channel);
			return true;
		} catch (InsufficientPermissionException ignore) {
		}
		return false;
	}

	public static boolean isFound(String commandName, boolean found, Command command) {
		if (command.getName().equalsIgnoreCase(commandName)) {
			found = true;
		}
		for (String alias : command.getAliases()) {
			if (alias.equalsIgnoreCase(commandName)) {
				found = true;
				break;
			}
		}
		if (command.getCategory().isEnabled()) {
			found = false;
		}
		return found;
	}
}
