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

package com.kuuhaku.handlers.games.tabletop.framework;

import com.github.ygimenez.model.ButtonWrapper;
import com.github.ygimenez.model.ThrowingConsumer;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.MatchDAO;
import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Shoukan;
import com.kuuhaku.model.common.DailyQuest;
import com.kuuhaku.model.enums.DailyTask;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.MatchHistory;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.model.persistent.MatchRound;
import com.kuuhaku.model.records.MatchInfo;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONObject;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.internal.entities.UserById;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class GlobalGame {
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private final ShardManager handler;
	private final Board board;
	private final GameChannel channel;
	private final JSONObject custom;
	private final MatchHistory history = new MatchHistory();
	private final Map<String, Double> divergence = new HashMap<>();
	private final boolean ranked;
	private final List<byte[]> frames = new ArrayList<>();
	private Consumer<Message> onExpiration;
	private Consumer<Message> onWO;
	private Future<?> timeout;
	private int round = 0;
	private boolean closed = false;
	private boolean wo = false;

	public GlobalGame(ShardManager handler, Board board, GameChannel channel, boolean ranked) {
		this.handler = handler;
		this.board = board;
		this.channel = channel;
		this.ranked = ranked;
		this.custom = null;
	}

	public GlobalGame(ShardManager handler, Board board, GameChannel channel, boolean ranked, JSONObject custom) {
		this.handler = handler;
		this.board = board;
		this.channel = channel;
		this.ranked = ranked;
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

		if (round > 0)
			timeout = executor.schedule(() ->
					channel.sendMessage(getCurrent().getAsMention() + " perdeu por W.O.! (" + getRound() + " turnos)")
							.queue(onWO), 3, TimeUnit.MINUTES);
		else timeout = executor.schedule(() ->
				channel.sendMessage("❌ | Tempo expirado, por favor inicie outra sessão.")
						.queue(onExpiration), 3, TimeUnit.MINUTES);
	}

	public void resetTimer(Shoukan shkn) {
		if (timeout != null) timeout.cancel(true);
		timeout = null;

		getCurrRound().setData(
				shkn.getHands().get(shkn.getCurrentSide()),
				shkn.getArena().getSlots().get(shkn.getCurrentSide())
		);

		round++;
		Player p = null;
		while (p == null || !p.isInGame()) {
			p = board.getPlayers().getNext();
		}

		if (round > 0)
			timeout = executor.schedule(() ->
					channel.sendMessage(getCurrent().getAsMention() + " perdeu por W.O.! (" + getRound() + " turnos)")
							.queue(onWO), 3, TimeUnit.MINUTES);
		else timeout = executor.schedule(() ->
				channel.sendMessage("❌ | Tempo expirado, por favor inicie outra sessão.")
						.queue(onExpiration), 3, TimeUnit.MINUTES);
	}

	public void resetTimerKeepTurn() {
		if (timeout != null) timeout.cancel(true);
		timeout = null;
		if (round > 0)
			timeout = executor.schedule(() ->
					channel.sendMessage(getCurrent().getAsMention() + " perdeu por W.O.! (" + getRound() + " turnos)")
							.queue(onWO), 3, TimeUnit.MINUTES);
		else timeout = executor.schedule(() ->
				channel.sendMessage("❌ | Tempo expirado, por favor inicie outra sessão.")
						.queue(onExpiration), 3, TimeUnit.MINUTES);
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
		return Helper.getOr(handler.getUserById(board.getPlayers().getCurrent().getId()), new UserById(0));
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

	public Map<String, Double> getDivergence() {
		return divergence;
	}

	public boolean isRanked() {
		return ranked;
	}

	public List<byte[]> getFrames() {
		return frames;
	}

	public abstract Map<Emoji, ThrowingConsumer<ButtonWrapper>> getButtons();

	public GameChannel getChannel() {
		return channel;
	}

	public MatchRound getCurrRound() {
		return history.getRound(round);
	}

	public boolean isOpen() {
		return !closed;
	}

	public void setWo() {
		this.wo = true;
	}

	public void close() {
		close(false);
	}

	public void close(boolean ignoreCustom) {
		if (closed) return;
		closed = true;
		if (timeout != null) timeout.cancel(true);
		timeout = null;
		executor.shutdownNow();

		if (round > 0 && (custom == null || ignoreCustom)) {
			history.setRanked(ranked);
			history.setWo(wo);
			MatchDAO.saveMatch(history);

			List<MatchInfo> stats = List.copyOf(history.getStats().values());
			for (MatchInfo stat : stats) {
				MatchMakingRating yourMMR = MatchMakingRatingDAO.getMMR(stat.id());
				long theirMMR = Helper.getAverageMMR(
						stats.stream()
								.filter(mi -> mi.side() != stat.side())
								.map(MatchInfo::id)
								.toArray(String[]::new)
				);

				long mmr = Math.round(250 * stat.manaEff() + (125 * stat.damageEff() + 125 * stat.sustainEff()));

				if (stat.winner()) {
					mmr *= Helper.clamp(stat.manaEff() / 2 + (stat.damageEff() + stat.sustainEff()) / 2, 0.5, 2);

					yourMMR.addMMR(mmr / (wo ? 2 : 1), theirMMR, ranked);
					yourMMR.addWin();
					if (ranked) yourMMR.increaseRankPoints(theirMMR);

					Account acc = AccountDAO.getAccount(yourMMR.getUid());
					if (acc.hasPendingQuest() && !wo) {
						Map<DailyTask, Integer> pg = acc.getDailyProgress();
						pg.merge(DailyTask.WINS_TASK, 1, Integer::sum);

						double div = divergence.getOrDefault(yourMMR.getUid(), -1d);
						if (div > -1) {
							DailyQuest dq = DailyQuest.getQuest(Long.parseLong(yourMMR.getUid()));
							if (div >= dq.getDivergence()) pg.merge(DailyTask.OFFMETA_TASK, 1, Integer::sum);
						}

						acc.setDailyProgress(pg);
						AccountDAO.saveAccount(acc);
					}
				} else {
					mmr /= Helper.clamp(stat.manaEff() / 2 + (stat.damageEff() + stat.sustainEff()) / 2, 0.5, 2);

					yourMMR.removeMMR(mmr * (wo ? 2 : 1), theirMMR, ranked);
					yourMMR.addLoss();
					if (ranked) yourMMR.decreaseRankPoints(theirMMR);
				}

				MatchMakingRatingDAO.saveMMR(yourMMR);
			}
		}
	}
}
