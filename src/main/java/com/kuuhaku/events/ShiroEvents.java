/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Slashed;
import com.kuuhaku.command.commands.PreparedCommand;
import com.kuuhaku.controller.postgresql.*;
import com.kuuhaku.model.common.AutoEmbedBuilder;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.DailyQuest;
import com.kuuhaku.model.common.PseudoMessage;
import com.kuuhaku.model.enums.Event;
import com.kuuhaku.model.enums.*;
import com.kuuhaku.model.exceptions.ValidationException;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.persistent.guild.LevelRole;
import com.kuuhaku.model.records.embed.Embed;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import me.xuender.unidecode.Unidecode;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateOwnerEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.user.UserTypingEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;
import net.jodah.expiringmap.ExpiringMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.persistence.NoResultException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ShiroEvents extends ListenerAdapter {
	private final Map<String, CopyOnWriteArrayList<SimpleMessageListener>> toHandle = new ConcurrentHashMap<>();
	private final Map<String, VoiceTime> voiceTimes = new ConcurrentHashMap<>();

	@Override
	public void onGuildUpdateName(GuildUpdateNameEvent event) {
		GuildConfig gc = GuildDAO.getGuildById(event.getGuild().getId());
		gc.setName(event.getNewName());
		GuildDAO.updateGuildSettings(gc);
	}

	@Override
	public void onGuildUpdateOwner(@NotNull GuildUpdateOwnerEvent event) {
		assert event.getOldOwner() != null;
		assert event.getNewOwner() != null;

		if (TagDAO.getTagById(event.getOldOwner().getId()).isBeta()) {
			TagDAO.removeTagBeta(event.getOldOwner().getId());

			try {
				TagDAO.getTagById(event.getNewOwner().getId());
			} catch (NoResultException e) {
				TagDAO.addUserTagsToDB(event.getNewOwner().getId());
			} finally {
				TagDAO.giveTagBeta(event.getNewOwner().getId());
			}
		}
	}

	@Override
	public void onGuildMessageUpdate(@NotNull GuildMessageUpdateEvent event) {
		if (event.getAuthor().isBot()) return;
		Message msg = Main.getInfo().retrieveCachedMessage(event.getGuild(), event.getMessageId());
		onGuildMessageReceived(new GuildMessageReceivedEvent(event.getJDA(), event.getResponseNumber(), event.getMessage()));

		if (msg != null)
			Helper.logToChannel(event.getAuthor(), false, null, "Uma mensagem foi editada no canal " + event.getChannel().getAsMention() + ":```diff\n- " + msg.getContentRaw() + "\n+ " + event.getMessage().getContentRaw() + "```", msg.getGuild());
	}

	@Override
	public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
		try {
			User author = event.getAuthor();
			Member member = event.getMember();
			Message message = event.getMessage();
			TextChannel channel = message.getTextChannel();
			Guild guild = message.getGuild();
			String rawMessage = message.getContentRaw().replaceAll("\s+", " ");

			if (guild.getId().equals("614904136437334044")) System.out.println("1");

			if (author.isBot() && !Main.getSelfUser().getId().equals(author.getId())) {
				handleExchange(author, message);
				return;
			} else if (member == null) return;

			if (guild.getId().equals("614904136437334044")) System.out.println("2");

			String prefix = "";
			try {
				prefix = GuildDAO.getGuildById(guild.getId()).getPrefix().toLowerCase(Locale.ROOT);
			} catch (NoResultException | NullPointerException ignore) {
			}

			if (guild.getId().equals("614904136437334044")) System.out.println("3");

			if (rawMessage.startsWith(";") && ShiroInfo.getDevelopers().contains(author.getId()) && rawMessage.length() > 1) {
				try {
					if (rawMessage.replace(";", "").length() == 0) {
						channel.sendFile(message.getAttachments().get(0).downloadToFile().get()).queue();
					} else {
						MessageAction send = channel.sendMessage(Helper.makeEmoteFromMention(rawMessage.substring(1).split(" ")));
						for (Message.Attachment a : message.getAttachments()) {
							try {
								//noinspection ResultOfMethodCallIgnored
								send.addFile(a.downloadToFile().get());
							} catch (InterruptedException | ExecutionException ignore) {
							}
						}
						send.queue();
						message.delete().queue();
					}
				} catch (InsufficientPermissionException | ExecutionException | InterruptedException ignore) {
				}
				return;
			}

			boolean blacklisted = BlacklistDAO.isBlacklisted(author);

			if (!blacklisted) MemberDAO.getMember(author.getId(), guild.getId());

			/*try {
				MutedMember mm = MemberDAO.getMutedMemberById(author.getId());
				if (mm != null && mm.isMuted()) {
					message.delete().complete();
					return;
				}
			} catch (InsufficientPermissionException | ErrorResponseException ignore) {
			}*/

			GuildConfig gc = GuildDAO.getGuildById(guild.getId());
			if (gc.getNoSpamChannels().contains(channel.getId()) && Main.getSelfUser().getId().equals(author.getId())) {
				if (message.getReactions().size() >= gc.getNoSpamAmount()) {
					message.delete()
							.flatMap(s -> channel.sendMessage(":warning: | Opa, sem spam meu amigo!"))
							.queue(msg -> {
								msg.delete().queueAfter(20, TimeUnit.SECONDS, null, Helper::doNothing);
								Helper.logToChannel(member.getUser(), false, null, "Um membro estava spammando no canal " + channel.getAsMention(), guild, msg.getContentRaw());
							}, Helper::doNothing);
				} else if (gc.isHardAntispam()) {
					channel.getHistory().retrievePast(20).queue(h -> {
						h.removeIf(m -> ChronoUnit.MILLIS.between(m.getTimeCreated().toLocalDateTime(), OffsetDateTime.now().atZoneSameInstant(ZoneOffset.UTC)) > 5000 || m.getAuthor() != author);

						countSpam(member, channel, guild, h);
					});
				} else {
					channel.getHistory().retrievePast(20).queue(h -> {
						h.removeIf(m -> ChronoUnit.MILLIS.between(m.getTimeCreated().toLocalDateTime(), OffsetDateTime.now().atZoneSameInstant(ZoneOffset.UTC)) > 5000 || m.getAuthor() != author && StringUtils.containsIgnoreCase(m.getContentRaw(), rawMessage));

						countSpam(member, channel, guild, h);
					});
				}
			}

			if (Helper.isPureMention(rawMessage) && Helper.isPinging(message, Main.getSelfUser().getId())) {
				channel.sendMessage("Quer saber como pode usar meus comandos? Digite `" + prefix + "ajuda` para ver todos eles ordenados por categoria!").queue(null, Helper::doNothing);
				return;
			}

			if (!author.isBot()) Main.getInfo().cache(guild, message);

			String commandName = "";
			String rawMsgNoCommand = "";
			if (rawMessage.toLowerCase(Locale.ROOT).startsWith(prefix)) {
				String rawMsgNoPrefix = rawMessage.substring(prefix.length()).trim();
				commandName = rawMsgNoPrefix.split(" ")[0].trim();
				rawMsgNoCommand = rawMessage.substring(prefix.length() + commandName.length()).trim();
			}

			CustomAnswer ca = CustomAnswerDAO.getCAByTrigger(rawMessage, guild.getId());

			if (ca != null) {
				Predicate<CustomAnswer> p = answer -> !Main.getSelfUser().getId().equals(author.getId());
				if (ca.getForUser() != null)
					p = p.and(answer -> answer.getForUser().equals(author.getId()));
				if (ca.getChance() != 100)
					p = p.and(answer -> Helper.chance(answer.getChance()));

				if (p.test(ca)) {
					if (message.getReferencedMessage() != null)
						Helper.typeMessage(channel, Helper.replaceTags(ca.getAnswer(), author, guild), message.getReferencedMessage());
					else
						Helper.typeMessage(channel, Helper.replaceTags(ca.getAnswer(), author, guild));
				}
			}

			String[] args = Arrays.stream(rawMsgNoCommand.split(" "))
					.filter(s -> !s.isBlank())
					.toArray(String[]::new);

			boolean found = false;
			if (!guild.getSelfMember().hasPermission(Permission.MESSAGE_WRITE)) {
				return;
			}

			if (toHandle.containsKey(guild.getId())) {
				List<SimpleMessageListener> evts = getHandler().get(guild.getId());
				for (SimpleMessageListener evt : evts) {
					evt.onGuildMessageReceived(event);
				}
				evts.removeIf(SimpleMessageListener::isClosed);
			}

			Account acc = AccountDAO.getAccount(author.getId());
			if (!author.isBot()) {
				if (acc.isAfk()) {
					message.reply(":sunrise_over_mountains: | Você não está mais AFK.").queue();
					acc.setAfkMessage(null);
					AccountDAO.saveAccount(acc);
				}

				for (Member m : message.getMentionedMembers()) {
					Account tgt = AccountDAO.getAccount(m.getId());
					if (tgt.isAfk()) {
						message.reply(":zzz: | " + m.getEffectiveName() + " está AFK: " + Helper.makeEmoteFromMention(tgt.getAfkMessage())).queue();
					}
				}
			}

			PreparedCommand command = Main.getCommandManager().getCommand(commandName);
			if (command != null && !Main.getInfo().getIgnore().contains(author.getId())) {
				found = command.getCategory().isEnabled(guild, author) && !gc.getDisabledCommands().contains(command.getCommand().getClass().getName());

				if (found) {
					if (gc.getNoCommandChannels().contains(channel.getId()) && !Helper.hasPermission(member, PrivilegeLevel.MOD)) {
						channel.sendMessage("❌ | Comandos estão bloqueados neste canal.").queue();
						return;
					} else if (author.getId().equals(Main.getSelfUser().getId())) {
						channel.sendMessage(I18n.getString("err_human-command")).queue();
						return;
					} else if (command.getCategory() == Category.NSFW && !channel.isNSFW()) {
						try {
							channel.sendMessage(I18n.getString("err_nsfw-in-non-nsfw-channel")).queue();
						} catch (InsufficientPermissionException ignore) {
						}
						return;
					} else if (!Helper.hasPermission(member, command.getCategory().getPrivilegeLevel())) {
						try {
							channel.sendMessage(I18n.getString("err_not-enough-permission")).queue();
						} catch (InsufficientPermissionException ignore) {
						}
						return;
					} else if (blacklisted) {
						channel.sendMessage(I18n.getString("err_user-blacklisted")).queue();
						return;
					} else if (Main.getInfo().getRatelimit().containsKey(author.getId())) {
						channel.sendMessage(I18n.getString("err_user-ratelimited")).queue();
						Main.getInfo().getRatelimit().put(author.getId(), true, 3 + Helper.rng(4, false), TimeUnit.SECONDS);
						return;
					} else if (command.getMissingPerms(channel).length > 0) {
						channel.sendMessage("❌ | Não possuo permissões suficientes para executar esse comando:\n%s".formatted(
								Arrays.stream(command.getPermissions())
										.map(p -> "- " + p.getName())
										.collect(Collectors.joining("\n"))
						)).queue();
						return;
					}

					if (!TagDAO.getTagById(author.getId()).isBeta() && !Helper.hasPermission(member, PrivilegeLevel.SUPPORT))
						Main.getInfo().getRatelimit().put(author.getId(), true, 2 + Helper.rng(3, false), TimeUnit.SECONDS);

					try {
						command.execute(author, member, rawMsgNoCommand, args, message, channel, guild, prefix);
					} catch (Exception e) {
						Helper.logger(command.getCommand().getClass()).error("Erro ao executar comando " + command.getName(), e);
					}
					Helper.spawnAd(channel);

					LogDAO.saveLog(new Log(guild, author, rawMessage));
					Helper.logToChannel(author, true, command, "Um comando foi usado no canal " + channel.getAsMention(), guild, rawMessage);
				}
			}

			if (!found && !author.isBot() && !blacklisted) {
				if (!acc.getTwitchId().isBlank() && channel.getId().equals(ShiroInfo.getTwitchChannelID()) && Main.getInfo().isLive()) {
					Main.getTwitch().getChat().sendMessage("kuuhaku_otgmz", author.getName() + " disse: " + Helper.stripEmotesAndMentions(rawMessage));
				}

				/*if (!ShiroInfo.getStaff().contains(author.getId()) && Helper.isPinging(message, ShiroInfo.getNiiChan())) {
					channel.sendMessage("✅ | Você comprou um \"Pingue o Sora por **1.000 de dívida**\" com sucesso!").queue();
					acc.addLoan(1000);
				}*/

				if (gc.isCardSpawn()) Helper.spawnKawaipon(gc, channel);
				if (gc.isDropSpawn()) Helper.spawnDrop(gc, channel);

				Event ev = Event.getCurrent();
				if (ev == Event.XMAS && gc.isDropSpawn())
					Helper.spawnPadoru(gc, channel);
				else if (ev == Event.EASTER && gc.isDropSpawn())
					Helper.spawnUsaTan(gc, channel);

				try {
					if (gc.getNoLinkChannels().contains(channel.getId()) && Helper.findURL(rawMessage) && !Helper.hasPermission(member, PrivilegeLevel.MOD)) {
						message.delete().reason("Mensagem possui um URL").queue();
						channel.sendMessage(member.getAsMention() + ", é proibido postar links neste canal!").queue();
						Helper.logToChannel(author, false, null, "Detectei um link no canal " + channel.getAsMention(), guild, rawMessage);
					}

					com.kuuhaku.model.persistent.Member m = MemberDAO.getMember(member.getId(), member.getGuild().getId());
					if (m.getUid() == null) {
						m.setUid(author.getId());
						m.setSid(guild.getId());
					}

					boolean lvlUp = m.addXp(guild);
					MemberDAO.saveMember(m);
					try {
						if (lvlUp && gc.isLevelNotif()) {
							if (m.getLevel() % 210 == 5 && m.getLevel() > 210)
								Helper.getOr(gc.getLevelChannel(), channel).sendMessage(author.getAsMention() + " subiu para o nível " + m.getLevel() + ". GG WP! :tada:")
										.addFile(Helper.getResourceAsStream(this.getClass(), "assets/transition_" + m.getLevel() + ".gif"), "upgrade.gif")
										.queue();
							else
								Helper.getOr(gc.getLevelChannel(), channel).sendMessage(author.getAsMention() + " subiu para o nível " + m.getLevel() + ". GG WP! :tada:").queue();
						}
					} catch (InsufficientPermissionException e) {
						if (m.getLevel() % 210 == 5 && m.getLevel() > 210)
							channel.sendMessage(author.getAsMention() + " subiu para o nível " + m.getLevel() + ". GG WP! :tada:")
									.addFile(Helper.getResourceAsStream(this.getClass(), "assets/transition_" + m.getLevel() + ".gif"), "upgrade.gif")
									.queue();
						else
							channel.sendMessage(author.getAsMention() + " subiu para o nível " + m.getLevel() + ". GG WP! :tada:").queue();
					}

					Set<LevelRole> roles = gc.getLevelRoles()
							.stream()
							.filter(e -> m.getLevel() >= e.getLevel())
							.collect(Collectors.toSet());

					int curr = roles.stream().mapToInt(LevelRole::getLevel).max().orElse(0);
					if (curr > 0) {
						List<Role> prev = new ArrayList<>();
						List<Role> rols = new ArrayList<>();
						for (LevelRole role : roles) {
							Role r = guild.getRoleById(role.getId());
							if (r == null) {
								gc.removeLevelRole(role.getId());
								continue;
							}

							if (role.getLevel() < curr) {
								prev.add(r);
							} else {
								rols.add(r);
							}
						}
						GuildDAO.updateGuildSettings(gc);

						rols.removeIf(member.getRoles()::contains);
						if (!rols.isEmpty()) {
							guild.modifyMemberRoles(member, rols, prev).queue(null, Helper::doNothing);
							if (gc.isLevelNotif()) {
								TextChannel chn = Helper.getOr(gc.getLevelChannel(), channel);
								if (rols.size() > 1) {
									String names = rols.stream()
											.map(rl -> "**`" + rl.getName() + "`**")
											.collect(Collectors.collectingAndThen(Collectors.toList(), Helper.properlyJoin()));
									chn.sendMessage(author.getAsMention() + " ganhou os cargos " + names + "! :tada:").queue();
								} else
									chn.sendMessage(author.getAsMention() + " ganhou o cargo **`" + rols.get(0).getName() + "`**! :tada:").queue();
							}
						}
					}
				} catch (ErrorResponseException | NullPointerException e) {
					Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
				} catch (HierarchyException | InsufficientPermissionException ignore) {
				}

				if (gc.isNQNMode() && Helper.hasEmote(rawMessage))
					try {
						com.kuuhaku.model.persistent.Member m = MemberDAO.getMember(author.getId(), guild.getId());

						Webhook wh = Helper.getOrCreateWebhook(channel, "Shiro");
						Map<String, Runnable> s = Helper.sendEmotifiedString(guild, rawMessage);

						WebhookMessageBuilder wmb = new WebhookMessageBuilder();
						wmb.setContent(String.valueOf(s.keySet().toArray()[0]));
						if (m.getPseudoAvatar() == null || m.getPseudoAvatar().isBlank())
							wmb.setAvatarUrl(author.getEffectiveAvatarUrl());
						else try {
							wmb.setAvatarUrl(m.getPseudoAvatar());
						} catch (RuntimeException e) {
							m.setPseudoAvatar("");
							MemberDAO.saveMember(m);
						}
						if (m.getPseudoName() == null || m.getPseudoName().isBlank()) wmb.setUsername(author.getName());
						else try {
							wmb.setUsername(m.getPseudoName());
						} catch (RuntimeException e) {
							m.setPseudoName("");
							MemberDAO.saveMember(m);
						}

						assert wh != null;
						WebhookClient wc = new WebhookClientBuilder(wh.getUrl()).build();
						message.delete().queue(d -> {
							try {
								wc.send(wmb.build()).thenAccept(rm -> s.get(String.valueOf(s.keySet().toArray()[0])).run()).get();
							} catch (InterruptedException | ExecutionException e) {
								Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
							}
						}, Helper::doNothing);
					} catch (IndexOutOfBoundsException | InsufficientPermissionException | ErrorResponseException | NullPointerException | InterruptedException | ExecutionException ignore) {
					}
			}

			if (acc.hasPendingQuest()) {
				DailyQuest tasks = DailyQuest.getQuest(author.getIdLong());
				Map<DailyTask, Integer> pg = acc.getDailyProgress();

				if (tasks.checkTasks(pg)) {
					acc.setLastQuest();

					float mod = tasks.getDifficultyMod();
					if (Helper.round(mod, 1) >= 3.8)
						acc.addGem();
					else
						acc.addCredit(Math.round(2000 * mod), this.getClass());

					AccountDAO.saveAccount(acc);
					channel.sendMessage(author.getAsMention() + " completou todos os desafios diários, parabéns! :confetti_ball:").queue();
				}
			}
		} catch (InsufficientPermissionException | ErrorResponseException ignore) {
		}
	}

	@Override
	public void onSlashCommand(@NotNull SlashCommandEvent evt) {
		InteractionHook hook = evt.deferReply().complete();

		if (!evt.isFromGuild()) {
			hook.sendMessage("❌ | Meus comandos não funcionam em canais privados.").queue();
		}

		Guild guild = evt.getGuild();
		TextChannel channel = evt.getTextChannel();
		Member member = evt.getMember();
		User author = evt.getUser();

		assert guild != null;
		boolean blacklisted = BlacklistDAO.isBlacklisted(author);
		if (blacklisted) {
			hook.sendMessage(I18n.getString("err_user-blacklisted")).queue();
		}

		GuildConfig gc = GuildDAO.getGuildById(guild.getId());
		PreparedCommand command;
		if (evt.getSubcommandName() == null)
			command = Main.getCommandManager().getSlash(null, evt.getName());
		else
			command = Main.getCommandManager().getSlash(evt.getName(), evt.getSubcommandName());

		String error = null;
		if (!(command.getCommand() instanceof Slashed)) {
			error = "❌ | Comando inexistente.";
		} else if (!command.getCategory().isEnabled(guild, author) || gc.getDisabledCommands().contains(command.getCommand().getClass().getName())) {
			error = "❌ | Comando desabilitado.";
		} else if (gc.getNoCommandChannels().contains(channel.getId()) && !Helper.hasPermission(member, PrivilegeLevel.MOD)) {
			error = "❌ | Comandos estão bloqueados neste canal.";
		} else if (command.getCategory() == Category.NSFW && !channel.isNSFW()) {
			error = I18n.getString("err_nsfw-in-non-nsfw-channel");
		} else if (!Helper.hasPermission(member, command.getCategory().getPrivilegeLevel())) {
			error = I18n.getString("err_not-enough-permission");
		} else if (Main.getInfo().getRatelimit().containsKey(author.getId())) {
			error = I18n.getString("err_user-ratelimited");
			Main.getInfo().getRatelimit().put(author.getId(), true, 3 + Helper.rng(4, false), TimeUnit.SECONDS);
		} else if (command.getMissingPerms(channel).length > 0) {
			error = "❌ | Não possuo permissões suficientes para executar esse comando:\n%s".formatted(
					Arrays.stream(command.getPermissions())
							.map(p -> "- " + p.getName())
							.collect(Collectors.joining("\n"))
			);
		}

		if (error != null) {
			hook.sendMessage(error).queue();
			return;
		}

		String commandLine;
		try {
			commandLine = ((Slashed) command.getCommand()).toCommand(evt);
		} catch (ValidationException e) {
			hook.sendMessage(e.getMessage()).queue();
			return;
		}

		String[] args = Arrays.stream(commandLine.split(" "))
				.filter(s -> !s.isBlank())
				.toArray(String[]::new);

		List<User> users = new ArrayList<>();
		List<Member> members = new ArrayList<>();
		List<Role> roles = new ArrayList<>();
		List<TextChannel> channels = new ArrayList<>();

		for (OptionMapping op : evt.getOptions()) {
			switch (op.getType()) {
				case USER -> {
					users.add(op.getAsUser());
					members.add(guild.getMember(op.getAsUser()));
				}
				case ROLE -> roles.add(op.getAsRole());
				case CHANNEL -> {
					if (op.getChannelType() == ChannelType.TEXT)
						channels.add((TextChannel) op.getAsGuildChannel());
				}
			}
		}

		members.removeIf(Objects::isNull);
		Message msg = new PseudoMessage(
				commandLine,
				author,
				member,
				channel,
				users,
				members,
				roles,
				channels
		);

		if (!TagDAO.getTagById(author.getId()).isBeta() && !Helper.hasPermission(member, PrivilegeLevel.SUPPORT))
			Main.getInfo().getRatelimit().put(author.getId(), true, 2 + Helper.rng(3, false), TimeUnit.SECONDS);

		hook.deleteOriginal().queue();
		command.execute(author, member, commandLine, args, msg, channel, guild, gc.getPrefix());
		Helper.spawnAd(channel);

		LogDAO.saveLog(new Log(guild, author, commandLine));
		Helper.logToChannel(author, true, command, "Um comando foi usado no canal " + channel.getAsMention(), guild, commandLine);
	}

	@Override
	public void onRoleDelete(@NotNull RoleDeleteEvent event) {
		GuildConfig gc = GuildDAO.getGuildById(event.getGuild().getId());

		gc.removeLevelRole(event.getRole().getId());

		GuildDAO.updateGuildSettings(gc);
		Helper.logToChannel(null, false, null, "Cargo deletado: " + event.getRole().getName(), event.getGuild());
	}

	@Override
	public void onGuildJoin(@NotNull GuildJoinEvent event) {
		try {
			Helper.sendPM(Objects.requireNonNull(event.getGuild().getOwner()).getUser(), "Obrigada por me adicionar ao seu servidor, utilize `s!ajuda` em um dos canais do servidor para ver meus comandos!\n\nDúvidas? Pergunte-me diretamente e um de meus suportes responderá assim que possível!");
		} catch (Exception err) {
			TextChannel dch = event.getGuild().getDefaultChannel();
			if (dch != null) {
				if (dch.canTalk()) {
					dch.sendMessage("Obrigada por me adicionar ao seu servidor, utilize `s!ajuda` para ver meus comandos!\n\nDúvidas? Pergunte-me diretamente no canal privado e um de meus suportes responderá assim que possível!").queue();
				}
			}
		}

		for (String d : ShiroInfo.getDevelopers()) {
			Main.getInfo().getUserByID(d).openPrivateChannel().queue(c -> {
				String msg = "Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".";
				c.sendMessage(msg).queue();
			});
		}
		Helper.logger(this.getClass()).info("Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".");
	}

	@Override
	public void onGuildLeave(@NotNull GuildLeaveEvent event) {
		for (String d : ShiroInfo.getDevelopers()) {
			Main.getInfo().getUserByID(d).openPrivateChannel().queue(c -> {
				GuildConfig gc = GuildDAO.getGuildById(event.getGuild().getId());
				GuildDAO.removeGuildFromDB(gc);
				String msg = "Acabei de sair do servidor \"" + event.getGuild().getName() + "\".";
				c.sendMessage(msg).queue();
			});
		}
		Helper.logger(this.getClass()).info("Acabei de sair do servidor \"" + event.getGuild().getName() + "\".");
	}

	@Override
	public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
		Guild guild = event.getGuild();
		Member member = event.getMember();
		User author = event.getUser();

		if (Main.getInfo().getAntiRaidStreak().get(guild.getId()) != null) {
			Main.getInfo().getAntiRaidStreak().computeIfPresent(guild.getId(), (k, p) -> Pair.of(p.getLeft(), p.getRight() + 1));
			guild.ban(member, 7, "Detectada tentativa de raid").queue();
			return;
		}

		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (gc.isAntiRaid()) {
			ExpiringMap<Long, String> arc = Main.getInfo().getAntiRaidCache()
					.computeIfAbsent(guild.getId(), k -> ExpiringMap.builder().expiration(5, TimeUnit.SECONDS).build());

			arc.put(System.currentTimeMillis(), author.getId());
			if (arc.size() >= gc.getAntiRaidLimit()) {
				TextChannel chn = gc.getGeneralChannel();
				if (chn != null) {
					EmbedBuilder eb = new EmbedBuilder()
							.setColor(Color.red)
							.setTitle("**⚠️ | RAID DETECTADA | ⚠️**")
							.setDescription("""
									Usuários permaneçam em suas casas, isto não é um treinamento, o servidor está sofrendo uma tentativa de raid.
									          
									Elevando nível de defesa para DEFCON 1...Ok
									Ativando sistemas de proteção R.A.ID...Ok
									Notificando dono do servidor...Ok
									""")
							.setFooter("Aguarde, o sistema será encerrado em breve")
							.setImage("https://i.imgur.com/KkhWWJf.gif");

					chn.sendMessageEmbeds(eb.build()).queue(null, Helper::doNothing);
				}

				User owner = Main.getInfo().getUserByID(guild.getOwnerId());
				if (owner != null) {
					owner.openPrivateChannel()
							.flatMap(s -> s.sendMessage("**ALERTA:** Seu servidor " + guild.getName() + " está sofrendo uma raid. Mas não se preocupe, se você recebeu esta mensagem é porque o sistema antiraid foi ativado."))
							.queue(null, Helper::doNothing);
				}

				Main.getInfo().getAntiRaidStreak().put(guild.getId(), Pair.of(System.currentTimeMillis(), arc.size()));

				for (TextChannel tc : guild.getTextChannels()) {
					if (guild.getPublicRole().hasPermission(tc, Permission.MESSAGE_WRITE)) {
						tc.getManager().setSlowmode(10).queue(null, Helper::doNothing);
					}
				}

				Main.getInfo().getAntiRaidCache().remove(guild.getId());
				List<AuditableRestAction<Void>> acts = new ArrayList<>();

				for (String id : arc.values()) {
					acts.add(guild.ban(id, 7, "Detectada tentativa de raid"));
				}

				RestAction.allOf(acts)
						.queue(s ->
								Helper.logToChannel(
										author,
										false,
										null,
										"ANTIRAID: " + arc.size() + " usuários banidos por entrarem ao mesmo tempo no servidor em um intervalo de 5 segundos (limite: " + gc.getAntiRaidLimit() + " membros/5 seg).",
										guild
								), Helper::doNothing
						);
				return;
			}
		}

		if (BlacklistDAO.isBlacklisted(author)) return;

		String name = member.getEffectiveName();
		if (gc.isMakeMentionable() && !Helper.regex(name, "[A-z0-9]{4}").find()) {
			name = Unidecode.decode(name);
		}

		if (gc.isAntiHoist()) {
			while (name.length() > 0 && name.charAt(0) < 65) {
				name = name.substring(1);
			}
		}

		if (!name.equals(member.getEffectiveName())) {
			if (name.length() < 2) {
				String[] names = {"Mencionável", "Unicode", "Texto", "Ilegível", "Símbolos", "Digite um nome"};
				name = names[Helper.rng(names.length, true)];
			}

			try {
				member.modifyNickname(name).queue(null, Helper::doNothing);
			} catch (InsufficientPermissionException ignore) {
			}
		}

		Role r = gc.getJoinRole();
		if (r != null)
			guild.addRoleToMember(member, r).queue(null, Helper::doNothing);

		try {
			if (!gc.getWelcomeMessage().isBlank()) {
				BufferedImage image = ImageIO.read(Helper.getImage(author.getEffectiveAvatarUrl()));

				String temp = Helper.replaceTags(gc.getEmbedTemplateRaw(), author, guild);
				EmbedBuilder eb;
				if (!temp.isEmpty()) {
					Embed e = gc.getEmbedTemplate();
					eb = new AutoEmbedBuilder(e)
							.setTitle(
									switch (Helper.rng(5, true)) {
										case 0 -> "Opa, parece que temos um novo membro?";
										case 1 -> "Mais um membro para nosso lindo servidor!";
										case 2 -> "Um novo jogador entrou na partida, pressione start 2P!";
										case 3 -> "Agora podemos iniciar a teamfight, um novo membro veio nos ajudar!";
										case 4 -> "Bem-vindo ao nosso servidor, puxe uma cadeira e fique à vontade!";
										default -> "";
									},
									e.title() != null ? e.title().url() : null
							);

					if (e.color() != null) eb.setColor(e.getParsedColor());
					else eb.setColor(Helper.colorThief(image));

					if (e.thumbnail() != null) eb.setThumbnail(e.thumbnail());
					else eb.setThumbnail(author.getEffectiveAvatarUrl());

					if (e.image() != null)
						eb.setImage(e.image().getRandomJoin());

					eb.setDescription(Helper.replaceTags(gc.getWelcomeMessage(), author, guild));

					if (e.footer() != null && e.footer().name() != null)
						eb.setFooter(e.footer().name(), e.footer().icon());
					else if (e.footer() != null)
						eb.setFooter("ID do usuário: " + author.getId(), Helper.getOr(e.footer().icon(), guild.getIconUrl()));
					else
						eb.setFooter("ID do usuário: " + author.getId(), guild.getIconUrl());
				} else {
					eb = new EmbedBuilder()
							.setTitle(
									switch (Helper.rng(5, true)) {
										case 0 -> "Opa, parece que temos um novo membro?";
										case 1 -> "Mais um membro para nosso lindo servidor!";
										case 2 -> "Um novo jogador entrou na partida, pressione start 2P!";
										case 3 -> "Agora podemos iniciar a teamfight, um novo membro veio nos ajudar!";
										case 4 -> "Bem-vindo ao nosso servidor, puxe uma cadeira e fique à vontade!";
										default -> "";
									}
							)
							.setAuthor(author.getAsTag(), author.getEffectiveAvatarUrl(), author.getEffectiveAvatarUrl())
							.setColor(Helper.colorThief(image))
							.setDescription(Helper.replaceTags(gc.getWelcomeMessage(), author, guild))
							.setThumbnail(author.getEffectiveAvatarUrl())
							.setFooter("ID do usuário: " + author.getId(), guild.getIconUrl());
				}

				TextChannel chn = gc.getWelcomeChannel();
				if (chn != null && chn.canTalk(guild.getSelfMember())) {
					Role welcomer = gc.getWelcomerRole();
					chn.sendMessage(author.getAsMention() + (welcomer != null ? " " + welcomer.getAsMention() : ""))
							.setEmbeds(eb.build())
							.queue();

					if (author.getId().equals(ShiroInfo.getNiiChan()))
						chn.sendMessage("<:b_shirolove:752890212371267676> | Seja bem-vindo Nii-chan!").queue();
				}
				Helper.logToChannel(author, false, null, "Um usuário entrou no servidor", guild);
			}
		} catch (IOException ignore) {
		}
	}

	@Override
	public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
		Guild guild = event.getGuild();
		User author = event.getUser();
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		try {
			if (!gc.getByeMessage().isBlank()) {
				BufferedImage image = ImageIO.read(Helper.getImage(author.getEffectiveAvatarUrl()));

				String temp = Helper.replaceTags(gc.getEmbedTemplateRaw(), author, guild);
				EmbedBuilder eb;
				if (!temp.isEmpty()) {
					Embed e = gc.getEmbedTemplate();
					eb = new AutoEmbedBuilder(e)
							.setTitle(
									switch (Helper.rng(5, true)) {
										case 0 -> "Nãããoo...um membro deixou este servidor!";
										case 1 -> "O quê? Temos um membro a menos neste servidor!";
										case 2 -> "Alguém saiu do servidor, deve ter acabado a pilha, só pode!";
										case 3 -> "Bem, alguém não está mais neste servidor, que pena!";
										case 4 -> "Saíram do servidor bem no meio de uma teamfight, da pra acreditar?";
										default -> "";
									},
									e.title() != null ? e.title().url() : null
							);

					if (e.color() != null) eb.setColor(e.getParsedColor());
					else eb.setColor(Helper.colorThief(image));

					if (e.thumbnail() != null) eb.setThumbnail(e.thumbnail());
					else eb.setThumbnail(author.getEffectiveAvatarUrl());

					if (e.image() != null)
						eb.setImage(e.image().getRandomLeave());

					eb.setDescription(Helper.replaceTags(gc.getByeMessage(), author, guild));

					if (e.footer() != null && e.footer().name() != null)
						eb.setFooter(e.footer().name(), e.footer().icon());
					else if (e.footer() != null)
						eb.setFooter("ID do usuário: " + author.getId(), Helper.getOr(e.footer().icon(), guild.getIconUrl()));
					else
						eb.setFooter("ID do usuário: " + author.getId(), guild.getIconUrl());
				} else {
					eb = new EmbedBuilder()
							.setTitle(
									switch (Helper.rng(5, true)) {
										case 0 -> "Nãããoo...um membro deixou este servidor!";
										case 1 -> "O quê? Temos um membro a menos neste servidor!";
										case 2 -> "Alguém saiu do servidor, deve ter acabado a pilha, só pode!";
										case 3 -> "Bem, alguém não está mais neste servidor, que pena!";
										case 4 -> "Saíram do servidor bem no meio de uma teamfight, da pra acreditar?";
										default -> "";
									}
							)
							.setAuthor(author.getAsTag(), author.getEffectiveAvatarUrl(), author.getEffectiveAvatarUrl())
							.setColor(Helper.colorThief(image))
							.setDescription(Helper.replaceTags(gc.getByeMessage(), author, guild))
							.setThumbnail(author.getEffectiveAvatarUrl())
							.setFooter("ID do usuário: " + author.getId(), guild.getIconUrl());
				}

				TextChannel chn = gc.getByeChannel();
				if (chn != null && chn.canTalk(guild.getSelfMember()))
					chn.sendMessageEmbeds(eb.build()).queue();
				Helper.logToChannel(author, false, null, "Um usuário saiu do servidor", guild);
			}
		} catch (IOException ignore) {
		}
	}

	@Override
	public void onGuildMessageReactionAdd(@NotNull GuildMessageReactionAddEvent event) {
		if (!event.getReactionEmote().isEmoji() || !event.getReactionEmote().getEmoji().equals("⭐")) return;

		Message msg;
		try {
			msg = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
		} catch (InsufficientPermissionException | ErrorResponseException e) {
			return;
		}

		Guild guild = event.getGuild();
		User author = event.getUser();
		if (author.isBot() || StarboardDAO.isStarboarded(msg)) return;

		int stars = msg.getReactions().stream()
				.filter(r -> r.getReactionEmote().isEmoji() && r.getReactionEmote().getEmoji().equals("⭐"))
				.mapToInt(MessageReaction::getCount)
				.sum();

		if (stars == 0) return;

		GuildConfig gc = GuildDAO.getGuildById(guild.getId());
		TextChannel chn = gc.getStarboardChannel();
		if (chn != null && chn.canTalk(guild.getSelfMember())) {
			if (stars < gc.getStarRequirement()) return;

			EmbedBuilder eb = new EmbedBuilder()
					.setColor(Color.orange)
					.setAuthor("Destaque de " + msg.getAuthor().getAsTag(), msg.getJumpUrl(), msg.getAuthor().getEffectiveAvatarUrl())
					.setDescription(msg.getContentRaw());

			if (!msg.getAttachments().isEmpty()) {
				Message.Attachment att = msg.getAttachments().get(0);

				if (att.isImage() || att.isVideo())
					eb.setImage(att.getUrl());
			}

			chn.sendMessage(":star: | " + msg.getTextChannel().getAsMention())
					.setEmbeds(eb.build())
					.queue(null, Helper::doNothing);

			StarboardDAO.starboard(msg);
		}
	}

	@Override
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
		Message msg = event.getMessage();
		if (msg.getAuthor().isBot()) return;

		String content = msg.getContentRaw();
		String[] args = content.split(" ");

		List<String> staffIds = ShiroInfo.getStaff();
		if (staffIds.contains(event.getAuthor().getId())) {
			try {
				switch (args[0].toLowerCase(Locale.ROOT)) {
					case "send", "s" -> {
						if (args.length < 2) return;
						String msgNoArgs = Helper.makeEmoteFromMention(content.replaceFirst(args[0] + " " + args[1], "")).trim();

						try {
							User u = Main.getInfo().getUserByID(args[1]);
							if (u == null) {
								event.getChannel().sendMessage("❌ | Não existe nenhum usuário com esse ID.").queue();
								return;
							} else if (msgNoArgs.length() == 0) {
								event.getChannel().sendMessage("❌ | Você não pode enviar uma mensagem vazia.").queue();
								return;
							}

							u.openPrivateChannel().queue(c ->
									c.sendMessage(event.getAuthor().getName() + " respondeu:\n>>> " + msgNoArgs).queue(null, Helper::doNothing));
							for (String d : staffIds) {
								if (!d.equals(event.getAuthor().getId())) {
									Main.getInfo().getUserByID(d).openPrivateChannel()
											.flatMap(c -> c.sendMessage(event.getAuthor().getName() + " respondeu o usuário " + u.getName() + ":\n>>> " + msgNoArgs))
											.queue();
								}
							}
							event.getChannel().sendMessage("✅ | Mensagem enviada com sucesso!").queue();
						} catch (NumberFormatException e) {
							event.getChannel().sendMessage("❌ | ID inválido.").queue();
						}
					}
					case "block", "b" -> {
						if (args.length < 2) return;
						String msgNoArgs = Helper.makeEmoteFromMention(content.replaceFirst(args[0] + " " + args[1], "")).trim();

						try {
							User u = Main.getInfo().getUserByID(args[1]);
							if (u == null) {
								event.getChannel().sendMessage("❌ | Não existe nenhum usuário com esse ID.").queue();
								return;
							} else if (msgNoArgs.length() == 0) {
								event.getChannel().sendMessage("❌ | Você precisa especificar uma razão.").queue();
								return;
							}

							EmbedBuilder eb = new EmbedBuilder()
									.setTitle("Você foi bloqueado dos canais de comunicação da Shiro")
									.setDescription("Razão: ```" + msgNoArgs + "```\n\nCaso acredite ser um engano ou deseje recorrer a um desbloqueio, por favor [preencha este formulário.](https://forms.gle/KrPHLZcijpzCXDoh9)")
									.setColor(Color.red)
									.setThumbnail("https://cdn.icon-icons.com/icons2/1380/PNG/512/vcsconflicting_93497.png")
									.setTimestamp(Instant.now());

							BlockDAO.block(new Block(args[1]));
							u.openPrivateChannel().queue(c ->
									c.sendMessageEmbeds(eb.build()).queue(null, Helper::doNothing));
							for (String d : staffIds) {
								if (!d.equals(event.getAuthor().getId())) {
									Main.getInfo().getUserByID(d).openPrivateChannel()
											.flatMap(c -> c.sendMessage(event.getAuthor().getName() + " bloqueou o usuário " + u.getName() + ". Razão: \n>>> " + msgNoArgs))
											.queue();
								}
							}
							event.getChannel().sendMessage("✅ | Usuário bloqueado com sucesso!").queue();
						} catch (NumberFormatException e) {
							event.getChannel().sendMessage("❌ | ID inválido.").queue();
						}
					}
					case "alert", "a" -> {
						if (args.length < 2) return;
						String msgNoArgs = Helper.makeEmoteFromMention(content.replaceFirst(args[0] + " " + args[1], "")).trim();

						try {
							User u = Main.getInfo().getUserByID(args[1]);
							if (u == null) {
								event.getChannel().sendMessage("❌ | Não existe nenhum usuário com esse ID.").queue();
								return;
							} else if (msgNoArgs.length() == 0) {
								event.getChannel().sendMessage("❌ | Você precisa especificar uma razão.").queue();
								return;
							}

							EmbedBuilder eb = new EmbedBuilder()
									.setTitle("Você recebeu um alerta:")
									.setDescription(msgNoArgs)
									.setColor(Color.orange)
									.setThumbnail("https://canopytools.com/wp-content/uploads/2019/10/alert-icon-17.png")
									.setTimestamp(Instant.now());

							u.openPrivateChannel().queue(c ->
									c.sendMessageEmbeds(eb.build()).queue(null, Helper::doNothing));
							for (String d : staffIds) {
								if (!d.equals(event.getAuthor().getId())) {
									Main.getInfo().getUserByID(d).openPrivateChannel()
											.flatMap(c -> c.sendMessage(event.getAuthor().getName() + " alertou o usuário " + u.getName() + ". Razão: \n>>> " + msgNoArgs))
											.queue();
								}
							}
							event.getChannel().sendMessage("✅ | Usuário alertado com sucesso!").queue();
						} catch (NumberFormatException e) {
							event.getChannel().sendMessage("❌ | ID inválido.").queue();
						}
					}
				}
			} catch (NullPointerException ignore) {
			}
		} else {
			event.getAuthor().openPrivateChannel().queue(c -> {
				if (!BlockDAO.blockedList().contains(event.getAuthor().getId())) {
					c.sendMessage("Mensagem enviada no canal de suporte, aguardando resposta...")
							.queue(s -> {
								EmbedBuilder eb = new ColorlessEmbedBuilder()
										.setDescription((content + "\n\n" + (msg.getAttachments().size() > 0 ? "`Contém " + msg.getAttachments().size() + " anexos`" : "")).trim())
										.setAuthor(event.getAuthor().getAsTag(), event.getAuthor().getEffectiveAvatarUrl())
										.setFooter(event.getAuthor().getId())
										.setTimestamp(Instant.now());

								for (String d : staffIds) {
									Main.getInfo().getUserByID(d).openPrivateChannel()
											.flatMap(ch -> ch.sendMessageEmbeds(eb.build()))
											.queue();
								}
								s.delete().queueAfter(1, TimeUnit.MINUTES);
							}, Helper::doNothing);
				}
			});
		}
	}

	@Override
	public void onGuildMessageDelete(@NotNull GuildMessageDeleteEvent event) {
		Message msg = Main.getInfo().retrieveCachedMessage(event.getGuild(), event.getMessageId());

		if (msg == null || msg.getAuthor().isBot()) return;

		Helper.logToChannel(msg.getAuthor(), false, null, "Uma mensagem foi deletada no canal " + event.getChannel().getAsMention() + ":```diff\n-" + msg.getContentRaw() + "```", msg.getGuild());
	}

	@Override
	public void onGuildVoiceJoin(@NotNull GuildVoiceJoinEvent event) {
		Member mb = event.getMember();
		if (mb.getUser().isBot()) return;
		boolean blacklisted = BlacklistDAO.isBlacklisted(event.getMember().getUser());

		if (!blacklisted)
			voiceTimes.put(mb.getId() + mb.getGuild().getId(), VoiceTimeDAO.getVoiceTime(mb.getId(), mb.getGuild().getId()));
	}

	@Override
	public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event) {
		Member mb = event.getMember();
		if (mb.getUser().isBot()) return;
		boolean blacklisted = BlacklistDAO.isBlacklisted(mb.getUser());

		if (!blacklisted) {
			VoiceTime vt = voiceTimes.remove(mb.getId() + mb.getGuild().getId());

			if (vt != null) {
				VoiceTimeDAO.saveVoiceTime(vt);
			}
		}
	}

	@Override
	public void onGuildMemberUpdateNickname(@NotNull GuildMemberUpdateNicknameEvent event) {
		String name = event.getNewNickname();
		if (name == null) return;

		boolean nonMentionable = !Helper.regex(name, "[A-z0-9]{4}").find();
		boolean isHoister = name.charAt(0) < 65;

		if (nonMentionable || isHoister) {
			GuildConfig gc = GuildDAO.getGuildById(event.getGuild().getId());
			if (gc.isMakeMentionable() && nonMentionable) {
				name = Unidecode.decode(name);
			}

			if (gc.isAntiHoist() && isHoister) {
				name = name.substring(1);
			}

			if (!name.equals(event.getNewNickname())) {
				if (name.length() < 2) {
					String[] names = {"Mencionável", "Unicode", "Texto", "Ilegível", "Símbolos", "Digite um nome"};
					name = names[Helper.rng(names.length, true)];
				}

				try {
					event.getMember().modifyNickname(name).queue(null, Helper::doNothing);
				} catch (InsufficientPermissionException ignore) {
				}
			}
		}

		Helper.logToChannel(event.getUser(), false, null, event.getUser().getAsMention() + " mudou o nome de `" + Helper.getOr(event.getOldNickname(), event.getMember().getUser().getName()) + "` para `" + Helper.getOr(event.getNewNickname(), event.getMember().getUser().getName()) + "`", event.getGuild());
	}

	@Override
	public void onUserTyping(@NotNull UserTypingEvent event) {
		User u = event.getUser();
		if (event.isFromType(ChannelType.PRIVATE) && ShiroInfo.getStaff().contains(u.getId())) {
			for (String d : ShiroInfo.getStaff()) {
				if (!d.equals(u.getId())) {
					Main.getInfo().getUserByID(d).openPrivateChannel()
							.flatMap(PrivateChannel::sendTyping)
							.queue();
				}
			}
		}
	}

	private void countSpam(Member member, MessageChannel channel, Guild guild, List<Message> h) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES, Permission.MANAGE_CHANNEL, Permission.MANAGE_PERMISSIONS) && h.size() >= gc.getNoSpamAmount() && guild.getSelfMember().canInteract(member)) {
			TextChannel ch = (TextChannel) channel;

			ch.deleteMessagesByIds(h.stream()
					.map(Message::getId)
					.collect(Collectors.toList())
			).queue(null, Helper::doNothing);

			channel.sendMessage(":warning: | Opa, sem spam meu amigo!").queue(msg -> {
				msg.delete().queueAfter(20, TimeUnit.SECONDS, null, Helper::doNothing);
				Helper.logToChannel(member.getUser(), false, null, "SPAM detectado no canal " + ch.getAsMention(), guild, msg.getContentRaw());
			});

			MutedMember m = Helper.getOr(MemberDAO.getMutedMemberById(member.getId()), new MutedMember(member.getId(), guild.getId()));

			m.setReason("Auto-mute por SPAM no canal " + ch.getAsMention());
			m.mute(gc.getMuteTime());

			List<PermissionOverrideAction> act = new ArrayList<>();
			for (TextChannel chn : guild.getTextChannels()) {
				act.add(chn.putPermissionOverride(member).deny(Helper.ALL_MUTE_PERMISSIONS));
			}

			RestAction.allOf(act)
					.queue(s -> {
						Helper.logToChannel(guild.getSelfMember().getUser(), false, null, member.getAsMention() + " foi silenciado por " + Helper.toStringDuration(gc.getMuteTime()) + ".\nRazão: `" + m.getReason() + "`", guild);
						MemberDAO.saveMutedMember(m);
					}, Helper::doNothing);
		}
	}

	private void handleExchange(User u, Message msg) {
		if (BotExchange.isBotAdded(u.getId()) && msg.getMentionedUsers().stream().anyMatch(usr -> usr.getId().equals(Main.getSelfUser().getId()))) {
			BotExchange be = BotExchange.getById(u.getId());

			if (be.matchTrigger(msg.getContentRaw())) {
				if (be.getReactionEmote() != null) msg.addReaction(be.getReactionEmote()).queue();
			} else if (be.matchConfirmation(msg.getContentRaw())) {
				Map<String, String> vals = be.getConfirmationValues(msg.getContentRaw());

				User from = msg.getMentionedUsers().stream()
						.filter(usr -> usr.getId().equals(vals.get("from")))
						.findFirst().orElseThrow();

				int value = Integer.parseInt(vals.get("value").replace(",", ""));

				Account acc = AccountDAO.getAccount(from.getId());
				acc.addVCredit((long) Math.ceil(value * be.getRate()), this.getClass());
				AccountDAO.saveAccount(acc);

				msg.getChannel().sendMessage("✅ | Obrigada, seus " + Helper.separate(value) + " " + Helper.separate(be.getCurrency()) + (value != 1 ? "s" : "") + " foram convertidos em " + (long) (value * be.getRate()) + " créditos voláteis com sucesso!").queue();
			}
		}
	}

	public Map<String, CopyOnWriteArrayList<SimpleMessageListener>> getHandler() {
		return toHandle;
	}

	public void addHandler(Guild guild, SimpleMessageListener sml) {
		getHandler().computeIfAbsent(guild.getId(), k -> new CopyOnWriteArrayList<>()).add(sml);
	}

	public Map<String, VoiceTime> getVoiceTimes() {
		return voiceTimes;
	}
}
