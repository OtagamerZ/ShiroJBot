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

package com.kuuhaku.events.guild;

import com.kuuhaku.Main;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL.Waifu;
import com.kuuhaku.controller.SQLiteOld;
import com.kuuhaku.events.JDAEvents;
import com.kuuhaku.model.CustomAnswers;
import com.kuuhaku.model.Log;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

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
			String rawMessage = message.getContentRaw();
			assert member != null;

			String prefix = "";
			if (!Main.getInfo().isDev()) {
				try {
					prefix = SQLiteOld.getGuildPrefix(guild.getId());
				} catch (NoResultException | NullPointerException ignore) {
				}
			} else prefix = Main.getInfo().getDefaultPrefix();

			if (rawMessage.startsWith(";") && author.getId().equals(Main.getInfo().getNiiChan())) {
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
			}

			if (author.isBot() && !Main.getInfo().getSelfUser().getId().equals(author.getId())) return;

			try {
				SQLiteOld.getMemberById(author.getId() + guild.getId());
			} catch (NoResultException e) {
				SQLiteOld.addMemberToDB(member);
			}

		/*
		if(event.getPrivateChannel()!=null) {
			try {
				Helper.sendPM(author, Helper.formatMessage(Messages.PM_CHANNEL, "help", author));
			} catch (Exception e) {
				DiscordHelper.sendAutoDeleteMessage(channel, YuiHelper.formatMessage(Messages.PM_CHANNEL, "help", author));
			}
			return;
		}

		if(message.getInvites().size()>0 && Helper.getPrivilegeLevel(member) == PrivilegeLevel.USER) {
            message.delete().queue();
            try {
				Helper.sendPM(author, Messages.INVITE_SENT);
            } catch (Exception e) {
				Helper.sendPM(author, ":x: | ");
            }
            return;
        }
		*/

			if (SQLiteOld.getGuildNoSpamChannels(guild.getId()).contains(channel.getId()) && author != Main.getInfo().getSelfUser()) {
				if (SQLiteOld.getGuildById(guild.getId()).isHardAntispam()) {
					channel.getHistory().retrievePast(20).queue(h -> {
						h.removeIf(m -> ChronoUnit.MILLIS.between(m.getTimeCreated().toLocalDateTime(), OffsetDateTime.now().atZoneSameInstant(ZoneOffset.UTC)) > 5000 || m.getAuthor() != author);

						countSpam(member, channel, guild, h);
					});
				} else {
					channel.getHistory().retrievePast(20).queue(h -> {
						h.removeIf(m -> ChronoUnit.MILLIS.between(m.getTimeCreated().toLocalDateTime(), OffsetDateTime.now().atZoneSameInstant(ZoneOffset.UTC)) > 5000 || m.getAuthor() != author && StringUtils.containsIgnoreCase(m.getContentRaw(), message.getContentRaw()));

						countSpam(member, channel, guild, h);
					});
				}
			}

			if (message.getContentRaw().trim().equals("<@" + Main.getInfo().getSelfUser().getId() + ">")) {
				channel.sendMessage("Quer saber como pode usar meus comandos? Digite `" + prefix + "ajuda` para ver todos eles ordenados por categoria!").queue();
				return;
			}

			String rawMsgNoPrefix = rawMessage;
			String commandName = "";
			if (rawMessage.toLowerCase().startsWith(prefix)) {
				rawMsgNoPrefix = rawMessage.substring(prefix.length()).trim();
				commandName = rawMsgNoPrefix.split(" ")[0].trim();
			}

			try {
				CustomAnswers ca = SQLiteOld.getCAByTrigger(rawMessage, guild.getId());
				if (!Objects.requireNonNull(ca).isMarkForDelete() && author != Main.getInfo().getSelfUser())
					Helper.typeMessage(channel, Objects.requireNonNull(ca).getAnswer().replace("%user%", author.getAsMention()).replace("%guild%", guild.getName()));
			} catch (NoResultException | NullPointerException ignore) {
			}

			boolean hasArgs = (rawMsgNoPrefix.split(" ").length > 1);
			String[] args = new String[]{};
			if (hasArgs) {
				args = Arrays.copyOfRange(rawMsgNoPrefix.split(" "), 1, rawMsgNoPrefix.split(" ").length);
			}

			boolean found = false;
			if (!guild.getSelfMember().hasPermission(Permission.MESSAGE_WRITE)) {
				return;
			}
			for (Command command : Main.getCommandManager().getCommands()) {
				found = JDAEvents.isFound(commandName, found, command);

				if (found) {
					com.kuuhaku.controller.MySQL.Log.saveLog(new Log().setGuild(guild.getName()).setUser(author.getAsTag()).setCommand(commandName));
					Helper.logToChannel(author, true, command, "Um comando foi usado no canal " + ((TextChannel) channel).getAsMention(), guild);
					if (JDAEvents.checkPermissions(event, author, member, message, channel, guild, prefix, rawMsgNoPrefix, args, command))
						break;
				}
			}

			if (!found && !author.isBot()) {
				MessageChannel lvlChannel = null;
				try {
					lvlChannel = guild.getTextChannelById(SQLiteOld.getGuildCanalLvlUp(guild.getId()));
				} catch (Exception ignore) {
				}
				try {
					Map<String, Object> rawLvls = SQLiteOld.getGuildCargosLvl(guild.getId()).entrySet().stream().filter(e -> SQLiteOld.getMemberById(author.getId() + guild.getId()).getLevel() >= Integer.parseInt(e.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
					Map<Integer, Role> sortedLvls = new TreeMap<>();
					rawLvls.forEach((k, v) -> sortedLvls.put(Integer.parseInt(k), guild.getRoleById((String) v)));
					MessageChannel finalLvlChannel = lvlChannel;
					sortedLvls.keySet().stream().max(Integer::compare).ifPresent(i -> {
						if (SQLiteOld.getGuildById(guild.getId()).getLvlNotif() && !member.getRoles().contains(sortedLvls.get(i))) {
							try {
								guild.addRoleToMember(member, sortedLvls.get(i)).queue(s -> {
									String content = author.getAsMention() + " ganhou o cargo " + sortedLvls.get(i).getAsMention() + "! :tada:";
									if (finalLvlChannel != null) {
										finalLvlChannel.getHistory().retrievePast(5).queue(m -> {
											if (m.stream().noneMatch(c -> c.getContentRaw().equals(content))) {
												finalLvlChannel.sendMessage(content).queue();
											}
										});
									} else {
										channel.getHistory().retrievePast(5).queue(m -> {
											if (m.stream().noneMatch(c -> c.getContentRaw().equals(content))) {
												channel.sendMessage(content).queue();
											}
										});
									}
								});
							} catch (IllegalArgumentException e) {
								SQLiteOld.updateGuildCargosLvl(String.valueOf(i), null, SQLiteOld.getGuildById(guild.getId()));
							}
						}
						rawLvls.remove(String.valueOf(i));
						List<Role> list = new ArrayList<>();
						rawLvls.forEach((k, v) -> list.add(guild.getRoleById((String) v)));
						guild.modifyMemberRoles(member, null, list).queue();
					});
				} catch (InsufficientPermissionException ignore) {
				}

				if (Main.getInfo().getQueue().stream().anyMatch(u -> u[1].getId().equals(author.getId()))) {
					final User[][] hw = {new User[2]};
					Main.getInfo().getQueue().stream().filter(u -> u[1].getId().equals(author.getId())).findFirst().ifPresent(users -> hw[0] = users);
					switch (message.getContentRaw().toLowerCase()) {
						case "sim":
							channel.sendMessage("Eu os declaro husbando e waifu, pode trancar ela no porão agora!").queue();
							Waifu.saveMemberWaifu(SQLiteOld.getMemberById(hw[0][0].getId() + guild.getId()), hw[0][1]);
							SQLiteOld.saveMemberWaifu(SQLiteOld.getMemberById(hw[0][0].getId() + guild.getId()), hw[0][1]);
							Waifu.saveMemberWaifu(SQLiteOld.getMemberById(hw[0][1].getId() + guild.getId()), hw[0][0]);
							SQLiteOld.saveMemberWaifu(SQLiteOld.getMemberById(hw[0][1].getId() + guild.getId()), hw[0][0]);
							Main.getInfo().getQueue().removeIf(u -> u[0].getId().equals(author.getId()) || u[1].getId().equals(author.getId()));
							break;
						case "não":
							channel.sendMessage("Pois é, hoje não tivemos um casamento, que pena.").queue();
							Main.getInfo().getQueue().removeIf(u -> u[0].getId().equals(author.getId()) || u[1].getId().equals(author.getId()));
							break;
					}
				}
				if (SQLiteOld.getGuildNoLinkChannels(guild.getId()).contains(channel.getId()) && Helper.findURL(message.getContentRaw())) {
					message.delete().reason("Mensagem possui um URL").queue(m -> channel.sendMessage(member.getAsMention() + ", é proibido postar links neste canal!").queue());
				}

				try {
					com.kuuhaku.model.Member m = SQLiteOld.getMemberById(member.getUser().getId() + member.getGuild().getId());
					if (m.getMid() == null) SQLiteOld.saveMemberMid(m, author);
					boolean lvlUp = m.addXp(guild);
					if (lvlUp && SQLiteOld.getGuildById(guild.getId()).getLvlNotif()) {
						if (lvlChannel != null) {
							lvlChannel.sendMessage(author.getAsMention() + " subiu para o nível " + m.getLevel() + ". GGWP! :tada:").queue();
						} else
							channel.sendMessage(author.getAsMention() + " subiu para o nível " + m.getLevel() + ". GGWP! :tada:").queue();
					}
					SQLiteOld.saveMemberToDB(m);
				} catch (NoResultException e) {
					SQLiteOld.addMemberToDB(member);
				}
			}
		} catch (InsufficientPermissionException ignore) {

		} catch (ErrorResponseException e) {
			Helper.logger(this.getClass()).error(e.getErrorCode() + ": " + e + " | " + e.getStackTrace()[0]);
		}
	}

	private void countSpam(Member member, MessageChannel channel, Guild guild, List<Message> h) {
		if (h.size() >= SQLiteOld.getGuildById(guild.getId()).getNoSpamAmount()) {
			h.forEach(m -> channel.deleteMessageById(m.getId()).queue());
			channel.sendMessage(":warning: | Opa, sem spam meu amigo!").queue();
			try {
				member.getRoles().add(guild.getRoleById(SQLiteOld.getGuildCargoWarn(guild.getId())));
				Main.getInfo().getScheduler().schedule(() -> member.getRoles().remove(guild.getRoleById(SQLiteOld.getGuildCargoWarn(guild.getId()))), SQLiteOld.getGuildWarnTime(guild.getId()), TimeUnit.SECONDS);
			} catch (Exception ignore) {
			}
		}
	}
}