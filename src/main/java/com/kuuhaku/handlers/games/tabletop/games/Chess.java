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
import com.kuuhaku.handlers.games.tabletop.entity.PieceBox;
import com.kuuhaku.handlers.games.tabletop.entity.Spot;
import com.kuuhaku.handlers.games.tabletop.enums.Board;
import com.kuuhaku.handlers.games.tabletop.pieces.King;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Chess extends Tabletop {
	private final Map<User, List<Piece>> pieces;
	private Future<?> timeout;
	private Message message = null;

	public Chess(TextChannel table, String id, User... players) {
		super(table, Board.SIZE_8X8(), id, players);
		pieces = Map.of(
				players[0], new PieceBox(players[0], false).getPieces(),
				players[1], new PieceBox(players[1], true).getPieces()
		);
		getBoard().setPattern(new Piece[][]{
				{pieces.get(players[0]).get(8), pieces.get(players[0]).get(9), pieces.get(players[0]).get(10), pieces.get(players[0]).get(11), pieces.get(players[0]).get(12), pieces.get(players[0]).get(13), pieces.get(players[0]).get(14), pieces.get(players[0]).get(15)},
				{pieces.get(players[0]).get(0), pieces.get(players[0]).get(1), pieces.get(players[0]).get(2), pieces.get(players[0]).get(3), pieces.get(players[0]).get(4), pieces.get(players[0]).get(5), pieces.get(players[0]).get(6), pieces.get(players[0]).get(7)},
				{/*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null},
				{/*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null},
				{/*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null},
				{/*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null},
				{null/*pieces.get(players[1]).get(0)*/, pieces.get(players[1]).get(1), pieces.get(players[1]).get(2), null/*pieces.get(players[1]).get(3)*/, pieces.get(players[1]).get(4), pieces.get(players[1]).get(5), pieces.get(players[1]).get(6), pieces.get(players[1]).get(7)},
				{pieces.get(players[1]).get(8), pieces.get(players[1]).get(9), pieces.get(players[1]).get(10), pieces.get(players[1]).get(11), pieces.get(players[1]).get(12), pieces.get(players[1]).get(13), pieces.get(players[1]).get(14), pieces.get(players[1]).get(15)}

		});
	}

	@Override
	public void execute(int bet) {
		for (int y = 0; y < getBoard().getLayout().length; y++) {
			for (int x = 0; x < getBoard().getLayout().length; x++) {
				Spot pos = Spot.of(x, y);
				Piece p = getBoard().getSpot(pos);
				if (p != null) p.move(getBoard(), pos);
			}
		}

		getPlayers().lastOneStarts();
		message = getTable().sendMessage("Turno de " + getPlayers().getCurrent().getAsMention()).addFile(Helper.getBytes(getBoard().render()), "board.jpg").complete();
		Main.getInfo().getAPI().addEventListener(new ListenerAdapter() {
			{
				refresh(this);
			}

			@Override
			public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
				User u = event.getAuthor();
				TextChannel chn = event.getChannel();
				Message m = event.getMessage();

				if (chn.getId().equals(getTable().getId()) && u.getId().equals(getPlayers().getCurrent().getId()) && (m.getContentRaw().length() == 5 || Helper.equalsAny(m.getContentRaw(), "desistir", "forfeit", "ff", "surrender"))) {
					if (Helper.equalsAny(m.getContentRaw(), "desistir", "forfeit", "ff", "surrender")) {
						Main.getInfo().getAPI().removeEventListener(this);
						ShiroInfo.getGames().remove(getId());
						getTable().sendMessage(getPlayers().getCurrent().getAsMention() + " desistiu! (" + getBoard().getRound() + " turnos)").queue();
						getPlayers().nextTurn();
						getPlayers().setWinner();
						//awardWinner(bet);
						timeout.cancel(true);
						return;
					}
					try {
						if (m.getContentRaw().length() != 5) return;
						String[] coords = m.getContentRaw().split(" ");

						if (coords.length < 2) {
							getTable().sendMessage(":x: | Você precisa especificar duas coordenadas, uma para escolher a peça e outra pra definir o destino dela.").queue();
							return;
						}

						Spot from = Spot.of(coords[0]);
						Spot to = Spot.of(coords[1]);
						Piece p = getBoard().getSpot(from);

						if (p == null) {
							getTable().sendMessage(":x: | Não há nenhuma peça nessa coordenada!").queue();
							return;
						} else if (!p.getOwner().getUser().getId().equals(getPlayers().getCurrent().getId())) {
							getTable().sendMessage(":x: | Essa peça não é sua!").queue();
							return;
						} else if (!p.move(getBoard(), to)) {
							getTable().sendMessage(":x: | Movimento inválido!").queue();
							return;
						}

						Piece pc;
						boolean foundKing = false;
						for (int y = 0; y < getBoard().getLayout().length; y++) {
							for (int x = 0; x < getBoard().getLayout().length; x++) {
								pc = getBoard().getSpot(Spot.of(x, y));
								if (pc instanceof King && !pc.getOwner().equals(p.getOwner())) {
									foundKing = true;
									break;
								}
							}
							if (foundKing) break;
						}

						if (!foundKing) getPlayers().setWinner();

						if (getPlayers().getWinner() != null) {
							Main.getInfo().getAPI().removeEventListener(this);
							ShiroInfo.getGames().remove(getId());
							getTable().sendMessage(getPlayers().getCurrent().getAsMention() + " venceu, CHECKMATE!! (" + getBoard().getRound() + " turnos)").addFile(Helper.getBytes(getBoard().render()), "board.jpg").queue();
							timeout.cancel(true);
						} else {
							getPlayers().nextTurn();
							getBoard().nextRound();
							if (message != null) message.delete().queue();
							message = getTable().sendMessage("Turno de " + getPlayers().getCurrent().getAsMention()).addFile(Helper.getBytes(getBoard().render()), "board.jpg").complete();
							timeout.cancel(true);
							if (getBoard().getRound() > 2)
								timeout = getTable().sendMessage(getPlayers().getCurrent().getAsMention() + " perdeu por W.O.! (" + getBoard().getRound() + " turnos)").queueAfter(180, TimeUnit.SECONDS, ms -> {
									Main.getInfo().getAPI().removeEventListener(this);
									ShiroInfo.getGames().remove(getId());
								}, Helper::doNothing);
							else refresh(this);
						}
					} catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
						getTable().sendMessage(":x: | Movimento inválido.").queue();
					}
				}
			}
		});
	}

	private void refresh(Object listener) {
		if (timeout != null && !timeout.isCancelled()) timeout.cancel(true);
		timeout = getTable().sendMessage(":x: | Tempo expirado, por favor inicie outra sessão.").queueAfter(180, TimeUnit.SECONDS, ms -> {
			Main.getInfo().getAPI().removeEventListener(listener);
			ShiroInfo.getGames().remove(getId());
		}, Helper::doNothing);
	}
}
