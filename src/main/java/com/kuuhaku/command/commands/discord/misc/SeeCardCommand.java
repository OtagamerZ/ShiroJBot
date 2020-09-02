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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.postgresql.RarityColorsDAO;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.AnimeName;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.KawaiponRarity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;

public class SeeCardCommand extends Command {

	public SeeCardCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public SeeCardCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public SeeCardCommand(@NonNls String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public SeeCardCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage("❌ | Você precisa informar uma carta.").queue();
			return;
		}

		Card tc = CardDAO.getCard(args[0], true);
		try {
			if (tc == null) {
				channel.sendMessage("❌ | Essa carta não existe.").queue();
				return;
			} else if (args.length < 2 && tc.getRarity() != KawaiponRarity.ULTIMATE) {
				channel.sendMessage("❌ | Você também precisa informar o tipo dela (`N` = normal, `C` = cromada).").queue();
				return;
			} else if (!Helper.equalsAny(args[1], "N", "C")) {
				channel.sendMessage("❌ | Você precisa informar o tipo da carta que deseja ver (`N` = normal, `C` = cromada).").queue();
				return;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			channel.sendMessage("❌ | Você precisa informar o tipo da carta que deseja ver (`N` = normal, `C` = cromada).").queue();
			return;
		}

		boolean foil = tc.getRarity() != KawaiponRarity.ULTIMATE && args[1].equalsIgnoreCase("C");
		KawaiponCard card = new KawaiponCard(tc, foil);
		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

		Set<KawaiponCard> cards = kp.getCards();
		for (AnimeName anime : AnimeName.values()) {
			if (CardDAO.totalCards(anime) == kp.getCards().stream().filter(k -> k.getCard().getAnime().equals(anime) && !k.isFoil()).count())
				cards.add(new KawaiponCard(CardDAO.getUltimate(anime), false));
		}

		EmbedBuilder eb = new ColorlessEmbedBuilder();

		eb.setTitle((foil ? ":star2:" : ":flower_playing_cards:") + " | " + card.getName());
		eb.setColor(RarityColorsDAO.getColor(tc.getRarity()).getPrimary());
		eb.addField("Obtida:", cards.contains(card) ? "Sim" : "Não", true);
		eb.addField("Raridade:", tc.getRarity().toString(), true);
		eb.addField("Tipo:", tc.getRarity() == KawaiponRarity.ULTIMATE ? "Única" : (card.isFoil() ? "Cromada" : "Normal"), true);
		eb.addField("Anime:", tc.getAnime().toString(), true);
		eb.setImage("attachment://kawaipon." + (cards.contains(card) ? "png" : "jpg"));

		try {
			BufferedImage bi = (ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("kawaipon/missing.jpg"))));

			if (cards.contains(card))
				channel.sendMessage(eb.build()).addFile(Helper.getBytes(tc.drawCard(foil), "png"), "kawaipon.png").queue();
			else
				channel.sendMessage(eb.build()).addFile(Helper.getBytes(bi), "kawaipon.jpg").queue();
		} catch (IOException e) {
			channel.sendMessage("❌ | Deu um pequeno erro aqui na hora de mostrar a carta, logo logo um dos meus desenvolvedores irá corrigi-lo!").queue();
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}
}