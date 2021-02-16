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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.sqlite.CustomAnswerDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.CustomAnswer;
import com.kuuhaku.utils.Helper;
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
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length == 0) {
			channel.sendMessage("❌ | Você precisa especificar um ID.").queue();
			return;
		} else if (Helper.equalsAny(args[0], "nada", "nothing")) {
			List<CustomAnswer> cas = CustomAnswerDAO.getCAByGuild(guild.getId());
			for (CustomAnswer ca : cas) {
				CustomAnswerDAO.removeCAFromDB(ca);
			}
			channel.sendMessage("Não vou mais responder nenhuma mensagem customizada configurada até este momento.").queue();
			return;
		} else if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage("❌ | O ID deve ser um valor numérico informado na lista de respostas (`" + prefix + "fale lista`).").queue();
			return;
		}

		CustomAnswer ca = CustomAnswerDAO.getCAByID(Integer.parseInt(args[0]));

		if (ca != null) {
			CustomAnswerDAO.removeCAFromDB(ca);
			channel.sendMessage("Não vou mais responder com a resposta `" + args[0] + "`.").queue();
		} else {
			channel.sendMessage("❌ | ID de resposta inválido.").queue();
		}
	}
}
