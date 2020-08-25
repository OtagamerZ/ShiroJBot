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

package com.kuuhaku.handlers.games.tabletop.framework;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.io.Closeable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class Game implements Closeable {
	private final JDA handler;
	private final Board board;
	private final TextChannel channel;
	private Consumer<Message> onExpiration;
	private Consumer<Message> onWO;
	private Future<?> timeout;
	private int round = 0;
	private User current;

	public Game(JDA handler, Board board, TextChannel channel) {
		this.handler = handler;
		this.board = board;
		this.channel = channel;
		this.current = handler.getUserById(board.getPlayers().getCurrent().getId());
	}

	public void setActions(Consumer<Message> onExpiration, Consumer<Message> onWO) {
		this.onExpiration = onExpiration;
		this.onWO = onWO;
	}

	public abstract void start();

	public abstract boolean canInteract(GuildMessageReceivedEvent evt);

	public abstract void play(GuildMessageReceivedEvent evt);

	public void resetTimer() {
		if (timeout != null) timeout.cancel(true);
		if (round > 1)
			timeout = channel.sendMessage(current.getAsMention() + " perdeu por W.O.! (" + getRound() + " turnos)")
					.queueAfter(3, TimeUnit.MINUTES, onWO);
		else timeout = channel.sendMessage("❌ | Tempo expirado, por favor inicie outra sessão.")
				.queueAfter(3, TimeUnit.MINUTES, onExpiration);
		round++;
		current = handler.getUserById(board.getInGamePlayers().peekNext().getId());
		board.getPlayers().getNext();

		for (int y = 0; y < board.getMatrix().length; y++) {
			for (int x = 0; x < board.getMatrix().length; x++) {
				Piece p = board.getPieceOrDecoyAt(Spot.of(x, y));
				if (p instanceof Decoy && current.getId().equals(p.getOwnerId()))
					board.setPieceAt(Spot.of(x, y), null);
			}
		}
	}

	public JDA getHandler() {
		return handler;
	}

	public Board getBoard() {
		return board;
	}

	public int getRound() {
		return round;
	}

	public User getCurrent() {
		return current;
	}

	@Override
	public void close() {
		if (timeout != null) timeout.cancel(true);
	}
}
