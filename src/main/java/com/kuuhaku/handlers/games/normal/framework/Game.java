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

package com.kuuhaku.handlers.games.normal.framework;

import com.github.ygimenez.model.ButtonWrapper;
import com.github.ygimenez.model.ThrowingConsumer;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.internal.entities.UserById;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class Game {
	private final ShardManager handler;
	private final Table table;
	private final TextChannel channel;
	private Consumer<Message> onExpiration;
	private Consumer<Message> onWO;
	private Future<?> timeout;
	private int round = 0;
	private boolean closed = false;
	private int time = 180;

	public Game(ShardManager handler, Table table, TextChannel channel) {
		this.handler = handler;
		this.table = table;
		this.channel = channel;
	}

	public void setActions(Consumer<Message> onExpiration, Consumer<Message> onWO) {
		this.onExpiration = onExpiration;
		this.onWO = onWO;
	}

	public void setTime(int time) {
		this.time = time;
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
			p = table.getPlayers().getNext();
		}

		if (round > 0)
			timeout = channel.sendMessage(getCurrent().getAsMention() + " perdeu por W.O.! (" + getRound() + " turnos)")
					.queueAfter(time, TimeUnit.SECONDS, onWO);
		else timeout = channel.sendMessage("❌ | Tempo expirado, por favor inicie outra sessão.")
				.queueAfter(time, TimeUnit.SECONDS, onExpiration);
	}

	public void resetTimerKeepTurn() {
		if (timeout != null) timeout.cancel(true);
		if (round > 0)
			timeout = channel.sendMessage(getCurrent().getAsMention() + " perdeu por W.O.! (" + getRound() + " turnos)")
					.queueAfter(time, TimeUnit.SECONDS, onWO);
		else timeout = channel.sendMessage("❌ | Tempo expirado, por favor inicie outra sessão.")
				.queueAfter(time, TimeUnit.SECONDS, onExpiration);
	}

	public ShardManager getHandler() {
		return handler;
	}

	public Table getTable() {
		return table;
	}

	public int getRound() {
		return round;
	}

	public User getCurrent() {
		return Helper.getOr(handler.getUserById(table.getPlayers().getCurrent().getId()), new UserById(0));
	}

	public User getPlayerById(String id) {
		return handler.getUserById(id);
	}

	public abstract Map<Emoji, ThrowingConsumer<ButtonWrapper>> getButtons();

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
			   table.equals(game.table) &&
			   channel.equals(game.channel);
	}

	@Override
	public int hashCode() {
		return Objects.hash(handler, table, channel);
	}
}
