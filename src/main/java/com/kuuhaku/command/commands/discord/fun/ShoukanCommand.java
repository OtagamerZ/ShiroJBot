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

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.*;
import com.kuuhaku.handlers.games.tabletop.framework.GameChannel;
import com.kuuhaku.handlers.games.tabletop.framework.GlobalGame;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Shoukan;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.MatchMaking;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.enums.RankedQueue;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Deck;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.model.persistent.tournament.Tournament;
import com.kuuhaku.model.records.DuoLobby;
import com.kuuhaku.model.records.RankedDuo;
import com.kuuhaku.model.records.SoloLobby;
import com.kuuhaku.model.records.TournamentMatch;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONObject;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Command(
		name = "shoukan",
		aliases = {"duelcards"},
		usage = "req_shoukan-args",
		category = Category.FUN
)
@Requires({
		Permission.MESSAGE_ADD_REACTION,
		Permission.MESSAGE_ATTACH_FILES
})
public class ShoukanCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		MatchMaking mm = Main.getInfo().getMatchMaking();

		if (mm.isLocked()) {
			channel.sendMessage("❌ | O Shoukan está bloqueado temporariamente para minha reinicialização, por favor aguarde.").queue();
			return;
		} else if (Main.getInfo().getShoukanSlot().containsKey(channel.getId())) {
			channel.sendMessage("❌ | Já existe uma partida sendo jogada neste canal, por favor aguarde.").queue();
			return;
		}

		boolean practice = args.length > 0 && Helper.equalsAny(args[0], "practice", "treino");
		boolean ranked = args.length > 0 && Helper.equalsAny(args[0], "ranqueada", "ranked");
		boolean tournament = args.length > 0 && Helper.equalsAny(args[0], "torneio", "tournament");
		Deck d = KawaiponDAO.getDeck(author.getId());

		if (practice) {
			JSONObject custom = Helper.getOr(Helper.findJson(argsAsText), new JSONObject());
			boolean daily = args.length > 1 && Helper.equalsAny(args[1], "daily", "diario");

			if (!daily && d.hasInvalidDeck(channel)) return;

			String id = author.getId() + "." + 0 + "." + guild.getId();

			if (Main.getInfo().gameInProgress(author.getId())) {
				channel.sendMessage(I18n.getString("err_you-are-in-game")).queue();
				return;
			}

			GlobalGame t = new Shoukan(Main.getShiroShards(), new GameChannel(channel), 0, custom, daily, false, false, null, author, author);
			t.start();
		} else if (ranked) {
			if (d.isNovice()) {
				channel.sendMessage("❌ | Você não pode jogar partidas ranqueadas com o deck de iniciante.").queue();
				return;
			}

			MatchMakingRating mmr = MatchMakingRatingDAO.getMMR(author.getId());
			com.kuuhaku.model.persistent.Member m = MemberDAO.getHighestProfile(author.getId());
			if (m.getLevel() < 30) {
				channel.sendMessage("❌ | É necessário ter ao menos nível 30 para poder jogar partidas ranqueadas.").queue();
				return;
			} else if (mmr.getWins() + mmr.getLosses() < 30) {
				channel.sendMessage("❌ | É necessário ter jogado ao menos 30 partidas para poder entrar na fila ranqueada.").queue();
				return;
			}

			if (d.hasInvalidDeck(channel)) return;

			String id = author.getId() + "." + 0 + "." + guild.getId();

			if (Main.getInfo().gameInProgress(author.getId())) {
				channel.sendMessage(I18n.getString("err_you-are-in-game")).queue();
				return;
			}

			if (mm.inGame(author.getId())) {
				channel.sendMessage(I18n.getString("err_you-are-in-game")).queue();
				return;
			} else if (mm.isInLobby(mmr)) {
				channel.sendMessage("❌ | Você já está em um saguão, por favor cancele-o antes de tentar entrar novamente.").queue();
				return;
			} else if (mmr.isBlocked()) {
				channel.sendMessage("❌ | Você está impedido de entrar no saguão ranqueado por mais %s.".formatted(Helper.toStringDuration(mmr.getRemainingBlock()))).queue();
				return;
			} else if (args.length < 2 || !Helper.equalsAny(args[1], "solo", "duo")) {
				channel.sendMessage("❌ | Você precisa informar o tipo de fila que deseja entrar (`SOLO` ou `DUO`)").queue();
				return;
			}

			RankedQueue rq = RankedQueue.valueOf(args[1].toUpperCase(Locale.ROOT));
			switch (rq) {
				case SOLO -> {
					mm.joinLobby(mmr, null, rq, channel);
					channel.sendMessage("Você entrou no saguão **SOLO** com sucesso, você será notificado caso uma partida seja encontrada (" + (mm.getSoloLobby().size() - 1) + " na fila).").queue();

					mmr.setJoins(mmr.getJoins() + 1);
					MatchMakingRatingDAO.saveMMR(mmr);

					for (SoloLobby sl : mm.getSoloLobby()) {
						if (sl.mmr().getUid().equals(author.getId())) continue;

						sl.mmr().getUser().openPrivateChannel()
								.flatMap(c -> c.sendMessage(author.getName() + " entrou no saguão (" + (mm.getSoloLobby().size() - 1) + " na fila)."))
								.queue(null, Helper::doNothing);
					}
				}
				case DUO -> {
					if (message.getMentionedUsers().isEmpty()) {
						channel.sendMessage(I18n.getString("err_no-user")).queue();
						return;
					}

					User u = message.getMentionedUsers().get(0);
					MatchMakingRating duo = MatchMakingRatingDAO.getMMR(u.getId());

					if (duo.isBlocked()) {
						channel.sendMessage("❌ | " + u.getAsMention() + " está impedido de entrar no saguão ranqueado por mais %s.".formatted(Helper.toStringDuration(mmr.getRemainingBlock()))).queue();
						return;
					} else if (Math.abs(mmr.getTier().getTier() - duo.getTier().getTier()) > 1) {
						channel.sendMessage("❌ | Diferença entre tiers muito alta.").queue();
						return;
					} else if (mm.isInLobby(duo)) {
						channel.sendMessage("❌ | " + u.getAsMention() + " já está em um saguão, espere-o cancelar antes de tentar convidar novamente.").queue();
						return;
					}


					Main.getInfo().getConfirmationPending().put(author.getId(), true);
					channel.sendMessage(u.getAsMention() + " você foi convidado a entrar no saguão DUO com " + author.getAsMention() + ", deseja aceitar?")
							.queue(s -> Pages.buttonize(s, Map.of(Helper.parseEmoji(Helper.ACCEPT), wrapper -> {
										if (wrapper.getUser().getId().equals(u.getId())) {
											Main.getInfo().getConfirmationPending().remove(author.getId());
											s.delete().queue(null, Helper::doNothing);

											if (mm.isInLobby(mmr)) {
												channel.sendMessage("❌ | Você já está em um saguão, por favor cancele-o antes de tentar entrar novamente.").queue();
												return;
											} else if (mm.isInLobby(duo)) {
												channel.sendMessage("❌ | " + u.getAsMention() + " já está em um saguão, espere-o cancelar antes de tentar convidar novamente.").queue();
												return;
											}

											RankedDuo rd = new RankedDuo(mmr, duo);
											mm.joinLobby(rd, rq, channel);
											channel.sendMessage("Você e " + u.getAsMention() + " entraram no saguão **DUO** com sucesso, você será notificado caso uma partida seja encontrada (" + (mm.getDuoLobby().size() - 1) + " na fila).").queue();

											mmr.setJoins(mmr.getJoins() + 1);
											MatchMakingRatingDAO.saveMMR(mmr);

											duo.setJoins(mmr.getJoins() + 1);
											MatchMakingRatingDAO.saveMMR(duo);

											for (DuoLobby sl : mm.getDuoLobby()) {
												if (sl.duo().p1().getUid().equals(author.getId())) continue;
												else if (sl.duo().p2().getUid().equals(author.getId())) continue;

												sl.duo().p1().getUser().openPrivateChannel()
														.flatMap(c -> c.sendMessage("Equipe " + author.getName() + " e " + u.getName() + " entraram no saguão (" + (mm.getDuoLobby().size() - 1) + " na fila)."))
														.queue(null, Helper::doNothing);
												sl.duo().p2().getUser().openPrivateChannel()
														.flatMap(c -> c.sendMessage("Equipe " + author.getName() + " e " + u.getName() + " entraram no saguão (" + (mm.getDuoLobby().size() - 1) + " na fila)."))
														.queue(null, Helper::doNothing);
											}
										}
									}), ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
									usr -> Helper.equalsAny(usr.getId(), author.getId(), u.getId()),
									ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
							));
				}
			}
		} else if (tournament) {
			Tournament tn = TournamentDAO.getUserTournament(author.getId());
			if (tn == null) {
				channel.sendMessage("❌ | Você não está registrado em nenhum torneio ou as chaves ainda não foram liberadas.").queue();
				return;
			}

			int phase = tn.getCurrPhase(author.getId());
			if (phase != -1) {
				TournamentMatch match = tn.generateMatch(phase, author.getId());
				if (match == null) return;

				User other = Main.getInfo().getUserByID(match.top().equals(author.getId()) ? match.bot() : match.top());
				if (Main.getInfo().getConfirmationPending().get(other.getId()) != null) {
					channel.sendMessage("❌ | " + other.getAsMention() + " possui um comando com confirmação pendente, por favor espere ele resolve-lo antes de usar este comando novamente.").queue();
					return;
				}

				if (d.hasInvalidDeck(channel) || Deck.hasInvalidDeck(KawaiponDAO.getDeck(other.getId()), other, channel))
					return;

				if (Main.getInfo().gameInProgress(author.getId())) {
					channel.sendMessage(I18n.getString("err_you-are-in-game")).queue();
					return;
				} else if (Main.getInfo().gameInProgress(other.getId())) {
					channel.sendMessage(I18n.getString("err_user-in-game")).queue();
					return;
				}

				Main.getInfo().getConfirmationPending().put(author.getId(), true);
				channel.sendMessage(other.getAsMention() + " você foi desafiado a uma partida de Shoukan, deseja aceitar? (torneio)")
						.queue(s -> Pages.buttonize(s, Map.of(Helper.parseEmoji(Helper.ACCEPT), wrapper -> {
									if (wrapper.getUser().getId().equals(other.getId())) {
										Main.getInfo().getConfirmationPending().remove(author.getId());
										if (Main.getInfo().gameInProgress(wrapper.getUser().getId())) {
											channel.sendMessage(I18n.getString("err_you-are-in-game")).queue();
											return;
										} else if (Main.getInfo().gameInProgress(other.getId())) {
											channel.sendMessage(I18n.getString("err_user-in-game")).queue();
											return;
										} else if (Main.getInfo().getShoukanSlot().containsKey(channel.getId())) {
											channel.sendMessage("❌ | Já existe uma partida sendo jogada neste canal, por favor aguarde.").queue();
											return;
										}

										//Main.getInfo().getGames().put(id, t);
										GlobalGame t = new Shoukan(Main.getShiroShards(), new GameChannel(channel), 0, null, false, false, true, match, Main.getInfo().getUsersByID(match.top(), match.bot()));
										s.delete().queue(null, Helper::doNothing);
										t.start();
									}
								}), ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
								u -> Helper.equalsAny(u.getId(), author.getId(), other.getId()),
								ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
						));
			}
		} else {
			if (message.getMentionedUsers().isEmpty()) {
				channel.sendMessage(I18n.getString("err_no-user")).queue();
				return;
			} else if (message.getMentionedUsers().size() != 1 && message.getMentionedUsers().size() != 3) {
				channel.sendMessage("❌ | Você precisa mencionar 1 usuário para jogar solo, ou 3 para jogar em equipes (1º e 3º equipe 1, você e o 2º equipe 2).").queue();
				return;
			}

			List<User> users = message.getMentionedUsers();
			for (User user : users) {
				if (Main.getInfo().getConfirmationPending().get(user.getId()) != null) {
					channel.sendMessage("❌ | " + user.getAsMention() + " possui um comando com confirmação pendente, por favor espere ele resolve-lo antes de usar este comando novamente.").queue();
					return;
				}
			}

			boolean team = users.size() == 3;
			boolean daily = Helper.findParam(args, "daily", "diario");

			int bet = 0;
			JSONObject custom = Helper.findJson(argsAsText);
			if (!team) {
				Account uacc = AccountDAO.getAccount(author.getId());
				Account tacc = AccountDAO.getAccount(message.getMentionedUsers().get(0).getId());

				if (args.length > 1 && StringUtils.isNumeric(args[1]) && custom == null) {
					bet = Integer.parseInt(args[1]);
					if (bet < 0) {
						channel.sendMessage(I18n.getString("err_invalid-credit-amount")).queue();
						return;
					} else if (uacc.getBalance() < bet) {
						channel.sendMessage(I18n.getString("err_insufficient-credits-user")).queue();
						return;
					} else if (tacc.getBalance() < bet) {
						channel.sendMessage(I18n.getString("err_insufficient-credits-target")).queue();
						return;
					}
				}
			}

			if (!daily) {
				User other = message.getMentionedUsers().get(0);
				if (d.hasInvalidDeck(channel) || Deck.hasInvalidDeck(KawaiponDAO.getDeck(other.getId()), other, channel))
					return;
			}

			String id = author.getId() + "." + users.get(0).getId() + "." + guild.getId();

			if (Main.getInfo().gameInProgress(author.getId())) {
				channel.sendMessage(I18n.getString("err_you-are-in-game")).queue();
				return;
			} else if (Main.getInfo().gameInProgress(message.getMentionedUsers().get(0).getId())) {
				channel.sendMessage(I18n.getString("err_user-in-game")).queue();
				return;
			} else if (users.stream().anyMatch(u -> u.getId().equals(author.getId()))) {
				channel.sendMessage(I18n.getString("err_cannot-play-with-yourself")).queue();
				return;
			}

			if (team) {
				if (!daily)
					for (int i = 0; i < 3; i++) {
						User u = message.getMentionedUsers().get(i);

						if (Deck.hasInvalidDeck(KawaiponDAO.getDeck(u.getId()), u, channel)) return;
					}

				List<User> players = new ArrayList<>() {{
					add(author);
					addAll(users);
				}};

				Set<String> accepted = new HashSet<>() {{
					add(author.getId());
				}};

				for (User player : players) {
					Main.getInfo().getConfirmationPending().put(player.getId(), true);
				}
				int finalBet = bet;
				channel.sendMessage(Helper.parseAndJoin(users, IMentionable::getAsMention) + " vocês foram desafiados a uma partida de Shoukan, desejam aceitar?" + (daily ? " (desafio diário)" : "") + (custom != null ? " (contém regras personalizadas)" : bet != 0 ? " (aposta: " + Helper.separate(bet) + " CR)" : ""))
						.queue(s -> Pages.buttonize(s, Map.of(Helper.parseEmoji(Helper.ACCEPT), wrapper -> {
									if (players.contains(wrapper.getUser())) {
										if (Main.getInfo().gameInProgress(wrapper.getUser().getId())) {
											channel.sendMessage(I18n.getString("err_you-are-in-game")).queue();
											return;
										} else if (Main.getInfo().gameInProgress(author.getId())) {
											channel.sendMessage(I18n.getString("err_user-in-game")).queue();
											return;
										} else if (Main.getInfo().getShoukanSlot().containsKey(channel.getId())) {
											channel.sendMessage("❌ | Já existe uma partida sendo jogada neste canal, por favor aguarde.").queue();
											return;
										}

										if (!accepted.contains(wrapper.getUser().getId())) {
											channel.sendMessage(wrapper.getUser().getAsMention() + " aceitou a partida.").queue();
											accepted.add(wrapper.getUser().getId());
										}

										if (accepted.size() == players.size()) {
											Main.getInfo().getConfirmationPending().remove(author.getId());
											//Main.getInfo().getGames().put(id, t);
											GlobalGame t = new Shoukan(Main.getShiroShards(), new GameChannel(channel), finalBet, custom, daily, false, true, null, players.toArray(User[]::new));
											s.delete().queue(null, Helper::doNothing);
											t.start();
										}
									}
								}), ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
								u -> players.parallelStream().map(User::getId).anyMatch(i -> i.equals(u.getId())),
								ms -> {
									for (User player : players) {
										Main.getInfo().getConfirmationPending().remove(player.getId());
									}
								}
						));
			} else {
				Main.getInfo().getConfirmationPending().put(author.getId(), true);
				int finalBet = bet;
				channel.sendMessage(message.getMentionedUsers().get(0).getAsMention() + " você foi desafiado a uma partida de Shoukan, deseja aceitar?" + (daily ? " (desafio diário)" : "") + (custom != null ? " (contém regras personalizadas)" : bet != 0 ? " (aposta: " + Helper.separate(bet) + " CR)" : ""))
						.queue(s -> Pages.buttonize(s, Map.of(Helper.parseEmoji(Helper.ACCEPT), wrapper -> {
									if (wrapper.getUser().getId().equals(message.getMentionedUsers().get(0).getId())) {
										Main.getInfo().getConfirmationPending().remove(author.getId());
										if (Main.getInfo().gameInProgress(wrapper.getUser().getId())) {
											channel.sendMessage(I18n.getString("err_you-are-in-game")).queue();
											return;
										} else if (Main.getInfo().gameInProgress(message.getMentionedUsers().get(0).getId())) {
											channel.sendMessage(I18n.getString("err_user-in-game")).queue();
											return;
										} else if (Main.getInfo().getShoukanSlot().containsKey(channel.getId())) {
											channel.sendMessage("❌ | Já existe uma partida sendo jogada neste canal, por favor aguarde.").queue();
											return;
										}

										//Main.getInfo().getGames().put(id, t);
										GlobalGame t = new Shoukan(Main.getShiroShards(), new GameChannel(channel), finalBet, custom, daily, false, true, null, author, message.getMentionedUsers().get(0));
										s.delete().queue(null, Helper::doNothing);
										t.start();
									}
								}), ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES,
								u -> Helper.equalsAny(u.getId(), author.getId(), message.getMentionedUsers().get(0).getId()),
								ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
						));
			}
		}
	}
}
