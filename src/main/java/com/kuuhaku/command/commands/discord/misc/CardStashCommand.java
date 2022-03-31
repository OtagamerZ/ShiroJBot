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

package com.kuuhaku.command.commands.discord.misc;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.model.ThrowingFunction;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.postgresql.StashDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import com.kuuhaku.utils.XStringBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Command(
		name = "retirar",
		aliases = {"recover", "armazem", "estoque"},
		usage = "req_id",
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION
})
public class CardStashCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());
		if (args.length < 1 || !StringUtils.isNumeric(args[0])) {
			Options opt = new Options()
					.addOption("n", "nome", true, "Busca por nome")
					.addOption("r", "raridade", true, "Busca por raridade")
					.addOption("a", "anime", true, "Busca por anime")
					.addOption("c", "cromada", false, "Apenas cartas cromadas")
					.addOption("k", "kawaipon", false, "Apenas cartas kawaipon")
					.addOption("e", "evogear", false, "Apenas cartas evogear")
					.addOption("f", "campo", false, "Apenas cartas de campo");

			DefaultParser parser = new DefaultParser(false);
			CommandLine cli;
			try {
				cli = parser.parse(opt, args, true);
			} catch (ParseException e) {
				cli = new CommandLine.Builder().build();
			}
			CommandLine finalCli = cli;

			int total = StashDAO.getTotalCards(author, finalCli);
			ThrowingFunction<Integer, Page> load = i -> {
				List<Stash> cards = StashDAO.getStashedCards(i, author, finalCli);
				if (cards.isEmpty()) return null;

				EmbedBuilder eb = new ColorlessEmbedBuilder()
						.setAuthor("Resultados: " + Helper.separate(total) + " | Página " + (i + 1))
						.setTitle(":package: | Armazém de cartas (Disponível: " + StashDAO.getRemainingSpace(author.getId()) + " slots)");

				if (i == 0) {
					XStringBuilder sb = new XStringBuilder()
							.append("Use `%sretirar ID` para retirar a carta do armazém.\n".formatted(prefix))
							.appendNewLine("**Parâmetros de pesquisa:**");

					for (Option op : opt.getOptions()) {
						sb.appendNewLine("`-%s/--%s` - %s".formatted(
								op.getOpt(),
								op.getLongOpt(),
								op.getDescription()
						));
					}

					eb.setDescription(sb.toString());
				}

				for (Stash s : cards) {
					String name = switch (s.getType()) {
						case EVOGEAR, FIELD -> s.getRawCard().getName();
						default -> ((KawaiponCard) s.getCard()).getName();
					};
					String rarity = switch (s.getType()) {
						case EVOGEAR -> "Equipamento (" + StringUtils.repeat("⭐", ((Equipment) s.getCard()).getTier()) + ")";
						case FIELD -> (((Field) s.getCard()).isDay() ? ":sunny: " : ":crescent_moon: ") + "Campo";
						default -> s.getRawCard().getRarity().getEmote() + s.getRawCard().getRarity().toString();
					};
					String anime = s.getRawCard().getAnime().toString();

					eb.addField("`ID: " + s.getId() + "` | " + name,
							rarity + (anime == null ? "" : " - " + anime),
							false
					);
				}

				return new InteractPage(eb.build());
			};

			Page p = load.apply(0);
			if (p == null) {
				channel.sendMessage("Ainda não há nenhuma carta armazenada.").queue();
				return;
			}

			channel.sendMessageEmbeds((MessageEmbed) p.getContent()).queue(s ->
					Pages.lazyPaginate(s, load, ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
			);
			return;
		}

		Stash s = StashDAO.getCard(Integer.parseInt(args[0]));
		if (s == null || !s.getOwner().equals(author.getId())) {
            channel.sendMessage("❌ | ID inválido ou a carta já foi retirada.").queue();
            return;
        }

		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
		switch (s.getType()) {
			case EVOGEAR -> {
				Deck dk = kp.getDeck();

				if (dk.checkEquipment(s.getCard(), channel)) return;

				dk.addEquipment(s.getCard());
			}
			case FIELD -> {
				Deck dk = kp.getDeck();

				if (dk.checkField(s.getCard(), channel)) return;

				dk.addField(s.getCard());

			}
			default -> {
				if (kp.getCards().contains((KawaiponCard) s.getCard())) {
					channel.sendMessage("❌ | Parece que você já possui essa carta!").queue();
					return;
				}

				kp.addCard(s.getCard());
			}
		}
		KawaiponDAO.saveKawaipon(kp);
		StashDAO.removeCard(s);

		channel.sendMessage("✅ | Carta `" + s.getRawCard().getName() + "` retirada com sucesso!").queue();
	}
}
