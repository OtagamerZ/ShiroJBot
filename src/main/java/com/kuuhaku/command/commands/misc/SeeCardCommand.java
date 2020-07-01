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

package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

public class SeeCardCommand extends Command {

	public SeeCardCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public SeeCardCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public SeeCardCommand(@NonNls String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public SeeCardCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 2) {
			channel.sendMessage(":x: | Você precisa informar uma carta e o tipo dela (`N` = normal, `C` = cromada).").queue();
			return;
		}

		Card tc = CardDAO.getCard(args[0], true);
		boolean foil = args[1].equalsIgnoreCase("C");
		KawaiponCard card = new KawaiponCard(tc, foil);
		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
		if (tc == null) {
			channel.sendMessage(":x: | Essa carta não existe.").queue();
			return;
		}

		EmbedBuilder eb = new EmbedBuilder();

		eb.setTitle(tc.getName() + " (" + tc.getRarity().toString() + ")");
		eb.addField("Obtida:", kp.getCards().contains(card) ? "SIM" : "NÃO", false);
		eb.setImage("attachment://kawaipon.jpg");

		channel.sendMessage(eb.build()).addFile(Helper.getBytes())
	}
}