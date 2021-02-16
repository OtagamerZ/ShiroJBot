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

import com.github.ygimenez.model.ThrowingBiConsumer;
import com.kuuhaku.model.persistent.MatchHistory;
import com.kuuhaku.model.persistent.MatchRound;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.json.JSONObject;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class Game {
	private final ShardManager handler;
	private final Board board;
	private final TextChannel channel;
	private final JSONObject custom;
	private final MatchHistory history = new MatchHistory();
	private Consumer<Message> onExpiration;
	private Consumer<Message> onWO;
	private Future<?> timeout;
	private int round = 0;
	private User current;
	private boolean closed = false;

	public Game(ShardManager handler, Board board, TextChannel channel) {
		this.handler = handler;
		this.board = board;
		this.channel = channel;
		this.current = handler.getUserById(board.getPlayers().getCurrent().getId());
		this.custom = null;
	}

	public Game(ShardManager handler, Board board, TextChannel channel, JSONObject custom) {
		this.handler = handler;
		this.board = board;
		this.channel = channel;
		this.current = handler.getUserById(board.getPlayers().getCurrent().getId());
		this.custom = custom;
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
		timeout = null;
		round++;
		Player p = null;
		while (p == null || !p.isInGame()) {
			p = board.getPlayers().getNext();
		}
		current = handler.getUserById(p.getId());
		assert current != null;
		if (round > 0)
			timeout = channel.sendMessage(current.getAsMention() + " perdeu por W.O.! (" + getRound() + " turnos)")
					.queueAfter(3, TimeUnit.MINUTES, s -> {
						onWO.accept(s);
						closed = true;
					});
		else timeout = channel.sendMessage("❌ | Tempo expirado, por favor inicie outra sessão.")
				.queueAfter(3, TimeUnit.MINUTES, s -> {
					onExpiration.accept(s);
					closed = true;
				});

		for (int y = 0; y < board.getMatrix().length; y++) {
			for (int x = 0; x < board.getMatrix().length; x++) {
				Piece pc = board.getPieceOrDecoyAt(Spot.of(x, y));
				if (pc instanceof Decoy && current.getId().equals(pc.getOwnerId()))
					board.setPieceAt(Spot.of(x, y), null);
			}
		}
	}

	public void resetTimerKeepTurn() {
		if (timeout != null) timeout.cancel(true);
		if (round > 0)
			timeout = channel.sendMessage(current.getAsMention() + " perdeu por W.O.! (" + getRound() + " turnos)")
					.queueAfter(3, TimeUnit.MINUTES, s -> {
						onWO.accept(s);
						closed = true;
					});
		else timeout = channel.sendMessage("❌ | Tempo expirado, por favor inicie outra sessão.")
				.queueAfter(3, TimeUnit.MINUTES, s -> {
					onExpiration.accept(s);
					closed = true;
				});
	}

	public ShardManager getHandler() {
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

	public User getPlayerById(String id) {
		return handler.getUserById(id);
	}

	public JSONObject getCustom() {
		return custom;
	}

	public MatchHistory getHistory() {
		return history;
	}

	public abstract Map<String, ThrowingBiConsumer<Member, Message>> getButtons();

	public MatchRound getCurrRound() {
		return history.getRound(round);
	}

	public boolean isOpen() {
		return !closed;
	}

	public void close() {
		if (timeout != null) timeout.cancel(true);
		timeout = null;

		closed = true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Game game = (Game) o;
		return handler.equals(game.handler) &&
			   board.equals(game.board) &&
			   channel.equals(game.channel) &&
			   custom.equals(game.custom);
	}

	@Override
	public int hashCode() {
		return Objects.hash(handler, board, channel, custom);
	}
}
