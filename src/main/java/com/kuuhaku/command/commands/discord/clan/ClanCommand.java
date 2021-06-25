/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.ClanDAO;
import com.kuuhaku.controller.postgresql.LogDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.ClanTier;
import com.kuuhaku.model.persistent.Clan;
import com.kuuhaku.model.persistent.ClanMember;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Command(
		name = "cla",
		aliases = {"clan", "party", "faction", "guild", "dynasty"},
		category = Category.CLAN
)
@Requires({
		Permission.MESSAGE_ATTACH_FILES,
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_EXT_EMOJI,
		Permission.MESSAGE_ADD_REACTION
})
public class ClanCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Clan c = ClanDAO.getUserClan(author.getId());
		if (c == null) {
			channel.sendMessage("❌ | Você não possui um clã.").queue();
			return;
		}

		List<Page> pages = new ArrayList<>();
		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle(c.getTier().getName() + " " + c.getName())
				.setThumbnail("attachment://icon.png")
				.setImage("attachment://banner.png")
				.setDescription(c.getMotd())
				.addField("Cofre", ":coin: | %s%s créditos".formatted(Helper.separate(c.getVault()), c.getTier() != ClanTier.DYNASTY ? "/" + Helper.separate(c.getTier().getVaultSize()) : ""), false)
				.addField("Aluguel", ":receipt: | %s créditos/mês (%s)".formatted(Helper.separate(c.getTier().getRent()), c.hasPaidRent() ? "PAGO" : "PENDENTE"), false);

		if (c.getTier() != ClanTier.DYNASTY)
			eb.addField("Metas para promoção", """
					Membros: %s/%s
					Créditos: %s/%s
										
					**Desbloqueia**
					%s
					""".formatted(
					c.getMembers().size(), c.getTier().getCapacity() / 2,
					Helper.separate(c.getVault()), Helper.separate(c.getTier().getNext().getCost()),
					switch (c.getTier()) {
						case PARTY -> """
								Título de facção
								Capacidade de membros (~~10~~ -> 50)
								Capacidade do cofre (~~50.000~~ -> 250.000)
								Mensagem do dia
								""";
						case FACTION -> """
								Título de guilda
								Capacidade de membros (~~50~~ -> 100)
								Capacidade do cofre (~~250.000~~ -> 1.500.000)
								Emblema
								""";
						case GUILD -> """
								Título de dinastia
								Capacidade de membros (~~100~~ -> 500)
								Capacidade do cofre (~~1.500.000~~ -> ilimitado)
								Banner
								""";
						default -> "";
					}
			), false);

		StringBuilder sb = new StringBuilder();
		List<ClanMember> mbs = c.getMembers();
		List<MessageEmbed.Field> fixed = List.copyOf(eb.getFields());
		List<List<ClanMember>> chunks = Helper.chunkify(mbs, 10);
		for (int i = 0; i < chunks.size(); i++) {
			List<ClanMember> chunk = chunks.get(i);
			sb.setLength(0);

			for (int j = 0; j < chunk.size(); j++) {
				ClanMember mb = chunk.get(j);
				sb.append("`%s` | %s %s\n".formatted(j + i * 10, mb.getRole().getIcon(), LogDAO.getUsername(mb.getUid()).split("#")[0]));
			}

			eb.clearFields().getFields().addAll(fixed);
			eb.addField("Membros (%s/%s)".formatted(c.getMembers().size(), c.getTier().getCapacity()), sb.toString(), false);
			pages.add(new Page(PageType.EMBED, eb.build()));
		}

		MessageAction ma = channel.sendMessage((MessageEmbed) pages.get(0).getContent());
		if (c.getIcon() != null) ma = ma.addFile(Helper.writeAndGet(c.getIcon(), "icon", "png"));
		if (c.getBanner() != null) ma = ma.addFile(Helper.writeAndGet(c.getBanner(), "banner", "png"));
		ma.queue(s ->
				Pages.paginate(s, pages, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
		);
	}

	private static String checkUser(String id) {
		try {
			return Main.getInfo().getUserByID(id).getName();
		} catch (Exception e) {
			return "`Desconhecido`";
		}
	}
}
