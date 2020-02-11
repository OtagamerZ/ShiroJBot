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

package com.kuuhaku.command.commands.partner;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.mysql.AccountDAO;
import com.kuuhaku.controller.sqlite.KGotchiDAO;
import com.kuuhaku.handlers.games.kawaigotchi.Food;
import com.kuuhaku.handlers.games.kawaigotchi.FoodMenu;
import com.kuuhaku.handlers.games.kawaigotchi.Kawaigotchi;
import com.kuuhaku.handlers.games.kawaigotchi.enums.FoodType;
import com.kuuhaku.handlers.games.kawaigotchi.exceptions.EmptyStockException;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class KGotchiCommand extends Command {

	public KGotchiCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public KGotchiCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public KGotchiCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public KGotchiCommand(String name, String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Account acc = AccountDAO.getAccount(author.getId());
		Kawaigotchi k = KGotchiDAO.getKawaigotchi(author.getId());

		if (k == null) {
			channel.sendMessage(":x: | Você não possui um Kawaigotchi.").queue();
			return;
		} else if (args.length == 0) {
			try {
				k.view((TextChannel) channel).queue();
			} catch (IOException e) {
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
			return;
		}

		if (Helper.containsAny(args[0], "alimentar", "feed", "darcomida", "givefood")) {
			if (args.length < 2) {
				JSONObject jo = new JSONObject(k.getBag());
				EmbedBuilder eb = new EmbedBuilder();

				Map<FoodType, List<MessageEmbed.Field>> fields = new HashMap<>();

				jo.toMap().forEach((f, v) -> {
					Food food = FoodMenu.getFood(f);
					List<MessageEmbed.Field> field = fields.getOrDefault(food.getType(), new ArrayList<>());
					field.add(new MessageEmbed.Field(food.getName() + " - " + v + " unidades\n(`" + prefix + "kgotchi alimentar " + f + "`)", "Bônus de humor: " + food.getMoodBoost() + "\nNutrição: " + food.getNutrition() + "\nSaúde: " + food.getHealthiness(), true));
					fields.put(food.getType(), field);
				});

				Map<String, Page> pages = new HashMap<>();

				fields.forEach((t, f) -> {
					eb.clear();
					eb.setTitle("Estoque de " + t.toString().toLowerCase());
					f.forEach(eb::addField);
					eb.setThumbnail(t.getIcon());
					eb.setFooter("Seus créditos: " + acc.getBalance(), "https://i.imgur.com/U0nPjLx.gif");
					eb.setColor(Helper.getRandomColor());

					pages.put(t.getButton(), new Page(PageType.EMBED, eb.build()));
				});

				channel.sendMessage((MessageEmbed) pages.get(FoodType.MEAT.getButton()).getContent()).queue(m -> Pages.categorize(Main.getInfo().getAPI(), m, pages, 60, TimeUnit.SECONDS));
			} else {
				Food f = FoodMenu.getFood(args[1].toLowerCase());
				JSONObject bag = new JSONObject(k.getBag());

				if (f == null || !bag.has(args[1])) {
					channel.sendMessage(":x: | Comida inválida, você não quis dizer **" + Helper.didYouMean(args[1], bag.keySet().toArray(new String[0])) + "**?").queue();
					return;
				}

				try {
					k.feed(f);

					String res = "";

					switch (f.getType()) {
						case RATION:
							res = "não gostou muito, mas da pro gasto!";
							break;
						case MEAT:
							res = "gostou bastante, e é bem nutritivo!";
							break;
						case SWEET:
							res = "amou, apesar de não ser muito saudável!";
							break;
						case PLANT:
							res = "gostou, e é bem saudável!";
							break;
					}

					channel.sendMessage("Você deu " + f.getName().toLowerCase() + " para " + k.getName() + ", parece que " + res).queue();
				} catch (EmptyStockException e) {
					channel.sendMessage(":x: | Seu estoque de " + f.getName().toLowerCase() + " está vazio!").queue();
				}
			}
		} else if (Helper.containsAny(args[0], "brincar", "play")) {
			switch (k.play()) {
				case FAILED:
					channel.sendMessage("Ah, parece que " + k.getName() + " não quer brincar. Tente novamente!").queue();
					return;
				case SUCCESS:
					channel.sendMessage("Vocês brincaram por bastante tempo, " + k.getName() + " está mais feliz agora!").queue();
					int rng = Helper.rng(100);
					if (rng > 90) {
						acc.addCredit(50 * (100 - rng));
						AccountDAO.saveAccount(acc);
						channel.sendMessage("Opa, o que é isso? Parece que " + k.getName() + " encontrou " + (50 * (100 - rng)) + " créditos!").queue();
					}
					return;
				case UNABLE:
					channel.sendMessage("Não parece que " + k.getName() + " possa brincar agora!").queue();
			}
		} else if (Helper.containsAny(args[0], "treinar", "train")) {
			switch (k.train()) {
				case FAILED:
					channel.sendMessage(k.getName() + " fugiu do treino, parece que está com preguiça. Tente novamente!").queue();
					return;
				case SUCCESS:
					channel.sendMessage(k.getName() + " treinou muito, agora ficou monstrão!").queue();
					return;
				case UNABLE:
					channel.sendMessage("Não parece que " + k.getName() + " possa treinar agora!").queue();
			}
		} else if (Helper.containsAny(args[0], "comprar", "buy")) {
			if (args.length < 2) {
				EmbedBuilder eb = new EmbedBuilder();

				Map<FoodType, List<MessageEmbed.Field>> fields = new HashMap<>();

				FoodMenu.getMenu().forEach((n, food) -> {
					List<MessageEmbed.Field> field = fields.getOrDefault(food.getType(), new ArrayList<>());
					field.add(new MessageEmbed.Field(food.getName() + " - " + food.getPrice() + " créditos\n(`" + prefix + "kgotchi comprar " + n + "`)", "Bônus de humor: " + food.getMoodBoost() + "\nNutrição: " + food.getNutrition() + "\nSaúde: " + food.getHealthiness(), true));
					fields.put(food.getType(), field);
				});

				Map<String, Page> pages = new HashMap<>();

				fields.forEach((t, f) -> {
					eb.clear();
					eb.setTitle("Setor de " + t.toStrings().toLowerCase());
					f.forEach(eb::addField);
					eb.setThumbnail(t.getIcon());
					eb.setFooter("Seus créditos: " + acc.getBalance(), "https://i.imgur.com/U0nPjLx.gif");
					eb.setColor(Helper.getRandomColor());

					pages.put(t.getButton(), new Page(PageType.EMBED, eb.build()));
				});

				channel.sendMessage((MessageEmbed) pages.get(FoodType.RATION.getButton()).getContent()).queue(m -> Pages.categorize(Main.getInfo().getAPI(), m, pages, 60, TimeUnit.SECONDS));
			} else {
				Food f = FoodMenu.getFood(args[1].toLowerCase());
				JSONObject bag = new JSONObject(k.getBag());

				if (f == null) {
					channel.sendMessage(":x: | Comida inválida, você não quis dizer **" + Helper.didYouMean(args[1], FoodMenu.getMenu().keySet().toArray(new String[0])) + "**?").queue();
					return;
				}

				k.addToBag(f);
				acc.removeCredit(f.getPrice());

				channel.sendMessage("Você comprou 1 unidade de " + f.getName().toLowerCase() + " por " + f.getPrice() + " créditos.").queue();

				KGotchiDAO.saveKawaigotchi(k);
				AccountDAO.saveAccount(acc);

				System.out.println(k.getBag());
			}
		}
	}
}
