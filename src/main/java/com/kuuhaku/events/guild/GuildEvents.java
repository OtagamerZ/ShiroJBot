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
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.TagDAO;
import com.kuuhaku.controller.sqlite.CustomAnswerDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.persistent.CustomAnswers;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.model.persistent.MutedMember;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
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

			if (rawMessage.startsWith(";") && Main.getInfo().getDevelopers().contains(author.getId())) {
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
						message.delete().complete();
					}
				} catch (InsufficientPermissionException | ExecutionException | InterruptedException ignore) {
				}
				return;
			}

			if (author.isBot() && !Main.getInfo().getSelfUser().getId().equals(author.getId())) return;

			try {
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

			if (GuildDAO.getGuildById(guild.getId()).getNoSpamChannels().contains(channel.getId()) && author != Main.getInfo().getSelfUser()) {
				if (GuildDAO.getGuildById(guild.getId()).isHardAntispam()) {
					List<Message> h = channel.getHistory().retrievePast(20).complete();
					h.removeIf(m -> ChronoUnit.MILLIS.between(m.getTimeCreated().toLocalDateTime(), OffsetDateTime.now().atZoneSameInstant(ZoneOffset.UTC)) > 5000 || m.getAuthor() != author);

					countSpam(member, channel, guild, h);
				} else {
					List<Message> h = channel.getHistory().retrievePast(20).complete();
					h.removeIf(m -> ChronoUnit.MILLIS.between(m.getTimeCreated().toLocalDateTime(), OffsetDateTime.now().atZoneSameInstant(ZoneOffset.UTC)) > 5000 || m.getAuthor() != author && StringUtils.containsIgnoreCase(m.getContentRaw(), rawMessage));

					countSpam(member, channel, guild, h);
				}
			}

			if (rawMessage.trim().equals("<@" + Main.getInfo().getSelfUser().getId() + ">") || rawMessage.trim().equals("<@!" + Main.getInfo().getSelfUser().getId() + ">")) {
				channel.sendMessage("Quer saber como pode usar meus comandos? Digite `" + prefix + "ajuda` para ver todos eles ordenados por categoria!").complete();
				return;
			}

			if (!author.isBot()) ShiroInfo.cache(guild, message);

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

					Helper.checkPermissions(author, member, message, channel, guild, prefix, rawMsgNoPrefix, args, command);
				}
			}

			if (!found && !author.isBot()) {
				GuildConfig gc = GuildDAO.getGuildById(guild.getId());

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
								guild.addRoleToMember(member, sortedLvls.get(i)).complete();
								String content = author.getAsMention() + " ganhou o cargo " + sortedLvls.get(i).getAsMention() + "! :tada:";
								if (finalLvlChannel != null) {
									List<Message> m = finalLvlChannel.getHistory().retrievePast(5).complete();
									if (m.stream().noneMatch(c -> c.getContentRaw().equals(content))) {
										finalLvlChannel.sendMessage(content).complete();
									}
								} else {
									List<Message> m = channel.getHistory().retrievePast(5).complete();
									if (m.stream().noneMatch(c -> c.getContentRaw().equals(content))) {
										channel.sendMessage(content).complete();
									}
								}
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
						guild.modifyMemberRoles(member, null, list).complete();
					});
				} catch (InsufficientPermissionException ignore) {
				}

				try {
					if (GuildDAO.getGuildById(guild.getId()).getNoLinkChannels().contains(channel.getId()) && Helper.findURL(rawMessage)) {
						message.delete().reason("Mensagem possui um URL").complete();
						channel.sendMessage(member.getAsMention() + ", é proibido postar links neste canal!").complete();
					}

					com.kuuhaku.model.persistent.Member m = MemberDAO.getMemberById(member.getUser().getId() + member.getGuild().getId());
					if (m.getMid() == null) {
						m.setMid(author.getId());
						m.setSid(guild.getId());
						MemberDAO.updateMemberConfigs(m);
					}

					boolean lvlUp = m.addXp(guild);
					if (lvlUp && GuildDAO.getGuildById(guild.getId()).isLvlNotif()) {
						if (lvlChannel != null) {
							lvlChannel.sendMessage(author.getAsMention() + " subiu para o nível " + m.getLevel() + ". GGWP! :tada:").complete();
						} else
							channel.sendMessage(author.getAsMention() + " subiu para o nível " + m.getLevel() + ". GGWP! :tada:").complete();
					}
					MemberDAO.updateMemberConfigs(m);
				} catch (NoResultException e) {
					MemberDAO.addMemberToDB(member);
				} catch (ErrorResponseException ignore) {
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
				Role r = guild.getRoleById(GuildDAO.getGuildById(guild.getId()).getCargoWarn());
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
}
