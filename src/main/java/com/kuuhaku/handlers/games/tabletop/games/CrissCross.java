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

package com.kuuhaku.handlers.games.tabletop.games;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.handlers.games.tabletop.entity.Piece;
import com.kuuhaku.handlers.games.tabletop.entity.Player;
import com.kuuhaku.handlers.games.tabletop.entity.Spot;
import com.kuuhaku.handlers.games.tabletop.entity.Tabletop;
import com.kuuhaku.handlers.games.tabletop.enums.Board;
import com.kuuhaku.handlers.games.tabletop.pieces.Circle;
import com.kuuhaku.handlers.games.tabletop.pieces.Cross;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class CrissCross extends Tabletop {
	private final Map<User, Piece> pieces;
	private Future<?> timeout;
	private Message message = null;

	public CrissCross(TextChannel table, String id, User... players) {
		super(table, Board.SIZE_3X3(), id, players);
		pieces = Map.of(
				players[0], new Cross(new Player(players[0], false)),
				players[1], new Circle(new Player(players[1], true))
		);
	}

	@Override
	public void execute(int bet) {
		final User[] turn = {getPlayers().nextTurn()};
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(getBoard().render(), "jpg", baos);

			message = getTable().sendMessage("Turno de " + turn[0].getAsMention()).addFile(baos.toByteArray(), "board.jpg").complete();
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}

		Main.getInfo().getAPI().addEventListener(new ListenerAdapter() {
			{
				timeout = getTable().sendMessage(":x: | Tempo expirado, por favor inicie outra sessão.").queueAfter(180, TimeUnit.SECONDS, ms -> {
					Main.getInfo().getAPI().removeEventListener(this);
					ShiroInfo.getGames().remove(getId());
				}, Helper::doNothing);
			}

			@Override
			public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
				User u = event.getAuthor();
				TextChannel chn = event.getChannel();
				Message m = event.getMessage();

				if (chn.getId().equals(getTable().getId()) && u.getId().equals(turn[0].getId()) && (m.getContentRaw().length() == 2 || Helper.containsAny(m.getContentRaw(), "desistir", "forfeit", "ff", "surrender"))) {
					if (Helper.containsAny(m.getContentRaw(), "desistir", "forfeit", "ff", "surrender")) {
						Main.getInfo().getAPI().removeEventListener(this);
						ShiroInfo.getGames().remove(getId());
						getTable().sendMessage(turn[0].getAsMention() + " desistiu!").queue();
						timeout.cancel(true);
						return;
					}
					try {
						Spot s = Spot.of(m.getContentRaw());

						if (getBoard().getLayout()[s.getY()][s.getX()] == null)
							getBoard().getLayout()[s.getY()][s.getX()] = pieces.get(u);
						else {
							getTable().sendMessage(":x: | Esta casa já está ocupada!").queue();
							return;
						}

						int fullRows = 0;
						for (int i = 0; i < getBoard().getLayout().length; i++) {
							if (Collections.frequency(Arrays.asList(getBoard().getColumn(i)), pieces.get(turn[0])) == 3) {
								getPlayers().setWinner(turn[0]);
							} else if (Collections.frequency(Arrays.asList(getBoard().getRow(i)), pieces.get(turn[0])) == 3) {
								getPlayers().setWinner(turn[0]);
							} else if (Collections.frequency(Arrays.asList(getBoard().getRow(i)), null) == 0) {
								fullRows++;
							}
						}

						if (Collections.frequency(Arrays.asList(getBoard().getCrossSection(true)), pieces.get(turn[0])) == 3) {
							getPlayers().setWinner(turn[0]);
						} else if (Collections.frequency(Arrays.asList(getBoard().getCrossSection(false)), pieces.get(turn[0])) == 3) {
							getPlayers().setWinner(turn[0]);
						}

						try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
							ImageIO.write(getBoard().render(), "jpg", baos);

							if (getPlayers().getWinner() != null) {
								Main.getInfo().getAPI().removeEventListener(this);
								ShiroInfo.getGames().remove(getId());
								getTable().sendMessage(turn[0].getAsMention() + " venceu!").addFile(baos.toByteArray(), "board.jpg").queue();
								timeout.cancel(true);

								if (bet > 0) {
									Account uacc = AccountDAO.getAccount(getPlayers().getWinner().getId());
									Account tacc = AccountDAO.getAccount(getPlayers().getLoser().getId());

									uacc.addCredit(bet);
									tacc.removeCredit(bet);

									AccountDAO.saveAccount(uacc);
									AccountDAO.saveAccount(tacc);
								}
							} else if (fullRows == 3) {
								Main.getInfo().getAPI().removeEventListener(this);
								ShiroInfo.getGames().remove(getId());
								getTable().sendMessage("Temos um empate!").addFile(baos.toByteArray(), "board.jpg").queue();
								timeout.cancel(true);
							} else {
								turn[0] = getPlayers().nextTurn();
								if (message != null) message.delete().queue();
								message = getTable().sendMessage("Turno de " + turn[0].getAsMention()).addFile(baos.toByteArray(), "board.jpg").complete();
								timeout.cancel(true);
								timeout = getTable().sendMessage(":x: | Tempo expirado, por favor inicie outra sessão.").queueAfter(180, TimeUnit.SECONDS, ms -> {
									Main.getInfo().getAPI().removeEventListener(this);
									ShiroInfo.getGames().remove(getId());
								}, Helper::doNothing);
							}
						} catch (IOException e) {
							Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
						}
					} catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
						getTable().sendMessage(":x: | Coordenada inválida.").queue();
					}
				}
			}
		});
	}
}
