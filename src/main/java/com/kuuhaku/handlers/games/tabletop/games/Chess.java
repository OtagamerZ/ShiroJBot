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
import com.kuuhaku.handlers.games.tabletop.entity.Piece;
import com.kuuhaku.handlers.games.tabletop.entity.Player;
import com.kuuhaku.handlers.games.tabletop.entity.Spot;
import com.kuuhaku.handlers.games.tabletop.entity.Tabletop;
import com.kuuhaku.handlers.games.tabletop.enums.Board;
import com.kuuhaku.handlers.games.tabletop.pieces.*;
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
import java.util.ArrayList;
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
				players[0], new ArrayList<>() {{
					add(new Pawn(new Player(players[0], false)));
					add(new Pawn(new Player(players[0], false)));
					add(new Pawn(new Player(players[0], false)));
					add(new Pawn(new Player(players[0], false)));
					add(new Pawn(new Player(players[0], false)));
					add(new Pawn(new Player(players[0], false)));
					add(new Pawn(new Player(players[0], false)));
					add(new Pawn(new Player(players[0], false)));

					add(new Rook(new Player(players[0], false)));
					add(new Knight(new Player(players[0], false)));
					add(new Bishop(new Player(players[0], false)));
					add(new Queen(new Player(players[0], false)));
					add(new King(new Player(players[0], false)));
					add(new Bishop(new Player(players[0], false)));
					add(new Knight(new Player(players[0], false)));
					add(new Rook(new Player(players[0], false)));
				}},
				players[1], new ArrayList<>() {{
					add(new Pawn(new Player(players[1], true)));
					add(new Pawn(new Player(players[1], true)));
					add(new Pawn(new Player(players[1], true)));
					add(new Pawn(new Player(players[1], true)));
					add(new Pawn(new Player(players[1], true)));
					add(new Pawn(new Player(players[1], true)));
					add(new Pawn(new Player(players[1], true)));
					add(new Pawn(new Player(players[1], true)));

					add(new Rook(new Player(players[1], true)));
					add(new Knight(new Player(players[1], true)));
					add(new Bishop(new Player(players[1], true)));
					add(new Queen(new Player(players[1], true)));
					add(new King(new Player(players[1], true)));
					add(new Bishop(new Player(players[1], true)));
					add(new Knight(new Player(players[1], true)));
					add(new Rook(new Player(players[1], true)));
				}}
		);
		getBoard().setPattern(new Piece[][]{
				{pieces.get(players[0]).get(8), pieces.get(players[0]).get(9), pieces.get(players[0]).get(10), pieces.get(players[0]).get(11), pieces.get(players[0]).get(12), pieces.get(players[0]).get(13), pieces.get(players[0]).get(14), pieces.get(players[0]).get(15)},
				{pieces.get(players[0]).get(0), pieces.get(players[0]).get(1), pieces.get(players[0]).get(2), pieces.get(players[0]).get(3), pieces.get(players[0]).get(4), pieces.get(players[0]).get(5), pieces.get(players[0]).get(6), pieces.get(players[0]).get(7)},
				{/*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null},
				{/*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null},
				{/*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null},
				{/*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null, /*---------------------*/null},
				{pieces.get(players[1]).get(0), pieces.get(players[1]).get(1), pieces.get(players[1]).get(2), pieces.get(players[1]).get(3), pieces.get(players[1]).get(4), pieces.get(players[1]).get(5), pieces.get(players[1]).get(6), pieces.get(players[1]).get(7)},
				{pieces.get(players[1]).get(8), pieces.get(players[1]).get(9), pieces.get(players[1]).get(10), pieces.get(players[1]).get(11), pieces.get(players[1]).get(12), pieces.get(players[1]).get(13), pieces.get(players[1]).get(14), pieces.get(players[1]).get(15)}
		});
	}

	@Override
	public void execute() {
		for (int y = 0; y < getBoard().getLayout().length; y++) {
			for (int x = 0; x < getBoard().getLayout().length; x++) {
				Spot pos = Spot.of(x, y);
				Piece p = getBoard().getSpot(pos);
				if (p != null) p.move(getBoard(), pos);
			}
		}
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

				if (chn.getId().equals(getTable().getId()) && u.getId().equals(turn[0].getId()) && m.getContentRaw().length() == 5) {
					try {
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
						} else if (!p.getOwner().getUser().getId().equals(turn[0].getId())) {
							getTable().sendMessage(":x: | Essa peça não é sua!").queue();
							return;
						} else if (!p.move(getBoard(), to)) {
							getTable().sendMessage(":x: | Movimento inválido!").queue();
							return;
						}

						boolean foundKing = false;
						for (int y = 0; y < getBoard().getLayout().length; y++) {
							for (int x = 0; x < getBoard().getLayout().length; x++) {
								Piece pc = getBoard().getSpot(Spot.of(x, y));
								if (pc instanceof King && !pc.getOwner().equals(p.getOwner())) {
									foundKing = true;
									break;
								}
							}
						}

						if (!foundKing) getPlayers().setWinner(turn[0]);

						try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
							ImageIO.write(getBoard().render(), "jpg", baos);

							if (getPlayers().getWinner() != null) {
								Main.getInfo().getAPI().removeEventListener(this);
								ShiroInfo.getGames().remove(getId());
								getTable().sendMessage(turn[0].getAsMention() + " venceu, CHECKMATE!! (" + getBoard().getRound() + " turnos)").addFile(baos.toByteArray(), "board.jpg").complete();
								timeout.cancel(true);
							} else {
								turn[0] = getPlayers().nextTurn();
								if (message != null) message.delete().queue();
								getBoard().nextRound();
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
						getTable().sendMessage(":x: | Movimento inválido.").queue();
					}
				}
			}
		});
	}
}
