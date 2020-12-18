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

package com.kuuhaku.events.cron;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.handlers.games.tabletop.framework.GameChannel;
import com.kuuhaku.handlers.games.tabletop.framework.GlobalGame;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Shoukan;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import javax.annotation.Nonnull;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TenthSecondEvent implements Job {
	public static JobDetail tenthSecond;
	private boolean lock = false;

	@Override
	public void execute(JobExecutionContext context) {
		if (lock) return;
		lock = true;
		List<Map.Entry<MatchMakingRating, Pair<Integer, TextChannel>>> lobby = new ArrayList<>(Main.getInfo().getMatchMaking().getLobby().entrySet());
		if (lobby.size() == 1) return;
		for (int a = 0; a < lobby.size(); a++) {
			for (int b = a; a < lobby.size(); b++) {
				try {
					Map.Entry<MatchMakingRating, Pair<Integer, TextChannel>> p1 = lobby.get(a);
					Map.Entry<MatchMakingRating, Pair<Integer, TextChannel>> p2 = lobby.get(b);

					if (!p1.equals(p2) && Math.abs(p1.getKey().getMMR() * 100d / (p2.getKey().getMMR() == 0 ? 1 : p2.getKey().getMMR())) <= p1.getValue().getLeft() * 5 && Math.abs(p1.getKey().getTier().getTier() - p2.getKey().getTier().getTier()) < 2) {
						Main.getInfo().getMatchMaking().getLobby().remove(p1.getKey());
						Main.getInfo().getMatchMaking().getLobby().remove(p2.getKey());

						TextChannel p1Channel = p1.getValue().getRight();
						TextChannel p2Channel = p2.getValue().getRight();
						List<Pair<Map.Entry<MatchMakingRating, Pair<Integer, TextChannel>>, Boolean>> match = new ArrayList<>();

						Runnable result = () -> {
							System.gc();

							if (match.stream().allMatch(Pair::getRight)) {
								GlobalGame g = new Shoukan(
										Main.getShiroShards(),
										new GameChannel(p1Channel, p2Channel),
										0,
										null,
										false,
										true,
										p2.getKey().getUser(),
										p1.getKey().getUser()
								);
								g.start();
							} else {
								for (Pair<Map.Entry<MatchMakingRating, Pair<Integer, TextChannel>>, Boolean> p : match) {
									if (p.getRight()) {
										p.getLeft().getValue().getRight().sendMessage("O oponente não confirmou a partida a tempo, você foi retornado ao saguão.").queue(s ->
												Pages.buttonize(s, Map.of(
														Helper.CANCEL, (mb, ms) -> {
															Main.getInfo().getMatchMaking().getLobby().remove(p.getLeft().getKey());
															ms.delete().queue();
														}), false, 30, TimeUnit.MINUTES
														, u -> u.getId().equals(p.getLeft().getKey().getUserId())
														, ms -> {
															Main.getInfo().getMatchMaking().getLobby().remove(p.getLeft().getKey());
															ms.delete().queue();
														}
												)
										);
										Pair<Integer, TextChannel> newPair = Pair.of(
												p.getLeft().getValue().getLeft() + 1,
												p.getLeft().getValue().getRight()
										);
										Main.getInfo().getMatchMaking().getLobby().put(p.getLeft().getKey(), newPair);
									}
								}

							}
						};

						Main.getInfo().getShiroEvents().addHandler(p1Channel.getGuild(), new SimpleMessageListener() {
							private Future<?> timeout = p1Channel.sendMessage("Tempo para aceitar a partida encerrado, você está impedido de entrar no saguão novamente por 10 minutos.")
									.queueAfter(5, TimeUnit.MINUTES, msg -> {
										p1.getKey().block(10, ChronoUnit.MINUTES);
										MatchMakingRatingDAO.saveMMR(p1.getKey());
										match.add(Pair.of(p1, false));
										close();
										if (match.size() == 2) result.run();
									});

							{
								p1Channel.sendMessage("""
										%s
										Oponente encontrado (%s), digite `aschente` para confirmar a partida.
										Demorar para responder resultará em um bloqueio de saguão de 10 minutos.
										""".formatted(
										p1.getKey().getUser().getAsMention(),
										p2.getKey().getUser().getName()
								)).queue();
							}

							@Override
							public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
								Message msg = event.getMessage();
								if (!msg.getAuthor().getId().equals(p1.getKey().getUserId()) || !msg.getContentRaw().equalsIgnoreCase("aschente"))
									return;

								msg.addReaction(Helper.ACCEPT)
										.flatMap(s -> p1Channel.sendMessage("Você aceitou a partida."))
										.flatMap(s -> p2Channel.sendMessage("%s aceitou a partida.".formatted(p1.getKey().getUser().getName())))
										.queue();

								match.add(Pair.of(p1, true));
								timeout.cancel(true);
								timeout = null;
								close();
								if (match.size() == 2) result.run();
							}
						});

						Main.getInfo().getShiroEvents().addHandler(p2Channel.getGuild(), new SimpleMessageListener() {
							private Future<?> timeout = p2Channel.sendMessage("Tempo para aceitar a partida encerrado, você está impedido de entrar no saguão novamente por 10 minutos.")
									.queueAfter(5, TimeUnit.MINUTES, msg -> {
										p2.getKey().block(10, ChronoUnit.MINUTES);
										MatchMakingRatingDAO.saveMMR(p2.getKey());
										match.add(Pair.of(p2, false));
										close();
										if (match.size() == 2) result.run();
									});

							{
								p2Channel.sendMessage("""
										%s
										Oponente encontrado (%s), digite `aschente` para confirmar a partida.
										Demorar para responder resultará em um bloqueio de saguão de 10 minutos.
										""".formatted(
										p2.getKey().getUser().getAsMention(),
										p1.getKey().getUser().getName()
								)).queue();
							}

							@Override
							public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
								Message msg = event.getMessage();
								if (!msg.getAuthor().getId().equals(p2.getKey().getUserId()) || !msg.getContentRaw().equalsIgnoreCase("aschente"))
									return;

								msg.addReaction(Helper.ACCEPT)
										.flatMap(s -> p2Channel.sendMessage("Você aceitou a partida."))
										.flatMap(s -> p1Channel.sendMessage("%s aceitou a partida.".formatted(p2.getKey().getUser().getName())))
										.queue();

								match.add(Pair.of(p2, true));
								timeout.cancel(true);
								timeout = null;
								close();
								if (match.size() == 2) result.run();
							}
						});

						return;
					}

					Main.getInfo().getMatchMaking().getLobby().computeIfPresent(p1.getKey(), (mmr, p) -> Pair.of(p.getLeft() + 1, p.getRight()));
				} catch (IndexOutOfBoundsException e) {
					return;
				}
			}
		}

		lock = false;
	}
}
