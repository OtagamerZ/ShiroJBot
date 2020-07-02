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

package com.kuuhaku.command.commands.fun;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.postgresql.QuizDAO;
import com.kuuhaku.controller.sqlite.PStateDAO;
import com.kuuhaku.handlers.games.disboard.model.PoliticalState;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.AnsweredQuizzes;
import com.kuuhaku.model.persistent.Quiz;
import com.kuuhaku.utils.ExceedEnums;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static com.kuuhaku.model.persistent.Quiz.OPTS;

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
		AnsweredQuizzes aq = QuizDAO.getUserState(author.getId());

		if (aq.getTimes() == 6) {
			int time = (6 - (LocalDateTime.now().getHour() % 6));
			channel.sendMessage(":x: | Você já jogou muitas vezes, aguarde " + time + " hora" + (time == 1 ? "" : "s") + " para jogar novamente!").queue();
			return;
		}

		Quiz q = QuizDAO.getRandomQuiz();
		Account acc = AccountDAO.getAccount(author.getId());
		aq.played();
		QuizDAO.saveUserState(aq);

		HashMap<String, Integer> values = new HashMap<>();

		for (int i = 0; i < OPTS.length; i++) {
			values.put(q.getOptions().getString(i), i + 1);
		}

		EmbedBuilder eb = new EmbedBuilder();
		eb.setThumbnail("https://images.vexels.com/media/users/3/152594/isolated/preview/d00d116b2c073ccf7f9fec677fec78e3---cone-de-ponto-de-interroga----o-quadrado-roxo-by-vexels.png");
		eb.setTitle("Hora do quiz!");
		eb.setDescription(q.getQuestion());
		eb.setColor(Color.decode("#2195f2"));

		List<String> shuffledOpts = Arrays.asList(OPTS);
		Collections.shuffle(shuffledOpts);
		List<MessageEmbed.Field> fields = new ArrayList<>();

		TreeMap<String, BiConsumer<Member, Message>> buttons = new TreeMap<>();

		AtomicInteger i = new AtomicInteger(0);
		q.getOptions().forEach(o -> {
			buttons.put(shuffledOpts.get(i.get()), (mb, ms) -> {
				if (!mb.getId().equals(author.getId())) return;
				eb.clear();
				eb.setThumbnail("https://images.vexels.com/media/users/3/152594/isolated/preview/d00d116b2c073ccf7f9fec677fec78e3---cone-de-ponto-de-interroga----o-quadrado-roxo-by-vexels.png");

				if (mb.getId().equals(author.getId())) {
					if (values.get(String.valueOf(o)) == 1) {
						int p = Helper.clamp(Helper.rng(q.getPrize()), q.getPrize() / 5, q.getPrize());
						acc.addCredit(p);
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
				}

				channel.sendMessage(eb.build()).queue();
			});

			fields.add(new MessageEmbed.Field("Alternativa " + shuffledOpts.get(i.get()), String.valueOf(o), false));
			i.getAndIncrement();
		});

		fields.sort(Comparator.comparing(MessageEmbed.Field::getName));
		fields.forEach(eb::addField);
		channel.sendMessage(eb.build()).queue(s -> Pages.buttonize(s, buttons, false, 60, TimeUnit.SECONDS));
	}
}
