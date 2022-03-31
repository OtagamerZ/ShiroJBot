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
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.postgresql.MarketDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Event;
import com.kuuhaku.model.enums.I18n;
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
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "comprar",
		aliases = {"buy", "loja"},
		usage = "req_id-override",
		category = Category.MISC
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION
})
public class BuyCardCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		boolean blackfriday = Event.getCurrent() == Event.BLACKFRIDAY;
		Account buyer = AccountDAO.getAccount(author.getId());
		if (args.length < 1 || !StringUtils.isNumeric(args[0])) {
			Options opt = new Options()
					.addOption("n", "nome", true, "Busca por nome")
					.addOption("r", "raridade", true, "Busca por raridade")
					.addOption("a", "anime", true, "Busca por anime")
					.addOption("j", "emoji", true, "Busca por emoji")
					.addOption(">", "min", true, "Define um valor mínimo")
					.addOption("<", "max", true, "Define um valor máximo")
					.addOption("c", "cromada", false, "Apenas cartas cromadas")
					.addOption("m", "minhas", false, "Apenas cartas suas")
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

			int total = MarketDAO.getTotalOffers(author, finalCli);
			ThrowingFunction<Integer, Page> load = i -> {
				List<Market> cards = MarketDAO.getOffers(i, author, finalCli);
				if (cards.isEmpty()) return null;

				EmbedBuilder eb = new ColorlessEmbedBuilder()
						.setAuthor("Resultados: " + Helper.separate(total) + " | Página " + (i + 1))
						.setTitle(":scales: | Mercado de cartas")
						.setFooter("Seus CR: " + Helper.separate(buyer.getBalance()), "https://i.imgur.com/U0nPjLx.gif");

				if (i == 0) {
					XStringBuilder sb = new XStringBuilder()
							.append("Use `%scomprar ID` para comprar uma carta.\n".formatted(prefix))
							.appendNewLine("**Parâmetros de pesquisa:**");

					for (Option op : opt.getOptions()) {
						sb.appendNewLine("`-%s/--%s` - %s".formatted(
								op.getOpt(),
								op.getLongOpt(),
								op.getDescription()
						));
					}
				}

				for (Market m : cards) {
					User seller = Main.getInfo().getUserByID(m.getSeller());
					String name = switch (m.getType()) {
						case EVOGEAR, FIELD -> m.getRawCard().getName();
						default -> ((KawaiponCard) m.getCard()).getName();
					};
					String rarity = switch (m.getType()) {
						case EVOGEAR -> "Equipamento (" + StringUtils.repeat("⭐", ((Equipment) m.getCard()).getTier()) + ")";
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
											? "~~" + Helper.separate(m.getPrice()) + "~~ **" + Helper.separate(Math.round(m.getPrice() * 0.75)) + "**"
											: "**" + Helper.separate(m.getPrice()) + "**"
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
					Pages.lazyPaginate(s, load, ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
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
		}

		Account seller = AccountDAO.getAccount(m.getSeller());
		if (!seller.getUid().equals(author.getId())) {
			int price = (int) Math.round(m.getPrice() * (blackfriday ? 0.75 : 1));
			if (buyer.getBalance() < price) {
				channel.sendMessage(I18n.getString("err_insufficient-credits-user")).queue();
				return;
			}

			String name = switch (m.getType()) {
				case EVOGEAR, FIELD -> m.getRawCard().getName();
				default -> ((KawaiponCard) m.getCard()).getName();
			};

			User sellerU = Main.getInfo().getUserByID(m.getSeller());

			if (args.length > 1 && args[1].equalsIgnoreCase("s")) {
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

				m.setBuyer(author.getId());
				MarketDAO.saveCard(m);

				KawaiponDAO.saveKawaipon(kp);

				seller.addCredit(price, this.getClass());
				buyer.removeCredit(price, this.getClass());

				AccountDAO.saveAccount(seller);
				AccountDAO.saveAccount(buyer);

				if (sellerU != null) sellerU.openPrivateChannel().queue(c ->
								c.sendMessage("✅ | Sua carta `" + name + "` foi comprada por " + author.getName() + " por **" + Helper.separate(price) + " CR**!").queue(null, Helper::doNothing),
						Helper::doNothing
				);

				channel.sendMessage("✅ | Carta `" + name + "` comprada com sucesso!").queue();
			} else {
				Market finalM = m;
				Main.getInfo().getConfirmationPending().put(author.getId(), true);
				channel.sendMessage("Você está prestes a comprar a carta `" + name + "` por **" + Helper.separate(price) + " CR**, deseja confirmar?")
						.queue(s -> Pages.buttonize(s, Map.of(Helper.parseEmoji(Helper.ACCEPT), wrapper -> {
									Main.getInfo().getConfirmationPending().remove(author.getId());
									Market mkt = MarketDAO.getCard(Integer.parseInt(args[0]));
									if (mkt == null) {
										channel.sendMessage("❌ | ID inválido ou a carta já foi comprada por alguém.").queue();
										return;
									}

									Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
									switch (finalM.getType()) {
										case EVOGEAR -> {
											Deck dk = kp.getDeck();

											if (dk.checkEquipment(finalM.getCard(), channel)) return;

											dk.addEquipment(finalM.getCard());
										}
										case FIELD -> {
											Deck dk = kp.getDeck();

											if (dk.checkField(finalM.getCard(), channel)) return;

											dk.addField(finalM.getCard());

										}
										default -> {
											if (kp.getCards().contains((KawaiponCard) finalM.getCard())) {
												channel.sendMessage("❌ | Parece que você já possui essa carta!").queue();
												return;
											}

											kp.addCard(finalM.getCard());
										}
									}

									mkt.setBuyer(author.getId());
									MarketDAO.saveCard(mkt);

									KawaiponDAO.saveKawaipon(kp);

									seller.addCredit(price, this.getClass());
									buyer.removeCredit(price, this.getClass());

									AccountDAO.saveAccount(seller);
									AccountDAO.saveAccount(buyer);

									if (sellerU != null) sellerU.openPrivateChannel().queue(c ->
													c.sendMessage("✅ | Sua carta `" + name + "` foi comprada por " + author.getName() + " por " + Helper.separate(price) + " CR!").queue(null, Helper::doNothing),
											Helper::doNothing
									);

									s.delete().mapToResult().flatMap(d -> channel.sendMessage("✅ | Carta `" + name + "` comprada com sucesso!")).queue();
								}), ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
								u -> u.getId().equals(author.getId()),
								ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
						));
			}
		} else {
			m = MarketDAO.getCard(Integer.parseInt(args[0]));
			if (m == null) {
				channel.sendMessage("❌ | ID inválido ou a carta já foi comprada por alguém.").queue();
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

			m.setBuyer(author.getId());
			MarketDAO.saveCard(m);

			KawaiponDAO.saveKawaipon(kp);

			channel.sendMessage("✅ | Carta `" + m.getRawCard().getName() + "` retirada com sucesso!").queue();
		}
	}
}
