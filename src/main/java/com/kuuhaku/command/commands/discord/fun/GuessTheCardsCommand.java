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

package com.kuuhaku.command.commands.discord.fun;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.sqlite.PStateDAO;
import com.kuuhaku.handlers.games.disboard.model.PoliticalState;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GuessTheCardsCommand extends Command {

	public GuessTheCardsCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public GuessTheCardsCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public GuessTheCardsCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public GuessTheCardsCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().gameInProgress(author.getId())) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_you-are-in-game")).queue();
			return;
		}

		try {
			BufferedImage mask = ImageIO.read(Objects.requireNonNull(GuessTheCardsCommand.class.getClassLoader().getResourceAsStream("assets/gtc_mask.png")));
			List<Card> c = Helper.getRandomN(CardDAO.getCards(), 3, 1);
			List<BufferedImage> imgs = c.stream()
					.map(Card::drawCardNoBorder)
					.collect(Collectors.toList());

			BufferedImage img = new BufferedImage(225, 350, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = img.createGraphics();
			for (int i = 0; i < imgs.size(); i++) {
				BufferedImage bi = imgs.get(i);
				Helper.applyMask(bi, mask, i + 1);
				g2d.drawImage(bi, 0, 0, null);
			}
			g2d.dispose();

			channel.sendMessage("Quais são as 3 cartas nesta imagem? Escreva os três nomes com `_` no lugar de espaços e separados por ponto-e-vírgula (`;`).")
					.addFile(Helper.getBytes(img, "png"), "image.png")
					.queue();

			Helper.awaitMessage(author, (TextChannel) channel, (msg) -> {
				String[] answers = msg.getContentRaw().split(";");

				if (answers.length != 3) {
					channel.sendMessage("❌ | Você deve informar exatamente 3 nomes separados por ponto-e-vírgula, este jogo foi anulado.").queue();
					return;
				}

				List<String> names = c.stream().map(Card::getId).collect(Collectors.toList());

				int points = 0;
				for (String s : answers)
					points += names.remove(s.toUpperCase()) ? 1 : 0;

				int reward = Helper.rng(250, false) * points;

				Account acc = AccountDAO.getAccount(author.getId());
				acc.addCredit(reward, GuessTheCardsCommand.class);
				if (ExceedDAO.hasExceed(author.getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnum.getByName(ExceedDAO.getExceed(author.getId())));
					ps.modifyInfluence(2 * points);
					PStateDAO.savePoliticalState(ps);
				}
				switch (points) {
					case 0 -> channel.sendMessage(
							"Você não acertou nenhum dos 3 nomes, que eram `%s`, `%s` e `%s`."
									.formatted(
											names.get(0),
											names.get(1),
											names.get(2)
									)).queue();
					case 1 -> channel.sendMessage(
							"Você acertou 1 dos 3 nomes, os outro eram `%s` e `%s`. (Recebeu %s créditos)."
									.formatted(
											names.get(0),
											names.get(1),
											reward
									)).queue();
					case 2 -> channel.sendMessage(
							"Você acertou 2 dos 3 nomes, o outro era `%s`. (Recebeu %s créditos)."
									.formatted(
											names.get(0),
											reward
									)).queue();
					case 3 -> channel.sendMessage(
							"Você acertou todos os nomes, parabéns! (Recebeu %s créditos)."
									.formatted(reward)).queue();
				}
			});
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}
}
