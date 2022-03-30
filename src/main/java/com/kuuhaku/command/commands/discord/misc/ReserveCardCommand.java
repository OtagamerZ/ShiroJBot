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
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.MarketDAO;
import com.kuuhaku.controller.postgresql.StashDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Evogear;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Event;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.model.persistent.Market;
import com.kuuhaku.model.persistent.Stash;
import com.kuuhaku.utils.Constants;
import com.kuuhaku.utils.helpers.MiscHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import com.vdurmont.emoji.EmojiManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Command(
		name = "reservar",
		aliases = {"reserve"},
		usage = "req_id-override",
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION
})
public class ReserveCardCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		boolean blackfriday = Event.getCurrent() == Event.BLACKFRIDAY;
		Account buyer = Account.find(Account.class, author.getId());
		if (args.length < 1 || !StringUtils.isNumeric(args[0])) {
			AtomicReference<String> byName = new AtomicReference<>(null);
			AtomicReference<String> byRarity = new AtomicReference<>(null);
			AtomicReference<String> byAnime = new AtomicReference<>(null);
			AtomicReference<String> byEmoji = new AtomicReference<>(null);
			AtomicReference<Integer> minPrice = new AtomicReference<>(-1);
			AtomicReference<Integer> maxPrice = new AtomicReference<>(-1);
			AtomicBoolean onlyFoil = new AtomicBoolean();
			AtomicBoolean onlyMine = new AtomicBoolean();
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

				params.stream()
						.filter(s -> s.startsWith("-j") && s.length() > 2)
						.filter(s -> EmojiManager.isEmoji(s.substring(2)))
						.findFirst()
						.ifPresent(emoji -> byEmoji.set(emoji.substring(2)));

				minPrice.set(params.stream()
						.filter(s -> s.startsWith("-min") && s.length() > 4)
						.filter(s -> StringUtils.isNumeric(s.substring(4)))
						.mapToInt(s -> Integer.parseInt(s.substring(4)))
						.findFirst()
						.orElse(-1));

				maxPrice.set(params.stream()
						.filter(s -> s.startsWith("-max") && s.length() > 4)
						.filter(s -> StringUtils.isNumeric(s.substring(4)))
						.mapToInt(s -> Integer.parseInt(s.substring(4)))
						.findFirst()
						.orElse(-1));

				onlyFoil.set(params.stream().anyMatch("-c"::equalsIgnoreCase));

				onlyMine.set(params.stream().anyMatch("-m"::equalsIgnoreCase));

				onlyKawaipon.set(params.stream().anyMatch("-k"::equalsIgnoreCase) || byAnime.get() != null || byRarity.get() != null || onlyFoil.get());

				onlyEquip.set(params.stream().anyMatch("-e"::equalsIgnoreCase));

				onlyField.set(params.stream().anyMatch("-f"::equalsIgnoreCase));
			}

			int total = MarketDAO.getTotalOffers();
			ThrowingFunction<Integer, Page> load = i -> {
				List<Market> cards = MarketDAO.getOffers(i,
						byName.get(),
						minPrice.get(),
						maxPrice.get(),
						byRarity.get() == null ? null : KawaiponRarity.getByName(byRarity.get()),
						byAnime.get(),
						byEmoji.get(),
						onlyFoil.get(),
						onlyKawaipon.get(),
						onlyEquip.get(),
						onlyField.get(),
						onlyMine.get() ? author.getId() : null
				);

				if (cards.isEmpty()) return null;

				EmbedBuilder eb = new ColorlessEmbedBuilder()
						.setAuthor("Cartas anunciadas: " + StringHelper.separate(total) + " | Página " + (i + 1))
						.setTitle(":scales: | Mercado de cartas")
						.setFooter("Seus CR: " + StringHelper.separate(buyer.getBalance()), "https://i.imgur.com/U0nPjLx.gif");

				if (i == 0) {
					eb.setDescription("""
							Use `%scomprar ID` para comprar uma carta.

							**Parâmetros de pesquisa:**
							`-n` - Busca cartas por nome
							`-r` - Busca cartas por raridade
							`-a` - Busca cartas por anime
							`-j` - Busca cartas por emoji
							`-c` - Busca apenas cartas cromadas
							`-k` - Busca apenas cartas kawaipon
							`-e` - Busca apenas cartas-equipamento
							`-f` - Busca apenas cartas de campo
							`-m` - Busca apenas suas cartas anunciadas
							`-min` - Define um preço mínimo
							`-max` - Define um preço máximo
							""".formatted(prefix)
					);
				}

				for (Market m : cards) {
					User seller = Main.getUserByID(m.getSeller());
					String name = switch (m.getType()) {
						case EVOGEAR, FIELD -> m.getRawCard().getName();
						default -> ((KawaiponCard) m.getCard()).getName();
					};
					String rarity = switch (m.getType()) {
						case EVOGEAR -> "Equipamento (" + StringUtils.repeat("⭐", ((Evogear) m.getCard()).getTier()) + ")";
						case FIELD -> (((Field) m.getCard()).isDay() ? ":sunny: " : ":crescent_moon: ") + "Campo";
						default -> m.getRawCard().getRarity().getEmote() + m.getRawCard().getRarity().toString();
					};
					String anime = m.getRawCard().getAnime().toString();

					eb.addField(
							"`" + (m.getEmoji() == null ? "" : m.getEmoji() + " ") + "ID: " + m.getId() + "` | " + name,
							"""
									%s
									Por %s
									Preço %s CR
									""".formatted(
									rarity + (anime == null ? "" : " - " + anime),
									seller == null ? "Desconhecido" : seller.getName(),
									blackfriday
											? "~~" + StringHelper.separate(m.getPrice()) + "~~ **" + StringHelper.separate(Math.round(m.getPrice() * 0.75)) + "**"
											: "**" + StringHelper.separate(m.getPrice()) + "**"
							),
							false
					);
				}

				return new InteractPage(eb.build());
			};

			Page p = load.apply(0);
			if (p == null) {
				channel.sendMessage("Ainda não há nenhuma carta anunciada.").queue();
				return;
			}

			channel.sendMessageEmbeds((MessageEmbed) p.getContent()).queue(s ->
					Pages.lazyPaginate(s, load, Constants.USE_BUTTONS, true, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
			);
			return;
		}

		if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage("❌ | O ID precisa ser um valor inteiro.").queue();
			return;
		}

		Market m = MarketDAO.getCard(Integer.parseInt(args[0]));
		if (m == null) {
			channel.sendMessage("❌ | ID inválido ou a carta já foi comprada por alguém.").queue();
			return;
		} else if (StashDAO.getRemainingSpace(author.getId()) <= 0) {
			channel.sendMessage("❌ | Você não possui mais espaço em seu armazém. Compre mais espaço para ele na loja de gemas ou retire alguma carta.").queue();
			return;
		}

		Account seller = Account.find(Account.class, m.getSeller());
		if (!seller.getUid().equals(author.getId())) {
			int price = (int) Math.round(m.getPrice() * (blackfriday ? 0.75 : 1));
			if (buyer.getBalance() < price) {
				channel.sendMessage(I18n.getString("err_insufficient-credits-user")).queue();
				return;
			} else if (StashDAO.getRemainingSpace(author.getId()) <= 0) {
				channel.sendMessage("❌ | Você não possui mais espaço em seu armazém. Compre mais espaço para ele na loja de gemas ou retire alguma carta.").queue();
				return;
			}

			String name = switch (m.getType()) {
				case EVOGEAR, FIELD -> m.getRawCard().getName();
				default -> ((KawaiponCard) m.getCard()).getName();
			};

			User sellerU = Main.getUserByID(m.getSeller());

			if (args.length > 1 && args[1].equalsIgnoreCase("s")) {
				m.setBuyer(author.getId());
				MarketDAO.saveCard(m);

				seller.addCredit(price, this.getClass());
				buyer.removeCredit(price, this.getClass());

				seller.save();
				buyer.save();

				StashDAO.saveCard(switch (m.getType()) {
					case EVOGEAR -> new Stash(author.getId(), (Evogear) m.getCard());
					case FIELD -> new Stash(author.getId(), (Field) m.getCard());
					default -> new Stash(author.getId(), (KawaiponCard) m.getCard());
				});

				if (sellerU != null) sellerU.openPrivateChannel().queue(c ->
								c.sendMessage("✅ | Sua carta `" + name + "` foi comprada por " + author.getName() + " por **" + StringHelper.separate(price) + " CR**!").queue(null, MiscHelper::doNothing),
						MiscHelper::doNothing
				);

				channel.sendMessage("✅ | Carta `" + name + "` comprada e reservada com sucesso!").queue();
			} else {
				Main.getInfo().getConfirmationPending().put(author.getId(), true);
				channel.sendMessage("Você está prestes a comprar e reservar a carta `" + name + "` por **" + StringHelper.separate(price) + " CR**, deseja confirmar?")
						.queue(s -> Pages.buttonize(s, Map.of(StringHelper.parseEmoji(Constants.ACCEPT), wrapper -> {
									Main.getInfo().getConfirmationPending().remove(author.getId());
									Market finalM = MarketDAO.getCard(Integer.parseInt(args[0]));
									if (finalM == null) {
										channel.sendMessage("❌ | ID inválido ou a carta já foi comprada por alguém.").queue();
										return;
									}

									finalM.setBuyer(author.getId());
									MarketDAO.saveCard(finalM);

									seller.addCredit(price, this.getClass());
									buyer.removeCredit(price, this.getClass());

									seller.save();
									buyer.save();

									StashDAO.saveCard(switch (finalM.getType()) {
										case EVOGEAR -> new Stash(author.getId(), (Evogear) finalM.getCard());
										case FIELD -> new Stash(author.getId(), (Field) finalM.getCard());
										default -> new Stash(author.getId(), (KawaiponCard) finalM.getCard());
									});

									if (sellerU != null) sellerU.openPrivateChannel().queue(c ->
													c.sendMessage("✅ | Sua carta `" + name + "` foi comprada por " + author.getName() + " por " + StringHelper.separate(price) + " CR!").queue(null, MiscHelper::doNothing),
											MiscHelper::doNothing
									);

									s.delete().mapToResult().flatMap(d -> channel.sendMessage("✅ | Carta `" + name + "` comprada e reservada com sucesso!")).queue();
								}), Constants.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
								u -> u.getId().equals(author.getId()),
								ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
						));
			}
		} else {
			m = MarketDAO.getCard(Integer.parseInt(args[0]));
			if (m == null) {
				channel.sendMessage("❌ | ID inválido ou a carta já foi comprada por alguém.").queue();
				return;
			} else if (StashDAO.getRemainingSpace(author.getId()) <= 0) {
				channel.sendMessage("❌ | Você não possui mais espaço em seu armazém. Compre mais espaço para ele na loja de gemas ou retire alguma carta.").queue();
				return;
			}

			StashDAO.saveCard(switch (m.getType()) {
				case EVOGEAR -> new Stash(author.getId(), (Evogear) m.getCard());
				case FIELD -> new Stash(author.getId(), (Field) m.getCard());
				default -> new Stash(author.getId(), (KawaiponCard) m.getCard());
			});

			m.setBuyer(author.getId());
			MarketDAO.saveCard(m);

			channel.sendMessage("✅ | Carta reservada com sucesso!").queue();
		}
	}
}
