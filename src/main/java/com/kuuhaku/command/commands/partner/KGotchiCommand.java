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
import com.kuuhaku.handlers.games.kawaigotchi.enums.Stance;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.*;
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
					eb.setTitle("Estoque de " + t.toStrings().toLowerCase());
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

				BufferedImage bi = k.getRace().extract(k.getStance(), k.getSkin());
				EmbedBuilder eb = new EmbedBuilder();
				Graphics2D g2d = bi.createGraphics();
				switch (k.feed(f)) {
					case FAILED:
						g2d.drawImage(new ImageIcon(Objects.requireNonNull(this.getClass().getClassLoader().getResource("troubled.png"))).getImage(), bi.getWidth() - 64, 0, 64, 64, null);
						eb.setTitle("Estoque vazio!");
						eb.setDescription("Seu estoque de " + f.getName().toLowerCase() + " está vazio!");
						eb.setColor(Color.yellow);
						break;
					case SUCCESS:
						bi = k.getRace().extract(Stance.HAPPY, k.getSkin());
						g2d = bi.createGraphics();
						g2d.drawImage(new ImageIcon(Objects.requireNonNull(this.getClass().getClassLoader().getResource("foodUp.png"))).getImage(), bi.getWidth() - 64, 0, 64, 64, null);
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

						eb.setTitle("Sucesso!");
						eb.setDescription("Você deu " + f.getName().toLowerCase() + " para " + k.getName() + ", parece que " + res);
						eb.setColor(Color.green);
						break;
					case UNABLE:
						if (k.getStance().isResting())
							g2d.drawImage(new ImageIcon(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sleeping.png"))).getImage(), bi.getWidth() - 128, 0, 128, 128, null);
						else
							g2d.drawImage(new ImageIcon(Objects.requireNonNull(this.getClass().getClassLoader().getResource("troubled.png"))).getImage(), bi.getWidth() - 64, 0, 64, 64, null);

						eb.setTitle("Impossibilitado.");
						eb.setDescription("Não parece que " + k.getName() + " possa comer agora!");
						eb.setColor(Color.red);
						break;
				}

				g2d.dispose();
				eb.setThumbnail("attachment://img.png");
				try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
					ImageIO.write(bi, "png", baos);
					baos.flush();

					channel.sendMessage(eb.build()).addFile(baos.toByteArray(), "img.png").queue();
				} catch (IOException e) {
					Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
				}
			}
		} else if (Helper.containsAny(args[0], "brincar", "play")) {
			BufferedImage bi = k.getRace().extract(k.getStance(), k.getSkin());
			EmbedBuilder eb = new EmbedBuilder();
			Graphics2D g2d = bi.createGraphics();
			switch (k.play()) {
				case FAILED:
					g2d.drawImage(new ImageIcon(Objects.requireNonNull(this.getClass().getClassLoader().getResource("troubled.png"))).getImage(), bi.getWidth() - 64, 0, 64, 64, null);
					eb.setTitle("Tente novamente!");
					eb.setDescription("Ah, parece que " + k.getName() + " não quer brincar.");
					eb.setColor(Color.yellow);
					break;
				case SUCCESS:
					bi = k.getRace().extract(Stance.HAPPY, k.getSkin());
					g2d = bi.createGraphics();
					g2d.drawImage(new ImageIcon(Objects.requireNonNull(this.getClass().getClassLoader().getResource("moodUp.png"))).getImage(), bi.getWidth() - 64, 0, 64, 64, null);

					eb.setTitle("Sucesso!");
					eb.setDescription("Vocês brincaram por bastante tempo, " + k.getName() + " está mais feliz agora!");
					eb.setColor(Color.green);
					int rng = Helper.rng(100);
					if (rng > 90) {
						acc.addCredit(50 * (100 - rng));
						AccountDAO.saveAccount(acc);
						channel.sendMessage("Opa, o que é isso? Parece que " + k.getName() + " encontrou " + (50 * (100 - rng)) + " créditos!").queue();
					}
					break;
				case UNABLE:
					if (k.getStance().isResting())
						g2d.drawImage(new ImageIcon(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sleeping.png"))).getImage(), bi.getWidth() - 128, 0, 128, 128, null);
					else
						g2d.drawImage(new ImageIcon(Objects.requireNonNull(this.getClass().getClassLoader().getResource("troubled.png"))).getImage(), bi.getWidth() - 64, 0, 64, 64, null);

					eb.setTitle("Impossibilitado.");
					eb.setDescription("Não parece que " + k.getName() + " possa brincar agora!");
					eb.setColor(Color.red);
					break;
			}

			g2d.dispose();
			sendEmbed(channel, k, bi, eb);
		} else if (Helper.containsAny(args[0], "treinar", "train")) {
			BufferedImage bi = k.getRace().extract(k.getStance(), k.getSkin());
			EmbedBuilder eb = new EmbedBuilder();
			Graphics2D g2d = bi.createGraphics();
			switch (k.train()) {
				case FAILED:
					g2d.drawImage(new ImageIcon(Objects.requireNonNull(this.getClass().getClassLoader().getResource("troubled.png"))).getImage(), bi.getWidth() - 64, 0, 64, 64, null);
					eb.setTitle("Tente novamente!");
					eb.setDescription(k.getName() + " fugiu do treino, parece que está com preguiça.");
					eb.setColor(Color.yellow);
					break;
				case SUCCESS:
					bi = k.getRace().extract(Stance.HAPPY, k.getSkin());
					g2d = bi.createGraphics();
					g2d.drawImage(new ImageIcon(Objects.requireNonNull(this.getClass().getClassLoader().getResource("xpUp.png"))).getImage(), bi.getWidth() - 64, 0, 64, 64, null);

					eb.setTitle("Sucesso!");
					eb.setDescription(k.getName() + " treinou muito, tá ficando monstrão!");
					eb.setColor(Color.green);
					eb.setFooter("+");
					break;
				case UNABLE:
					if (k.getStance().isResting())
						g2d.drawImage(new ImageIcon(Objects.requireNonNull(this.getClass().getClassLoader().getResource("sleeping.png"))).getImage(), bi.getWidth() - 128, 0, 128, 128, null);
					else
						g2d.drawImage(new ImageIcon(Objects.requireNonNull(this.getClass().getClassLoader().getResource("troubled.png"))).getImage(), bi.getWidth() - 64, 0, 64, 64, null);

					eb.setTitle("Impossibilitado.");
					eb.setDescription("Não parece que " + k.getName() + " possa treinar agora!");
					eb.setColor(Color.red);
					break;
			}

			g2d.dispose();
			sendEmbed(channel, k, bi, eb);
		} else if (Helper.containsAny(args[0], "comprar", "buy")) {
			if (args.length < 2) {
				EmbedBuilder eb = new EmbedBuilder();

				Map<FoodType, List<MessageEmbed.Field>> fields = new HashMap<>();

				FoodMenu.getMenu().forEach((n, food) -> {
					List<MessageEmbed.Field> field = fields.getOrDefault(food.getType(), new ArrayList<>());
					field.add(new MessageEmbed.Field(food.getName() + " - " + food.getPrice() + " créditos\n(`" + prefix + "kgotchi comprar " + n + "`)", food.getType() == FoodType.SPECIAL ? food.getSpecialDesc() : "Bônus de humor: " + food.getMoodBoost() + "\nNutrição: " + food.getNutrition() + "\nSaúde: " + food.getHealthiness(), true));
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

				channel.sendMessage((MessageEmbed) pages.get(FoodType.MEAT.getButton()).getContent()).queue(m -> Pages.categorize(Main.getInfo().getAPI(), m, pages, 60, TimeUnit.SECONDS));
			} else {
				Food f = FoodMenu.getFood(args[1].toLowerCase());

				if (f == null) {
					channel.sendMessage(":x: | Comida inválida, você não quis dizer **" + Helper.didYouMean(args[1], FoodMenu.getMenu().keySet().toArray(new String[0])) + "**?").queue();
					return;
				}

				if (args.length < 3) {
					k.addToBag(f);
					acc.removeCredit(f.getPrice());

					channel.sendMessage("Você comprou 1 unidade de " + f.getName().toLowerCase() + " por " + f.getPrice() + " créditos.").queue();

				} else {
					if (!StringUtils.isNumeric(args[2])) {
						channel.sendMessage(":x: | A quantidade deve ser numérica.").queue();
						return;
					} else if (Integer.parseInt(args[2]) <= 0) {
						channel.sendMessage(":x: | A quantidade deve ser maior que zero.").queue();
						return;
					}

					k.addToBag(f, Integer.parseInt(args[2]));
					acc.removeCredit(f.getPrice() * Integer.parseInt(args[2]));

					channel.sendMessage("Você comprou " + args[2] + " unidades de " + f.getName().toLowerCase() + " por " + (f.getPrice() * Integer.parseInt(args[2])) + " créditos.").queue();

				}
				KGotchiDAO.saveKawaigotchi(k);
				AccountDAO.saveAccount(acc);
			}
		}
	}

	public void sendEmbed(MessageChannel channel, Kawaigotchi k, BufferedImage bi, EmbedBuilder eb) {
		int xp = k.getLastXpRoll();
		int mood = k.getLastMoodRoll();
		int resource = k.getLastResourceRoll(true);

		eb.setFooter(k.getTier().toString() + " -> " + k.getTier().next() + ": " + Math.round(k.getXp()) + "/" + k.getTier().next().getRequiredXp() + " xp");
		eb.setThumbnail("attachment://img.png");
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(bi, "png", baos);
			baos.flush();

			channel.sendMessage(eb.build()).addFile(baos.toByteArray(), "img.png").queue();
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}
}
