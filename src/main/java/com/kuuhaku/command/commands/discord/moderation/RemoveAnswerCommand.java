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

package com.kuuhaku.command.commands.discord.moderation;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.CustomAnswer;
import com.kuuhaku.utils.helpers.LogicHelper;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Command(
		name = "naofale",
		aliases = {"dontsay", "removeanswer", "removerresposta"},
		usage = "req_id-nothing",
		category = Category.MODERATION
)
public class RemoveAnswerCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			channel.sendMessage("❌ | Você precisa especificar um ID.").queue();
			return;
		} else if (LogicHelper.equalsAny(args[0], "nada", "nothing")) {
			List<CustomAnswer> cas = CustomAnswer.queryAll(CustomAnswer.class, "SELECT c FROM CustomAnswer c WHERE c.guildId = :guild", guild.getId());
			for (CustomAnswer ca : cas) {
				ca.delete();
			}

			channel.sendMessage("Não vou mais responder nenhuma das mensagens configuradas até então.").queue();
			return;
		} else if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage("❌ | O ID deve ser um valor numérico informado na lista de respostas (`" + prefix + "fale lista`).").queue();
			return;
		}

		try {
			CustomAnswer ca = CustomAnswer.query(CustomAnswer.class, "SELECT c FROM CustomAnswer c WHERE c.id = :id AND c.guildId = :guild", args[0], guild.getId());
			if (ca != null) {
				ca.delete();
				Main.getInfo().removeCustomAnswer(ca);
				channel.sendMessage("Não vou mais responder com a resposta de ID " + args[0] + ".").queue();
			} else {
				channel.sendMessage("❌ | ID de resposta inválido.").queue();
			}
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | ID de resposta inválido.").queue();
		}
	}
}
