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

package com.kuuhaku.handlers.games.normal.games;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.ButtonWrapper;
import com.github.ygimenez.model.ThrowingConsumer;
import com.kuuhaku.Main;
import com.kuuhaku.events.SimpleMessageListener;
import com.kuuhaku.handlers.games.normal.framework.Game;
import com.kuuhaku.handlers.games.normal.framework.Player;
import com.kuuhaku.handlers.games.normal.framework.Table;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class Shiritori extends Game {
	private final TextChannel channel;
	private final SimpleMessageListener listener = new SimpleMessageListener() {
		@Override
		public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
			if (canInteract(event)) play(event);
		}
	};
	private final File list = Helper.getResourceAsFile(this.getClass(), "shiritori/ptBR_dict.txt");
	private final Set<String> used = new HashSet<>();
	private Message message = null;
	private String word;
	private boolean suddenDeath = false;

	public Shiritori(ShardManager handler, TextChannel channel, int bet, User... players) {
		super(handler, new Table(bet, Arrays.stream(players).map(User::getId).toArray(String[]::new)), channel);
		this.channel = channel;
		setTime(60);

		setActions(
				s -> close(),
				s -> {
					getTable().leaveGame();
					resetTimer();

					if (getTable().getInGamePlayers().size() == 1) {
						getTable().awardWinner(this, getCurrent().getId());
						close();
						channel.sendMessage(getCurrent().getAsMention() + " é o último jogador na mesa, temos um vencedor!! (" + getRound() + " turnos)")
								.queue(msg -> {
									if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
								});
					} else {
						channel.sendMessage(getCurrent().getAsMention() + " agora é sua vez (palavra atual: " + getHighlightedWord() + ").")
								.queue(msg -> {
									if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
									this.message = msg;
									Pages.buttonize(msg, getButtons(), ShiroInfo.USE_BUTTONS, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
								});
					}
				}
		);
	}

	@Override
	public void start() {
		channel.sendMessage(getCurrent().getAsMention() + " você começa, digite uma palavra para começar!")
				.queue(s -> {
					this.message = s;
					ShiroInfo.getShiroEvents().addHandler(channel.getGuild(), listener);
					Main.getInfo().setGameInProgress(listener, getTable().getPlayers().stream().map(Player::getId).toArray(String[]::new));
					Pages.buttonize(s, getButtons(), ShiroInfo.USE_BUTTONS, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
				});
	}

	@Override
	public boolean canInteract(GuildMessageReceivedEvent evt) {
		Predicate<GuildMessageReceivedEvent> condition = e -> e.getChannel().getId().equals(channel.getId());

		return condition
				.and(e -> e.getMessage().getContentRaw().length() > 2)
				.and(e -> e.getAuthor().getId().equals(getCurrent().getId()))
				.and(e -> !e.getMessage().getContentRaw().contains(" "))
				.and(e -> isOpen())
				.test(evt);
	}

	@Override
	public synchronized void play(GuildMessageReceivedEvent evt) {
		Message message = evt.getMessage();
		String command = StringUtils.stripAccents(message.getContentRaw().toLowerCase(Locale.ROOT));

		if (word != null && !word.endsWith(command.substring(0, 2))) {
			channel.sendMessage("❌ | Palavra não permitida. Só são permitidas palavras onde as duas primeiras letras sejam as mesmas que as 2 últimas letras da palavra anterior (ex: maca**co** -> **co**lmeia).").queue();
		} else if (used.contains(command)) {
			channel.sendMessage(":negative_squared_cross_mark: | " + getCurrent().getAsMention() + " escreveu uma palavra já usada, ESTÁ FORA!").queue(null, Helper::doNothing);
			getTable().leaveGame();
			resetTimer();

			if (getTable().getInGamePlayers().size() == 1) {
				getTable().awardWinner(this, getCurrent().getId());
				close();
				channel.sendMessage(getCurrent().getAsMention() + " é o último jogador na mesa, temos um vencedor!! (" + getRound() + " turnos)")
						.queue(msg -> {
							if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
						});
			} else {
				channel.sendMessage(getCurrent().getAsMention() + " agora é sua vez (palavra atual: " + getHighlightedWord() + ").")
						.queue(s -> {
							if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
							this.message = s;
							Pages.buttonize(s, getButtons(), ShiroInfo.USE_BUTTONS, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						});
			}
		} else if (Helper.findStringInFile(list, command) > -1) {
			used.add(command);
			word = command;

			resetTimer();
			channel.sendMessage(getCurrent().getAsMention() + " agora é sua vez (palavra atual: " + getHighlightedWord() + ").")
					.queue(s -> {
						if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
						this.message = s;
						Pages.buttonize(s, getButtons(), ShiroInfo.USE_BUTTONS, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
					});
		} else {
			channel.sendMessage("❌ | Palavra inválida, veja se escreveu-a corretamente.").queue();
		}

		if (getRound() > 50 && !suddenDeath) {
			channel.sendMessage(":warning: | ALERTA: Morte-súbita ativada, o tempo para W.O. foi reduzido para 30 segundos!").queue();
			setTime(30);
			suddenDeath = true;
		}
	}

	public TextChannel getChannel() {
		return channel;
	}

	@Override
	public Map<Emoji, ThrowingConsumer<ButtonWrapper>> getButtons() {
		Map<Emoji, ThrowingConsumer<ButtonWrapper>> buttons = new LinkedHashMap<>();
		buttons.put(Helper.parseEmoji("\uD83C\uDFF3️"), wrapper -> {
			channel.sendMessage(getCurrent().getAsMention() + " desistiu!").queue(null, Helper::doNothing);
			getTable().leaveGame();
			resetTimer();

			if (getTable().getInGamePlayers().size() == 1) {
				getTable().awardWinner(this, getCurrent().getId());
				close();
				channel.sendMessage(getCurrent().getAsMention() + " é o último jogador na mesa, temos um vencedor!! (" + getRound() + " turnos)")
						.queue(msg -> {
							if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
						});
			} else {
				channel.sendMessage(getCurrent().getAsMention() + " agora é sua vez (palavra atual: " + getHighlightedWord() + ").")
						.queue(s -> {
							if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
							this.message = s;
							Pages.buttonize(s, getButtons(), ShiroInfo.USE_BUTTONS, false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
						});
			}
		});

		return buttons;
	}

	private String getHighlightedWord() {
		if (word == null) return "nenhuma";

		return word.substring(0, word.length() - 2) + "**" + word.substring(word.length() - 2) + "**";
	}

	@Override
	public void close() {
		listener.close();
		super.close();
	}
}
