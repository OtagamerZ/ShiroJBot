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
import com.kuuhaku.handlers.games.framework.Tabletop;
import com.kuuhaku.handlers.games.tabletop.entity.Piece;
import com.kuuhaku.handlers.games.tabletop.entity.Player;
import com.kuuhaku.handlers.games.tabletop.entity.Spot;
import com.kuuhaku.handlers.games.tabletop.enums.Board;
import com.kuuhaku.handlers.games.tabletop.pieces.Circle;
import com.kuuhaku.handlers.games.tabletop.pieces.Cross;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
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
				players[0], new Circle(new Player(players[0], false)),
				players[1], new Cross(new Player(players[1], true))
		);
	}

	@Override
	public void execute(int bet) {
		getPlayers().lastOneStarts();
		message = getTable().sendMessage("Turno de " + getPlayers().getCurrent().getAsMention()).addFile(Helper.getBytes(getBoard().render()), "board.jpg").complete();

		Main.getInfo().getAPI().addEventListener(new ListenerAdapter() {
			{
				timeout = getTable().sendMessage(":x: | Tempo expirado, por favor inicie outra sessão.").queueAfter(180, TimeUnit.SECONDS, ms -> {
					Main.getInfo().getAPI().removeEventListener(this);
					Main.getInfo().getGames().remove(getId());
				}, Helper::doNothing);
			}

			@Override
			public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
				User u = event.getAuthor();
				TextChannel chn = event.getChannel();
				Message m = event.getMessage();

				if (chn.getId().equals(getTable().getId()) && u.getId().equals(getPlayers().getCurrent().getId()) && (m.getContentRaw().length() == 2 || Helper.equalsAny(m.getContentRaw(), "desistir", "forfeit", "ff", "surrender"))) {
					if (Helper.equalsAny(m.getContentRaw(), "desistir", "forfeit", "ff", "surrender")) {
						Main.getInfo().getAPI().removeEventListener(this);
						Main.getInfo().getGames().remove(getId());
						getTable().sendMessage(getPlayers().getCurrent().getAsMention() + " desistiu!").queue();
						getPlayers().nextTurn();
						getPlayers().setWinner();
						awardWinner(bet);
						timeout.cancel(true);
						return;
					}
					try {
						if (m.getContentRaw().length() != 2) return;
						Spot s = Spot.of(m.getContentRaw());

						if (getBoard().getLayout()[s.getY()][s.getX()] == null)
							getBoard().getLayout()[s.getY()][s.getX()] = pieces.get(u);
						else {
							getTable().sendMessage(":x: | Esta casa já está ocupada!").queue();
							return;
						}

						int fullRows = 0;
						for (int i = 0; i < getBoard().getLayout().length; i++) {
							if (Collections.frequency(Arrays.asList(getBoard().getColumn(i)), pieces.get(getPlayers().getCurrent())) == 3) {
								getPlayers().setWinner();
							} else if (Collections.frequency(Arrays.asList(getBoard().getRow(i)), pieces.get(getPlayers().getCurrent())) == 3) {
								getPlayers().setWinner();
							} else if (Collections.frequency(Arrays.asList(getBoard().getRow(i)), null) == 0) {
								fullRows++;
							}
						}

						if (Collections.frequency(Arrays.asList(getBoard().getCrossSection(true)), pieces.get(getPlayers().getCurrent())) == 3) {
							getPlayers().setWinner();
						} else if (Collections.frequency(Arrays.asList(getBoard().getCrossSection(false)), pieces.get(getPlayers().getCurrent())) == 3) {
							getPlayers().setWinner();
						}

						if (getPlayers().getWinner() != null) {
							Main.getInfo().getAPI().removeEventListener(this);
							Main.getInfo().getGames().remove(getId());
							getTable().sendMessage(getPlayers().getCurrent().getAsMention() + " venceu!").addFile(Helper.getBytes(getBoard().render()), "board.jpg").queue();
							timeout.cancel(true);

							awardWinner(bet);
						} else if (fullRows == 3) {
							Main.getInfo().getAPI().removeEventListener(this);
							Main.getInfo().getGames().remove(getId());
							getTable().sendMessage("Temos um empate!").addFile(Helper.getBytes(getBoard().render()), "board.jpg").queue();
							timeout.cancel(true);
						} else {
							getPlayers().nextTurn();
							getBoard().nextRound();
							if (message != null) message.delete().queue();
							message = getTable().sendMessage("Turno de " + getPlayers().getCurrent().getAsMention()).addFile(Helper.getBytes(getBoard().render()), "board.jpg").complete();
							timeout.cancel(true);
							if (getBoard().getRound() > 2)
								timeout = getTable().sendMessage(getPlayers().getCurrent().getAsMention() + " perdeu por W.O.!").queueAfter(180, TimeUnit.SECONDS, ms -> {
									Main.getInfo().getAPI().removeEventListener(this);
									Main.getInfo().getGames().remove(getId());
									getPlayers().setWinner(getPlayers().nextTurn());

									awardWinner(bet);
								}, Helper::doNothing);
							else refresh(this);
						}
					} catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
						getTable().sendMessage(":x: | Coordenada inválida.").queue();
					}
				}
			}
		});
	}

	private void refresh(Object listener) {
		if (timeout != null && !timeout.isCancelled()) timeout.cancel(true);
		timeout = getTable().sendMessage(":x: | Tempo expirado, por favor inicie outra sessão.").queueAfter(180, TimeUnit.SECONDS, ms -> {
			Main.getInfo().getAPI().removeEventListener(listener);
			Main.getInfo().getGames().remove(getId());
		}, Helper::doNothing);
	}
}
