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
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.postgresql.StashDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Evogear;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.Constants;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

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
		Permission.MESSAGE_ADD_REACTION
})
public class CardStashCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Account acc = Account.find(Account.class, author.getId());
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

			int total = acc.getCardStashCapacity() - StashDAO.getRemainingSpace(author.getId());
			ThrowingFunction<Integer, Page> load = i -> {
				List<Stash> cards = StashDAO.getStashedCards(i,
						byName.get(),
						byRarity.get() == null ? null : KawaiponRarity.getByName(byRarity.get()),
						byAnime.get(),
						onlyFoil.get(),
						onlyKawaipon.get(),
						onlyEquip.get(),
						onlyField.get(),
						author.getId()
				);

				if (cards.isEmpty()) return null;

				EmbedBuilder eb = new ColorlessEmbedBuilder()
						.setAuthor("Cartas armazenadas: " + StringHelper.separate(total) + "/" + StringHelper.separate(acc.getCardStashCapacity()) + " | Página " + (i + 1))
						.setTitle(":package: | Armazém de cartas");

				if (i == 0) {
					eb.setDescription("""
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
				}

				for (Stash s : cards) {
					String name = switch (s.getType()) {
						case EVOGEAR, FIELD -> s.getRawCard().getName();
						default -> ((KawaiponCard) s.getCard()).getName();
					};
					String rarity = switch (s.getType()) {
						case EVOGEAR -> "Equipamento (" + StringUtils.repeat("⭐", ((Evogear) s.getCard()).getTier()) + ")";
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
					Pages.lazyPaginate(s, load, Constants.USE_BUTTONS, true, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
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
