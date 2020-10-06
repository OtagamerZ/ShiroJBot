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

package com.kuuhaku.events.guild;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.TagDAO;
import com.kuuhaku.controller.sqlite.BlacklistDAO;
import com.kuuhaku.controller.sqlite.CustomAnswerDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.enums.BotExchange;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.enums.PrivilegeLevel;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.CustomAnswers;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.model.persistent.MutedMember;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;

import javax.annotation.Nonnull;
import javax.persistence.NoResultException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GuildEvents extends ListenerAdapter {
	@Override
	public void onGuildMessageUpdate(@NotNull GuildMessageUpdateEvent event) {
		onGuildMessageReceived(new GuildMessageReceivedEvent(event.getJDA(), event.getResponseNumber(), event.getMessage()));
	}

	@Override
	public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
		try {
			User author = event.getAuthor();
			Member member = event.getMember();
			Message message = event.getMessage();
			MessageChannel channel = message.getChannel();
			Guild guild = message.getGuild();
			String rawMessage = StringUtils.normalizeSpace(message.getContentRaw());
			assert member != null;

			String prefix = "";
			if (!Main.getInfo().isDev()) {
				try {
					prefix = GuildDAO.getGuildById(guild.getId()).getPrefix().toLowerCase();
				} catch (NoResultException | NullPointerException ignore) {
				}
			} else prefix = Main.getInfo().getDefaultPrefix().toLowerCase();

			if (rawMessage.startsWith(";") && ShiroInfo.getDevelopers().contains(author.getId())) {
				try {
					if (rawMessage.replace(";", "").length() == 0) {
						channel.sendFile(message.getAttachments().get(0).downloadToFile().get()).queue();
					} else {
						MessageAction send = channel.sendMessage(Helper.makeEmoteFromMention(rawMessage.substring(1).split(" ")));
						message.getAttachments().forEach(a -> {
							try {
								//noinspection ResultOfMethodCallIgnored
								send.addFile(a.downloadToFile().get());
							} catch (InterruptedException | ExecutionException ignore) {
							}
						});
						send.queue();
						message.delete().queue();
					}
				} catch (InsufficientPermissionException | ExecutionException | InterruptedException ignore) {
				}
				return;
			}

			if (author.isBot() && !Main.getInfo().getSelfUser().getId().equals(author.getId())) {
				handleExchange(author, message);
				return;
			}

			boolean blacklisted = BlacklistDAO.isBlacklisted(author);

			if (!blacklisted) try {
				MemberDAO.getMemberById(author.getId() + guild.getId());
			} catch (NoResultException e) {
				MemberDAO.addMemberToDB(member);
			}

			/*try {
				MutedMember mm = com.kuuhaku.controller.postgresql.MemberDAO.getMutedMemberById(author.getId());
				if (mm != null && mm.isMuted()) {
					message.delete().complete();
					return;
				}
			} catch (InsufficientPermissionException | ErrorResponseException ignore) {
			}*/

			if (guild.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE) && GuildDAO.getGuildById(guild.getId()).getNoSpamChannels().contains(channel.getId()) && author != Main.getInfo().getSelfUser()) {
				if (GuildDAO.getGuildById(guild.getId()).isHardAntispam()) {
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

			if (rawMessage.trim().equals("<@" + Main.getInfo().getSelfUser().getId() + ">") || rawMessage.trim().equals("<@!" + Main.getInfo().getSelfUser().getId() + ">")) {
				channel.sendMessage("Quer saber como pode usar meus comandos? Digite `" + prefix + "ajuda` para ver todos eles ordenados por categoria!").queue(null, Helper::doNothing);
				return;
			}

			if (!author.isBot()) Main.getInfo().cache(guild, message);

			String rawMsgNoPrefix = rawMessage;
			String commandName = "";
			if (rawMessage.toLowerCase().startsWith(prefix)) {
				rawMsgNoPrefix = rawMessage.substring(prefix.length()).trim();
				commandName = rawMsgNoPrefix.split(" ")[0].trim();
			}

			try {
				CustomAnswers ca = CustomAnswerDAO.getCAByTrigger(rawMessage, guild.getId());
				if (!Objects.requireNonNull(ca).isMarkForDelete() && author != Main.getInfo().getSelfUser())
					Helper.typeMessage(channel, Objects.requireNonNull(ca).getAnswer().replace("%user%", author.getAsMention()).replace("%guild%", guild.getName()));
			} catch (NoResultException | NullPointerException ignore) {
			}

			boolean hasArgs = (rawMsgNoPrefix.split(" ").length > 1);
			String[] args = new String[]{};
			if (hasArgs) {
				args = Arrays.copyOfRange(rawMsgNoPrefix.split(" "), 1, rawMsgNoPrefix.split(" ").length);
				args = ArrayUtils.removeAllOccurences(args, "");
			}

			boolean found = false;
			if (!guild.getSelfMember().hasPermission(Permission.MESSAGE_WRITE)) {
				return;
			}

			Command command = Main.getCommandManager().getCommand(commandName);
			if (command != null) {
				found = command.getCategory().isEnabled(GuildDAO.getGuildById(guild.getId()), guild, author);

				if (found) {
					if (Helper.showMMError(author, channel, guild, rawMessage, command)) return;

					if (command.getCategory() == Category.NSFW && !((TextChannel) channel).isNSFW()) {
						try {
							channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_nsfw-in-non-nsfw-channel")).queue();
						} catch (InsufficientPermissionException ignore) {
						}
						return;
					} else if (!Helper.hasPermission(member, command.getCategory().getPrivilegeLevel())) {
						try {
							channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_not-enough-permission")).queue();
						} catch (InsufficientPermissionException ignore) {
						}
						return;
					} else if (blacklisted) {
						channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_user-blacklisted")).queue();
						return;
					} else if (Main.getInfo().getRatelimit().getIfPresent(author.getId()) != null) {
						channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_user-ratelimited")).queue();
						return;
					}

					command.execute(author, member, rawMsgNoPrefix, args, message, channel, guild, prefix);
					if (!TagDAO.getTagById(author.getId()).isBeta() || !Helper.hasPermission(member, PrivilegeLevel.SUPPORT))
						Main.getInfo().getRatelimit().put(author.getId(), true);
					Helper.spawnAd(channel);
				}
			}

			if (!found && !author.isBot() && !blacklisted) {
				GuildConfig gc = GuildDAO.getGuildById(guild.getId());

				Account acc = AccountDAO.getAccount(author.getId());
				if (!acc.getTwitchId().isBlank() && channel.getId().equals(ShiroInfo.getTwitchChannelID()) && Main.getInfo().isLive()) {
					Main.getTwitch().getChat().sendMessage("kuuhaku_otgmz", author.getName() + " disse: " + Helper.stripEmotesAndMentions(rawMessage));
				}

				if (!TagDAO.getTagById(guild.getOwnerId()).isToxic()) {
					if (gc.isKawaiponEnabled()) Helper.spawnKawaipon(gc, (TextChannel) channel);
					if (gc.isDropEnabled()) Helper.spawnDrop(gc, (TextChannel) channel);
				}

				MessageChannel lvlChannel = null;
				try {
					lvlChannel = guild.getTextChannelById(GuildDAO.getGuildById(guild.getId()).getCanalLvl());
				} catch (Exception ignore) {
				}
				try {
					Map<String, Object> rawLvls = GuildDAO.getGuildById(guild.getId()).getCargoslvl().entrySet().stream().filter(e -> MemberDAO.getMemberById(author.getId() + guild.getId()).getLevel() >= Integer.parseInt(e.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
					Map<Integer, Role> sortedLvls = new TreeMap<>();
					rawLvls.forEach((k, v) -> sortedLvls.put(Integer.parseInt(k), guild.getRoleById((String) v)));
					MessageChannel finalLvlChannel = lvlChannel;
					sortedLvls.keySet().stream().max(Integer::compare).ifPresent(i -> {
						if (GuildDAO.getGuildById(guild.getId()).isLvlNotif() && !member.getRoles().contains(sortedLvls.get(i))) {
							try {
								if (!guild.getSelfMember().getRoles().get(0).canInteract(sortedLvls.get(i))) return;
								guild.addRoleToMember(member, sortedLvls.get(i)).queue(s -> {
									String content = author.getAsMention() + " ganhou o cargo " + sortedLvls.get(i).getAsMention() + "! :tada:";
									if (finalLvlChannel != null) {
										finalLvlChannel.getHistory().retrievePast(5).queue(m -> {
											if (m.stream().noneMatch(c -> c.getContentRaw().equals(content))) {
												finalLvlChannel.sendMessage(content).queue();
											}
										}, Helper::doNothing);
									} else {
										channel.getHistory().retrievePast(5).queue(m -> {
											if (m.stream().noneMatch(c -> c.getContentRaw().equals(content))) {
												channel.sendMessage(content).queue();
											}
										}, Helper::doNothing);
									}
								}, Helper::doNothing);
							} catch (IllegalArgumentException e) {
								Map<String, Object> cl = gc.getCargoslvl();
								cl.remove(String.valueOf(i));
								GuildDAO.updateGuildSettings(gc);
							} catch (ErrorResponseException ignore) {
							}
						}
						rawLvls.remove(String.valueOf(i));
						List<Role> list = new ArrayList<>();
						rawLvls.forEach((k, v) -> {
							Role r = guild.getRoleById((String) v);

							if (r != null) list.add(r);
							else {
								Map<String, Object> cl = gc.getCargoslvl();
								cl.remove(String.valueOf(i));
								GuildDAO.updateGuildSettings(gc);
							}
						});
						guild.modifyMemberRoles(member, null, list).queue(null, Helper::doNothing);
					});
				} catch (HierarchyException | InsufficientPermissionException ignore) {
				}

				try {
					if (GuildDAO.getGuildById(guild.getId()).getNoLinkChannels().contains(channel.getId()) && Helper.findURL(rawMessage)) {
						message.delete().reason("Mensagem possui um URL").queue();
						channel.sendMessage(member.getAsMention() + ", é proibido postar links neste canal!").queue();
					}

					com.kuuhaku.model.persistent.Member m = MemberDAO.getMemberById(member.getUser().getId() + member.getGuild().getId());
					if (m.getMid() == null) {
						m.setMid(author.getId());
						m.setSid(guild.getId());
					}

					boolean lvlUp = m.addXp(guild);
					try {
						if (lvlUp && GuildDAO.getGuildById(guild.getId()).isLvlNotif()) {
							Objects.requireNonNullElse(lvlChannel, channel).sendMessage(author.getAsMention() + " subiu para o nível " + m.getLevel() + ". GGWP! :tada:").queue();
						}
					} catch (InsufficientPermissionException e) {
						channel.sendMessage(author.getAsMention() + " subiu para o nível " + m.getLevel() + ". GGWP! :tada:").queue();
					}

					MemberDAO.updateMemberConfigs(m);
				} catch (NoResultException e) {
					MemberDAO.addMemberToDB(member);
				} catch (ErrorResponseException | NullPointerException e) {
					Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
				}
			}
		} catch (InsufficientPermissionException | ErrorResponseException ignore) {
		}
	}

	@Override
	public void onRoleDelete(@Nonnull RoleDeleteEvent event) {
		GuildConfig gc = GuildDAO.getGuildById(event.getGuild().getId());
		Map<String, Object> jo = gc.getCargoslvl();

		jo.entrySet().removeIf(e -> String.valueOf(e.getValue()).equals(event.getRole().getId()));

		GuildDAO.updateGuildSettings(gc);
	}

	private void countSpam(Member member, MessageChannel channel, Guild guild, List<Message> h) {
		if (h.size() >= GuildDAO.getGuildById(guild.getId()).getNoSpamAmount() && Helper.hasRoleHigherThan(guild.getSelfMember(), member)) {
			((TextChannel) channel).deleteMessagesByIds(h.stream().map(Message::getId).collect(Collectors.toList())).queue(null, Helper::doNothing);
			channel.sendMessage(":warning: | Opa, sem spam meu amigo!").queue(
					msg -> msg.delete().queueAfter(20, TimeUnit.SECONDS, null, Helper::doNothing)
			);
			try {
				Role r = guild.getRoleById(GuildDAO.getGuildById(guild.getId()).getCargoMute());
				if (r != null) {
					JSONArray roles = new JSONArray(member.getRoles().stream().map(Role::getId).collect(Collectors.toList()));
					guild.modifyMemberRoles(member, r).queue(null, Helper::doNothing);
					MutedMember mm = Helper.getOr(com.kuuhaku.controller.postgresql.MemberDAO.getMutedMemberById(member.getId()), new MutedMember(member.getId(), guild.getId(), roles));
					mm.mute(GuildDAO.getGuildById(guild.getId()).getWarnTime());

					com.kuuhaku.controller.postgresql.MemberDAO.saveMutedMember(mm);
				}
			} catch (Exception ignore) {
			}
		}
	}

	private void handleExchange(User u, Message msg) {
		if (BotExchange.isBotAdded(u.getId()) && msg.getMentionedUsers().stream().anyMatch(usr -> usr.getId().equals(Main.getInfo().getSelfUser().getId()))) {
			BotExchange be = BotExchange.getById(u.getId());

			if (be.matchTrigger(msg.getContentRaw()).find()) {
				if (be.getReactionEmote() != null) msg.addReaction(be.getReactionEmote()).queue();
			} else if (be.matchConfirmation(msg.getContentRaw()).find()) {
				String[] args = msg.getContentRaw().replaceAll(be.getConfirmation(), "").replace(",", "").split(" ");
				long value = 0;
				for (String arg : args) {
					if (StringUtils.isNumeric(arg)) {
						value = Long.parseLong(arg);
						break;
					}
				}
				if (value == 0) return;

				User target = msg.getMentionedUsers().stream().filter(usr -> !usr.getId().equals(Main.getInfo().getSelfUser().getId())).findFirst().orElse(null);
				if (target == null) return;

				Account acc = AccountDAO.getAccount(target.getId());
				acc.addCredit((long) (value * be.getRate()), this.getClass());
				AccountDAO.saveAccount(acc);

				msg.getChannel().sendMessage("Obrigada, seus " + value + " " + be.getCurrency() + (value != 1 ? "s" : "") + " foram convertidos em " + (long) (value * be.getRate()) + " créditos com sucesso!").queue();
			}
		}
	}
}
