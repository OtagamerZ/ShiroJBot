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

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.Tradutor;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.postgresql.QuizDAO;
import com.kuuhaku.controller.sqlite.PStateDAO;
import com.kuuhaku.handlers.games.disboard.model.PoliticalState;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.AnsweredQuizzes;
import com.kuuhaku.utils.ExceedEnums;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class QuizCommand extends Command {

	public QuizCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public QuizCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public QuizCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public QuizCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().gameInProgress(author.getId())) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_you-are-in-game")).queue();
			return;
		}

		AnsweredQuizzes aq = QuizDAO.getUserState(author.getId());

		if (aq.getTimes() == 10) {
			int time = (6 - (LocalDateTime.now().getHour() % 6));
			channel.sendMessage("❌ | Você já jogou muitas vezes, aguarde " + time + " hora" + (time == 1 ? "" : "s") + " para jogar novamente!").queue();
			return;
		}

		String diff;

		if (args.length > 0) switch (StringUtils.stripAccents(args[0].toLowerCase())) {
			case "facil":
			case "easy":
				diff = "easy";
				break;
			case "medio":
			case "medium":
				diff = "medium";
				break;
			case "dificil":
			case "hard":
				diff = "hard";
				break;
			default:
				diff = null;
		}
		else diff = null;

		try {
			JSONObject res = Helper.callApi("https://opentdb.com/api.php?amount=1&category=" + (Math.random() > 0.5 ? 15 : 31) + (diff == null ? "" : "&difficulty=" + diff) + "&type=multiple&encode=url3986");
			assert res != null;
			String question = URLDecoder.decode(res
					.getJSONArray("results")
					.getJSONObject(0)
					.getString("question"), StandardCharsets.UTF_8);

			String correct = Tradutor.translate("en", "pt", URLDecoder.decode(res
					.getJSONArray("results")
					.getJSONObject(0)
					.getString("correct_answer"), StandardCharsets.UTF_8));

			List<String> wrong = res
					.getJSONArray("results")
					.getJSONObject(0)
					.getJSONArray("incorrect_answers")
					.toList()
					.stream()
					.map(o -> {
						try {
							return Tradutor.translate("en", "pt", URLDecoder.decode(String.valueOf(o), StandardCharsets.UTF_8));
						} catch (IOException e) {
							Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
							return "ERRO";
						}
					})
					.collect(Collectors.toList());
			String difficulty = Tradutor.translate("en", "pt", res.getJSONArray("results").getJSONObject(0).getString("difficulty"));

			int modif;
			switch (res.getJSONArray("results").getJSONObject(0).getString("difficulty")) {
				case "easy":
					modif = 1;
					break;
				case "medium":
					modif = 2;
					break;
				case "hard":
					modif = 3;
					break;
				default:
					modif = 0;
			}

			Account acc = AccountDAO.getAccount(author.getId());
			aq.played();
			QuizDAO.saveUserState(aq);

			EmbedBuilder eb = new EmbedBuilder();
			eb.setThumbnail("https://images.vexels.com/media/users/3/152594/isolated/preview/d00d116b2c073ccf7f9fec677fec78e3---cone-de-ponto-de-interroga----o-quadrado-roxo-by-vexels.png");
			eb.setTitle("Hora do quiz! (" + difficulty.toUpperCase() + ")");
			eb.setDescription("**Traduzida:**\n" + Tradutor.translate("en", "pt", question) + "\n\n**Original:\n**" + question);
			eb.setColor(Color.decode("#2195f2"));

			List<String> opts = List.of(
					"\uD83C\uDDE6",
					"\uD83C\uDDE7",
					"\uD83C\uDDE8",
					"\uD83C\uDDE9"
			);
			List<String> shuffledOpts = new ArrayList<>(wrong);
			shuffledOpts.add(correct);
			Collections.shuffle(shuffledOpts);
			List<MessageEmbed.Field> fields = new ArrayList<>();

			TreeMap<String, BiConsumer<Member, Message>> buttons = new TreeMap<>();

			for (int i = 0; i < opts.size(); i++) {
				int finalI = i;
				buttons.put(opts.get(i), (mb, ms) -> {
					if (!mb.getId().equals(author.getId())) return;
					eb.clear();
					eb.setThumbnail("https://images.vexels.com/media/users/3/152594/isolated/preview/d00d116b2c073ccf7f9fec677fec78e3---cone-de-ponto-de-interroga----o-quadrado-roxo-by-vexels.png");

					if (shuffledOpts.get(finalI).equalsIgnoreCase(correct)) {
						int p = Helper.clamp(Helper.rng(150 * modif, false), (150 * modif) / 3, 150 * modif);
						acc.addCredit(p, this.getClass());
						AccountDAO.saveAccount(acc);

						eb.setTitle("Resposta correta!");
						eb.setDescription("Seu prêmio é de " + p + " créditos!");
						eb.setColor(Color.green);

						if (ExceedDAO.hasExceed(author.getId())) {
							PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnums.getByName(ExceedDAO.getExceed(author.getId())));
							ps.modifyInfluence(true);
							PStateDAO.savePoliticalState(ps);
						}
					} else {
						eb.setTitle("Resposta incorreta.");
						eb.setDescription("Você errou. Tente novamente!");
						eb.setColor(Color.red);

						if (ExceedDAO.hasExceed(author.getId())) {
							PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnums.getByName(ExceedDAO.getExceed(author.getId())));
							ps.modifyInfluence(false);
							PStateDAO.savePoliticalState(ps);
						}
					}

					ms.delete().queue();

					channel.sendMessage(eb.build()).queue();
				});

				fields.add(new MessageEmbed.Field("Alternativa " + opts.get(i), shuffledOpts.get(i), true));
			}

			fields.sort(Comparator.comparing(MessageEmbed.Field::getName));
			fields.forEach(eb::addField);
			channel.sendMessage(eb.build()).queue(s -> Pages.buttonize(s, buttons, false, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId())));
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}
}
