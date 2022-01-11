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

package com.kuuhaku.events.cron;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.handlers.games.tabletop.framework.GameChannel;
import com.kuuhaku.handlers.games.tabletop.framework.GlobalGame;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Shoukan;
import com.kuuhaku.model.enums.RankedTier;
import com.kuuhaku.model.persistent.Deck;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.model.records.DuoLobby;
import com.kuuhaku.model.records.RankedDuo;
import com.kuuhaku.model.records.SoloLobby;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import javax.annotation.Nonnull;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TenthSecondEvent implements Job {
	public static JobDetail tenthSecond;
	private boolean lock = false;

	@Override
	public void execute(JobExecutionContext context) {
		if (lock) return;
		lock = true;

		if (Main.getInfo().getMatchMaking().getSoloLobby().size() >= 5) {
			for (SoloLobby lobby : Main.getInfo().getMatchMaking().getSoloLobby()) {
				lobby.unlocked().set(true);
			}
		}

		List<SoloLobby> soloLobby = Main.getInfo().getMatchMaking().getSoloLobby().stream()
				.filter(sl -> sl.unlocked().get())
				.collect(Collectors.toList());
		if (soloLobby.size() >= 2) {
			Collections.shuffle(soloLobby);

			for (int x = 0; x < soloLobby.size(); x++) {
				SoloLobby first = soloLobby.get(x);
				if (!first.unlocked().get()) continue;

				for (int y = x + 1; y < soloLobby.size(); y++) {
					SoloLobby second = soloLobby.get(y);
					if (!second.unlocked().get()) continue;

					if (trySoloMatching(first, second)) {
						first.unlocked().set(false);
						second.unlocked().set(false);
					} else {
						first.threshold().getAndAdd(2);
					}
				}
			}
		}

		List<DuoLobby> duoLobby = Main.getInfo().getMatchMaking().getDuoLobby().stream()
				.filter(dl -> dl.unlocked().get())
				.collect(Collectors.toList());
		if (duoLobby.size() >= 2) {
			Collections.shuffle(duoLobby);

			for (int x = 0; x < duoLobby.size(); x++) {
				DuoLobby first = duoLobby.get(x);
				if (!first.unlocked().get()) continue;

				for (int y = x + 1; y < duoLobby.size(); y++) {
					DuoLobby second = duoLobby.get(y);
					if (!second.unlocked().get()) continue;

					if (tryDuoMatching(first, second)) {
						first.unlocked().set(false);
						second.unlocked().set(false);
					} else {
						first.threshold().getAndAdd(2);
					}
				}
			}
		}

		lock = false;
	}

	private boolean trySoloMatching(SoloLobby p1, SoloLobby p2) {
		try {
			MatchMakingRating mmr1 = p1.mmr();
			MatchMakingRating mmr2 = p2.mmr();

			if (!mmr1.equals(mmr2)
				&& Helper.prcntToInt(mmr1.getMMR(), mmr2.getMMR() == 0 ? 1 : mmr2.getMMR()) * 100 <= p1.threshold().get() * 10
				&& (Math.abs(mmr1.getTier().getTier() - mmr2.getTier().getTier()) < 2 || mmr2.getTier() == RankedTier.UNRANKED)
				&& (!p1.channel().getGuild().getId().equals(p2.channel().getGuild().getId()) || p1.threshold().get() > 50)
			) {
				Main.getInfo().getMatchMaking().getSoloLobby().remove(p1);
				Main.getInfo().getMatchMaking().getSoloLobby().remove(p2);

				List<Pair<SoloLobby, Boolean>> match = new ArrayList<>();

				Runnable result = () -> {
					Main.getInfo().getMatchMaking().getSoloLobby().remove(p1);
					Main.getInfo().getMatchMaking().getSoloLobby().remove(p2);

					mmr1.setJoins(0);
					mmr2.setJoins(0);

					boolean p1Starts = Helper.chance(50);
					if (match.stream().allMatch(Pair::getRight)) {
						mmr1.setEvades(0);
						mmr2.setEvades(0);

						GlobalGame g = new Shoukan(
								Main.getShiroShards(),
								new GameChannel(p1.channel(), p2.channel()),
								0,
								null,
								false,
								true,
								true,
								null,
								p1Starts ? mmr1.getUser() : mmr2.getUser(),
								p1Starts ? mmr2.getUser() : mmr1.getUser()
						);
						g.start();
						Main.getInfo().getMatchMaking().getGames().add(g);
					} else {
						mmr1.setEvades(0);

						for (Pair<SoloLobby, Boolean> p : match) {
							if (p.getRight()) {
								p.getLeft().channel().sendMessage("O oponente não confirmou a partida a tempo, você foi retornado ao saguão.").queue();
								Main.getInfo().getMatchMaking().getSoloLobby().add(p.getLeft());
							}
						}
					}

					MatchMakingRatingDAO.saveMMR(mmr1);
					MatchMakingRatingDAO.saveMMR(mmr2);
				};

				sendSoloConfirmation(p1, p1.channel(), p2.channel(), match, result);
				sendSoloConfirmation(p2, p2.channel(), p1.channel(), match, result);

				return true;
			}
		} catch (IndexOutOfBoundsException e) {
			return true;
		}
		return false;
	}

	private boolean tryDuoMatching(DuoLobby p1, DuoLobby p2) {
		try {
			RankedDuo t1 = p1.duo();
			RankedDuo t2 = p2.duo();

			if (!t1.equals(t2)
				&& Helper.prcnt(t1.getAvgMMR(), t2.getAvgMMR() == 0 ? 1 : t2.getAvgMMR()) * 100 <= p1.threshold().get() * 10
				&& (Math.abs(t1.getAvgTier() - t2.getAvgTier()) < 2 || t2.getAvgTier() == 0)
				&& (!p1.channel().getGuild().getId().equals(p2.channel().getGuild().getId()) || p1.threshold().get() > 50)
			) {
				Main.getInfo().getMatchMaking().getDuoLobby().remove(p1);
				Main.getInfo().getMatchMaking().getDuoLobby().remove(p2);

				List<Pair<DuoLobby, Boolean>> match = new ArrayList<>();

				Runnable result = () -> {
					Main.getInfo().getMatchMaking().getDuoLobby().remove(p1);
					Main.getInfo().getMatchMaking().getDuoLobby().remove(p2);

					t1.p1().setJoins(0);
					t1.p2().setJoins(0);
					t2.p1().setJoins(0);
					t2.p2().setJoins(0);

					boolean p1Starts = Helper.chance(50);
					boolean leaderStarts = Helper.chance(50);
					if (match.stream().allMatch(Pair::getRight)) {
						t1.p1().setEvades(0);
						t1.p2().setEvades(0);
						t2.p1().setEvades(0);
						t2.p2().setEvades(0);

						GlobalGame g = new Shoukan(
								Main.getShiroShards(),
								new GameChannel(p1.channel(), p2.channel()),
								0,
								null,
								false,
								true,
								true,
								null,
								p1Starts ?
										leaderStarts ?
												t1.p1().getUser() : t1.p2().getUser()
										:
										leaderStarts ?
												t2.p1().getUser() : t2.p2().getUser(),
								p1Starts ?
										leaderStarts ?
												t2.p1().getUser() : t2.p2().getUser()
										:
										leaderStarts ?
												t1.p1().getUser() : t1.p2().getUser()
						);
						g.start();
						Main.getInfo().getMatchMaking().getGames().add(g);
					} else {
						t1.p1().setEvades(0);
						t1.p2().setEvades(0);

						for (Pair<DuoLobby, Boolean> p : match) {
							if (p.getRight()) {
								p.getLeft().channel().sendMessage("O oponente não confirmou a partida a tempo, você foi retornado ao saguão.").queue();
								Main.getInfo().getMatchMaking().getDuoLobby().add(p.getLeft());
							}
						}
					}

					MatchMakingRatingDAO.saveMMR(t1.p1());
					MatchMakingRatingDAO.saveMMR(t1.p2());
					MatchMakingRatingDAO.saveMMR(t2.p1());
					MatchMakingRatingDAO.saveMMR(t2.p2());
				};

				sendDuoConfirmation(p1, p1.channel(), p2.channel(), match, result);
				sendDuoConfirmation(p2, p2.channel(), p1.channel(), match, result);

				return true;
			}
		} catch (IndexOutOfBoundsException e) {
			return true;
		}
		return false;
	}

	private void sendSoloConfirmation(SoloLobby p1, TextChannel p1Channel, TextChannel p2Channel, List<Pair<SoloLobby, Boolean>> match, Runnable result) {
		MatchMakingRating mmr = p1.mmr();

		ShiroInfo.getShiroEvents().addHandler(p1Channel.getGuild(), new SimpleMessageListener() {
			private Future<?> timeout = p1Channel.sendMessage("Tempo para aceitar a partida esgotado, você está impedido de entrar no saguão novamente por " + (10 * (mmr.getEvades() + 1)) + " minutos.")
					.queueAfter(1, TimeUnit.MINUTES, msg -> {
						mmr.setEvades(mmr.getEvades() + 1);
						mmr.block(10 * mmr.getEvades(), ChronoUnit.MINUTES);
						MatchMakingRatingDAO.saveMMR(mmr);
						match.add(Pair.of(p1, false));
						close();
						if (match.size() == 2) result.run();
					});

			{
				p1Channel.sendMessage("""
						%s
						Partida encontrada, digite `aschente` para confirmar a partida.
						Demorar para responder resultará em um bloqueio de saguão de 10 minutos.
						""".formatted(mmr.getUser().getAsMention())).queue();
				mmr.getUser().openPrivateChannel()
						.flatMap(c -> c.sendMessage("**Partida encontrada**: vá para o canal " + p1Channel.getAsMention() + " para confirmar."))
						.queue(null, Helper::doNothing);
			}

			@Override
			public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
				if (!match.isEmpty() && !match.get(0).getRight()) {
					close();
					return;
				}

				Message msg = event.getMessage();
				if (!msg.getAuthor().getId().equals(mmr.getUid()) || !msg.getContentRaw().equalsIgnoreCase("aschente"))
					return;

				Deck d = KawaiponDAO.getDeck(msg.getAuthor().getId());
				if (d.hasInvalidDeck(p1Channel)) return;
				else if (d.isNovice()) {
					p1Channel.sendMessage("❌ | Você não pode jogar partidas ranqueadas com o deck de iniciante.").queue();
					return;
				}

				if (p1Channel.getId().equals(p2Channel.getId()))
					msg.addReaction(Helper.ACCEPT)
							.flatMap(s -> p1Channel.sendMessage(msg.getAuthor().getName() + " aceitou a partida."))
							.queue();
				else
					msg.addReaction(Helper.ACCEPT)
							.flatMap(s -> p1Channel.sendMessage("Você aceitou a partida."))
							.flatMap(s -> p2Channel.sendMessage("O oponente aceitou a partida."))
							.queue();

				match.add(Pair.of(p1, true));
				timeout.cancel(true);
				timeout = null;
				close();
				if (match.size() == 2) result.run();
			}
		});
	}

	private void sendDuoConfirmation(DuoLobby p1, TextChannel p1Channel, TextChannel p2Channel, List<Pair<DuoLobby, Boolean>> match, Runnable result) {
		RankedDuo rd = p1.duo();
		Set<String> confirmations = new HashSet<>() {{
			add(rd.p1().getUid());
			add(rd.p2().getUid());
		}};

		ShiroInfo.getShiroEvents().addHandler(p1Channel.getGuild(), new SimpleMessageListener() {
			private Future<?> p1Timeout = p1Channel.sendMessage("Tempo para aceitar a partida esgotado, " + rd.p1().getUser().getName() + " está impedido de entrar no saguão novamente por " + (10 * (rd.p1().getEvades() + 1)) + " minutos.")
					.queueAfter(1, TimeUnit.MINUTES, msg -> {
						MatchMakingRating mmr = rd.p1();
						mmr.setEvades(mmr.getEvades() + 1);
						mmr.block(10 * mmr.getEvades(), ChronoUnit.MINUTES);
						MatchMakingRatingDAO.saveMMR(mmr);
						match.add(Pair.of(p1, false));
						close();
						if (match.size() == 2) result.run();
					});
			private Future<?> p2Timeout = p1Channel.sendMessage("Tempo para aceitar a partida esgotado, " + rd.p2().getUser().getName() + " está impedido de entrar no saguão novamente por " + (10 * (rd.p2().getEvades() + 1)) + " minutos.")
					.queueAfter(1, TimeUnit.MINUTES, msg -> {
						MatchMakingRating mmr = rd.p2();
						mmr.setEvades(mmr.getEvades() + 1);
						mmr.block(10 * mmr.getEvades(), ChronoUnit.MINUTES);
						MatchMakingRatingDAO.saveMMR(mmr);
						match.add(Pair.of(p1, false));
						close();
						if (match.size() == 2) result.run();
					});

			{
				p1Channel.sendMessage("""
						%s e %s
						Partida encontrada, digite `aschente` para confirmar a partida.
						Demorar para responder resultará em um bloqueio de saguão de 10 minutos.
						""".formatted(rd.p1().getUser().getAsMention(), rd.p2().getUser().getAsMention())).queue();
				rd.p1().getUser().openPrivateChannel()
						.flatMap(c -> c.sendMessage("**Partida encontrada**: vá para o canal " + p1Channel.getAsMention() + " para confirmar."))
						.queue(null, Helper::doNothing);
				rd.p2().getUser().openPrivateChannel()
						.flatMap(c -> c.sendMessage("**Partida encontrada**: vá para o canal " + p1Channel.getAsMention() + " para confirmar."))
						.queue(null, Helper::doNothing);
			}

			@Override
			public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
				if (!match.isEmpty() && !match.get(0).getRight()) {
					close();
					return;
				}

				Message msg = event.getMessage();
				if (!msg.getContentRaw().equalsIgnoreCase("aschente") || confirmations.remove(msg.getAuthor().getId()))
					return;

				Deck d = KawaiponDAO.getDeck(msg.getAuthor().getId());
				if (d.hasInvalidDeck(p1Channel)) return;
				else if (d.isNovice()) {
					p1Channel.sendMessage("❌ | Você não pode jogar partidas ranqueadas com o deck de iniciante.").queue();
					return;
				}

				if (p1Channel.getId().equals(p2Channel.getId()))
					msg.addReaction(Helper.ACCEPT)
							.flatMap(s -> p1Channel.sendMessage(msg.getAuthor().getName() + " aceitou a partida."))
							.queue();
				else
					msg.addReaction(Helper.ACCEPT)
							.flatMap(s -> p1Channel.sendMessage("Você aceitou a partida (" + (2 - confirmations.size()) + "/2)."))
							.flatMap(s -> p2Channel.sendMessage("O oponente aceitou a partida (" + (2 - confirmations.size()) + "/2)."))
							.queue();

				if (confirmations.isEmpty())
					match.add(Pair.of(p1, true));

				if (msg.getAuthor().getId().equals(rd.p1().getUid())) {
					p1Timeout.cancel(true);
					p1Timeout = null;
				} else {
					p2Timeout.cancel(true);
					p2Timeout = null;
				}

				close();
				if (match.size() == 2) result.run();
			}
		});
	}
}
