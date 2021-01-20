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

package com.kuuhaku.command.commands.discord.clan;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.ClanDAO;
import com.kuuhaku.model.enums.ClanPermission;
import com.kuuhaku.model.persistent.Clan;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class KickClanMemberCommand extends Command {

	public KickClanMemberCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public KickClanMemberCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public KickClanMemberCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public KickClanMemberCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Clan c = ClanDAO.getUserClan(author.getId());
		if (c == null) {
			channel.sendMessage("❌ | Você não possui um clã.").queue();
			return;
		} else if (!c.hasPermission(author.getId(), ClanPermission.ALTER_HIERARCHY)) {
			channel.sendMessage("❌ | Você não tem permissão para expulsar membros.").queue();
			return;
		} else if (args.length < 1) {
			channel.sendMessage("❌ | Você precisa mencionar ou informar o ID do membro a ser expulso.").queue();
			return;
		}

		User usr = message.getMentionedUsers().size() == 0 ? Main.getInfo().getUserByID(args[0]) : message.getMentionedUsers().get(0);

		if (c.getMembers().get(args[0]) == null) {
			channel.sendMessage("❌ | Membro inexistente.").queue();
			return;
		} else if (c.getMembers().get(args[0]).ordinal() <= c.getMembers().get(author.getId()).ordinal()) {
			channel.sendMessage("❌ | Você não pode expulsar membros com hierarquia maior ou igual à sua.").queue();
			return;
		}

		String hash = Helper.generateHash(guild, author);
		ShiroInfo.getHashes().add(hash);
		Main.getInfo().getConfirmationPending().put(author.getId(), true);
		channel.sendMessage("Tem certeza que deseja expulsar o membro " + (usr == null ? "ID " + args[0] : usr.getName()) + "?")
				.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
							if (!ShiroInfo.getHashes().remove(hash)) return;
							Main.getInfo().getConfirmationPending().invalidate(author.getId());

							if (usr != null) c.kick(usr, author);
							else c.kick(args[0], author);

							ClanDAO.saveClan(c);

							s.delete().flatMap(d -> channel.sendMessage("✅ | Membro expulso com sucesso.")).queue();
						}), true, 1, TimeUnit.MINUTES,
						u -> u.getId().equals(author.getId()),
						ms -> {
							ShiroInfo.getHashes().remove(hash);
							Main.getInfo().getConfirmationPending().invalidate(author.getId());
						})
				);
	}
}
