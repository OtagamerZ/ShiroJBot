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
import com.kuuhaku.controller.postgresql.*;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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
			AtomicReference<Integer> onlyEquip = new AtomicReference<>(-1);
			AtomicBoolean onlyFoil = new AtomicBoolean();
			AtomicBoolean onlyMine = new AtomicBoolean();
			AtomicBoolean onlyKawaipon = new AtomicBoolean();
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

				if (params.stream().anyMatch(p -> p.startsWith("-e")))
					onlyEquip.set(params.stream()
							.filter(s -> s.startsWith("-e") && s.length() > 2)
							.filter(s -> StringUtils.isNumeric(s.substring(2)))
							.mapToInt(s -> Integer.parseInt(s.substring(2)))
							.findFirst()
							.orElse(0));

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
					`-e` - Busca apenas cartas-equipamento (podendo informar um tier)
					`-f` - Busca apenas cartas de campo
					`-m` - Busca apenas suas cartas anunciadas
					`-min` - Define um preço mínimo
					`-max` - Define um preço máximo
					       
					Cartas com valores acima de 50x o valor base não serão exibidas sem usar `-m`.
					""".formatted(prefix)
			);
			eb.setFooter("Seus créditos: " + buyer.getBalance(), "https://i.imgur.com/U0nPjLx.gif");

			List<Page> pages = new ArrayList<>();
			List<Pair<Object, CardType>> cards = new ArrayList<>();

			if (onlyEquip.get() == -1 && !onlyField.get())
				cards.addAll(
						CardMarketDAO.getCardsForMarket(
								byName.get(),
								minPrice.get(),
								maxPrice.get(),
								byRarity.get(),
								byAnime.get(),
								onlyFoil.get(),
								onlyMine.get() ? author.getId() : null
						).stream()
								.map(cm -> Pair.of((Object) cm, CardType.KAWAIPON))
								.collect(Collectors.toList())
				);

			if (!onlyKawaipon.get() && !onlyField.get())
				cards.addAll(
						EquipmentMarketDAO.getCardsForMarket(
								byName.get(),
								minPrice.get(),
								maxPrice.get(),
								onlyEquip.get() > 0 ? onlyEquip.get() : -1,
								onlyMine.get() ? author.getId() : null
						).stream()
								.map(em -> Pair.of((Object) em, CardType.EVOGEAR))
								.collect(Collectors.toList())
				);

			if (!onlyKawaipon.get() && onlyEquip.get() == -1)
				cards.addAll(
						FieldMarketDAO.getCardsForMarket(
								byName.get(),
								minPrice.get(),
								maxPrice.get(),
								onlyMine.get() ? author.getId() : null
						).stream()
								.map(fm -> Pair.of((Object) fm, CardType.FIELD))
								.collect(Collectors.toList())
				);

			for (int i = 0; i < Math.ceil(cards.size() / 10f); i++) {
				eb.clearFields();

				for (int p = i * 10; p < cards.size() && p < 10 * (i + 1); p++) {
					switch (cards.get(p).getRight()) {
						case KAWAIPON -> {
							CardMarket cm = (CardMarket) cards.get(p).getLeft();
							User seller = Main.getInfo().getUserByID(cm.getSeller());
							eb.addField(
									"`ID: " + cm.getId() + "` | " + cm.getCard().getName() + " (" + cm.getCard().getCard().getRarity().toString() + ")",
									blackfriday ?
											"Por " + (seller == null ? "Desconhecido" : seller.getName()) + " | Preço: " + (cm.getPrice() > (cm.getCard().getCard().getRarity().getIndex() * Helper.BASE_CARD_PRICE * 50 * (cm.getCard().isFoil() ? 2 : 1)) ? "**`valor muito alto`**" : "~~" + Helper.separate(cm.getPrice()) + "~~ **" + Helper.separate(Math.round(cm.getPrice() * 0.75)) + "** créditos")
											:
											"Por " + (seller == null ? "Desconhecido" : seller.getName()) + " | Preço: " + (cm.getPrice() > (cm.getCard().getCard().getRarity().getIndex() * Helper.BASE_CARD_PRICE * 50 * (cm.getCard().isFoil() ? 2 : 1)) ? "**`valor muito alto`**" : "**" + Helper.separate(cm.getPrice()) + "** créditos"),
									false
							);
						}
						case EVOGEAR -> {
							EquipmentMarket em = (EquipmentMarket) cards.get(p).getLeft();
							User seller = Main.getInfo().getUserByID(em.getSeller());
							eb.addField(
									"`ID: " + em.getId() + "` | " + em.getCard().getCard().getName() + " (" + em.getCard().getCard().getRarity().toString() + ")",
									blackfriday ?
											"Por " + (seller == null ? "Desconhecido" : seller.getName()) + " | Preço: " + (em.getPrice() > (em.getCard().getTier() * Helper.BASE_CARD_PRICE * 50) ? "**`valor muito alto`**" : "~~" + Helper.separate(em.getPrice()) + "~~ **" + Helper.separate(Math.round(em.getPrice() * 0.75)) + "** créditos")
											:
											"Por " + (seller == null ? "Desconhecido" : seller.getName()) + " | Preço: " + (em.getPrice() > (em.getCard().getTier() * Helper.BASE_CARD_PRICE * 50) ? "**`valor muito alto`**" : "**" + Helper.separate(em.getPrice()) + "** créditos"),
									false
							);
						}
						case FIELD -> {
							FieldMarket fm = (FieldMarket) cards.get(p).getLeft();
							User seller = Main.getInfo().getUserByID(fm.getSeller());
							eb.addField(
									"`ID: " + fm.getId() + "` | " + fm.getCard().getCard().getName() + " (" + fm.getCard().getCard().getRarity().toString() + ")",
									blackfriday ?
											"Por " + (seller == null ? "Desconhecido" : seller.getName()) + " | Preço: " + (fm.getPrice() > Helper.BASE_FIELD_PRICE ? "**`valor muito alto`**" : "~~" + Helper.separate(fm.getPrice()) + "~~ **" + Helper.separate(Math.round(fm.getPrice() * 0.75)) + "** créditos")
											:
											"Por " + (seller == null ? "Desconhecido" : seller.getName()) + " | Preço: " + (fm.getPrice() > Helper.BASE_FIELD_PRICE ? "**`valor muito alto`**" : "**" + Helper.separate(fm.getPrice()) + "** créditos"),
									false
							);
						}
					}
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

		CardMarket cm = CardMarketDAO.getCard(Integer.parseInt(args[0]));
		EquipmentMarket em = EquipmentMarketDAO.getCard(Integer.parseInt(args[0]));
		FieldMarket fm = FieldMarketDAO.getCard(Integer.parseInt(args[0]));

		if (buyer.getLoan() > 0) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_cannot-transfer-with-loan")).queue();
			return;
		}

		int type = cm == null ? em == null ? fm == null ? -1 : 3 : 2 : 1;
		switch (type) {
			case 1 -> {
				Account seller = AccountDAO.getAccount(cm.getSeller());
				if (!seller.getUid().equals(author.getId())) {
					if (buyer.getBalance() < (blackfriday ? cm.getPrice() * 0.75 : cm.getPrice())) {
						channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user")).queue();
						return;
					}

					Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

					if (kp.getCards().contains(cm.getCard())) {
						channel.sendMessage("❌ | Parece que você já possui essa carta!").queue();
						return;
					}

					kp.addCard(cm.getCard());
					KawaiponDAO.saveKawaipon(kp);

					int rawAmount = cm.getPrice();
					int liquidAmount = Helper.applyTax(seller.getUid(), rawAmount, 0.1);
					boolean taxed = rawAmount != liquidAmount;

					seller.addCredit(liquidAmount, this.getClass());
					buyer.removeCredit(blackfriday ? Math.round(cm.getPrice() * 0.75) : cm.getPrice(), this.getClass());

					LotteryValue lv = LotteryDAO.getLotteryValue();
					lv.addValue(rawAmount - liquidAmount);
					LotteryDAO.saveLotteryValue(lv);

					AccountDAO.saveAccount(seller);
					AccountDAO.saveAccount(buyer);

					cm.setBuyer(author.getId());
					CardMarketDAO.saveCard(cm);

					User sellerU = Main.getInfo().getUserByID(cm.getSeller());
					User buyerU = Main.getInfo().getUserByID(cm.getBuyer());
					if (taxed) {
						if (sellerU != null) sellerU.openPrivateChannel().queue(c ->
										c.sendMessage("✅ | Sua carta `" + cm.getCard().getName() + "` foi comprada por " + buyerU.getName() + " por " + Helper.separate(cm.getPrice()) + " créditos!  (Exceed vitorioso isento de taxa)").queue(null, Helper::doNothing),
								Helper::doNothing
						);
					} else {
						if (sellerU != null) sellerU.openPrivateChannel().queue(c ->
										c.sendMessage("✅ | Sua carta `" + cm.getCard().getName() + "` foi comprada por " + buyerU.getName() + " por " + Helper.separate(cm.getPrice()) + " créditos!  (Taxa de venda: " + Helper.roundToString((liquidAmount * 100D / rawAmount) - 100, 1) + "%)").queue(null, Helper::doNothing),
								Helper::doNothing
						);
					}
					channel.sendMessage("✅ | Carta comprada com sucesso!").queue();
				} else {
					Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

					if (kp.getCards().contains(cm.getCard())) {
						channel.sendMessage("❌ | Parece que você já possui essa carta!").queue();
						return;
					}

					kp.addCard(cm.getCard());
					KawaiponDAO.saveKawaipon(kp);

					cm.setBuyer(author.getId());
					CardMarketDAO.saveCard(cm);

					channel.sendMessage("✅ | Carta retirada com sucesso!").queue();
				}
			}
			case 2 -> {
				Account seller = AccountDAO.getAccount(em.getSeller());
				if (!seller.getUid().equals(author.getId())) {
					if (buyer.getBalance() < (blackfriday ? em.getPrice() * 0.75 : em.getPrice())) {
						channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user")).queue();
						return;
					}

					Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

					if (kp.checkEquipment(em.getCard(), channel)) return;

					kp.addEquipment(em.getCard());
					KawaiponDAO.saveKawaipon(kp);

					int rawAmount = em.getPrice();
					int liquidAmount = Helper.applyTax(seller.getUid(), rawAmount, 0.1);
					boolean taxed = rawAmount != liquidAmount;

					seller.addCredit(liquidAmount, this.getClass());
					buyer.removeCredit(blackfriday ? Math.round(em.getPrice() * 0.75) : em.getPrice(), this.getClass());

					LotteryValue lv = LotteryDAO.getLotteryValue();
					lv.addValue(rawAmount - liquidAmount);
					LotteryDAO.saveLotteryValue(lv);

					AccountDAO.saveAccount(seller);
					AccountDAO.saveAccount(buyer);

					em.setBuyer(author.getId());
					EquipmentMarketDAO.saveCard(em);

					User sellerU = Main.getInfo().getUserByID(em.getSeller());
					User buyerU = Main.getInfo().getUserByID(em.getBuyer());
					if (taxed) {
						if (sellerU != null) sellerU.openPrivateChannel().queue(c ->
										c.sendMessage("✅ | Sua carta `" + em.getCard().getCard().getName() + "` foi comprada por " + buyerU.getName() + " por " + Helper.separate(em.getPrice()) + " créditos!  (Exceed vitorioso isento de taxa)").queue(null, Helper::doNothing),
								Helper::doNothing
						);
					} else {
						if (sellerU != null) sellerU.openPrivateChannel().queue(c ->
										c.sendMessage("✅ | Sua carta `" + em.getCard().getCard().getName() + "` foi comprada por " + buyerU.getName() + " por " + Helper.separate(em.getPrice()) + " créditos!  (Taxa de venda: " + Helper.roundToString((liquidAmount * 100D / rawAmount) - 100, 1) + "%)").queue(null, Helper::doNothing),
								Helper::doNothing
						);
					}
					channel.sendMessage("✅ | Equipamento comprado com sucesso!").queue();
				} else {
					Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

					if (kp.checkEquipment(em.getCard(), channel)) return;

					kp.addEquipment(em.getCard());
					KawaiponDAO.saveKawaipon(kp);

					em.setBuyer(author.getId());
					EquipmentMarketDAO.saveCard(em);

					channel.sendMessage("✅ | Equipamento retirado com sucesso!").queue();
				}
			}
			case 3 -> {
				Account seller = AccountDAO.getAccount(fm.getSeller());
				if (!seller.getUid().equals(author.getId())) {
					if (buyer.getBalance() < (blackfriday ? fm.getPrice() * 0.75 : fm.getPrice())) {
						channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user")).queue();
						return;
					}

					Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

					if (kp.checkField(fm.getCard(), channel)) return;

					kp.addField(fm.getCard());
					KawaiponDAO.saveKawaipon(kp);

					int rawAmount = fm.getPrice();
					int liquidAmount = Helper.applyTax(seller.getUid(), rawAmount, 0.1);
					boolean taxed = rawAmount != liquidAmount;

					seller.addCredit(liquidAmount, this.getClass());
					buyer.removeCredit(blackfriday ? Math.round(fm.getPrice() * 0.75) : fm.getPrice(), this.getClass());

					LotteryValue lv = LotteryDAO.getLotteryValue();
					lv.addValue(rawAmount - liquidAmount);
					LotteryDAO.saveLotteryValue(lv);

					AccountDAO.saveAccount(seller);
					AccountDAO.saveAccount(buyer);

					fm.setBuyer(author.getId());
					FieldMarketDAO.saveCard(fm);

					User sellerU = Main.getInfo().getUserByID(fm.getSeller());
					User buyerU = Main.getInfo().getUserByID(fm.getBuyer());
					if (taxed) {
						if (sellerU != null) sellerU.openPrivateChannel().queue(c ->
										c.sendMessage("✅ | Sua carta `" + fm.getCard().getCard().getName() + "` foi comprada por " + buyerU.getName() + " por " + Helper.separate(fm.getPrice()) + " créditos!  (Exceed vitorioso isento de taxa)").queue(null, Helper::doNothing),
								Helper::doNothing
						);
					} else {
						if (sellerU != null) sellerU.openPrivateChannel().queue(c ->
										c.sendMessage("✅ | Sua carta `" + fm.getCard().getCard().getName() + "` foi comprada por " + buyerU.getName() + " por " + Helper.separate(fm.getPrice()) + " créditos!  (Taxa de venda: " + Helper.roundToString((liquidAmount * 100D / rawAmount) - 100, 1) + "%)").queue(null, Helper::doNothing),
								Helper::doNothing
						);
					}
					channel.sendMessage("✅ | Arena comprada com sucesso!").queue();
				} else {
					Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

					if (kp.checkField(fm.getCard(), channel)) return;

					kp.addField(fm.getCard());
					KawaiponDAO.saveKawaipon(kp);

					fm.setBuyer(author.getId());
					FieldMarketDAO.saveCard(fm);

					channel.sendMessage("✅ | Arena retirada com sucesso!").queue();
				}
			}
			case -1 -> channel.sendMessage("❌ | ID inválido ou a carta já foi comprada por alguém.").queue();
		}
	}
}
