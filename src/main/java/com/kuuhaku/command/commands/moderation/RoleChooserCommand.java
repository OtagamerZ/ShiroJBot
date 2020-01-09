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

package com.kuuhaku.command.commands.moderation;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.model.guildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

public class RoleChooserCommand extends Command {

	public RoleChooserCommand() {
		super("botaocargo", new String[]{"rolebutton", "bc", "rb"}, "<reset>/[<ID> <emote> <@cargo>]", "Adiciona botões de cargo na mensagem informada.", Category.MODERACAO);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		guildConfig gc = GuildDAO.getGuildById(guild.getId());

		if (args.length == 1 && Helper.containsAny(args[0], "reboot", "reset", "restart", "refresh")) {
			Helper.refreshButtons(gc, author);
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
		}

		try {
			JSONObject root = gc.getButtonConfigs();
			String msgId = channel.retrieveMessageById(args[0]).complete().getId();

			JSONObject msg = new JSONObject();

			JSONObject btn = new JSONObject();
			btn.put("emote", args[1]);
			btn.put("role", message.getMentionedRoles().get(0).getId());

			if (!root.has(msgId)) {
				msg.put("msgId", msgId);
				msg.put("canalId", channel.getId());
				msg.put("buttons", new JSONObject());
			} else {
				msg = root.getJSONObject(msgId);
			}

			msg.getJSONObject("buttons").put(args[1], btn);

			root.put(msgId, msg);

			gc.setButtonConfigs(root);
			GuildDAO.updateGuildSettings(gc);
			channel.sendMessage("Botão adicionado com sucesso!").queue();
			Helper.refreshButtons(gc, author);
		} catch (IllegalArgumentException e) {
			channel.sendMessage(":x: | Erro em um dos argumentos: " + e).queue();
		}
	}
}
