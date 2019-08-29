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
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.CustomAnswers;
import com.kuuhaku.model.guildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
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

	@Override//removeGuildFromDB
	public void onGuildJoin(GuildJoinEvent event) {
		SQLite.addGuildToDB(event.getGuild());
		Main.getInfo().getDevelopers().forEach(d -> Main.getInfo().getUserByID(d).openPrivateChannel().queue(c -> {
			String msg = "Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".";
			c.sendMessage(msg).queue();
		}));
		Helper.log(this.getClass(), LogLevel.INFO, "Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".");
	}

	@Override
	public void onGuildLeave(GuildLeaveEvent event) {
		guildConfig gc = new guildConfig();
		gc.setGuildId(event.getGuild().getId());
		SQLite.removeGuildFromDB(gc);
		Main.getInfo().getDevelopers().forEach(d -> Main.getInfo().getUserByID(d).openPrivateChannel().queue(c -> {
			String msg = "Acabei de sair do servidor \"" + event.getGuild().getName() + "\".";
			c.sendMessage(msg).queue();
		}));
		Helper.log(this.getClass(), LogLevel.INFO, "Acabei de sair do servidor \"" + event.getGuild().getName() + "\".");
	}

	@Override
	public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
		if (Main.getInfo().isReady()) {
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
					prefix = SQLite.getGuildPrefix(guild.getId());
				} catch (NoResultException | NullPointerException ignore) {
				}
			} else prefix = Main.getInfo().getDefaultPrefix();

			if (rawMessage.startsWith(";") && author.getId().equals(Main.getInfo().getNiiChan())) {
				try {
					message.delete().queue();
					if (rawMessage.replace(";", "").length() == 0) {
						channel.sendFile(message.getAttachments().get(0).downloadToFile().get()).queue();
					} else {
						MessageAction send = channel.sendMessage(rawMessage.substring(1));
						message.getAttachments().forEach(a -> {
							try {
								//noinspection ResultOfMethodCallIgnored
								send.addFile(a.downloadToFile().get());
							} catch (InterruptedException | ExecutionException ignore) {
							}
						});
						send.queue();
					}
				} catch (InsufficientPermissionException | ExecutionException | InterruptedException ignore) {
				}
			}

			if (author.isBot() && !Main.getInfo().getSelfUser().getId().equals(author.getId())) return;

			try {
				SQLite.getMemberById(author.getId() + guild.getId());
			} catch (NoResultException e) {
				SQLite.addMemberToDB(member);
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

			Helper.battle(event);

			if (SQLite.getGuildNoSpamChannels(guild.getId()).contains(channel.getId()) && author != Main.getInfo().getSelfUser()) {
				if (SQLite.getGuildById(guild.getId()).isHardAntispam()) {
					channel.getHistory().retrievePast(20).queue(h -> {
						h.removeIf(m -> ChronoUnit.MILLIS.between(m.getTimeCreated().toLocalDateTime(), OffsetDateTime.now().atZoneSameInstant(ZoneOffset.UTC)) > 5000 || m.getAuthor() != author);

						if (h.size() >= SQLite.getGuildById(guild.getId()).getNoSpamAmount()) {
							h.forEach(m -> channel.deleteMessageById(m.getId()).queue());
							channel.sendMessage(":warning: | Opa, sem spam meu amigo!").queue();
							try {
								member.getRoles().add(guild.getRoleById(SQLite.getGuildCargoWarn(guild.getId())));
								Main.getInfo().getScheduler().schedule(() -> member.getRoles().remove(guild.getRoleById(SQLite.getGuildCargoWarn(guild.getId()))), SQLite.getGuildWarnTime(guild.getId()), TimeUnit.SECONDS);
							} catch (Exception ignore) {
							}
						}
					});
				} else {
					channel.getHistory().retrievePast(20).queue(h -> {
						h.removeIf(m -> ChronoUnit.MILLIS.between(m.getTimeCreated().toLocalDateTime(), OffsetDateTime.now().atZoneSameInstant(ZoneOffset.UTC)) > 5000 || m.getAuthor() != author && StringUtils.containsIgnoreCase(m.getContentRaw(), message.getContentRaw()));

						if (h.size() >= SQLite.getGuildById(guild.getId()).getNoSpamAmount()) {
							h.forEach(m -> channel.deleteMessageById(m.getId()).queue());
							channel.sendMessage(":warning: | Opa, sem spam meu amigo!").queue();
							try {
								member.getRoles().add(guild.getRoleById(SQLite.getGuildCargoWarn(guild.getId())));
								Main.getInfo().getScheduler().schedule(() -> member.getRoles().remove(guild.getRoleById(SQLite.getGuildCargoWarn(guild.getId()))), SQLite.getGuildWarnTime(guild.getId()), TimeUnit.SECONDS);
							} catch (Exception ignore) {
							}
						}
					});
				}
			}

			if (message.getContentDisplay().equals(Main.getInfo().getSelfUser().getAsMention())) {
				channel.sendMessage("Para obter ajuda sobre como me utilizar use `" + prefix + "ajuda`.").queue();
				return;
			}

			String rawMsgNoPrefix = rawMessage;
			String commandName = "";
			if (rawMessage.toLowerCase().startsWith(prefix)) {
				rawMsgNoPrefix = rawMessage.substring(prefix.length()).trim();
				commandName = rawMsgNoPrefix.split(" ")[0].trim();
			}

			try {
				CustomAnswers ca = SQLite.getCAByTrigger(rawMessage, guild.getId());
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
				if (command.getName().equalsIgnoreCase(commandName)) {
					found = true;
				}
				for (String alias : command.getAliases()) {
					if (alias.equalsIgnoreCase(commandName)) {
						found = true;
					}
				}
				if (command.getCategory().isEnabled()) {
					found = false;
				}

				if (found) {
					Helper.logToChannel(author, true, command, "Um comando foi usado no canal " + ((TextChannel) channel).getAsMention(), guild);
					if (Helper.hasPermission(member, command.getCategory().getPrivilegeLevel())) {
						command.execute(author, member, rawMsgNoPrefix, args, message, channel, guild, event, prefix);
						Helper.spawnAd(channel);
						break;
					}
					try {
						channel.sendMessage(":x: | Você não tem permissão para executar este comando!").queue();
						Helper.spawnAd(channel);
						break;
					} catch (InsufficientPermissionException ignore){
					}
				}
			}

			if (!found && !author.isBot()) {
				MessageChannel lvlChannel = null;
				try {
					lvlChannel = guild.getTextChannelById(SQLite.getGuildCanalLvlUp(guild.getId()));
				} catch (Exception ignore) {
				}
				Map<String, Object> rawLvls = SQLite.getGuildCargosLvl(guild.getId()).entrySet().stream().filter(e -> SQLite.getMemberById(author.getId() + guild.getId()).getLevel() >= Integer.parseInt(e.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
				Map<Integer, Role> sortedLvls = new TreeMap<>();
				rawLvls.forEach((k, v) -> sortedLvls.put(Integer.parseInt(k), guild.getRoleById((String) v)));
				MessageChannel finalLvlChannel = lvlChannel;
				sortedLvls.keySet().stream().max(Integer::compare).ifPresent(i -> {
					if (SQLite.getGuildById(guild.getId()).getLvlNotif() && !member.getRoles().contains(sortedLvls.get(i))) {
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
							SQLite.updateGuildCargosLvl(String.valueOf(i), null, SQLite.getGuildById(guild.getId()));
						}
					}
					rawLvls.remove(String.valueOf(i));
					List<Role> list = new ArrayList<>();
					rawLvls.forEach((k, v) -> list.add(guild.getRoleById((String) v)));
					guild.modifyMemberRoles(member, null, list).queue();
				});

				if (Main.getInfo().getQueue().stream().anyMatch(u -> u[1].getId().equals(author.getId()))) {
					final User[][] hw = {new User[2]};
					Main.getInfo().getQueue().stream().filter(u -> u[1].getId().equals(author.getId())).findFirst().ifPresent(users -> hw[0] = users);
					switch (message.getContentRaw().toLowerCase()) {
						case "sim":
							channel.sendMessage("Eu os declaro husbando e waifu, pode trancar ela no porão agora!").queue();
							MySQL.saveMemberWaifu(SQLite.getMemberById(hw[0][0].getId() + guild.getId()), hw[0][1]);
							SQLite.saveMemberWaifu(SQLite.getMemberById(hw[0][0].getId() + guild.getId()), hw[0][1]);
							MySQL.saveMemberWaifu(SQLite.getMemberById(hw[0][1].getId() + guild.getId()), hw[0][0]);
							SQLite.saveMemberWaifu(SQLite.getMemberById(hw[0][1].getId() + guild.getId()), hw[0][0]);
							Main.getInfo().getQueue().removeIf(u -> u[0].getId().equals(author.getId()) || u[1].getId().equals(author.getId()));
							break;
						case "não":
							channel.sendMessage("Pois é, hoje não tivemos um casamento, que pena.").queue();
							Main.getInfo().getQueue().removeIf(u -> u[0].getId().equals(author.getId()) || u[1].getId().equals(author.getId()));
							break;
					}
				}
				if (SQLite.getGuildNoLinkChannels(guild.getId()).contains(channel.getId()) && Helper.findURL(message.getContentRaw())) {
					message.delete().reason("Mensagem possui um URL").queue(m -> channel.sendMessage(member.getAsMention() + ", é proibido postar links neste canal!").queue());
				}

				try {
					com.kuuhaku.model.Member m = SQLite.getMemberById(member.getUser().getId() + member.getGuild().getId());
					if (m.getMid() == null) SQLite.saveMemberMid(m, author);
					boolean lvlUp = m.addXp();
					if (lvlUp && SQLite.getGuildById(guild.getId()).getLvlNotif()) {
						if (lvlChannel != null) {
							lvlChannel.sendMessage(author.getAsMention() + " subiu para o nível " + m.getLevel() + ". GGWP! :tada:").queue();
						} else
							channel.sendMessage(author.getAsMention() + " subiu para o nível " + m.getLevel() + ". GGWP! :tada:").queue();
					}
					SQLite.saveMemberToDB(m);
				} catch (NoResultException e) {
					SQLite.addMemberToDB(member);
				} catch (InsufficientPermissionException ignore) {
				}
			}
		}
	}
}