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

package com.kuuhaku.command.commands.discord.support;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.VotesDAO;
import com.kuuhaku.model.persistent.DevRating;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class RatingCommand extends Command {

	public RatingCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public RatingCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public RatingCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public RatingCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_rating-no-id")).queue();
			return;
		}

		channel.sendMessage("Avaliação requisitada com sucesso!").queue();
		Main.getInfo().getUserByID(args[0]).openPrivateChannel().queue(c -> {
					c.sendFile(new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("assets/feedback.png")).getPath()))
							.queue(s -> {
								c.sendMessage(eval(author)).queue(s1 -> {
									c.sendMessage(questions()[0])
											.queue(m -> {
												addRates(author, m, (dev, i) -> {
													dev.setInteraction(dev.getInteraction() == 0 ? i : (dev.getInteraction() + i) / 2f);
													dev.setLastHelped();
												});
												m.delete().queueAfter(5, TimeUnit.MINUTES, msg -> s.delete().queue(), Helper::doNothing);
											});
									c.sendMessage(questions()[1])
											.queue(m -> {
												addRates(author, m, (dev, i) -> {
													dev.setSolution(dev.getSolution() == 0 ? i : (dev.getSolution() + i) / 2f);
													dev.setLastHelped();
												});
												m.delete().queueAfter(5, TimeUnit.MINUTES, msg -> s.delete().queue(), Helper::doNothing);
											});
									c.sendMessage(questions()[2])
											.queue(m -> {
												addRates(author, m, (dev, i) -> {
													dev.setKnowledge(dev.getKnowledge() == 0 ? i : (dev.getKnowledge() + i) / 2f);
													dev.setLastHelped();
												});
												m.delete().queueAfter(5, TimeUnit.MINUTES, msg -> s.delete().queue(), Helper::doNothing);
											});
									s1.delete().queueAfter(5, TimeUnit.MINUTES, msg -> s.delete().queue(), Helper::doNothing);
								});

								s.delete().queueAfter(5, TimeUnit.MINUTES, msg -> s.delete().queue(), Helper::doNothing);
							});
				},
				ex -> channel.sendMessage(MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("err_cannot-request-rating"), ex)).queue()
		);
	}

	private String eval(User author) {
		return "Olá, gostaria que você avaliasse o atendimento de " + author.getName() + " se possível, isso irá levar apenas alguns segundos! (Essa enquete será removida após 5 minutos para não poluir este chat)";
	}

	private MessageEmbed[] questions() {
		MessageEmbed[] embeds = {null, null, null};
		EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(Helper.getRandomColor());

		eb.setTitle("Atendimento");
		eb.setDescription("Que nota você daria para o suporte, considerando a interação com o usuário?");
		embeds[0] = eb.build();

		eb.setTitle("Solução");
		eb.setDescription("Que nota você daria para o suporte, considerando o esclarecimento de sua dúvida ou solução de seu problema?");
		embeds[1] = eb.build();

		eb.setTitle("Conhecimento");
		eb.setDescription("Que nota você daria para o conhecimento geral do suporte sobre a Shiro/Jibril/Tet?");
		embeds[2] = eb.build();

		return embeds;
	}

	private void addRates(User author, Message msg, BiConsumer<DevRating, Integer> act) {
		Map<String, BiConsumer<Member, Message>> buttons = new LinkedHashMap<>() {{
			put("1️⃣", (mb, ms) -> {
				DevRating dev = VotesDAO.getRating(author.getId());
				act.accept(dev, 1);
				VotesDAO.evaluate(dev);
				ms.delete()
						.flatMap(s -> ms.getChannel().sendMessage("Obrigada por votar!"))
						.queue();
			});
			put("2️⃣", (mb, ms) -> {
				DevRating dev = VotesDAO.getRating(author.getId());
				act.accept(dev, 2);
				VotesDAO.evaluate(dev);
				ms.delete()
						.flatMap(s -> ms.getChannel().sendMessage("Obrigada por votar!"))
						.queue();
			});
			put("3️⃣", (mb, ms) -> {
				DevRating dev = VotesDAO.getRating(author.getId());
				act.accept(dev, 3);
				VotesDAO.evaluate(dev);
				ms.delete()
						.flatMap(s -> ms.getChannel().sendMessage("Obrigada por votar!"))
						.queue();
			});
			put("4️⃣", (mb, ms) -> {
				DevRating dev = VotesDAO.getRating(author.getId());
				act.accept(dev, 4);
				VotesDAO.evaluate(dev);
				ms.delete()
						.flatMap(s -> ms.getChannel().sendMessage("Obrigada por votar!"))
						.queue();
			});
			put("5️⃣", (mb, ms) -> {
				DevRating dev = VotesDAO.getRating(author.getId());
				act.accept(dev, 5);
				VotesDAO.evaluate(dev);
				ms.delete()
						.flatMap(s -> ms.getChannel().sendMessage("Obrigada por votar!"))
						.queue();
			});
		}};
		Pages.buttonize(msg, buttons, false);
	}
}
