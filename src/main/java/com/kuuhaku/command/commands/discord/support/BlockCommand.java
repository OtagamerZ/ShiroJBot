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

package com.kuuhaku.command.commands.discord.support;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.model.common.RelayBlockList;
import com.kuuhaku.model.enums.PrivilegeLevel;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.util.Arrays;

public class BlockCommand extends Command {

	public BlockCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public BlockCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public BlockCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public BlockCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		try {
			if (args.length > 2) {
				if (StringUtils.isNumeric(args[0])) {
					String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
					boolean isMentioned = message.getMentionedUsers().size() > 0;
					Member m = guild.getMember(Main.getJibril().getSelfUser());
					assert m != null;
					switch (args[1]) {
						case "temp" -> {
							RelayBlockList.blockID(isMentioned ? message.getMentionedUsers().get(0).getId() : args[0], reason);
							Main.getRelay().relayMessage(message, (isMentioned ? message.getMentionedUsers().get(0).getAsMention() : "<@" + args[0] + ">") + " bloqueado do chat global.\nRazão: " + reason, m, guild, null);
						}
						case "perma" -> {
							if (Helper.hasPermission(member, PrivilegeLevel.DEV)) {
								RelayBlockList.permaBlockID(isMentioned ? message.getMentionedUsers().get(0).getId() : args[0], reason);
								Main.getRelay().relayMessage(message, (isMentioned ? message.getMentionedUsers().get(0).getAsMention() : "<@" + args[0] + ">") + " banido permanentemente do chat global.\nRazão: " + reason, m, guild, null);
							} else {
								channel.sendMessage("❌ | Permissões insuficientes.").queue();
							}
						}
						case "thumb" -> {
							RelayBlockList.blockThumb(isMentioned ? message.getMentionedUsers().get(0).getId() : args[0]);
							Main.getRelay().relayMessage(message, (isMentioned ? message.getMentionedUsers().get(0).getAsMention() : "Avatar de <@" + args[0] + ">") + " foi censurado do chat global.", m, guild, null);
						}
						default -> channel.sendMessage("❌ | Tipo inválido, o tipo deve ser thumb, temp ou perma.").queue();
					}
				} else {
					channel.sendMessage("❌ | ID inválido, identificadores possuem apenas dígitos de 0 à 9.").queue();
				}
			} else {
				channel.sendMessage("❌ | Você precisa passar o ID do usuário a ser bloqueado, o tipo de bloqueio (thumb/temp/perma) e a razão para o bloqueio.").queue();
			}
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | ID de usuário incorreto.").queue();
		}
	}
}
