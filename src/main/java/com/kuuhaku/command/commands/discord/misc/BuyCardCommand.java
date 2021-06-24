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
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.postgresql.LotteryDAO;
import com.kuuhaku.controller.postgresql.MarketDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Command(
		name = "comprar",
		aliases = {"buy"},
		usage = "req_id",
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_ADD_REACTION
})
public class BuyCardCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		Calendar today = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("GMT-3")));
		boolean blackfriday = today.get(Calendar.MONTH) == Calendar.NOVEMBER && today.get(Calendar.DAY_OF_MONTH) == 27;
		Account buyer = AccountDAO.getAccount(author.getId());
		if (args.length < 1 || !StringUtils.isNumeric(args[0])) {
			AtomicReference<String> byName = new AtomicReference<>(null);
			AtomicReference<String> byRarity = new AtomicReference<>(null);
			AtomicReference<String> byAnime = new AtomicReference<>(null);
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
			EmbedBuilder eb = new ColorlessEmbedBuilder();

			eb.setTitle(":scales: | Mercado de cartas");
			eb.setDescription("""
					Use `%scomprar ID` para comprar uma carta.
					       
					**Parâmetros de pesquisa:**
					`-n` - Busca cartas por nome
					`-r` - Busca cartas por raridade
					`-a` - Busca cartas por anime
					`-c` - Busca apenas cartas cromadas
					`-k` - Busca apenas cartas kawaipon
					`-e` - Busca apenas cartas-equipamento
					`-f` - Busca apenas cartas de campo
					`-m` - Busca apenas suas cartas anunciadas
					`-min` - Define um preço mínimo
					`-max` - Define um preço máximo
					       
					Cartas com valores acima de 50x o valor base não serão exibidas sem usar `-m`.
					""".formatted(prefix)
			);
			eb.setFooter("Seus créditos: " + buyer.getBalance(), "https://i.imgur.com/U0nPjLx.gif");

			List<Page> pages = new ArrayList<>();
			List<Market> cards = MarketDAO.getOffers(
					byName.get(),
					minPrice.get(),
					maxPrice.get(),
					byRarity.get() == null ? null : KawaiponRarity.getByFragment(byRarity.get()),
					byAnime.get(),
					onlyFoil.get(),
					onlyKawaipon.get(),
					onlyEquip.get(),
					onlyField.get(),
					onlyMine.get() ? author.getId() : null
			);

			for (int i = 0; i < Math.ceil(cards.size() / 10f); i++) {
				eb.clearFields();

				for (int p = i * 10; p < cards.size() && p < 10 * (i + 1); p++) {
					Market m = cards.get(p);
					User seller = Main.getInfo().getUserByID(m.getSeller());
					String name = switch (m.getType()) {
						case EVOGEAR, FIELD -> m.getRawCard().getName();
						default -> ((KawaiponCard) m.getCard()).getName();
					};
					String rarity = switch (m.getType()) {
						case EVOGEAR -> "Equipamento";
						case FIELD -> "Campo";
						default -> m.getRawCard().getRarity().toString();
					};

					eb.addField(
							"`ID: " + m.getId() + "` | " + name + " (" + rarity + ")",
							blackfriday ?
									"Por " + (seller == null ? "Desconhecido" : seller.getName()) + " | Preço: " + (m.getPrice() > m.getPriceLimit() ? "**`valor muito alto`**" : "~~" + Helper.separate(m.getPrice()) + "~~ **" + Helper.separate(Math.round(m.getPrice() * 0.75)) + "** créditos")
									:
									"Por " + (seller == null ? "Desconhecido" : seller.getName()) + " | Preço: " + (m.getPrice() > m.getPriceLimit() ? "**`valor muito alto`**" : "**" + Helper.separate(m.getPrice()) + "** créditos"),
							false
					);
				}

				pages.add(new Page(PageType.EMBED, eb.build()));
			}

			if (pages.isEmpty()) {
				channel.sendMessage("Ainda não há nenhuma carta anunciada.").queue();
			} else
				channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s ->
						Pages.paginate(s, pages, 1, TimeUnit.MINUTES, 5, u -> u.getId().equals(author.getId()))
				);
			return;
		}

		Market m = MarketDAO.getCard(Integer.parseInt(args[0]));
		if (m == null) {
			channel.sendMessage("❌ | ID inválido ou a carta já foi comprada por alguém.").queue();
			return;
		}

		if (buyer.getLoan() > 0) {
			channel.sendMessage(I18n.getString("err_cannot-transfer-with-loan")).queue();
			return;
		}

		Account seller = AccountDAO.getAccount(m.getSeller());
		if (!seller.getUid().equals(author.getId())) {
			if (m.getPrice() > m.getPriceLimit()) {
				channel.sendMessage("❌ | Essa carta está marcada como privada!").queue();
				return;
			} else if (buyer.getBalance() < (blackfriday ? m.getPrice() * 0.75 : m.getPrice())) {
				channel.sendMessage(I18n.getString("err_insufficient-credits-user")).queue();
				return;
			}

			Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
			switch (m.getType()) {
				case EVOGEAR -> {
					Deck dk = kp.getDeck();

					if (dk.checkEquipment(m.getCard(), channel)) return;

					dk.addEquipment(m.getCard());
				}
				case FIELD -> {
					Deck dk = kp.getDeck();

					if (dk.checkField(m.getCard(), channel)) return;

					dk.addField(m.getCard());

				}
				default -> {
					if (kp.getCards().contains((KawaiponCard) m.getCard())) {
						channel.sendMessage("❌ | Parece que você já possui essa carta!").queue();
						return;
					}

					kp.addCard(m.getCard());
				}
			}
			KawaiponDAO.saveKawaipon(kp);

			int rawAmount = m.getPrice();
			int liquidAmount = Helper.applyTax(seller.getUid(), rawAmount, 0.1);

			seller.addCredit(liquidAmount, this.getClass());
			buyer.removeCredit(blackfriday ? Math.round(m.getPrice() * 0.75) : m.getPrice(), this.getClass());

			LotteryValue lv = LotteryDAO.getLotteryValue();
			lv.addValue(rawAmount - liquidAmount);
			LotteryDAO.saveLotteryValue(lv);

			AccountDAO.saveAccount(seller);
			AccountDAO.saveAccount(buyer);

			m.setBuyer(author.getId());
			MarketDAO.saveCard(m);

			User sellerU = Main.getInfo().getUserByID(m.getSeller());
			User buyerU = Main.getInfo().getUserByID(m.getBuyer());

			String name = switch (m.getType()) {
				case EVOGEAR, FIELD -> m.getRawCard().getName();
				default -> ((KawaiponCard) m.getCard()).getName();
			};

			boolean taxed = rawAmount != liquidAmount;
			String taxMsg = taxed ? " (Taxa: " + Helper.roundToString(100 - Helper.prcnt(liquidAmount, rawAmount) * 100, 1) + "%)" : "";
			if (sellerU != null) sellerU.openPrivateChannel().queue(c ->
							c.sendMessage("✅ | Sua carta `" + name + "` foi comprada por " + buyerU.getName() + " por " + Helper.separate(m.getPrice()) + " créditos!" + taxMsg).queue(null, Helper::doNothing),
					Helper::doNothing
			);

			channel.sendMessage("✅ | Carta comprada com sucesso!").queue();
		} else {
			Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
			switch (m.getType()) {
				case EVOGEAR -> {
					Deck dk = kp.getDeck();

					if (dk.checkEquipment(m.getCard(), channel)) return;

					dk.addEquipment(m.getCard());
				}
				case FIELD -> {
					Deck dk = kp.getDeck();

					if (dk.checkField(m.getCard(), channel)) return;

					dk.addField(m.getCard());

				}
				default -> {
					if (kp.getCards().contains((KawaiponCard) m.getCard())) {
						channel.sendMessage("❌ | Parece que você já possui essa carta!").queue();
						return;
					}

					kp.addCard(m.getCard());
				}
			}
			KawaiponDAO.saveKawaipon(kp);

			m.setBuyer(author.getId());
			MarketDAO.saveCard(m);

			channel.sendMessage("✅ | Carta retirada com sucesso!").queue();
		}
	}
}
