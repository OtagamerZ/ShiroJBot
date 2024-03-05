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
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.util.API;
import com.kuuhaku.util.Utils;
import com.kuuhaku.websocket.CommonSocket;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.messages.MessageRequest;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import static net.dv8tion.jda.api.entities.Message.MentionType.EVERYONE;
import static net.dv8tion.jda.api.entities.Message.MentionType.HERE;

public class Application implements Thread.UncaughtExceptionHandler {
	private final ShardManager shiro;

	public Application() {
		long latency = Manager.ping();
		if (latency <= 100) {
			Constants.LOGGER.info("Database latency: " + latency + "ms");
		} else if (latency <= 250) {
			Constants.LOGGER.warn("Database latency: " + latency + "ms");
		} else {
			Constants.LOGGER.error("Database latency: " + latency + "ms");
		}

		shiro = DefaultShardManagerBuilder.create(Constants.BOT_TOKEN, EnumSet.allOf(GatewayIntent.class))
				.disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS)
				.setMemberCachePolicy(MemberCachePolicy.ONLINE
						.and(MemberCachePolicy.OWNER)
						.and(m -> !m.getUser().isBot()))
				.addEventListeners(
						new GuildListener(),
						new AutoModListener()
				)
				.setBulkDeleteSplittingEnabled(false)
				.setEventPool(new ForkJoinPool(
						Runtime.getRuntime().availableProcessors(),
						ForkJoinPool.defaultForkJoinWorkerThreadFactory,
						this,
						true
				), true)
				.build();

		CompletableFuture.runAsync(() ->
				shiro.getShards().stream()
						.sorted(Comparator.comparingInt(s -> s.getShardInfo().getShardId()))
						.peek(s -> {
							int id = s.getShardInfo().getShardId();

							try {
								s.awaitReady();
								Constants.LOGGER.info("Shard " + id + " ready");
							} catch (InterruptedException e) {
								Constants.LOGGER.error("Failed to initialize shard " + id + ": " + e);
							}
						})
						.forEach(this::setRandomAction)
		);

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
			Constants.LOGGER.error("Failed to start pagination library: " + e);
			System.exit(1);
		}

		API.connectSocket(CommonSocket.class, Constants.SOCKET_ROOT);
		Constants.LOGGER.info("<----------END OF BOOT---------->");

		Main.boot.stop();
		Constants.LOGGER.info("Finished in " + Utils.toStringDuration(I18N.EN, Main.boot.getTime()));
	}

	public ShardManager getShiro() {
		return shiro;
	}

	public User getUserById(String id) {
		return Pages.subGet(shiro.retrieveUserById(id));
	}

	public GuildMessageChannel getMessageChannelById(String id) {
		List<Class<? extends Channel>> types = List.of(
				StandardGuildMessageChannel.class, ThreadChannel.class, VoiceChannel.class
		);

		for (Class<? extends Channel> type : types) {
			GuildMessageChannel gmc = (GuildMessageChannel) shiro.getChannelById(type, id);
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

	public void setRandomAction(JDA jda) {
		final List<Activity> activities = List.of(
				Activity.watching(Utils.separate(shiro.getGuildCache().size()) + " servidores, e estou apenas começando!"),
				Activity.competing("Shoukan ranqueado!"),
				Activity.watching(DAO.queryNative(String.class, """
						SELECT c.name||' pela '||(SELECT count(1) FROM card x WHERE x.anime_id = c.anime_id)||'ª vez!'
						FROM card c
						INNER JOIN anime a on a.id = c.anime_id
						WHERE a.visible
						AND c.rarity = 'ULTIMATE'
						ORDER BY RANDOM()
						""")),
				Activity.playing("com minhas cartas Kawaipon!"),
				Activity.of(Activity.ActivityType.LISTENING, "Use " + Constants.DEFAULT_PREFIX + "help para ver os meus comandos!")
		);

		jda.getPresence().setActivity(Utils.getRandomEntry(activities));
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		Constants.LOGGER.error(e, e);
	}
}
