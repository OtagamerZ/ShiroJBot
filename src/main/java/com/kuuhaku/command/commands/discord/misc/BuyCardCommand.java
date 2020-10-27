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
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardMarketDAO;
import com.kuuhaku.controller.postgresql.EquipmentMarketDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.AnimeName;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.CardMarket;
import com.kuuhaku.model.persistent.EquipmentMarket;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class BuyCardCommand extends Command {

	public BuyCardCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public BuyCardCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public BuyCardCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public BuyCardCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Account buyer = AccountDAO.getAccount(author.getId());
		if (args.length < 1 || !StringUtils.isNumeric(args[0])) {
			AtomicReference<String> byName = new AtomicReference<>(null);
			AtomicReference<KawaiponRarity> byRarity = new AtomicReference<>(null);
			AtomicReference<AnimeName[]> byAnime = new AtomicReference<>(null);
			AtomicBoolean onlyFoil = new AtomicBoolean();
			AtomicBoolean onlyMine = new AtomicBoolean();
			AtomicBoolean onlyEquip = new AtomicBoolean();

			if (args.length > 0) {
				List<String> params = List.of(args);
				byName.set(params.stream().filter(s -> s.startsWith("-n") && s.length() > 2).findFirst().orElse(null));
				if (byName.get() != null) byName.set(byName.get().substring(2));

				String rarity = params.stream().filter(s -> s.startsWith("-r") && s.length() > 2).findFirst().orElse(null);
				if (rarity != null) {
					byRarity.set(KawaiponRarity.getByName(rarity.substring(2)));
					if (byRarity.get() == null) {
						channel.sendMessage("❌ | Raridade inválida, verifique se digitou-a corretamente.").queue();
						return;
					}
				}

				String anime = params.stream().filter(s -> s.startsWith("-a") && s.length() > 2).findFirst().orElse(null);
				if (anime != null) {
					AnimeName[] an = Arrays.stream(AnimeName.validValues())
							.filter(a -> StringUtils.containsIgnoreCase(a.name(), anime.substring(2).toUpperCase()))
							.toArray(AnimeName[]::new);
					if (an.length == 0) {
						channel.sendMessage("❌ | Anime inválido, verifique se digitou-o corretamente.").queue();
						return;
					}
					byAnime.set(an);
				}

				onlyFoil.set(params.stream().anyMatch("-c"::equalsIgnoreCase));

				onlyMine.set(params.stream().anyMatch("-m"::equalsIgnoreCase));

				onlyEquip.set(params.stream().anyMatch("-e"::equalsIgnoreCase));
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
					`-e` - Busca apenas cartas-equipamento
					`-m` - Busca apenas suas cartas anunciadas
					       
					Cartas com valores acima de 50x o valor base não serão exibidas sem usar `-m`.
					""".formatted(prefix)
			);
			eb.setFooter("Seus créditos: " + buyer.getBalance(), "https://i.imgur.com/U0nPjLx.gif");

			List<Page> pages = new ArrayList<>();
			List<Object> cards;
			if (onlyEquip.get())
				cards = EquipmentMarketDAO.getCards().stream()
						.filter(em -> byName.get() == null || StringUtils.containsIgnoreCase(em.getCard().getCard().getName(), byName.get()))
						.filter(em -> onlyMine.get() ? em.getSeller().equals(author.getId()) : em.getPrice() <= (em.getCard().getTier() * Helper.BASE_CARD_PRICE * 50))
						.sorted(Comparator
								.comparingInt(EquipmentMarket::getPrice)
								.thenComparing(k -> k.getCard().getCard().getName(), String.CASE_INSENSITIVE_ORDER))
						.collect(Collectors.toList());
			else
				cards = CardMarketDAO.getCards().stream()
						.filter(cm -> byName.get() == null || StringUtils.containsIgnoreCase(cm.getCard().getName(), byName.get()))
						.filter(cm -> byRarity.get() == null || byRarity.get().equals(cm.getCard().getCard().getRarity()))
						.filter(cm -> byAnime.get() == null || ArrayUtils.contains(byAnime.get(), cm.getCard().getCard().getAnime()))
						.filter(cm -> !onlyFoil.get() || cm.getCard().isFoil())
						.filter(cm -> onlyMine.get() ? cm.getSeller().equals(author.getId()) : cm.getPrice() <= (cm.getCard().getCard().getRarity().getIndex() * Helper.BASE_CARD_PRICE * 50 * (cm.getCard().isFoil() ? 2 : 1)))
						.sorted(Comparator
								.comparingInt(CardMarket::getPrice)
								.thenComparing(k -> k.getCard().isFoil(), Comparator.reverseOrder())
								.thenComparing(k -> k.getCard().getCard().getRarity(), Comparator.comparingInt(KawaiponRarity::getIndex).reversed())
								.thenComparing(k -> k.getCard().getCard().getAnime(), Comparator.comparing(AnimeName::toString, String.CASE_INSENSITIVE_ORDER))
								.thenComparing(k -> k.getCard().getCard().getName(), String.CASE_INSENSITIVE_ORDER))
						.collect(Collectors.toList());


			for (int i = 0; i < Math.ceil(cards.size() / 10f); i++) {
				eb.clearFields();
				if (onlyEquip.get())
					for (int p = i * 10; p < cards.size() && p < 10 * (i + 1); p++) {
						EquipmentMarket em = (EquipmentMarket) cards.get(p);
						User seller = Main.getInfo().getUserByID(em.getSeller());
						eb.addField(
								"`ID: " + em.getId() + "` | " + em.getCard().getCard().getName() + " (" + em.getCard().getCard().getRarity().toString() + ")",
								"Por " + (seller == null ? "Desconhecido" : seller.getName()) + " | Preço: **" + (em.getPrice() > (em.getCard().getTier() * Helper.BASE_CARD_PRICE * 50) ? "`valor muito alto`**" : em.getPrice() + "** créditos"),
								false
						);
					}
				else
					for (int p = i * 10; p < cards.size() && p < 10 * (i + 1); p++) {
						CardMarket cm = (CardMarket) cards.get(p);
						User seller = Main.getInfo().getUserByID(cm.getSeller());
						eb.addField(
								"`ID: " + cm.getId() + "` | " + cm.getCard().getName() + " (" + cm.getCard().getCard().getRarity().toString() + ")",
								"Por " + (seller == null ? "Desconhecido" : seller.getName()) + " | Preço: **" + (cm.getPrice() > (cm.getCard().getCard().getRarity().getIndex() * Helper.BASE_CARD_PRICE * 50 * (cm.getCard().isFoil() ? 2 : 1)) ? "`valor muito alto`**" : cm.getPrice() + "** créditos"),
								false
						);
					}
				pages.add(new Page(PageType.EMBED, eb.build()));
			}

			if (pages.size() == 0) {
				channel.sendMessage("Ainda não há nenhuma carta anunciada.").queue();
			} else
				channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s -> Pages.paginate(s, pages, 1, TimeUnit.MINUTES, 5, u -> u.getId().equals(author.getId())));
			return;
		}

		CardMarket cm = CardMarketDAO.getCard(Integer.parseInt(args[0]));
		EquipmentMarket em = EquipmentMarketDAO.getCard(Integer.parseInt(args[0]));

		if (cm == null) {
			if (em == null) {
				channel.sendMessage("❌ | ID inválido ou a carta já foi comprada por alguém.").queue();
				return;
			}

			Account seller = AccountDAO.getAccount(em.getSeller());
			if (!seller.getUserId().equals(author.getId())) {
				if (buyer.getBalance() < em.getPrice()) {
					channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user")).queue();
					return;
				}

				Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

				if (Collections.frequency(kp.getEquipments(), em.getCard()) == 3) {
					channel.sendMessage("❌ | Parece que você já possui 3 cópias desse equipamento!").queue();
					return;
				} else if (kp.getEquipments().stream().filter(e -> e.getTier() == 4).count() == 1 && em.getCard().getTier() == 4) {
					channel.sendMessage("❌ | Parece que você já possui 1 equipamento tier 4!").queue();
					return;
				}

				kp.addEquipment(em.getCard());
				KawaiponDAO.saveKawaipon(kp);

				seller.addCredit(em.getPrice(), this.getClass());
				buyer.removeCredit(em.getPrice(), this.getClass());

				AccountDAO.saveAccount(seller);
				AccountDAO.saveAccount(buyer);

				em.setBuyer(author.getId());
				EquipmentMarketDAO.saveCard(em);

				User sellerU = Main.getInfo().getUserByID(em.getSeller());
				User buyerU = Main.getInfo().getUserByID(em.getBuyer());
				if (sellerU != null) sellerU.openPrivateChannel().queue(c ->
								c.sendMessage(":white_check_mark: | Seu equipamento `" + em.getCard().getCard().getName() + "` foi comprado por " + buyerU.getName() + " por " + em.getPrice() + " créditos.").queue(),
						Helper::doNothing
				);
				channel.sendMessage(":white_check_mark: | Equipamento comprado com sucesso!").queue();
			} else {
				Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

				if (Collections.frequency(kp.getEquipments(), em.getCard()) == 3) {
					channel.sendMessage("❌ | Parece que você já possui 3 cópias desse equipamento!").queue();
					return;
				}

				kp.addEquipment(em.getCard());
				KawaiponDAO.saveKawaipon(kp);

				em.setBuyer(author.getId());
				EquipmentMarketDAO.saveCard(em);

				channel.sendMessage(":white_check_mark: | Equipamento retirado com sucesso!").queue();
			}
			return;
		}

		Account seller = AccountDAO.getAccount(cm.getSeller());
		if (!seller.getUserId().equals(author.getId())) {
			if (buyer.getBalance() < cm.getPrice()) {
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

			seller.addCredit(cm.getPrice(), this.getClass());
			buyer.removeCredit(cm.getPrice(), this.getClass());

			AccountDAO.saveAccount(seller);
			AccountDAO.saveAccount(buyer);

			cm.setBuyer(author.getId());
			CardMarketDAO.saveCard(cm);

			User sellerU = Main.getInfo().getUserByID(cm.getSeller());
			User buyerU = Main.getInfo().getUserByID(cm.getBuyer());
			if (sellerU != null) sellerU.openPrivateChannel().queue(c ->
							c.sendMessage(":white_check_mark: | Sua carta `" + cm.getCard().getName() + "` foi comprada por " + buyerU.getName() + " por " + cm.getPrice() + " créditos.").queue(),
					Helper::doNothing
			);
			channel.sendMessage(":white_check_mark: | Carta comprada com sucesso!").queue();
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

			channel.sendMessage(":white_check_mark: | Carta retirada com sucesso!").queue();
		}
	}
}
