/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.command.commands.dev;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.mysql.VotesDAO;
import com.kuuhaku.model.persistent.DevRating;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class RatingCommand extends Command {

	public RatingCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public RatingCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public RatingCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public RatingCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(":x: | Preciso do ID do usuário para requisitar a avaliação").queue();
			return;
		}

		Main.getInfo().getUserByID(args[0]).openPrivateChannel().queue(c -> {
					c.sendFile(new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("feedback.png")).getPath()))
							.flatMap(s -> c.sendMessage(eval(author)))
							.queue(s -> {
								c.sendMessage(questions()[0]).queue(m -> addRates(author, m, (dev, i) -> dev.setInteraction(dev.getInteraction() == 0 ? i : (dev.getInteraction() + i) / 2f)));
								c.sendMessage(questions()[1]).queue(m -> addRates(author, m, (dev, i) -> dev.setInteraction(dev.getInteraction() == 0 ? i : (dev.getInteraction() + i) / 2f)));
								c.sendMessage(questions()[2]).queue(m -> addRates(author, m, (dev, i) -> dev.setInteraction(dev.getInteraction() == 0 ? i : (dev.getInteraction() + i) / 2f)));
							});
				},
				ex -> channel.sendMessage(":x: | Não foi possível enviar a avaliação ao usuário. Razão: " + ex).queue()
		);
	}

	private String eval(User author) {
		return "Olá, gostaria que você avaliasse o atendimento de " + author.getName() + " se possível, isso irá levar apenas alguns segundos!";
	}

	private MessageEmbed[] questions() {
		MessageEmbed[] embeds = {null, null, null};
		EmbedBuilder eb = new EmbedBuilder();

		eb.setTitle("Atendimento");
		eb.setDescription("Que nota você daria para o atendimento, considerando a interação com o usuário?");
		embeds[0] = eb.build();

		eb.setTitle("Solução");
		eb.setDescription("Que nota você daria para o atendimento, considerando o esclarecimento de sua dúvida ou solução de seu problema?");
		embeds[1] = eb.build();

		eb.setTitle("Conhecimento");
		eb.setDescription("Que nota você daria para o conhecimento geral do atendente sobre a Shiro/Jibril/Tet?");
		embeds[2] = eb.build();

		return embeds;
	}

	private void addRates(User author, Message msg, BiConsumer<DevRating, Integer> act) {
		DevRating dev = VotesDAO.getRating(author.getId());
		Map<String, BiConsumer<Member, Message>> buttons = new HashMap<String, BiConsumer<Member, Message>>() {{
			put("\u0031", (mb, ms) -> {
				act.accept(dev, 1);
				VotesDAO.evaluate(dev);
				ms.clearReactions().queue();
			});
			put("\u0032", (mb, ms) -> {
				act.accept(dev, 2);
				VotesDAO.evaluate(dev);
				ms.clearReactions().queue();
			});
			put("\u0033", (mb, ms) -> {
				act.accept(dev, 3);
				VotesDAO.evaluate(dev);
				ms.clearReactions().queue();
			});
			put("\u0034", (mb, ms) -> {
				act.accept(dev, 4);
				VotesDAO.evaluate(dev);
				ms.clearReactions().queue();
			});
			put("\u0035", (mb, ms) -> {
				act.accept(dev, 5);
				VotesDAO.evaluate(dev);
				ms.clearReactions().queue();
			});
		}};
		Pages.buttonize(Main.getInfo().getAPI(), msg, buttons, false, 5, TimeUnit.MINUTES);
	}
}
