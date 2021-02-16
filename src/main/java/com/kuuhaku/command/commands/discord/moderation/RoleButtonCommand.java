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

package com.kuuhaku.command.commands.discord.moderation;

import com.coder4.emoji.EmojiUtils;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;

@Command(
		name = "botaocargo",
		aliases = {"rolebutton", "rb"},
		usage = "req_role-button",
		category = Category.MODERATION
)
@Requires({Permission.MANAGE_ROLES, Permission.MESSAGE_EXT_EMOJI})
public class RoleButtonCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (args.length == 1 && Helper.equalsAny(args[0], "reboot", "regen", "reset", "restart", "refresh")) {
			Helper.refreshButtons(gc);
			channel.sendMessage("✅ | Botões atualizados com sucesso!").queue();
			return;
		} else if (args.length < 3) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_role-chooser-invalid-arguments")).queue();
			return;
		} else if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-message-id")).queue();
			return;
		} else if (message.getMentionedRoles().isEmpty()) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_role-chooser-invalid-role")).queue();
			return;
		} else if (message.getMentionedRoles().get(0).getPosition() > guild.getSelfMember().getRoles().get(0).getPosition()) {
			channel.sendMessage("❌ | Não posso manipular cargos que estejam acima de mim.").queue();
			return;
		} else if (args[1].equals(Helper.CANCEL)) {
			channel.sendMessage(MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("err_role-chooser-cannot-assign-role"), Helper.CANCEL)).queue();
			return;
		} else if (!EmojiUtils.containsEmoji(args[1]) && message.getEmotes().isEmpty()) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_role-chooser-invalid emote")).queue();
			return;
		}

		try {
			Helper.addButton(args, message, channel, gc, message.getEmotes().size() > 0 ? message.getEmotes().get(0).getId() : args[1], false);

			channel.sendMessage("✅ | Botão adicionado com sucesso!").queue(s -> Helper.refreshButtons(gc));
		} catch (IllegalArgumentException e) {
			channel.sendMessage(MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("err_role-chooser-argument-error"), e)).queue();
		} catch (ErrorResponseException e) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_role-chooser-invalid-channel")).queue();
		}
	}
}
