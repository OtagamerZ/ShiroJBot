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

import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.ibm.watson.assistant.v1.model.DialogRuntimeResponseGeneric;
import com.ibm.watson.assistant.v1.model.MessageInput;
import com.ibm.watson.assistant.v1.model.MessageOptions;
import com.ibm.watson.assistant.v1.model.MessageResponse;
import com.kuuhaku.Main;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.CustomAnswers;
import com.kuuhaku.model.guildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.persistence.NoResultException;
import java.util.List;
import java.util.Objects;

public class GuildEvents extends ListenerAdapter {

	@Override//removeGuildFromDB
	public void onGuildJoin(GuildJoinEvent event) {
		SQLite.addGuildToDB(event.getGuild());
		Main.getInfo().getDevelopers().forEach(d -> Main.getInfo().getUserByID(d).openPrivateChannel().queue(c -> c.sendMessage("Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".").queue()));
		Helper.log(this.getClass(), LogLevel.INFO, "Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".");
	}

	@Override
	public void onGuildLeave(GuildLeaveEvent event) {
		guildConfig gc = new guildConfig();
		gc.setGuildId(event.getGuild().getId());
		SQLite.removeGuildFromDB(gc);
		Main.getInfo().getDevelopers().forEach(d -> Main.getInfo().getUserByID(d).openPrivateChannel().queue(c -> c.sendMessage("Acabei de entrar no servidor \"" + event.getGuild().getName() + "\".").queue()));
		Helper.log(this.getClass(), LogLevel.INFO, "Acabei de sair do servidor \"" + event.getGuild().getName() + "\".");
	}

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (Main.getInfo().isReady()) {
			User author = event.getAuthor();
			Member member = event.getMember();
			Message message = event.getMessage();
			MessageChannel channel = message.getChannel();
			Guild guild = message.getGuild();
			String rawMessage = message.getContentRaw();

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
					channel.sendMessage(rawMessage.substring(1)).queue();
				} catch (InsufficientPermissionException ignore) {
				}
			}

			if (author.isBot() && !Main.getInfo().getSelfUser().getId().equals(author.getId())) return;

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

			if (message.getContentDisplay().equals(Main.getInfo().getSelfUser().getAsMention())) {
				channel.sendMessage("Para obter ajuda sobre como me utilizar use `" + prefix + "ajuda`.").queue();
				return;
			}

			String rawMsgNoPrefix = rawMessage;
			String commandName = "";
			if (rawMessage.toLowerCase().contains(prefix)) {
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
				args = rawMsgNoPrefix.substring(commandName.length()).trim().split(" ");
			}

			boolean found = false;
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
					if (!Helper.hasPermission(member, command.getCategory().getPrivilegeLevel())) {
						channel.sendMessage(":x: | Você não tem permissão para executar este comando!").queue();
						Helper.spawnAd(channel);
						break;
					}
					command.execute(author, member, rawMsgNoPrefix, args, message, channel, guild, event, prefix);
					Helper.spawnAd(channel);
					break;
				}
			}

			if (!found && !author.isBot()) {
				if (Helper.queue.stream().anyMatch(u -> u[1].getId().equals(author.getId()))) {
					final User[][] hw = {new User[2]};
					Helper.queue.stream().filter(u -> u[1].getId().equals(author.getId())).findFirst().ifPresent(users -> hw[0] = users);
					switch (message.getContentRaw().toLowerCase()) {
						case "sim":
							channel.sendMessage("Eu os declaro husbando e waifu, pode trancar ela no porão agora!").queue();
							MySQL.saveMemberWaifu(SQLite.getMemberById(hw[0][0].getId() + guild.getId()), hw[0][1]);
							SQLite.saveMemberWaifu(SQLite.getMemberById(hw[0][0].getId() + guild.getId()), hw[0][1]);
							MySQL.saveMemberWaifu(SQLite.getMemberById(hw[0][1].getId() + guild.getId()), hw[0][0]);
							SQLite.saveMemberWaifu(SQLite.getMemberById(hw[0][1].getId() + guild.getId()), hw[0][0]);
							Helper.queue.removeIf(u -> u[0].getId().equals(author.getId()) || u[1].getId().equals(author.getId()));
							break;
						case "não":
							channel.sendMessage("Pois é, hoje não tivemos um casamento, que pena.").queue();
							Helper.queue.removeIf(u -> u[0].getId().equals(author.getId()) || u[1].getId().equals(author.getId()));
							break;
					}
				}
				if (SQLite.getGuildNoLinkChannels(guild.getId()).contains(channel.getId()) && Helper.findURL(message.getContentRaw())) {
					message.delete().reason("Mensagem possui um URL").queue(m -> channel.sendMessage(member.getAsMention() + ", é proibido postar links neste canal!").queue());
				}
				if (SQLite.getGuildIaMode(guild.getId()) && channel.getId().equals(SQLite.getGuildCanalIA(guild.getId()))) {
					try {
						MessageInput msg = new MessageInput();
						msg.setText(message.getContentRaw());

						MessageOptions opts = new MessageOptions.Builder(Main.getInfo().getInfoInstance()).context(Main.getInfo().getContext()).input(msg).build();
						MessageResponse answer = Main.getInfo().getAi().message(opts).execute().getResult();
						Main.getInfo().updateContext(answer);

						List<DialogRuntimeResponseGeneric> responseGeneric = answer.getOutput().getGeneric();
						if (responseGeneric.size() > 0) {
							Helper.typeMessage(channel, responseGeneric.get(0).getText());
						}
					} catch (ServiceResponseException e) {
						Helper.log(this.getClass(), LogLevel.WARN, e.toString());
					}
				}
				try {
					com.kuuhaku.model.Member m = SQLite.getMemberById(member.getUser().getId() + member.getGuild().getId());
					if (m.getMid() == null) SQLite.saveMemberMid(m, author);
					boolean lvlUp = m.addXp();
					if (lvlUp && SQLite.getGuildById(guild.getId()).getLvlNotif()) {
						channel.sendMessage(member.getEffectiveName() + " subiu para o nível " + m.getLevel() + ". GGWP! :tada:").queue();
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