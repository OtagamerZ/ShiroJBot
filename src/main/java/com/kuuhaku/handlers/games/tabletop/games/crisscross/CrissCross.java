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

package com.kuuhaku.handlers.games.tabletop.games.crisscross;

import com.kuuhaku.handlers.games.tabletop.framework.Board;
import com.kuuhaku.handlers.games.tabletop.framework.Game;
import com.kuuhaku.handlers.games.tabletop.framework.Piece;
import com.kuuhaku.handlers.games.tabletop.framework.Spot;
import com.kuuhaku.handlers.games.tabletop.framework.enums.BoardSize;
import com.kuuhaku.handlers.games.tabletop.framework.enums.Neighbor;
import com.kuuhaku.handlers.games.tabletop.games.crisscross.pieces.Circle;
import com.kuuhaku.handlers.games.tabletop.games.crisscross.pieces.Cross;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;

public class CrissCross extends Game {
	private final Map<String, Piece> pieces;
	private final TextChannel channel;
	private Message message;
	private final ListenerAdapter listener = new ListenerAdapter() {
		@Override
		public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
			if (canInteract(event)) play(event);
		}
	};

	public CrissCross(JDA handler, TextChannel channel, int bet, User... players) {
		super(handler, new Board(BoardSize.S_3X3, bet, Arrays.stream(players).map(User::getId).toArray(String[]::new)), channel);
		this.channel = channel;
		this.pieces = Map.of(
				players[0].getId(), new Circle(players[0].getId(), false, "pieces/circle.png"),
				players[1].getId(), new Cross(players[1].getId(), true, "pieces/cross.png")
		);

		setActions(
				s -> close(),
				s -> getBoard().awardWinner(this, getBoard().getPlayers().getNext().getId())
		);
	}

	@Override
	public void start() {
		resetTimer();
		message = channel.sendMessage(getCurrent().getAsMention() + " você começa!").addFile(Helper.getBytes(getBoard().render()), "board.jpg").complete();
		getHandler().addEventListener(listener);
	}

	@Override
	public boolean canInteract(GuildMessageReceivedEvent evt) {
		Predicate<GuildMessageReceivedEvent> condition = e -> e.getChannel().getId().equals(channel.getId());

		return condition
				.and(e -> e.getAuthor().getId().equals(getCurrent().getId()))
				.and(e -> e.getMessage().getContentRaw().length() == 2)
				.and(e -> {
					char[] chars = e.getMessage().getContentRaw().toCharArray();
					if (e.getMessage().getContentRaw().equalsIgnoreCase("ff")) return true;
					else return Character.isLetter(chars[0]) && Character.isDigit(chars[1]);
				})
				.test(evt);
	}

	@Override
	public void play(GuildMessageReceivedEvent evt) {
		Message message = evt.getMessage();
		String command = message.getContentRaw();

		if (command.equalsIgnoreCase("ff")) {
			channel.sendMessage(getCurrent().getAsMention() + " desistiu! (" + getRound() + " turnos)").queue();
			getBoard().awardWinner(this, getBoard().getPlayers().getNext().getId());
			close();
			return;
		}

		try {
			Spot s = Spot.of(command);

			if (getBoard().getPieceAt(s) == null) {
				getBoard().setPieceAt(s, pieces.get(getCurrent().getId()));
			} else {
				channel.sendMessage("❌ | Essa casa já está ocupada!").queue();
				return;
			}

			String winner = null;
			int fullRows = 0;
			for (int i = 0; i < getBoard().getSize().getHeight(); i++) {
				if (Collections.frequency(Arrays.asList(getBoard().getColumn(i)), pieces.get(getCurrent().getId())) == 3) {
					winner = getCurrent().getId();
				} else if (Collections.frequency(Arrays.asList(getBoard().getRow(i)), pieces.get(getCurrent().getId())) == 3) {
					winner = getCurrent().getId();
				} else if (Collections.frequency(Arrays.asList(getBoard().getRow(i)), null) == 0) {
					fullRows++;
				}
			}

			if (Collections.frequency(Arrays.asList(getBoard().getLine(Spot.of(0, 0), Neighbor.LOWER_RIGHT, true)), pieces.get(getCurrent().getId())) == 3) {
				winner = getCurrent().getId();
			} else if (Collections.frequency(Arrays.asList(getBoard().getLine(Spot.of(2, 0), Neighbor.LOWER_LEFT, true)), pieces.get(getCurrent().getId())) == 3) {
				winner = getCurrent().getId();
			}

			if (winner != null) {
				channel.sendMessage(getCurrent().getAsMention() + " venceu! (" + getRound() + " turnos)").addFile(Helper.getBytes(getBoard().render()), "board.jpg").queue();
				getBoard().awardWinner(this, winner);
			} else if (fullRows == 3) {
				channel.sendMessage("Temos um empate!").addFile(Helper.getBytes(getBoard().render()), "board.jpg").queue();
				close();
			} else {
				resetTimer();
				this.message.delete().queue();
				this.message = channel.sendMessage("Turno de " + getCurrent().getAsMention()).addFile(Helper.getBytes(getBoard().render()), "board.jpg").complete();
			}
		} catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
			channel.sendMessage("❌ | Coordenada inválida.").queue();
		}
	}

	@Override
	public void close() {
		super.close();
		getHandler().removeEventListener(listener);
	}
}