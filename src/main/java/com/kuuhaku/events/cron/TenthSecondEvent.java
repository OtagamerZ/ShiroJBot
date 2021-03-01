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
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.handlers.games.tabletop.framework.GameChannel;
import com.kuuhaku.handlers.games.tabletop.framework.GlobalGame;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Shoukan;
import com.kuuhaku.model.common.RankedDuo;
import com.kuuhaku.model.enums.RankedTier;
import com.kuuhaku.model.persistent.Kawaipon;
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
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TenthSecondEvent implements Job {
	public static JobDetail tenthSecond;
	private boolean lock = false;

	@Override
	public void execute(JobExecutionContext context) {
		if (lock) return;
		lock = true;
		List<Map.Entry<MatchMakingRating, Pair<Integer, TextChannel>>> soloLobby = new ArrayList<>(Main.getInfo().getMatchMaking().getSoloLobby().entrySet());
		if (soloLobby.size() > 1) {
			var indexes = Helper.getRandomN(Helper.getNumericList(0, soloLobby.size()), 2, 1);
			if (tryMatching(soloLobby, indexes.get(0), indexes.get(1))) {
				lock = false;
				return;
			}
			Main.getInfo().getMatchMaking().getSoloLobby().computeIfPresent(soloLobby.get(indexes.get(0)).getKey(), (mmr, p) -> Pair.of(p.getLeft() + 1, p.getRight()));
		}

		List<Map.Entry<RankedDuo, Pair<Integer, TextChannel>>> duoLobby = new ArrayList<>(Main.getInfo().getMatchMaking().getDuoLobby().entrySet());
		if (duoLobby.size() > 3) {
			var indexes = Helper.getRandomN(Helper.getNumericList(0, duoLobby.size()), 4, 1);
			if (tryMatching(duoLobby, indexes.get(0), indexes.get(1), indexes.get(2), indexes.get(3))) {
				lock = false;
				return;
			}
			Main.getInfo().getMatchMaking().getDuoLobby().computeIfPresent(duoLobby.get(indexes.get(0)).getKey(), (mmr, p) -> Pair.of(p.getLeft() + 1, p.getRight()));
		}

		lock = false;
	}

	private boolean tryMatching(List<Map.Entry<MatchMakingRating, Pair<Integer, TextChannel>>> lobby, int a, int b) {
		try {
			Map.Entry<MatchMakingRating, Pair<Integer, TextChannel>> p1 = lobby.get(a);
			Map.Entry<MatchMakingRating, Pair<Integer, TextChannel>> p2 = lobby.get(b);

			MatchMakingRating mmr1 = p1.getKey();
			MatchMakingRating mmr2 = p2.getKey();

			if (!mmr1.equals(mmr2)
				&& Helper.prcnt(mmr1.getMMR(), mmr2.getMMR() == 0 ? 1 : mmr2.getMMR()) * 100 <= p1.getValue().getLeft() * 10
				&& (Math.abs(mmr1.getTier().getTier() - mmr2.getTier().getTier()) < 2 || mmr2.getTier() == RankedTier.UNRANKED)) {
				Main.getInfo().getMatchMaking().getSoloLobby().remove(mmr1);
				Main.getInfo().getMatchMaking().getSoloLobby().remove(mmr2);

				TextChannel p1Channel = p1.getValue().getRight();
				TextChannel p2Channel = p2.getValue().getRight();
				List<Pair<Map.Entry<MatchMakingRating, Pair<Integer, TextChannel>>, Boolean>> match = new ArrayList<>();

				Runnable result = () -> {
					Main.getInfo().getMatchMaking().getSoloLobby().remove(mmr1);
					Main.getInfo().getMatchMaking().getSoloLobby().remove(mmr2);

					mmr1.setEvades(0);
					mmr2.setEvades(0);

					MatchMakingRatingDAO.saveMMR(mmr1);
					MatchMakingRatingDAO.saveMMR(mmr2);

					boolean p1Starts = Helper.chance(50);
					if (match.stream().allMatch(Pair::getRight)) {
						GlobalGame g = new Shoukan(
								Main.getShiroShards(),
								new GameChannel(p1Channel, p2Channel),
								0,
								null,
								false,
								true,
								p1Starts ? mmr1.getUser() : mmr2.getUser(),
								p1Starts ? mmr2.getUser() : mmr1.getUser()
						);
						g.start();
						Main.getInfo().getMatchMaking().getGames().add(g);
					} else {
						for (Pair<Map.Entry<MatchMakingRating, Pair<Integer, TextChannel>>, Boolean> p : match) {
							MatchMakingRating mmr = p.getLeft().getKey();
							if (p.getRight()) {
								p.getLeft().getValue().getRight().sendMessage("O oponente não confirmou a partida a tempo, você foi retornado ao saguão.").queue(s ->
										Pages.buttonize(s, Map.of(
												Helper.CANCEL, (mb, ms) -> {
													Main.getInfo().getMatchMaking().getSoloLobby().remove(mmr);
													ms.delete().queue();
												}), false, 30, TimeUnit.MINUTES
												, u -> u.getId().equals(mmr.getUid())
												, ms -> {
													Main.getInfo().getMatchMaking().getSoloLobby().remove(mmr);
													ms.delete().queue();
												}
										)
								);
								Pair<Integer, TextChannel> newPair = Pair.of(
										p.getLeft().getValue().getLeft() + 1,
										p.getLeft().getValue().getRight()
								);
								Main.getInfo().getMatchMaking().getSoloLobby().put(mmr, newPair);
							}
						}
					}
				};

				sendSoloConfirmation(p1, p1Channel, p2Channel, match, result);
				sendSoloConfirmation(p2, p2Channel, p1Channel, match, result);

				return true;
			}
		} catch (IndexOutOfBoundsException e) {
			return true;
		}
		return false;
	}

	private boolean tryMatching(List<Map.Entry<RankedDuo, Pair<Integer, TextChannel>>> lobby, int a, int b, int c, int d) {
		try {
			Map.Entry<RankedDuo, Pair<Integer, TextChannel>> p1 = lobby.get(a);
			Map.Entry<RankedDuo, Pair<Integer, TextChannel>> p2 = lobby.get(b);

			RankedDuo t1 = p1.getKey();
			RankedDuo t2 = p2.getKey();

			if (!t1.equals(t2)
				&& Helper.prcnt(t1.getAvgMMR(), t2.getAvgMMR() == 0 ? 1 : t2.getAvgMMR()) * 100 <= p1.getValue().getLeft() * 10
				&& (Math.abs(t1.getAvgTier() - t2.getAvgTier()) < 2 || t2.getAvgTier() == 0)) {
				Main.getInfo().getMatchMaking().getDuoLobby().remove(t1);
				Main.getInfo().getMatchMaking().getDuoLobby().remove(t2);

				TextChannel p1Channel = p1.getValue().getRight();
				TextChannel p2Channel = p2.getValue().getRight();
				List<Pair<Map.Entry<RankedDuo, Pair<Integer, TextChannel>>, Boolean>> match = new ArrayList<>();

				Runnable result = () -> {
					Main.getInfo().getMatchMaking().getDuoLobby().remove(t1);
					Main.getInfo().getMatchMaking().getDuoLobby().remove(t2);

					t1.getP1().setEvades(0);
					t1.getP2().setEvades(0);
					t2.getP1().setEvades(0);
					t2.getP2().setEvades(0);

					MatchMakingRatingDAO.saveMMR(t1.getP1());
					MatchMakingRatingDAO.saveMMR(t1.getP2());
					MatchMakingRatingDAO.saveMMR(t2.getP1());
					MatchMakingRatingDAO.saveMMR(t2.getP2());

					boolean p1Starts = Helper.chance(50);
					boolean leaderStarts = Helper.chance(50);
					if (match.stream().allMatch(Pair::getRight)) {
						GlobalGame g = new Shoukan(
								Main.getShiroShards(),
								new GameChannel(p1Channel, p2Channel),
								0,
								null,
								false,
								true,
								p1Starts ?
										leaderStarts ?
												t1.getP1().getUser() : t1.getP2().getUser()
										:
										leaderStarts ?
												t2.getP1().getUser() : t2.getP2().getUser(),
								p1Starts ?
										leaderStarts ?
												t2.getP1().getUser() : t2.getP2().getUser()
										:
										leaderStarts ?
												t1.getP1().getUser() : t1.getP2().getUser()
						);
						g.start();
						Main.getInfo().getMatchMaking().getGames().add(g);
					} else {
						for (Pair<Map.Entry<RankedDuo, Pair<Integer, TextChannel>>, Boolean> p : match) {
							RankedDuo rd = p.getLeft().getKey();
							if (p.getRight()) {
								p.getLeft().getValue().getRight().sendMessage("O oponente não confirmou a partida a tempo, você foi retornado ao saguão.").queue(s ->
										Pages.buttonize(s, Map.of(
												Helper.CANCEL, (mb, ms) -> {
													Main.getInfo().getMatchMaking().getDuoLobby().remove(rd);
													ms.delete().queue();
												}), false, 30, TimeUnit.MINUTES
												, u -> Helper.equalsAny(u.getId(), rd.getP1().getUid(), rd.getP2().getUid())
												, ms -> {
													Main.getInfo().getMatchMaking().getDuoLobby().remove(rd);
													ms.delete().queue();
												}
										)
								);
								Pair<Integer, TextChannel> newPair = Pair.of(
										p.getLeft().getValue().getLeft() + 1,
										p.getLeft().getValue().getRight()
								);
								Main.getInfo().getMatchMaking().getDuoLobby().put(rd, newPair);
							}
						}
					}
				};

				sendDuoConfirmation(p1, p1Channel, p2Channel, match, result);
				sendDuoConfirmation(p2, p2Channel, p1Channel, match, result);

				return true;
			}
		} catch (IndexOutOfBoundsException e) {
			return true;
		}
		return false;
	}

	private void sendSoloConfirmation(Map.Entry<MatchMakingRating, Pair<Integer, TextChannel>> p1, TextChannel p1Channel, TextChannel p2Channel, List<Pair<Map.Entry<MatchMakingRating, Pair<Integer, TextChannel>>, Boolean>> match, Runnable result) {
		MatchMakingRating mmr1 = p1.getKey();

		Main.getInfo().getShiroEvents().addHandler(p1Channel.getGuild(), new SimpleMessageListener() {
			private Future<?> timeout = p1Channel.sendMessage("Tempo para aceitar a partida esgotado, você está impedido de entrar no saguão novamente por " + (10 * (mmr1.getEvades() + 1)) + " minutos.")
					.queueAfter(1, TimeUnit.MINUTES, msg -> {
						mmr1.setEvades(mmr1.getEvades() + 1);
						mmr1.block(10 * mmr1.getEvades(), ChronoUnit.MINUTES);
						MatchMakingRatingDAO.saveMMR(mmr1);
						match.add(Pair.of(p1, false));
						close();
						if (match.size() == 2) result.run();
					});

			{
				p1Channel.sendMessage("""
						%s
						Partida encontrada, digite `aschente` para confirmar a partida.
						Demorar para responder resultará em um bloqueio de saguão de 10 minutos.
						""".formatted(mmr1.getUser().getAsMention())).queue();
			}

			@Override
			public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
				Message msg = event.getMessage();
				if (!msg.getAuthor().getId().equals(mmr1.getUid()) || !msg.getContentRaw().equalsIgnoreCase("aschente"))
					return;

				Kawaipon kp = KawaiponDAO.getKawaipon(msg.getAuthor().getId());
				if (kp.getChampions().size() < 30) {
					p1Channel.sendMessage("❌ | Você está com um deck que possui menos que 30 cartas. Você precisa corrigir antes de poder aceitar a partida.").queue();
					return;
				} else if (kp.getEquipments().stream().mapToInt(Equipment::getTier).sum() > 24) {
					p1Channel.sendMessage("❌ | Seus equipamentos ultrapassam a soma total de slots permitidos, você precisa corrigir antes de poder aceitar a partida.").queue();
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

	private void sendDuoConfirmation(Map.Entry<RankedDuo, Pair<Integer, TextChannel>> p1, TextChannel p1Channel, TextChannel p2Channel, List<Pair<Map.Entry<RankedDuo, Pair<Integer, TextChannel>>, Boolean>> match, Runnable result) {
		RankedDuo rd = p1.getKey();
		Set<String> confirmations = new HashSet<>() {{
			add(rd.getP1().getUid());
			add(rd.getP2().getUid());
		}};

		Main.getInfo().getShiroEvents().addHandler(p1Channel.getGuild(), new SimpleMessageListener() {
			private Future<?> p1Timeout = p1Channel.sendMessage("Tempo para aceitar a partida esgotado, " + rd.getP1().getUser().getName() + " está impedido de entrar no saguão novamente por " + (10 * (rd.getP1().getEvades() + 1)) + " minutos.")
					.queueAfter(1, TimeUnit.MINUTES, msg -> {
						MatchMakingRating mmr = rd.getP1();
						mmr.setEvades(mmr.getEvades() + 1);
						mmr.block(10 * mmr.getEvades(), ChronoUnit.MINUTES);
						MatchMakingRatingDAO.saveMMR(mmr);
						match.add(Pair.of(p1, false));
						close();
						if (match.size() == 2) result.run();
					});
			private Future<?> p2Timeout = p1Channel.sendMessage("Tempo para aceitar a partida esgotado, " + rd.getP2().getUser().getName() + " está impedido de entrar no saguão novamente por " + (10 * (rd.getP2().getEvades() + 1)) + " minutos.")
					.queueAfter(1, TimeUnit.MINUTES, msg -> {
						MatchMakingRating mmr = rd.getP2();
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
						""".formatted(rd.getP1().getUser().getAsMention(), rd.getP2().getUser().getAsMention())).queue();
			}

			@Override
			public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
				Message msg = event.getMessage();
				if (!msg.getContentRaw().equalsIgnoreCase("aschente") || confirmations.remove(msg.getAuthor().getId()))
					return;

				Kawaipon kp = KawaiponDAO.getKawaipon(msg.getAuthor().getId());
				if (kp.getChampions().size() < 30) {
					p1Channel.sendMessage("❌ | Você está com um deck que possui menos que 30 cartas. Você precisa corrigir antes de poder aceitar a partida.").queue();
					return;
				} else if (kp.getEquipments().stream().mapToInt(Equipment::getTier).sum() > 24) {
					p1Channel.sendMessage("❌ | Seus equipamentos ultrapassam a soma total de slots permitidos, você precisa corrigir antes de poder aceitar a partida.").queue();
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

				if (msg.getAuthor().getId().equals(rd.getP1().getUid())) {
					p1Timeout.cancel(true);
					p1Timeout = null;
					close();
				} else {
					p2Timeout.cancel(true);
					p2Timeout = null;
					close();
				}
				if (match.size() == 2) result.run();
			}
		});
	}
}
