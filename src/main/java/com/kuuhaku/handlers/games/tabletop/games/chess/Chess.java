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

package com.kuuhaku.handlers.games.tabletop.games.chess;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.handlers.games.tabletop.framework.Board;
import com.kuuhaku.handlers.games.tabletop.framework.Game;
import com.kuuhaku.handlers.games.tabletop.framework.Piece;
import com.kuuhaku.handlers.games.tabletop.framework.Spot;
import com.kuuhaku.handlers.games.tabletop.framework.enums.BoardSize;
import com.kuuhaku.handlers.games.tabletop.games.chess.pieces.*;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class Chess extends Game {
	private final Map<String, List<Piece>> pieces;
	private final TextChannel channel;
	private Message message;
	private final SimpleMessageListener listener = new SimpleMessageListener() {
		@Override
		public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
			if (canInteract(event)) play(event);
		}
	};

	public Chess(ShardManager handler, TextChannel channel, int bet, User... players) {
		super(handler, new Board(BoardSize.S_8X8, bet, Arrays.stream(players).map(User::getId).toArray(String[]::new)), channel);
		this.channel = channel;
		this.pieces = Map.of(
				players[0].getId(), List.of(
						new EligibleRook(players[0].getId(), false, "pieces/rook.png"),
						new Knight(players[0].getId(), false, "pieces/knight.png"),
						new Bishop(players[0].getId(), false, "pieces/bishop.png"),
						new Queen(players[0].getId(), false, "pieces/queen.png"),
						new EligibleKing(players[0].getId(), false, "pieces/king.png"),
						new Pawn(players[0].getId(), false, "pieces/pawn.png")
				),
				players[1].getId(), List.of(
						new EligibleRook(players[1].getId(), true, "pieces/rook.png"),
						new Knight(players[1].getId(), true, "pieces/knight.png"),
						new Bishop(players[1].getId(), true, "pieces/bishop.png"),
						new Queen(players[1].getId(), true, "pieces/queen.png"),
						new EligibleKing(players[1].getId(), true, "pieces/king.png"),
						new Pawn(players[1].getId(), true, "pieces/pawn.png")
				)
		);
		getBoard().setMatrix(new Piece[][]{
				new Piece[]{
						pieces.get(players[0].getId()).get(0),
						pieces.get(players[0].getId()).get(1),
						pieces.get(players[0].getId()).get(2),
						pieces.get(players[0].getId()).get(3),
						pieces.get(players[0].getId()).get(4),
						pieces.get(players[0].getId()).get(2),
						pieces.get(players[0].getId()).get(1),
						pieces.get(players[0].getId()).get(0)
				},
				Collections.nCopies(8, pieces.get(players[0].getId()).get(5)).toArray(Piece[]::new),
				new Piece[]{null, null, null, null, null, null, null, null},
				new Piece[]{null, null, null, null, null, null, null, null},
				new Piece[]{null, null, null, null, null, null, null, null},
				new Piece[]{null, null, null, null, null, null, null, null},
				Collections.nCopies(8, pieces.get(players[1].getId()).get(5)).toArray(Piece[]::new),
				new Piece[]{
						pieces.get(players[1].getId()).get(0),
						pieces.get(players[1].getId()).get(1),
						pieces.get(players[1].getId()).get(2),
						pieces.get(players[1].getId()).get(3),
						pieces.get(players[1].getId()).get(4),
						pieces.get(players[1].getId()).get(2),
						pieces.get(players[1].getId()).get(1),
						pieces.get(players[1].getId()).get(0)
				},
		});

		setActions(
				s -> {
					close();
					channel.sendFile(Helper.getBytes(getBoard().render()), "board.jpg")
							.queue(msg -> {
								if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
							});
				},
				s -> {
					getBoard().awardWinner(this, getBoard().getPlayers().getNext().getId());
					channel.sendFile(Helper.getBytes(getBoard().render()), "board.jpg")
							.queue(msg -> {
								if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
							});
				}
		);
	}

	@Override
	public void start() {
		channel.sendMessage(getCurrent().getAsMention() + " você começa!").addFile(Helper.getBytes(getBoard().render()), "board.jpg")
				.queue(s -> {
					this.message = s;
					Main.getInfo().getShiroEvents().addHandler(channel.getGuild(), listener);
					Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
				});
	}

	@Override
	public boolean canInteract(GuildMessageReceivedEvent evt) {
		Predicate<GuildMessageReceivedEvent> condition = e -> e.getChannel().getId().equals(channel.getId());

		return condition
				.and(e -> e.getAuthor().getId().equals(getCurrent().getId()))
				.and(e -> e.getMessage().getContentRaw().replace(" ", "").length() == 4)
				.and(e -> {
					char[] chars = e.getMessage().getContentRaw().replace(" ", "").toCharArray();
					return Character.isLetter(chars[0])
						   && Character.isDigit(chars[1])
						   && Character.isLetter(chars[2])
						   && Character.isDigit(chars[3]);
				})
				.and(e -> !isClosed())
				.test(evt);
	}

	@Override
	public void play(GuildMessageReceivedEvent evt) {
		Message message = evt.getMessage();
		String[] command = {message.getContentRaw().substring(0, 2), message.getContentRaw().substring(2)};

		try {
			Spot from = Spot.of(command[0]);
			Spot to = Spot.of(command[1]);

			ChessPiece toMove = (ChessPiece) getBoard().getPieceAt(from);
			ChessPiece atSpot = (ChessPiece) getBoard().getPieceAt(to);
			if (toMove == null) {
				channel.sendMessage("❌ | Não há nenhuma peça nessa casa!").queue(null, Helper::doNothing);
				return;
			} else if (!toMove.getOwnerId().equals(evt.getAuthor().getId())) {
				channel.sendMessage("❌ | Essa peça não é sua!").queue(null, Helper::doNothing);
				return;
			}

			String winner = null;
			if (toMove.validate(getBoard(), from, to)) {
				if (atSpot instanceof King) {
					winner = getCurrent().getId();
				}

				if (toMove instanceof Pawn) {
					if (toMove.isWhite() && to.getY() == 0) {
						toMove = (ChessPiece) pieces.get(evt.getAuthor().getId()).get(3);
					} else if (to.getY() == 7) {
						toMove = (ChessPiece) pieces.get(evt.getAuthor().getId()).get(3);
					}
				} else if (toMove instanceof EligibleKing) {
					toMove = new King(toMove.getOwnerId(), toMove.isWhite(), toMove.getIconPath());
				} else if (toMove instanceof EligibleRook) {
					toMove = new Rook(toMove.getOwnerId(), toMove.isWhite(), toMove.getIconPath());
				}

				getBoard().setPieceAt(from, null);
				getBoard().setPieceAt(to, toMove);
			} else {
				channel.sendMessage("❌ | Movimento inválido!").queue(null, Helper::doNothing);
				return;
			}

			int remaining = 0;
			for (Piece[] pieces : getBoard().getMatrix()) {
				for (Piece p : pieces) {
					if (p != null) remaining++;
				}
			}

			if (winner != null) {
				getBoard().awardWinner(this, getCurrent().getId());
				channel.sendMessage(getCurrent().getAsMention() + " venceu! (" + getRound() + " turnos)")
						.addFile(Helper.getBytes(getBoard().render()), "board.jpg")
						.queue(msg -> {
							if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
						});
			} else if (remaining == 2) {
				close();
				channel.sendMessage("Temos um empate! (" + getRound() + " turnos)")
						.addFile(Helper.getBytes(getBoard().render()), "board.jpg")
						.queue(msg -> {
							if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
						});
			} else {
				resetTimer();
				channel.sendMessage("Turno de " + getCurrent().getAsMention())
						.addFile(Helper.getBytes(getBoard().render()), "board.jpg")
						.queue(s -> {
							if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
							this.message = s;
							Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						});
			}
		} catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
			channel.sendMessage("❌ | Coordenada inválida.").queue(null, Helper::doNothing);
		}
	}

	@Override
	public Map<String, BiConsumer<Member, Message>> getButtons() {
		AtomicReference<String> hash = new AtomicReference<>(Helper.generateHash(this));
		ShiroInfo.getHashes().add(hash.get());

		Map<String, BiConsumer<Member, Message>> buttons = new LinkedHashMap<>();
		buttons.put("\uD83C\uDFF3️", (mb, ms) -> {
			if (!ShiroInfo.getHashes().remove(hash.get())) return;
			getBoard().awardWinner(this, getBoard().getPlayers().getNext().getId());
			close();
			channel.sendMessage(getCurrent().getAsMention() + " desistiu! (" + getRound() + " turnos)")
					.addFile(Helper.getBytes(getBoard().render()), "board.jpg")
					.queue(msg -> {
						if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
					});
		});

		return buttons;
	}

	@Override
	public void close() {
		listener.close();
		super.close();
	}
}