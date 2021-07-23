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

package com.kuuhaku.command.commands.discord.misc;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.postgresql.StashDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Command(
		name = "retirar",
		aliases = {"recover", "armazem", "estoque"},
		usage = "req_id",
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_ADD_REACTION
})
public class CardStashCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());
		if (args.length < 1 || !StringUtils.isNumeric(args[0])) {
			AtomicReference<String> byName = new AtomicReference<>(null);
			AtomicReference<String> byRarity = new AtomicReference<>(null);
			AtomicReference<String> byAnime = new AtomicReference<>(null);
			AtomicBoolean onlyFoil = new AtomicBoolean();
			AtomicBoolean onlyKawaipon = new AtomicBoolean();
			AtomicBoolean onlyEquip = new AtomicBoolean();
			AtomicBoolean onlyField = new AtomicBoolean();

			if (args.length > 0) {
				List<String> params = List.of(args);

				params.stream()
						.filter(s -> s.startsWith("-n") && s.length() > 2)
						.findFirst()
						.ifPresent(name -> byName.set(name.substring(2)));

				params.stream()
						.filter(s -> s.startsWith("-r") && s.length() > 2)
						.findFirst()
						.ifPresent(rarity -> byRarity.set(rarity.substring(2)));

				params.stream()
						.filter(s -> s.startsWith("-a") && s.length() > 2)
						.findFirst()
						.ifPresent(anime -> byAnime.set(anime.substring(2)));

				onlyFoil.set(params.stream().anyMatch("-c"::equalsIgnoreCase));

				onlyKawaipon.set(params.stream().anyMatch("-k"::equalsIgnoreCase) || byAnime.get() != null || byRarity.get() != null || onlyFoil.get());

				onlyEquip.set(params.stream().anyMatch("-e"::equalsIgnoreCase));

				onlyField.set(params.stream().anyMatch("-f"::equalsIgnoreCase));
			}

			List<Stash> cards = StashDAO.getStashedCards(
					byName.get(),
					byRarity.get() == null ? null : KawaiponRarity.getByFragment(byRarity.get()),
					byAnime.get(),
					onlyFoil.get(),
					onlyKawaipon.get(),
					onlyEquip.get(),
					onlyField.get(),
					author.getId()
			);


			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setAuthor("Cartas armazenadas: " + Helper.separate(cards.size()) + "/" + Helper.separate(acc.getCardStashCapacity()))
					.setTitle(":package: | Armazém de cartas")
					.setDescription("""
							Use `%sretirar ID` para retirar a carta do armazém.
							       
							**Parâmetros de pesquisa:**
							`-n` - Busca cartas por nome
							`-r` - Busca cartas por raridade
							`-a` - Busca cartas por anime
							`-c` - Busca apenas cartas cromadas
							`-k` - Busca apenas cartas kawaipon
							`-e` - Busca apenas cartas-equipamento
							`-f` - Busca apenas cartas de campo
							""".formatted(prefix)
					);

			List<Page> pages = new ArrayList<>();
			List<List<Stash>> chunks = Helper.chunkify(cards, 10);
			for (List<Stash> chunk : chunks) {
				eb.clearFields();

				for (Stash s : chunk) {
					String name = switch (s.getType()) {
						case EVOGEAR, FIELD -> s.getRawCard().getName();
						default -> ((KawaiponCard) s.getCard()).getName();
					};
					String rarity = switch (s.getType()) {
						case EVOGEAR -> "Equipamento";
						case FIELD -> "Campo";
						default -> s.getRawCard().getRarity().toString();
					};

					eb.addField("`ID: " + s.getId() + "` | " + name + " (" + rarity + ")", Helper.VOID, false);
				}

				pages.add(new Page(eb.build()));
			}

			if (pages.isEmpty()) {
				channel.sendMessage("Ainda não há nenhuma carta armazenada.").queue();
			} else
				channel.sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(s ->
						Pages.paginate(s, pages, 1, TimeUnit.MINUTES, 5, u -> u.getId().equals(author.getId()))
				);
			return;
		}

		Stash s = StashDAO.getCard(Integer.parseInt(args[0]));
		if (s == null) {
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

		channel.sendMessage("✅ | Carta retirada com sucesso!").queue();
	}
}
