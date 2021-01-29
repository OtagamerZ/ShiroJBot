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

package com.kuuhaku.command.commands.discord.fun;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.ClanDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.handlers.games.tabletop.framework.GameChannel;
import com.kuuhaku.handlers.games.tabletop.framework.GlobalGame;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Shoukan;
import com.kuuhaku.model.common.MatchMaking;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Clan;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ShoukanCommand extends Command {

	public ShoukanCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public ShoukanCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public ShoukanCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public ShoukanCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		boolean practice = args.length > 0 && Helper.equalsAny(args[0], "practice", "treino");
		boolean ranked = args.length > 0 && Helper.equalsAny(args[0], "ranqueada", "ranked");

		if (practice) {
			JSONObject custom = Helper.getOr(Helper.findJson(argsAsText), new JSONObject());
			boolean daily = args.length > 1 && Helper.equalsAny(args[1], "daily", "diario");

			if (!daily) {
				Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
				if (kp.getChampions().size() < 30) {
					channel.sendMessage("❌ | É necessário ter ao menos 30 cartas no deck para poder jogar Shoukan.").queue();
					return;
				}
			}

			String id = author.getId() + "." + 0 + "." + guild.getId();

			if (Main.getInfo().gameInProgress(author.getId())) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_you-are-in-game")).queue();
				return;
			}

			GlobalGame t = new Shoukan(Main.getShiroShards(), new GameChannel((TextChannel) channel), 0, custom, daily, false, author, author);
			t.start();
		} else if (ranked) {
			MatchMaking mm = Main.getInfo().getMatchMaking();

			if (mm.isLocked()) {
				channel.sendMessage("❌ | A fila ranqueada está bloqueada temporariamente para minha reinicialização, por favor aguarde.").queue();
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

			Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
			if (kp.getChampions().size() < 30) {
				channel.sendMessage("❌ | É necessário ter ao menos 30 cartas no deck para poder jogar Shoukan.").queue();
				return;
			}

			String id = author.getId() + "." + 0 + "." + guild.getId();

			if (Main.getInfo().gameInProgress(author.getId())) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_you-are-in-game")).queue();
				return;
			}

			if (mm.inGame(author.getId())) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_you-are-in-game")).queue();
				return;
			} else if (mm.getLobby().keySet().stream().anyMatch(mr -> mr.getUserId().equals(author.getId()))) {
				channel.sendMessage("❌ | Você já está no saguão, por favor cancele-o antes de tentar entrar novamente.").queue();
				return;
			} else if (mmr.isBlocked()) {
				channel.sendMessage("❌ | Você está impedido de entrar no saguão ranqueado devido a um abandono recente (Tempo restante: %s seg).".formatted(mmr.getRemainingBlock())).queue();
				return;
			}

			mm.joinLobby(mmr, (TextChannel) channel);
			channel.sendMessage("Você entrou no saguão com sucesso, você será notificado caso uma partida seja encontrada (" + (mm.getLobby().size() - 1) + " no saguão).").queue(s ->
					Pages.buttonize(s, Collections.emptyMap(), true, 30, TimeUnit.MINUTES
							, u -> u.getId().equals(author.getId())
							, ms -> {
								mm.getLobby().remove(mmr);
								ms.delete().queue();
							}
					)
			);
		} else {
			if (message.getMentionedUsers().size() == 0) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-user")).queue();
				return;
			} else if (message.getMentionedUsers().size() != 1 && message.getMentionedUsers().size() != 3) {
				channel.sendMessage("❌ | Você precisa mencionar 1 usuário para jogar solo, ou 3 para jogar em equipes (1º e 3º equipe 1, você e o 2º equipe 2).").queue();
				return;
			} else if (Main.getInfo().getConfirmationPending().getIfPresent(author.getId()) != null) {
				channel.sendMessage("❌ | Você possui um comando com confirmação pendente, por favor resolva-o antes de usar este comando novamente.").queue();
				return;
			}

			List<User> users = message.getMentionedUsers();
			for (User user : users) {
				if (Main.getInfo().getConfirmationPending().getIfPresent(user.getId()) != null) {
					channel.sendMessage("❌ | " + user.getAsMention() + " possui um comando com confirmação pendente, por favor espere ele resolve-lo antes de usar este comando novamente.").queue();
					return;
				}
			}

			boolean team = users.size() == 3;
			boolean clan = !team && (args.length > 1 && args[1].equalsIgnoreCase("clan")) || (args.length > 2 && args[2].equalsIgnoreCase("clan"));
			boolean daily = !clan && (args.length > 1 && Helper.equalsAny(args[1], "daily", "diario")) || (args.length > 2 && Helper.equalsAny(args[2], "daily", "diario"));

			int bet = 0;
			JSONObject custom = Helper.findJson(argsAsText);
			if (!team) {
				Account uacc = AccountDAO.getAccount(author.getId());
				Account tacc = AccountDAO.getAccount(message.getMentionedUsers().get(0).getId());

				if (args.length > 1 && StringUtils.isNumeric(args[1]) && custom == null) {
					bet = Integer.parseInt(args[1]);
					if (bet < 0) {
						channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-credit-amount")).queue();
						return;
					} else if (uacc.getBalance() < bet) {
						channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-user")).queue();
						return;
					} else if (tacc.getBalance() < bet) {
						channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_insufficient-credits-target")).queue();
						return;
					}
				}
			}

			Clan c = ClanDAO.getUserClan(author.getId());
			Clan other = ClanDAO.getUserClan(message.getMentionedUsers().get(0).getId());
			if (clan) {
				if (c == null) {
					channel.sendMessage("❌ | Você não possui um clã.").queue();
					return;
				} else if (other == null) {
					channel.sendMessage("❌ | Ele/ela não possui um clã.").queue();
					return;
				} else if (c.getDeck().getChampions().size() < 30) {
					channel.sendMessage("❌ | " + c.getName() + " não possui cartas suficientes, é necessário ter ao menos 30 cartas para poder jogar Shoukan.").queue();
					return;
				} else if (other.getDeck().getChampions().size() < 30) {
					channel.sendMessage("❌ | " + other.getName() + " não possui cartas suficientes, é necessário ter ao menos 30 cartas para poder jogar Shoukan.").queue();
					return;
				}
			} else if (!daily) {
				Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
				Kawaipon target = KawaiponDAO.getKawaipon(message.getMentionedUsers().get(0).getId());
				if (kp.getChampions().size() < 30) {
					channel.sendMessage("❌ | É necessário ter ao menos 30 cartas no deck para poder jogar Shoukan.").queue();
					return;
				} else if (target.getChampions().size() < 30) {
					channel.sendMessage("❌ | " + message.getMentionedUsers().get(0).getAsMention() + " não possui cartas suficientes, é necessário ter ao menos 30 cartas para poder jogar Shoukan.").queue();
					return;
				}
			}

			String id = author.getId() + "." + users.get(0).getId() + "." + guild.getId();

			if (Main.getInfo().gameInProgress(author.getId())) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_you-are-in-game")).queue();
				return;
			} else if (Main.getInfo().gameInProgress(message.getMentionedUsers().get(0).getId())) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_user-in-game")).queue();
				return;
			} else if (users.stream().anyMatch(u -> u.getId().equals(author.getId()))) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_cannot-play-with-yourself")).queue();
				return;
			}

			if (team) {
				List<User> players = new ArrayList<>() {{
					add(author);
					addAll(users);
				}};
				Set<String> accepted = new HashSet<>() {{
					add(author.getId());
				}};

				for (User player : players) {
					String hash = Helper.generateHash(guild, player);
					ShiroInfo.getHashes().add(hash);
					Main.getInfo().getConfirmationPending().put(player.getId(), true);
				}
				GlobalGame t = new Shoukan(Main.getShiroShards(), new GameChannel((TextChannel) channel), bet, custom, daily, false, players.toArray(User[]::new));
				channel.sendMessage(users.stream().map(User::getAsMention).map(s -> s + ", ").collect(Collectors.joining()) + " vocês foram desafiados a uma partida de Shoukan, desejam aceitar?" + (daily ? " (desafio diário)" : "") + (custom != null ? " (contém regras personalizadas)" : ""))
						.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
									if (players.contains(mb.getUser())) {
										if (Main.getInfo().gameInProgress(mb.getId())) {
											channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_you-are-in-game")).queue();
											return;
										} else if (Main.getInfo().gameInProgress(author.getId())) {
											channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_user-in-game")).queue();
											return;
										}

										if (ShiroInfo.getHashes().remove(Helper.generateHash(guild, mb.getUser()))) {
											if (!accepted.contains(mb.getId())) {
												channel.sendMessage(mb.getAsMention() + " aceitou a partida.").queue();
												accepted.add(mb.getId());
											}

											if (accepted.size() == players.size()) {
												Main.getInfo().getConfirmationPending().invalidate(author.getId());
												//Main.getInfo().getGames().put(id, t);
												s.delete().queue(null, Helper::doNothing);
												t.start();
											}
										}
									}
								}), true, 1, TimeUnit.MINUTES,
								u -> Helper.equalsAny(u.getId(), players.stream().map(User::getId).toArray(String[]::new)),
								ms -> {
									for (User player : players) {
										String hash = Helper.generateHash(guild, player);
										ShiroInfo.getHashes().remove(hash);
										Main.getInfo().getConfirmationPending().invalidate(player.getId());
									}
								})
						);
			} else if (clan) {
				String hash = Helper.generateHash(guild, author);
				ShiroInfo.getHashes().add(hash);
				Main.getInfo().getConfirmationPending().put(author.getId(), true);
				GlobalGame t = new Shoukan(Main.getShiroShards(), new GameChannel((TextChannel) channel), bet, custom, daily, false, List.of(c, other), author, message.getMentionedUsers().get(0));
				channel.sendMessage(message.getMentionedUsers().get(0).getAsMention() + " seu clã foi desafiado a uma partida de Shoukan, deseja aceitar?" + (custom != null ? " (contém regras personalizadas)" : bet != 0 ? " (aposta: " + bet + " créditos)" : ""))
						.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
									if (mb.getId().equals(message.getMentionedUsers().get(0).getId())) {
										if (!ShiroInfo.getHashes().remove(hash)) return;
										Main.getInfo().getConfirmationPending().invalidate(author.getId());
										if (Main.getInfo().gameInProgress(mb.getId())) {
											channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_you-are-in-game")).queue();
											return;
										} else if (Main.getInfo().gameInProgress(message.getMentionedUsers().get(0).getId())) {
											channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_user-in-game")).queue();
											return;
										}

										//Main.getInfo().getGames().put(id, t);
										s.delete().queue(null, Helper::doNothing);
										t.start();
									}
								}), true, 1, TimeUnit.MINUTES,
								u -> Helper.equalsAny(u.getId(), author.getId(), message.getMentionedUsers().get(0).getId()),
								ms -> {
									ShiroInfo.getHashes().remove(hash);
									Main.getInfo().getConfirmationPending().invalidate(author.getId());
								})
						);
			} else {
				String hash = Helper.generateHash(guild, author);
				ShiroInfo.getHashes().add(hash);
				Main.getInfo().getConfirmationPending().put(author.getId(), true);
				GlobalGame t = new Shoukan(Main.getShiroShards(), new GameChannel((TextChannel) channel), bet, custom, daily, false, author, message.getMentionedUsers().get(0));
				channel.sendMessage(message.getMentionedUsers().get(0).getAsMention() + " você foi desafiado a uma partida de Shoukan, deseja aceitar?" + (daily ? " (desafio diário)" : "") + (custom != null ? " (contém regras personalizadas)" : bet != 0 ? " (aposta: " + bet + " créditos)" : ""))
						.queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
									if (mb.getId().equals(message.getMentionedUsers().get(0).getId())) {
										if (!ShiroInfo.getHashes().remove(hash)) return;
										Main.getInfo().getConfirmationPending().invalidate(author.getId());
										if (Main.getInfo().gameInProgress(mb.getId())) {
											channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_you-are-in-game")).queue();
											return;
										} else if (Main.getInfo().gameInProgress(message.getMentionedUsers().get(0).getId())) {
											channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_user-in-game")).queue();
											return;
										}

										//Main.getInfo().getGames().put(id, t);
										s.delete().queue(null, Helper::doNothing);
										t.start();
									}
								}), true, 1, TimeUnit.MINUTES,
								u -> Helper.equalsAny(u.getId(), author.getId(), message.getMentionedUsers().get(0).getId()),
								ms -> {
									ShiroInfo.getHashes().remove(hash);
									Main.getInfo().getConfirmationPending().invalidate(author.getId());
								})
						);
			}
		}
	}
}
