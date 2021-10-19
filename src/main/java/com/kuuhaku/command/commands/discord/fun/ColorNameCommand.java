/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.LeaderboardsDAO;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Command(
		name = "acerteacor",
		aliases = {"colormatch", "namethecolor", "combinacor"},
		category = Category.FUN
)
public class ColorNameCommand implements Executable {
	private static final Map<String, Color> colors = Map.of(
			"azul", new Color(0x3C63FF),
			"vermelho", new Color(0x3CFF3F),
			"verde", new Color(0xFF3C3C),
			"amarelo", new Color(0xFFD83C),
			"ciano", new Color(0x3CFFE2),
			"laranja", new Color(0xFF943C),
			"roxo", new Color(0x7D3CFF),
			"rosa", new Color(0xE53CFF),
			"marrom", new Color(0x803C1F),
			"branco", new Color(0xFFFFFF)
	);

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().gameInProgress(author.getId())) {
			channel.sendMessage(I18n.getString("err_you-are-in-game")).queue();
			return;
		}

		List<Pair<String, Color>> sequence = new ArrayList<>();

		List<Map.Entry<String, Color>> pairs = List.copyOf(colors.entrySet());
		for (int a = 0; a < colors.size(); a++) {
			String name = pairs.get(a).getKey();

			for (int b = 0; b < colors.size(); b++) {
				Color color = pairs.get(b).getValue();

				sequence.add(Pair.of(name, color));
			}
		}
		Collections.shuffle(sequence);

		channel.sendMessage("Prepare-se, o jogo começará em 5 segundos (você deve digitar **O NOME** da cor (não a cor em si))!")
				.delay(5, TimeUnit.SECONDS)
				.flatMap(s -> s.editMessage("VALENDO!"))
				.queue(t -> {
					BufferedImage bi = new BufferedImage(500, 250, BufferedImage.TYPE_INT_ARGB);
					Font font = Fonts.DOREKING.deriveFont(Font.BOLD, 200);

					Pair<String, Color> next = sequence.get(0);
					Graphics2D g2d = bi.createGraphics();
					g2d.setColor(next.getRight());
					g2d.setFont(font);
					Profile.printCenteredString(next.getLeft(), 500, 0, 225, g2d);
					g2d.dispose();

					t.editMessage("VALENDO! (0/" + (int) Math.pow(colors.size(), 2) + ")")
							.clearFiles()
							.addFile(Helper.getBytes(bi, "png"), "colors.png")
							.queue();
					ShiroInfo.getShiroEvents().addHandler(guild, new SimpleMessageListener() {
						private final Consumer<Void> success = s -> close();
						private final AtomicBoolean win = new AtomicBoolean();
						private int hit = 0;
						private long lastMillis = 0;
						private ScheduledFuture<?> timeout = Main.getInfo().getScheduler().schedule(() -> {
									if (!win.get()) {
										win.set(true);
										success.accept(null);

										int prize = (int) (hit * Math.pow(1.03, hit));
										channel.sendMessage(":alarm_clock: | Tempo esgotado, sua pontuação foi " + hit + "/" + (int) Math.pow(colors.size(), 2) + " e recebeu " + (int) (hit * Math.pow(1.03, hit)) + " créditos!").complete();

										Account acc = AccountDAO.getAccount(author.getId());
										acc.addCredit(prize, this.getClass());
										AccountDAO.saveAccount(acc);
									}
								}, 30_000, TimeUnit.MILLISECONDS
						);

						@Override
						public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
							if (!event.getAuthor().getId().equals(author.getId()) || !event.getChannel().getId().equals(channel.getId()))
								return;

							if (System.currentTimeMillis() - lastMillis < 1000) {
								channel.sendMessage("Calma, você está muito apressado!").queue();
								return;
							}

							Pair<String, Color> next = sequence.get(hit);
							String value = event.getMessage().getContentRaw();

							lastMillis = System.currentTimeMillis();
							if (value.equalsIgnoreCase(next.getKey())) {
								hit++;
								if (hit == (int) Math.pow(colors.size(), 2)) {
									win.set(true);
									success.accept(null);
									timeout.cancel(true);
									timeout = null;

									int prize = (int) (hit * Math.pow(1.03, hit));
									t.editMessage(":confetti_ball: | Você acertou todas as cores! Seu prêmio é de " + prize + " créditos.")
											.clearFiles()
											.queue();

									Account acc = AccountDAO.getAccount(author.getId());
									acc.addCredit(prize, this.getClass());
									AccountDAO.saveAccount(acc);
									return;
								}

								next = sequence.get(hit);
								Graphics2D g2d = bi.createGraphics();
								g2d.clearRect(0, 0, 500, 250);
								g2d.setColor(next.getRight());
								g2d.setFont(font);
								Profile.printCenteredString(next.getLeft(), 500, 0, 225, g2d);
								g2d.dispose();

								t.editMessage("PRÓXIMO! (" + hit + "/" + (int) Math.pow(colors.size(), 2) + ")")
										.clearFiles()
										.addFile(Helper.getBytes(bi, "png"), "colors.png")
										.queue();
							} else {
								win.set(true);
								success.accept(null);
								timeout.cancel(true);
								timeout = null;

								int prize = (int) (hit * Math.pow(1.03, hit));
								t.editMessage(":confetti_ball: | Você errou! Seu prêmio é de " + prize + " créditos.")
										.clearFiles()
										.queue();

								Account acc = AccountDAO.getAccount(author.getId());
								acc.addCredit(prize, this.getClass());
								AccountDAO.saveAccount(acc);

								LeaderboardsDAO.submit(author, ColorNameCommand.class, hit);
							}
						}
					});
				});
	}
}
