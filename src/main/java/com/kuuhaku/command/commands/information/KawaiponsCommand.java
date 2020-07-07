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

package com.kuuhaku.command.commands.information;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.model.common.KawaiponBook;
import com.kuuhaku.model.common.NewKawaiponBook;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.AnimeName;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class KawaiponsCommand extends Command {

	public KawaiponsCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public KawaiponsCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public KawaiponsCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public KawaiponsCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (author.getId().equals(Main.getInfo().getNiiChan())) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("str_generating-collection")).queue(m -> {
				try {
					Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

					if (kp.getCards().size() == 0) {
						m.editMessage(":x: | Você ainda não coletou nenhum Kawaipon.").queue();
						return;
					} else if (args.length == 0) {
						Set<KawaiponCard> collection = new HashSet<>();
						for (AnimeName anime : AnimeName.values()) {
							if (CardDAO.animeCount(anime) == kp.getCards().stream().filter(k -> k.getCard().getAnime().equals(anime)).count())
								collection.add(new KawaiponCard(CardDAO.getUltimate(anime), false));
						}

						NewKawaiponBook kb = new NewKawaiponBook(collection);
						BufferedImage cards = kb.view(null);

						EmbedBuilder eb = new EmbedBuilder();
						int count = collection.size();

						eb.setTitle("\uD83C\uDFB4 | Kawaipons de " + author.getName());
						eb.addField(":red_envelope: | Coleções completas:", count + " de " + AnimeName.values().length + " (" + Helper.prcntToInt(count, AnimeName.values().length) + "%)", true);
						eb.setImage("attachment://cards.png");
						eb.setFooter("Total coletado (normais + cromadas): " + Helper.prcntToInt(kp.getCards().size(), CardDAO.totalCards() * 2) + "%");

						m.delete().queue();
						channel.sendMessage(eb.build()).addFile(Helper.getBytes(cards, "png"), "cards.png").queue();
						return;
					} else if (Arrays.stream(AnimeName.values()).noneMatch(a -> a.name().equals(args[0].toUpperCase()))) {
						channel.sendMessage(":x: | Anime inválido ou ainda não adicionado (colocar `_` no lugar de espaços).").queue();
						return;
					}

					AnimeName anime = AnimeName.valueOf(args[0].toUpperCase());
					Set<KawaiponCard> collection = kp.getCards().stream().filter(k -> k.getCard().getAnime().equals(anime)).collect(Collectors.toSet());
					if (CardDAO.animeCount(anime) == kp.getCards().stream().filter(k -> k.getCard().getAnime().equals(anime)).count())
						collection.add(new KawaiponCard(CardDAO.getUltimate(anime), false));

					NewKawaiponBook kb = new NewKawaiponBook(collection);
					BufferedImage cards = kb.view(anime);

					EmbedBuilder eb = new EmbedBuilder();
					int foil = (int) kp.getCards().stream().filter(KawaiponCard::isFoil).count();
					int common = kp.getCards().size() - foil;

					eb.setTitle("\uD83C\uDFB4 | Kawaipons de " + author.getName() + " (" + anime.toString() + ")");
					eb.addField(":red_envelope: | Cartas comuns:", common + " de " + CardDAO.getCardsByAnime(anime).size() + " (" + Helper.prcntToInt(common, CardDAO.getCardsByAnime(anime).size()) + "%)", true);
					eb.addField(":star2: | Cartas cromadas:", foil + " de " + CardDAO.getCardsByAnime(anime).size() + " (" + Helper.prcntToInt(foil, CardDAO.getCardsByAnime(anime).size()) + "%)", true);
					eb.setImage("attachment://cards.png");
					eb.setFooter("Total coletado (normais + cromadas): " + Helper.prcntToInt(kp.getCards().size(), CardDAO.getCardsByAnime(anime).size() * 2) + "%");

					m.delete().queue();
					channel.sendMessage(eb.build()).addFile(Helper.getBytes(cards, "png"), "cards.png").queue();
				} catch (IOException | InterruptedException e) {
					m.editMessage(ShiroInfo.getLocale(I18n.PT).getString("err_collection-generation-error")).queue();
					Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
				}
			});
		} else {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("str_generating-collection")).queue(m -> {
				try {
					Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

					if (kp.getCards().size() == 0) {
						m.editMessage(":x: | Você ainda não coletou nenhum Kawaipon.").queue();
						return;
					}

					Set<KawaiponCard> collection = kp.getCards();
					for (AnimeName anime : AnimeName.values()) {
						if (CardDAO.animeCount(anime) == kp.getCards().stream().filter(k -> k.getCard().getAnime().equals(anime)).count())
							collection.add(new KawaiponCard(CardDAO.getUltimate(anime), false));
					}

					KawaiponBook kb = new KawaiponBook(collection);
					List<BufferedImage> cards = kb.view();
					int page;
					if (args.length < 1) page = 0;
					else {
						if (!StringUtils.isNumeric(args[0])) {
							channel.sendMessage(":x: | A página precisa ser um valor inteiro.").queue();
							return;
						}

						page = Integer.parseInt(args[0]) - 1;
					}

					if (page >= cards.size() || page < 0) {
						m.editMessage(":x: | A página precisa ser um valor entre 1 e " + cards.size() + ".").queue();
						return;
					}

					EmbedBuilder eb = new EmbedBuilder();
					int foil = (int) kp.getCards().stream().filter(KawaiponCard::isFoil).count();
					int common = kp.getCards().size() - foil;

					eb.setTitle("\uD83C\uDFB4 | Kawaipons de " + author.getName() + " (página " + (page + 1) + ")");
					eb.addField(":red_envelope: | Cartas comuns:", common + " de " + CardDAO.totalCards() + " (" + Helper.prcntToInt(common, CardDAO.totalCards()) + "%)", true);
					eb.addField(":star2: | Cartas cromadas:", foil + " de " + CardDAO.totalCards() + " (" + Helper.prcntToInt(foil, CardDAO.totalCards()) + "%)", true);
					eb.setImage("attachment://page.jpg");
					eb.setFooter("Total coletado (normais + cromadas): " + Helper.prcntToInt(kp.getCards().size(), CardDAO.totalCards() * 2) + "%");

					m.delete().queue();
					channel.sendMessage(eb.build()).addFile(Helper.getBytes(cards.get(page)), "page.jpg").queue();
				} catch (IOException e) {
					m.editMessage(ShiroInfo.getLocale(I18n.PT).getString("err_collection-generation-error")).queue();
					Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
				}
			});
		}
	}
}
