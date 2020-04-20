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

package com.kuuhaku.command.commands.moderation;

import com.coder4.emoji.EmojiUtils;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

public class RoleChooserCommand extends Command {

	public RoleChooserCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public RoleChooserCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public RoleChooserCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public RoleChooserCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (args.length == 1 && Helper.containsAny(args[0], "reboot", "regen", "reset", "restart", "refresh")) {
			Helper.refreshButtons(gc);
			channel.sendMessage("Botões atualizados com sucesso!").queue();
			return;
		} else if (args.length < 3) {
			channel.sendMessage(":x: | É necessário informar o ID da mensagem, o emote do botão e o cargo a ser dado ao usuário que clicar nele.").queue();
			return;
		} else if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage(":x: | O ID da mensagem deve ser numérico.").queue();
			return;
		} else if (message.getMentionedRoles().size() == 0) {
			channel.sendMessage(":x: | É necessário informar o cargo a ser dado ao clicar no botão.").queue();
			return;
		} else if (args[1].equals(Helper.CANCEL)) {
			channel.sendMessage(":x: | Não é possível atribuir um cargo ao emote " + Helper.CANCEL + ".").queue();
			return;
		} else if (!EmojiUtils.containsEmoji(args[1]) && message.getEmotes().size() == 0) {
			channel.sendMessage(":x: | Emote inválido, verifique se você colocou o emote na posição correta (após o ID, separado por espaço).").queue();
			return;
		}

		try {
			Helper.addButton(args, message, channel, gc, message.getEmotes().size() > 0 ? message.getEmotes().get(0).getId() : args[1], false);

			channel.sendMessage("Botão adicionado com sucesso!").queue(s -> Helper.refreshButtons(gc));
		} catch (IllegalArgumentException e) {
			channel.sendMessage(":x: | Erro em um dos argumentos: " + e).queue();
		} catch (ErrorResponseException e) {
			channel.sendMessage(":x: | Este comando deve ser enviado no mesmo canal onde se encontra a mensagem a receber o botão.").queue();
		}
	}
}
