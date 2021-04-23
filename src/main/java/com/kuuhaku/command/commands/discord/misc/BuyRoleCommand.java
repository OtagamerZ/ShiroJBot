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

package com.kuuhaku.command.commands.discord.misc;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.persistent.guild.PaidRole;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Command(
		name = "comprarcargo",
		aliases = {"ccargo", "buyrole", "brole"},
		usage = "req_id-opt",
		category = Category.MISC
)
@Requires({Permission.MESSAGE_EMBED_LINKS, Permission.MANAGE_ROLES})
public class BuyRoleCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		GuildConfig gc = GuildDAO.getGuildById(guild.getId());
		Account acc = AccountDAO.getAccount(author.getId());
		List<PaidRole> prs = new ArrayList<>(gc.getPaidRoles());

		if (args.length == 0) {
			List<Page> pages = new ArrayList<>();

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle(":bank: | Cargos disponíveis para compra");

			if (prs.size() == 0) {
				channel.sendMessage("Não há nenhum cargo pago configurado neste servidor.").queue();
				return;
			}

			Map<Integer, String> fields = new TreeMap<>();
			for (int i = 0; i < prs.size(); i++) {
				PaidRole role = prs.get(i);
				Role r = guild.getRoleById(role.getId());
				if (r == null) {
					gc.removeLevelRole(role.getId());
					continue;
				}

				fields.merge(role.getPrice(), "`ID: " + i + "` | " + r.getAsMention(), (p, n) -> String.join("\n", p, n));
			}

			List<List<Integer>> chunks = Helper.chunkify(fields.keySet(), 10);
			for (List<Integer> chunk : chunks) {
				eb.clearFields();
				for (Integer level : chunk)
					eb.addField("Valor: " + level + " créditos", fields.get(level), true);

				pages.add(new Page(PageType.EMBED, eb.build()));
			}

			channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s ->
					Pages.paginate(s, pages, 1, TimeUnit.MINUTES, 5, u -> u.getId().equals(author.getId()))
			);
			return;
		}

		try {
			int id = Integer.parseInt(args[0]);

			if (!Helper.between(id, 0, prs.size())) {
				channel.sendMessage("❌ | Não existe nenhum cargo pago com esse ID.").queue();
				return;
			}

			PaidRole pr = prs.get(id);
			Role r = guild.getRoleById(pr.getId());
			if (r == null) {
				gc.removePaidRole(pr.getId());
				GuildDAO.updateGuildSettings(gc);
				channel.sendMessage("O cargo não existe mais no servidor, já removi da listagem.").queue();
				return;
			} else if (acc.getBalance() < pr.getPrice()) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user")).queue();
				return;
			} else if (member.getRoles().contains(r)) {
				channel.sendMessage("❌ | Você já possui esse cargo.").queue();
				return;
			}

			Main.getInfo().getConfirmationPending().put(author.getId(), true);
			channel.sendMessage("Você está prestes a comprar o cargo `" + r.getName() + "` por **" + pr.getPrice() + " créditos**, deseja confirmar?").queue(s ->
					Pages.buttonize(s, Collections.singletonMap(Helper.ACCEPT, (mb, ms) -> {
								Main.getInfo().getConfirmationPending().remove(author.getId());

								guild.addRoleToMember(member, r)
										.flatMap(m -> channel.sendMessage("✅ | Cargo comprado com sucesso!"))
										.queue(m -> {
											Account facc = AccountDAO.getAccount(author.getId());
											Account oacc = AccountDAO.getAccount(guild.getOwnerId());

											int rawAmount = pr.getPrice();
											int liquidAmount = Helper.applyTax(guild.getOwnerId(), rawAmount, 0.1);

											facc.removeCredit(pr.getPrice(), BuyRoleCommand.class);
											oacc.addCredit(liquidAmount, BuyRoleCommand.class);

											AccountDAO.saveAccount(facc);
											AccountDAO.saveAccount(oacc);

											s.delete().queue(null, Helper::doNothing);
										}, t -> channel.sendMessage("❌ | Erro ao comprar o cargo.").queue());
							}), true, 1, TimeUnit.MINUTES,
							u -> u.getId().equals(author.getId()),
							ms -> Main.getInfo().getConfirmationPending().remove(author.getId()))
			);
		} catch (NumberFormatException e) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-id-value")).queue();
		}
	}
}
