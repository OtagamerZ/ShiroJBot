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
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.model.enums.Fonts;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Leaderboards;
import com.kuuhaku.utils.helpers.ImageHelper;
import com.kuuhaku.utils.helpers.MathHelper;
import com.kuuhaku.utils.helpers.MiscHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.TreeBidiMap;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Command(
		name = "acerteacor",
		aliases = {"colormatch", "namethecolor", "combinacor"},
		category = Category.FUN
)
@Requires({Permission.MESSAGE_ATTACH_FILES})
public class ColorNameCommand implements Executable {
	private static final BidiMap<String, Integer> colors = new TreeBidiMap<>(Map.of(
			"azul", 0x3C63FF,
			"verde", 0x3CFF3F,
			"vermelho", 0xFF3C3C,
			"amarelo", 0xFFD83C,
			"ciano", 0x3CFFE2,
			"laranja", 0xFF943C,
			"roxo", 0x7D3CFF,
			"rosa", 0xE53CFF,
			"marrom", 0x803C1F,
			"branco", 0xFFFFFF
	));

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (Main.getInfo().gameInProgress(author.getId())) {
			channel.sendMessage(I18n.getString("err_you-are-in-game")).queue();
			return;
		}

		List<Pair<String, Integer>> sequence = new ArrayList<>();

		List<Map.Entry<String, Integer>> pairs = List.copyOf(colors.entrySet());
		for (int a = 0; a < colors.size(); a++) {
			String name = pairs.get(a).getKey();

			for (int b = 0; b < colors.size(); b++) {
				Integer color = pairs.get(b).getValue();

				sequence.add(Pair.of(name, color));
			}
		}
		Collections.shuffle(sequence);
		sequence.subList(50, sequence.size()).clear();

		channel.sendMessage("Prepare-se, o jogo começará em 5 segundos!")
				.delay(5, TimeUnit.SECONDS)
				.flatMap(s -> s.editMessage("VALENDO!"))
				.queue(t -> {
					BufferedImage bi = new BufferedImage(500, 150, BufferedImage.TYPE_INT_ARGB);
					Font font = Fonts.DOREKING.deriveFont(Font.PLAIN, 100);

					Pair<String, Integer> next = sequence.get(0);
					Graphics2D g2d = bi.createGraphics();
					g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
					g2d.setColor(new Color(next.getRight()));
					g2d.setFont(font);
					Profile.printCenteredString(next.getLeft(), 500, 0, 125, g2d);
					g2d.dispose();

					t.editMessage("VALENDO: O nome da cor é...? (0/" + sequence.size() + ")")
							.clearFiles()
							.addFile(ImageHelper.getBytes(bi, "png"), "colors.png")
							.queue();
					Main.getEvents().addHandler(guild, new SimpleMessageListener(channel) {
						private final Consumer<Void> success = s -> close();
						private final AtomicBoolean win = new AtomicBoolean();
						private int hit = 0;
						private long lastMillis = 0;
						private ScheduledFuture<?> timeout = Main.getInfo().getSchedulerPool().schedule(() -> {
									if (!win.get()) {
										win.set(true);
										success.accept(null);

										int prize = (int) (hit * Math.pow(1.075, hit));
										channel.sendMessage(":alarm_clock: | Tempo esgotado, sua pontuação foi " + hit + "/" + sequence.size() + " e recebeu " + (int) (hit * Math.pow(1.03, hit)) + " CR!").complete();

										Account acc = Account.find(Account.class, author.getId());
										acc.addCredit(prize, this.getClass());
										acc.save();
									}
								}, 10_000, TimeUnit.MILLISECONDS
						);
						private Message msg = t;

						{
							Main.getInfo().setGameInProgress(mutex, author);
						}

						@Override
						public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
							if (!event.getAuthor().getId().equals(author.getId())) return;

							if (win.get()) return;
							else if (System.currentTimeMillis() - lastMillis < 1000) {
								channel.sendMessage("Calma, você está muito apressado!").queue();
								return;
							}

							Pair<String, Integer> next = sequence.get(hit);
							String value = event.getMessage().getContentRaw();

							boolean name = !msg.getContentRaw().contains("texto escrito");

							String correct = name ? colors.getKey(next.getRight()) : next.getLeft();

							lastMillis = System.currentTimeMillis();
							if (value.equalsIgnoreCase(correct)) {
								hit++;
								if (hit == sequence.size()) {
									win.set(true);
									success.accept(null);
									timeout.cancel(true);
									timeout = null;

									int prize = (int) (hit * Math.pow(1.075, hit));
									msg.delete().queue(null, MiscHelper::doNothing);
									channel.sendMessage(":confetti_ball: | Você acertou todas as cores! Seu prêmio é de " + StringHelper.separate(prize) + " CR.").queue();

									Account acc = Account.find(Account.class, author.getId());
									acc.addCredit(prize, this.getClass());
									acc.save();
									return;
								}

								next = sequence.get(hit);
								name = MathHelper.chance(50);

								Graphics2D g2d = bi.createGraphics();
								g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

								g2d.setComposite(AlphaComposite.Clear);
								g2d.fillRect(0, 0, 500, 150);
								g2d.setComposite(AlphaComposite.SrcOver);

								g2d.setColor(new Color(next.getRight()));
								g2d.setFont(font);
								Profile.printCenteredString(next.getLeft(), 500, 0, 125, g2d);
								g2d.dispose();

								timeout.cancel(true);
								timeout = Main.getInfo().getSchedulerPool().schedule(() -> {
											if (!win.get()) {
												win.set(true);
												success.accept(null);

												int prize = (int) (hit * Math.pow(1.075, hit));
												channel.sendMessage(":alarm_clock: | Tempo esgotado, sua pontuação foi " + hit + "/" + sequence.size() + " e recebeu " + StringHelper.separate(prize) + " CR!").complete();

												Account acc = Account.find(Account.class, author.getId());
												acc.addCredit(prize, this.getClass());
												acc.save();
											}
										}, 10_000 - (hit * 7_000L / 49), TimeUnit.MILLISECONDS
								);

								msg.delete().queue(null, MiscHelper::doNothing);
								try {
									msg = channel.sendMessage("**PRÓXIMO:** O " + (name ? "nome da cor" : "texto escrito") + " é...? (" + hit + "/" + sequence.size() + ")")
											.clearFiles()
											.addFile(ImageHelper.getBytes(bi, "png"), "colors.png")
											.submit().get();
								} catch (ExecutionException | InterruptedException ignore) {
								}
							} else {
								win.set(true);
								success.accept(null);
								timeout.cancel(true);
								timeout = null;

								int prize = (int) (hit * Math.pow(1.075, hit));
								channel.sendMessage("Você errou! Seu prêmio é de " + StringHelper.separate(prize) + " CR.").queue();

								Account acc = Account.find(Account.class, author.getId());
								acc.addCredit(prize, this.getClass());
								acc.save();

								new Leaderboards(author, getThis(), hit).save();
							}
						}
					});
				});
	}
}
