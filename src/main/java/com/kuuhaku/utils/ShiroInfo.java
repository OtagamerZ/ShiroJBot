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

package com.kuuhaku.utils;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.CustomAnswerDAO;
import com.kuuhaku.controller.postgresql.VersionDAO;
import com.kuuhaku.handlers.api.websocket.EncoderClient;
import com.kuuhaku.model.common.MatchMaking;
import com.kuuhaku.model.common.interfaces.Prize;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.enums.SupportTier;
import com.kuuhaku.model.enums.Version;
import com.kuuhaku.model.persistent.CustomAnswer;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.model.records.ApiInfo;
import com.kuuhaku.utils.collections.RandomList;
import com.kuuhaku.utils.collections.RefreshingMap;
import com.kuuhaku.utils.helpers.CollectionHelper;
import com.kuuhaku.utils.helpers.MiscHelper;
import com.sun.management.OperatingSystemMXBean;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.jodah.expiringmap.ExpiringMap;
import org.apache.commons.collections4.map.ReferenceMap;
import org.apache.commons.lang3.tuple.Pair;
import org.discordbots.api.client.DiscordBotListAPI;

import javax.websocket.DeploymentException;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength.HARD;
import static org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength.WEAK;

@SuppressWarnings("localvariable")
public class ShiroInfo {
	private static final OperatingSystemMXBean systemInfo = ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean());

	public static final String NAME = "Shiro J. Bot";
	public static final String VERSION = VersionDAO.getBuildVersion(Version.V3);
	public static final String FULL_NAME = I18n.getString("str_version", NAME, VERSION);;
	public static final String TOKEN = System.getenv("BOT_TOKEN");
	public static final String NIICHAN = "350836145921327115"; //KuuHaKu
	public static final String SUPPORT_SERVER_ID = "421495229594730496";
	public static final String SUPPORT_SERVER_NAME = "Shiro Support";
	public static final String ANNOUNCEMENT_CHANNEL_ID = "597587565809369089";

	private static final List<String> developers = List.of(
			NIICHAN, //KuuHaKu
			"321665807988031495" //Reydux
	);
	private static final Map<String, SupportTier> supports = Map.of(
			"656542716108472340", SupportTier.NORMAL   //Lazuli
			, "553244700258336825", SupportTier.NORMAL //Caos
			//, "435229114132201493", SupportTier.NORMAL //Megu
	);
	private static final String[] emoteRepo = {
			"666619034103447642"   //Shiro Emote Repository 1
			, "726171298044313694" //Shiro Emote Repository 2
			, "732300321673576498" //Shiro Emote Repository 3
			, "763775306095788033" //Shiro Emote Repository 4
	};
	private static final String[] levelEmoteRepo = {
			"806891504442277969"   //Low level emotes
			, "806891669345271849" //Medium level emotes
			, "806891903990628362" //High level emotes
			, "806892045327007794" //Top level emotes
	};
	private static final Map<String, ApiInfo> thirdParty = Map.of(
			"TOPGG", new ApiInfo(null, System.getenv("TOPGG_TOKEN"))
	);

	private static final ExecutorService compilationPool = Executors.newFixedThreadPool(3);
	private static final ScheduledExecutorService schedulerPool = Executors.newScheduledThreadPool(5);
	private static final Set<String> pruneQueue = new HashSet<>();

	private final ReferenceMap<String, AtomicReference<?>> games = new ReferenceMap<>(HARD, WEAK);
	private final ReferenceMap<String, AtomicReference<?>> gameSlot = new ReferenceMap<>(HARD, WEAK);

	private final MatchMaking matchMaking = new MatchMaking();
	private final Set<String> ignore = new HashSet<>();

	private final ConcurrentMap<String, ExpiringMap<String, Message>> messageTracker = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, ExpiringMap<Long, String>> raidTracker = new ConcurrentHashMap<>();

	private final ExpiringMap<String, Boolean> ratelimit = CollectionHelper.makeExpMap().build();
	private final ExpiringMap<String, Boolean> confirmationPending = CollectionHelper.makeExpMap(1, TimeUnit.MINUTES).build();
	private final ExpiringMap<String, Boolean> specialEvent = CollectionHelper.makeExpMap(30, TimeUnit.MINUTES).build();
	private final ExpiringMap<String, KawaiponCard> currentCard = CollectionHelper.makeExpMap(1, TimeUnit.MINUTES).build();
	private final ExpiringMap<String, Prize<?>> currentDrop = CollectionHelper.makeExpMap(1, TimeUnit.MINUTES).build();

	private final Map<Pair<String, String>, RandomList<CustomAnswer>> customAnswers = new RefreshingMap<>(() -> {
		List<CustomAnswer> cas = CustomAnswerDAO.getCustomAnswers();
		Map<Pair<String, String>, RandomList<CustomAnswer>> out = new HashMap<>();

		for (CustomAnswer ca : cas) {
			registerCustomAnswer(ca);
		}

		return out;
	}, 30, TimeUnit.MINUTES);

	private EncoderClient encoderClient = null;
	private DiscordBotListAPI topggClient = null;

	public ShiroInfo() {
		try {
			encoderClient = new EncoderClient(Constants.SOCKET_ROOT + "/encoder");
		} catch (URISyntaxException | DeploymentException | IOException e) {
			MiscHelper.logger(ShiroInfo.class).error(e + " | " + e.getStackTrace()[0]);
		}

		thirdParty.computeIfPresent("TOPGG", (k, v) -> {
			topggClient = new DiscordBotListAPI.Builder()
					.token(v.auth())
					.botId(Main.getSelfUser().getId())
					.build();

			return v;
		});
	}

	public static OperatingSystemMXBean getSystemInfo() {
		return systemInfo;
	}

	public static List<String> getDevelopers() {
		return developers;
	}

	public static Map<String, SupportTier> getSupports() {
		return supports;
	}

	public static List<String> getStaff() {
		return Stream.concat(developers.stream(), supports.keySet().stream())
				.distinct()
				.toList();
	}

	public static String[] getEmoteRepo() {
		return emoteRepo;
	}

	public static String[] getLevelEmoteRepo() {
		return levelEmoteRepo;
	}

	public static Map<String, ApiInfo> getThirdParty() {
		return thirdParty;
	}

	public static ExecutorService getCompilationPool() {
		return compilationPool;
	}

	public ScheduledExecutorService getSchedulerPool() {
		return schedulerPool;
	}

	public static Set<String> getPruneQueue() {
		return pruneQueue;
	}

	public static ResourceBundle getLocale(I18n lang, String prefix) {
		return ResourceBundle.getBundle("i18n/" + prefix + "/locale", lang.getLocale());
	}

	public Map<String, AtomicReference<?>> getGames() {
		games.values().removeIf(ref -> ref == null || ref.get() == null);
		return games;
	}

	public void setGameInProgress(AtomicReference<?> mutex, String... players) {
		for (String player : players) {
			getGames().putIfAbsent(player, mutex);
		}
	}

	public void setGameInProgress(AtomicReference<?> mutex, User... players) {
		for (User player : players) {
			getGames().putIfAbsent(player.getId(), mutex);
		}
	}

	public void setGameInProgress(AtomicReference<?> mutex, List<User> players) {
		for (User player : players) {
			getGames().putIfAbsent(player.getId(), mutex);
		}
	}

	public boolean gameInProgress(String id) {
		return getGames().containsKey(id);
	}

	public Map<String, AtomicReference<?>> getGameSlot() {
		gameSlot.values().removeIf(ref -> ref == null || ref.get() == null);
		return gameSlot;
	}

	public boolean isOccupied(String channel) {
		return getGameSlot().containsKey(channel);
	}

	public MatchMaking getMatchMaking() {
		return matchMaking;
	}

	public Set<String> getIgnore() {
		return ignore;
	}

	public void trackMessage(Guild guild, Message message) {
		messageTracker.computeIfAbsent(guild.getId(), k -> ExpiringMap.builder()
						.maxSize(64)
						.expiration(1, TimeUnit.DAYS)
						.build()
				)
				.put(message.getId(), message);
	}

	public Message getTrackedMessage(Guild guild, String id) {
		return messageTracker.getOrDefault(
				guild.getId(),
				ExpiringMap.builder()
						.maxSize(64)
						.expiration(1, TimeUnit.DAYS)
						.build()
		).get(id);
	}

	public ExpiringMap<String, Message> getTrackedMessages(Guild guild) {
		return messageTracker.getOrDefault(
				guild.getId(),
				ExpiringMap.builder()
						.maxSize(64)
						.expiration(1, TimeUnit.DAYS)
						.build()
		);
	}

	public ConcurrentMap<String, ExpiringMap<Long, String>> getRaidTracker() {
		return raidTracker;
	}

	public File getCollectionsFolder() {
		File f = Constants.COLLECTIONS_FOLDER;

		if (!f.exists()) {
			if (!f.mkdir()) {
				MiscHelper.logger(this.getClass()).warn("Failed to create collections folder");
			}
		}

		return f;
	}

	public File getTemporaryFolder() {
		File f = Constants.TEMPORARY_FOLDER;

		if (!f.exists()) {
			if (!f.mkdir()) {
				MiscHelper.logger(this.getClass()).warn("Failed to create temporary folder");
			}
		}

		return f;
	}

	public ExpiringMap<String, Boolean> getRatelimit() {
		return ratelimit;
	}

	public ExpiringMap<String, Boolean> getConfirmationPending() {
		return confirmationPending;
	}

	public ExpiringMap<String, Boolean> getSpecialEvent() {
		return specialEvent;
	}

	public synchronized ExpiringMap<String, KawaiponCard> getCurrentCard() {
		return currentCard;
	}

	public synchronized ExpiringMap<String, Prize<?>> getCurrentDrop() {
		return currentDrop;
	}

	public CustomAnswer getCustomAnswer(String guild, String msg) {
		CustomAnswer ca = customAnswers.entrySet().parallelStream()
				.filter(e -> e.getKey().getLeft().equals(guild))
				.filter(e -> msg.contains(e.getKey().getRight().toLowerCase(Locale.ROOT)))
				.map(Map.Entry::getValue)
				.map(RandomList::get)
				.findFirst()
				.orElse(null);

		if (ca == null) return null;
		return ca.isAnywhere() || msg.equalsIgnoreCase(ca.getTrigger()) ? ca : null;
	}

	public void registerCustomAnswer(CustomAnswer ca) {
		Pair<String, String> key = Pair.of(ca.getGuildId(), ca.getTrigger().toLowerCase(Locale.ROOT));
		customAnswers.computeIfAbsent(key, k -> new RandomList<>()).add(ca);
	}

	public void removeCustomAnswer(CustomAnswer ca) {
		Pair<String, String> key = Pair.of(ca.getGuildId(), ca.getTrigger().toLowerCase(Locale.ROOT));
		customAnswers.computeIfPresent(key, (k, v) -> {
			v.remove(ca);
			return v;
		});
	}

	public EncoderClient getEncoderClient() {
		return encoderClient;
	}

	public boolean isEncoderDisconnected() {
		return encoderClient == null || encoderClient.getSession() == null || !encoderClient.getSession().isOpen();
	}

	public void setEncoderClient(EncoderClient encoderClient) {
		this.encoderClient = encoderClient;
	}

	public DiscordBotListAPI getTopggClient() {
		return topggClient;
	}
}
