/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku;

import com.github.ygimenez.exception.InvalidHandlerException;
import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.PaginatorBuilder;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.controller.Manager;
import com.kuuhaku.listener.AutoModListener;
import com.kuuhaku.listener.GuildListener;
import com.kuuhaku.listener.PrivateChannelListener;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.util.API;
import com.kuuhaku.util.Utils;
import com.kuuhaku.websocket.CommonSocket;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.messages.MessageRequest;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Executors;

import static net.dv8tion.jda.api.entities.Message.MentionType.EVERYONE;
import static net.dv8tion.jda.api.entities.Message.MentionType.HERE;
import static net.dv8tion.jda.api.utils.MemberCachePolicy.*;

public class Application implements Thread.UncaughtExceptionHandler {
	private final ShardManager shiro;

	public Application() {
		long latency = Manager.ping();
		if (latency <= 100) {
			Constants.LOGGER.info("Database latency: {}ms", latency);
		} else if (latency <= 250) {
			Constants.LOGGER.warn("Database latency: {}ms", latency);
		} else {
			Constants.LOGGER.error("Database latency: {}ms", latency);
		}

		shiro = DefaultShardManagerBuilder.create(Constants.BOT_TOKEN, EnumSet.allOf(GatewayIntent.class))
				.disableCache(EnumSet.complementOf(EnumSet.of(CacheFlag.EMOJI)))
				.setMemberCachePolicy(all(ONLINE.or(OWNER), m -> !m.getUser().isBot()))
				.setBulkDeleteSplittingEnabled(false)
				.setActivity(getRandomAction())
				.addEventListeners(
						new GuildListener(),
						new AutoModListener(),
						new PrivateChannelListener()
				)
				.setEventPool(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()), true)
				.build();

		MessageRequest.setDefaultMentions(EnumSet.complementOf(EnumSet.of(EVERYONE, HERE)));

		try {
			PaginatorBuilder.createPaginator()
					.setHandler(shiro)
					.shouldEventLock(true)
					.setOnRemove(h ->
							h.editOriginalComponents()
									.map(m -> {
										Interaction i = h.getInteraction();
										if (i.isFromGuild() && i.getGuild() != null) {
											GuildConfig gc = DAO.find(GuildConfig.class, i.getGuild().getId());

											h.setEphemeral(true)
													.sendMessage(gc.getLocale().get("error/event_not_mapped"))
													.queue();
										}

										return m;
									})
									.queue(null, Utils::doNothing)
					)
					.activate();
		} catch (InvalidHandlerException e) {
			Constants.LOGGER.error("Failed to start pagination library: {}", e.toString());
			System.exit(1);
		}

		API.connectSocket(CommonSocket.class, Constants.SOCKET_ROOT);
		Constants.LOGGER.info("<----------END OF BOOT---------->");

		Main.boot.stop();
		Constants.LOGGER.info("Finished in {}", Utils.toStringDuration(I18N.EN, Main.boot.getTime()));
	}

	public ShardManager getShiro() {
		return shiro;
	}

	public User getUserById(String id) {
		return Pages.subGet(shiro.retrieveUserById(id));
	}

	public GuildMessageChannel getMessageChannelById(String id) {
		List<Class<? extends GuildMessageChannel>> types = List.of(
				StandardGuildMessageChannel.class, ThreadChannel.class, VoiceChannel.class
		);

		for (Class<? extends GuildMessageChannel> type : types) {
			GuildMessageChannel gmc = shiro.getChannelById(type, id);
			if (gmc != null) return gmc;
		}

		return null;
	}

	public JDA getMainShard() {
		return shiro.getShards().getFirst();
	}

	public String getId() {
		return getMainShard().getSelfUser().getId();
	}

	public Activity getRandomAction() {
		final List<String> activities = List.of(
				"Derrotando noobs no Shoukan!",
				DAO.queryNative(String.class, """
						SELECT 'Assistindo '||c.name||' pela '||(SELECT count(1) FROM card x WHERE x.anime_id = c.anime_id)||'ª vez!'
						FROM card c
						INNER JOIN anime a ON a.id = c.anime_id
						WHERE a.visible
						AND c.rarity = 'ULTIMATE'
						ORDER BY RANDOM()
						"""),
				"Coletando cartas Kawaipon!",
				"Use `" + Constants.DEFAULT_PREFIX + "help` para ver os meus comandos!"
		);

		return Activity.customStatus(Utils.getRandomEntry(activities));
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		Constants.LOGGER.error(e, e);
	}
}
