/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.events;

import com.kuuhaku.Main;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.RelayDAO;
import com.kuuhaku.controller.sqlite.BlacklistDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.model.persistent.PermaBlock;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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
	public void onGuildJoin(GuildJoinEvent event) {
		GuildDAO.addGuildToDB(event.getGuild());
		try {
			Helper.sendPM(Objects.requireNonNull(event.getGuild().getOwner()).getUser(), "Obrigada por me adicionar ao seu servidor, utilize `s!ajuda` em um dos canais do servidor para ver meus comandos!\n\nDúvidas? Pergunte-me diretamente e um de meus desenvolvedores responderá assim que possível!");
		} catch (Exception err) {
			TextChannel dch = event.getGuild().getDefaultChannel();
			if (dch != null) {
				if (dch.canTalk()) {
					dch.sendMessage("Obrigada por me adicionar ao seu servidor, utilize `s!ajuda` para ver meus comandos!\n\nDúvidas? Pergunte-me diretamente e um de meus desenvolvedores responderá assim que possível!").queue();
				}
			}
		}

		ShiroInfo.getDevelopers().forEach(d -> Main.getInfo().getUserByID(d).openPrivateChannel().queue(c -> {
			String msg = "Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".";
			c.sendMessage(msg).queue();
		}));
		Helper.logger(this.getClass()).info("Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".");
	}

	@Override
	public void onGuildLeave(GuildLeaveEvent event) {
		ShiroInfo.getDevelopers().forEach(d -> Main.getInfo().getUserByID(d).openPrivateChannel().queue(c -> {
			GuildConfig gc = GuildDAO.getGuildById(event.getGuild().getId());
			gc.setMarkForDelete(true);
			GuildDAO.updateGuildSettings(gc);
			String msg = "Acabei de sair do servidor \"" + event.getGuild().getName() + "\".";
			c.sendMessage(msg).queue();
		}));
		Helper.logger(this.getClass()).info("Acabei de sair do servidor \"" + event.getGuild().getName() + "\".");
	}

	@Override
	public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
		Guild guild = event.getGuild();
		Member member = event.getMember();
		try {
			if (BlacklistDAO.isBlacklisted(event.getUser())) return;
			GuildConfig gc = GuildDAO.getGuildById(guild.getId());

			MemberDAO.addMemberToDB(member);

			if (!gc.getMsgBoasVindas().equals("")) {
				if (gc.isAntiRaid() && ChronoUnit.MINUTES.between(event.getUser().getTimeCreated().toLocalDateTime(), OffsetDateTime.now().atZoneSameInstant(ZoneOffset.UTC)) < 10) {
					Helper.logToChannel(event.getUser(), false, null, "Um usuário foi expulso automaticamente por ter uma conta muito recente.\n`(data de criação: " + event.getUser().getTimeCreated().format(DateTimeFormatter.ofPattern("dd/MM/yyyy - hh:mm:ss")) + "h)`", guild);
					guild.kick(member).queue();
					return;
				}
				URL url = new URL(Objects.requireNonNull(event.getUser().getAvatarUrl()));
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setRequestProperty("User-Agent", "Mozilla/5.0");
				BufferedImage image = ImageIO.read(con.getInputStream());

				EmbedBuilder eb = new EmbedBuilder();

				eb.setAuthor(event.getUser().getAsTag(), event.getUser().getAvatarUrl(), event.getUser().getAvatarUrl());
				eb.setColor(Helper.colorThief(image));
				eb.setDescription(gc.getMsgBoasVindas().replace("%user%", event.getUser().getName()).replace("%guild%", guild.getName()));
				eb.setThumbnail(event.getUser().getAvatarUrl());
				eb.setFooter("ID do usuário: " + event.getUser().getId(), guild.getIconUrl());
				switch ((int) (Math.random() * 5)) {
					case 0 -> eb.setTitle("Opa, parece que temos um novo membro?");
					case 1 -> eb.setTitle("Mais um membro para nosso lindo servidor!");
					case 2 -> eb.setTitle("Um novo jogador entrou na partida, pressione start 2P!");
					case 3 -> eb.setTitle("Agora podemos iniciar a teamfight, um novo membro veio nos ajudar!");
					case 4 -> eb.setTitle("Bem-vindo ao nosso servidor, puxe uma cadeira e fique à vontade!");
				}

				Objects.requireNonNull(guild.getTextChannelById(gc.getCanalBV())).sendMessage(event.getUser().getAsMention()).embed(eb.build()).queue();
				Helper.logToChannel(event.getUser(), false, null, "Um usuário entrou no servidor", guild);
			} else if (gc.isAntiRaid() && ChronoUnit.MINUTES.between(event.getUser().getTimeCreated().toLocalDateTime(), OffsetDateTime.now().atZoneSameInstant(ZoneOffset.UTC)) < 10) {
				Helper.logToChannel(event.getUser(), false, null, "Um usuário foi bloqueado de entrar no servidor", guild);
				guild.kick(member).queue();
			}
		} catch (Exception ignore) {
		}
	}

	@Override
	public void onGuildMemberRemove(@Nonnull GuildMemberRemoveEvent event) {
		Guild guild = event.getGuild();
		Member member = event.getMember();
		try {
			GuildConfig gc = GuildDAO.getGuildById(event.getGuild().getId());

			/*com.kuuhaku.model.persistent.Member m = MemberDAO.getMemberById(event.getMember().getId() + event.getGuild().getId());
			m.setMarkForDelete(true);
			MemberDAO.updateMemberConfigs(m);*/

			if (!gc.getMsgAdeus().equals("")) {
				URL url = new URL(Objects.requireNonNull(event.getUser().getAvatarUrl()));
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setRequestProperty("User-Agent", "Mozilla/5.0");
				BufferedImage image = ImageIO.read(con.getInputStream());

				int rmsg = (int) (Math.random() * 5);

				EmbedBuilder eb = new EmbedBuilder();

				eb.setAuthor(event.getUser().getAsTag(), event.getUser().getAvatarUrl(), event.getUser().getAvatarUrl());
				eb.setColor(Helper.colorThief(image));
				eb.setThumbnail(event.getUser().getAvatarUrl());
				eb.setDescription(gc.getMsgAdeus().replace("%user%", event.getUser().getName()).replace("%guild%", event.getGuild().getName()));
				eb.setFooter("ID do usuário: " + event.getUser().getId() + "\n\nServidor gerenciado por " + Objects.requireNonNull(event.getGuild().getOwner()).getEffectiveName(), event.getGuild().getOwner().getUser().getAvatarUrl());
				switch (rmsg) {
					case 0 -> eb.setTitle("Nãããoo...um membro deixou este servidor!");
					case 1 -> eb.setTitle("O quê? Temos um membro a menos neste servidor!");
					case 2 -> eb.setTitle("Alguém saiu do servidor, deve ter acabado a pilha, só pode!");
					case 3 -> eb.setTitle("Bem, alguém não está mais neste servidor, que pena!");
					case 4 -> eb.setTitle("Saíram do servidor bem no meio de uma teamfight, da pra acreditar?");
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
		List<String> staffIds = ShiroInfo.getStaff();
		if (staffIds.contains(event.getAuthor().getId())) {
			String msg = event.getMessage().getContentRaw();
			String[] args = msg.split(" ");
			if (args.length < 2) return;
			String msgNoArgs = msg.replaceFirst(args[0] + " " + args[1], "").trim();

			try {
				switch (args[0].toLowerCase()) {
					case "send", "s" -> {
						User u = Main.getInfo().getUserByID(args[1]);
						if (u == null) {
							event.getChannel().sendMessage("❌ | Não existe nenhum usuário com esse ID.").queue();
							return;
						}
						u.openPrivateChannel().queue(c ->
								c.sendMessage(event.getAuthor().getName() + " respondeu:\n>>> " + msgNoArgs).queue());
						staffIds.forEach(d -> {
							if (!d.equals(event.getAuthor().getId())) {
								Main.getInfo().getUserByID(d).openPrivateChannel().queue(c ->
										c.sendMessage(event.getAuthor().getName() + " respondeu:\n>>> " + msgNoArgs).queue());
							}
						});
						event.getChannel().sendMessage("Mensagem enviada com sucesso!").queue();
					}
					case "block", "b" -> {
						User us = Main.getInfo().getUserByID(args[1]);
						if (us == null) {
							event.getChannel().sendMessage("❌ | Não existe nenhum usuário com esse ID.").queue();
							return;
						}
						RelayDAO.permaBlock(new PermaBlock(args[1]));
						us.openPrivateChannel().queue(c ->
								c.sendMessage("Você foi bloqueado dos canais de comunicação da Shiro pela seguinte razão: `" + msgNoArgs + "`").queue());
						staffIds.forEach(d -> {
							if (!d.equals(event.getAuthor().getId())) {
								Main.getInfo().getUserByID(d).openPrivateChannel().queue(c ->
										c.sendMessage(event.getAuthor().getName() + " bloqueou o usuário " + Main.getInfo().getUserByID(args[1]) + ". Razão: \n>>> " + msgNoArgs).queue());
							}
						});
						event.getChannel().sendMessage("Usuário bloqueado com sucesso!").queue();
					}
				}
			} catch (NullPointerException ignore) {
			}
		} else {
			try {
				event.getAuthor().openPrivateChannel().queue(c -> {
					if (RelayDAO.blockedList().contains(event.getAuthor().getId())) {
						c.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_you-are-blocked")).queue();
					} else
						c.sendMessage("Mensagem enviada no canal de suporte, aguardando resposta...")
								.queue(s -> {
									EmbedBuilder eb = new ColorlessEmbedBuilder();

									eb.setAuthor(event.getAuthor().getAsTag(), event.getAuthor().getAvatarUrl());
									eb.setFooter(event.getAuthor().getId() + " - " + LocalDateTime.now().atOffset(ZoneOffset.ofHours(-3)).format(DateTimeFormatter.ofPattern("HH:mm | dd/MMM/yyyy")), null);
									staffIds.forEach(d ->
											Main.getInfo().getUserByID(d).openPrivateChannel().queue(ch -> ch.sendMessage(event.getMessage()).embed(eb.build()).queue()));
									s.delete().queueAfter(1, TimeUnit.MINUTES);
								});
				});
			} catch (Exception ignored) {
			}
		}
	}

	public static boolean isFound(GuildConfig gc, Guild g, String commandName, boolean found, Command command, User u) {
		if (command.getName().equalsIgnoreCase(commandName)) {
			found = true;
		}
		for (String alias : command.getAliases()) {
			if (alias.equalsIgnoreCase(commandName)) {
				found = true;
				break;
			}
		}
		if (!command.getCategory().isEnabled(gc, g, u)) {
			found = false;
		}
		return found;
	}

	@Override
	public void onGuildMessageDelete(@Nonnull GuildMessageDeleteEvent event) {
		Message msg = Main.getInfo().retrieveCachedMessage(event.getGuild(), event.getMessageId());

		if (msg == null || msg.getAuthor().isBot()) return;

		Helper.logToChannel(msg.getAuthor(), false, null, "Uma mensagem foi deletada no canal " + event.getChannel().getAsMention() + ":```diff\n-" + msg.getContentRaw() + "```", msg.getGuild());
	}

	@Override
	public void onGuildMessageUpdate(@Nonnull GuildMessageUpdateEvent event) {
		if (event.getAuthor().isBot()) return;

		Message msg = Main.getInfo().retrieveCachedMessage(event.getGuild(), event.getMessageId());

		if (msg != null)
			Helper.logToChannel(event.getAuthor(), false, null, "Uma mensagem foi editada no canal " + event.getChannel().getAsMention() + ":```diff\n- " + msg.getContentRaw() + "\n+ " + event.getMessage().getContentRaw() + "```", msg.getGuild());
	}
}
