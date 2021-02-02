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
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.ClanDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.ClanHierarchy;
import com.kuuhaku.model.enums.ClanPermission;
import com.kuuhaku.model.persistent.Clan;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

@Command(
		name = "cargos",
		aliases = {"hierarquia", "hierarchy", "roles"},
		usage = "req_hierarchy",
		category = Category.CLAN
)
@Requires({
		Permission.MESSAGE_ATTACH_FILES,
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_ADD_REACTION
})
public class ClanPermissionCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Clan c = ClanDAO.getUserClan(author.getId());
		if (c == null) {
			channel.sendMessage("❌ | Você não possui um clã.").queue();
			return;
		} else if (!c.hasPermission(author.getId(), ClanPermission.ALTER_HIERARCHY)) {
			channel.sendMessage("❌ | Você não tem permissão para mudar as permissões dos cargos.").queue();
			return;
		}

		if (args.length == 0) {
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle(c.getTier().getName() + " " + c.getName())
					.setThumbnail("attachment://icon.jpg");

			for (ClanHierarchy ch : ClanHierarchy.values()) {
				EnumSet<ClanPermission> perms = c.getPermissions(ch);
				eb.addField(ch.getName(), """
						Promover/rebaixar membros: %s
						Expulsar membros: %s
						Sacar créditos do cofre: %s
						Convidar membros: %s
						Alterar o deck: %s
						""".formatted(
						perms.contains(ClanPermission.ALTER_HIERARCHY) ? "✅" : "❌",
						perms.contains(ClanPermission.KICK) ? "✅" : "❌",
						perms.contains(ClanPermission.WITHDRAW) ? "✅" : "❌",
						perms.contains(ClanPermission.INVITE) ? "✅" : "❌",
						perms.contains(ClanPermission.DECK) ? "✅" : "❌"
				), false);
			}

			MessageAction ma = channel.sendMessage(eb.build());
			if (c.getIcon() != null) ma = ma.addFile(Helper.getBytes(c.getIcon()), "icon.jpg");
			ma.queue();
		} else {
			ClanHierarchy ch = ClanHierarchy.getByName(args[0]);
			if (ch == null) {
				channel.sendMessage("❌ | Precisa especificar um cargo da hierarquia para alterar.").queue();
				return;
			} else if (ch == ClanHierarchy.LEADER) {
				channel.sendMessage("❌ | Você não pode alterar as permissões do líder.").queue();
				return;
			}

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle(c.getTier().getName() + " " + c.getName())
					.setThumbnail("attachment://icon.jpg")
					.setFooter("Digite `" + prefix + "permissoes CARGO` para alterar as permissões de um cargo do clã.");

			EnumSet<ClanPermission> perms = c.getPermissions(ch);
			refreshPermField(eb, ch, perms);

			Main.getInfo().getConfirmationPending().put(author.getId(), true);
			MessageAction ma = channel.sendMessage(eb.build());
			if (c.getIcon() != null) ma = ma.addFile(Helper.getBytes(c.getIcon()), "icon.jpg");
			ma.queue(s -> Pages.buttonize(s,
					new LinkedHashMap<>() {{
						put(Helper.getNumericEmoji(1), (mb, ms) -> {
							EnumSet<ClanPermission> p = c.getPermissions(ch);
							boolean enabled = p.contains(ClanPermission.ALTER_HIERARCHY);
							if (!enabled) p.add(ClanPermission.ALTER_HIERARCHY);
							else p.remove(ClanPermission.ALTER_HIERARCHY);
							c.setPermissions(ch, p);

							refreshPermField(eb, ch, p);
							ms.editMessage(eb.build()).queue(null, Helper::doNothing);
						});
						put(Helper.getNumericEmoji(2), (mb, ms) -> {
							EnumSet<ClanPermission> p = c.getPermissions(ch);
							boolean enabled = p.contains(ClanPermission.KICK);
							if (!enabled) p.add(ClanPermission.KICK);
							else p.remove(ClanPermission.KICK);
							c.setPermissions(ch, p);

							refreshPermField(eb, ch, p);
							ms.editMessage(eb.build()).queue(null, Helper::doNothing);
						});
						put(Helper.getNumericEmoji(3), (mb, ms) -> {
							EnumSet<ClanPermission> p = c.getPermissions(ch);
							boolean enabled = p.contains(ClanPermission.WITHDRAW);
							if (!enabled) p.add(ClanPermission.WITHDRAW);
							else p.remove(ClanPermission.WITHDRAW);
							c.setPermissions(ch, p);

							refreshPermField(eb, ch, p);
							ms.editMessage(eb.build()).queue(null, Helper::doNothing);
						});
						put(Helper.getNumericEmoji(4), (mb, ms) -> {
							EnumSet<ClanPermission> p = c.getPermissions(ch);
							boolean enabled = p.contains(ClanPermission.INVITE);
							if (!enabled) p.add(ClanPermission.INVITE);
							else p.remove(ClanPermission.INVITE);
							c.setPermissions(ch, p);

							refreshPermField(eb, ch, p);
							ms.editMessage(eb.build()).queue(null, Helper::doNothing);
						});
						put(Helper.getNumericEmoji(5), (mb, ms) -> {
							EnumSet<ClanPermission> p = c.getPermissions(ch);
							boolean enabled = p.contains(ClanPermission.DECK);
							if (!enabled) p.add(ClanPermission.DECK);
							else p.remove(ClanPermission.DECK);
							c.setPermissions(ch, p);

							refreshPermField(eb, ch, p);
							ms.editMessage(eb.build()).queue(null, Helper::doNothing);
						});
					}},
					true,
					1, TimeUnit.MINUTES,
					u -> u.getId().equals(author.getId()),
					ms -> {
						ClanDAO.saveClan(c);
						s.delete().flatMap(d -> channel.sendMessage("✅ | Permissões salvas com sucesso.")).queue();
						Main.getInfo().getConfirmationPending().invalidate(author.getId());
					}
			));
		}
	}

	private static void refreshPermField(EmbedBuilder eb, ClanHierarchy ch, EnumSet<ClanPermission> p) {
		eb.clearFields()
				.addField(ch.getName(), """
						Promover/rebaixar membros: %s
						Expulsar membros: %s
						Sacar créditos do cofre: %s
						Convidar membros: %s
						Alterar o deck: %s
						""".formatted(
						p.contains(ClanPermission.ALTER_HIERARCHY) ? "✅" : "❌",
						p.contains(ClanPermission.KICK) ? "✅" : "❌",
						p.contains(ClanPermission.WITHDRAW) ? "✅" : "❌",
						p.contains(ClanPermission.INVITE) ? "✅" : "❌",
						p.contains(ClanPermission.DECK) ? "✅" : "❌"
				), false);
	}
}
