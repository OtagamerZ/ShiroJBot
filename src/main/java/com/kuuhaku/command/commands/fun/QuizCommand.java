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

package com.kuuhaku.command.commands.fun;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.mysql.AccountDAO;
import com.kuuhaku.controller.mysql.QuizDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Quiz;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
		Quiz q = QuizDAO.getRandomQuiz();
		Account acc = AccountDAO.getAccount(author.getId());
		HashMap<String, String> opts = IntStream.range(0, q.getOptions().length()).boxed().collect(Collectors.toMap(k -> OPTS[k], v -> String.valueOf(q.getOptions().get(v)), (k, v) -> k, LinkedHashMap::new));

		HashMap<String, Integer> values = new HashMap<>();

		for (int i = 0; i < OPTS.length; i++) {
			values.put(q.getOptions().getString(i), i + 1);
		}

		EmbedBuilder eb = new EmbedBuilder();
		eb.setThumbnail("https://lh3.googleusercontent.com/proxy/ZvixvksWEH9fKXQXNtDTQYMRNxvRQDCrCDmMiC2g5tkotFwRPcSp9L8c4doZAcR31p5n5sXYmSSyNnQltoPOuRAUPh6fQtyf_PoeDLIUFJINbX0");
		eb.setTitle("Hora do quiz!");
		eb.setDescription(q.getQuestion());
		eb.setColor(Color.decode("#2195f2"));
		opts.forEach((k, v) -> eb.addField("Alternativa " + k, v, false));

		channel.sendMessage(eb.build()).queue(s -> {
			HashMap<String, BiConsumer<Member, Message>> buttons = new LinkedHashMap<>();
			List<String> btns = Arrays.asList(OPTS);
			Collections.shuffle(btns);

			AtomicInteger i = new AtomicInteger(0);
			HashMap<String, BiConsumer<Member, Message>> finalButtons = buttons;
			q.getOptions().forEach(o -> {
				finalButtons.put(btns.get(i.get()), (mb, ms) -> {
					eb.clear();
					eb.setThumbnail("https://lh3.googleusercontent.com/proxy/ZvixvksWEH9fKXQXNtDTQYMRNxvRQDCrCDmMiC2g5tkotFwRPcSp9L8c4doZAcR31p5n5sXYmSSyNnQltoPOuRAUPh6fQtyf_PoeDLIUFJINbX0");

					if (mb.getId().equals(author.getId())) {
						if (values.get(String.valueOf(o)) == q.getCorrect()) {
							int p = Helper.clamp(Helper.rng(q.getPrize()), q.getPrize() / 5, q.getPrize());
							acc.addCredit(p);
							AccountDAO.saveAccount(acc);

							eb.setTitle("Resposta correta!");
							eb.setDescription("Seu prêmio é de " + p + " créditos!");
							eb.setColor(Color.green);
						} else {
							eb.setTitle("Resposta incorreta.");
							eb.setDescription("Você errou. Tente novamente!");
							eb.setColor(Color.red);
						}
						s.clearReactions().queue();
					}

					s.editMessage(eb.build()).queue();
				});

				i.getAndIncrement();
			});


			buttons = buttons.entrySet().stream()
					.sorted(Map.Entry.comparingByKey())
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k, v) -> k, LinkedHashMap::new));
			Pages.buttonize(Main.getInfo().getAPI(), s, buttons, false, 60, TimeUnit.SECONDS);
		});
	}
}
