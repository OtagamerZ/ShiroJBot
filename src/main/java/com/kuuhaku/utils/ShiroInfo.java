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
import com.kuuhaku.controller.postgresql.CanvasDAO;
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.controller.postgresql.RaidDAO;
import com.kuuhaku.controller.postgresql.VersionDAO;
import com.kuuhaku.events.ShiroEvents;
import com.kuuhaku.handlers.api.websocket.EncoderClient;
import com.kuuhaku.handlers.api.websocket.WebSocketConfig;
import com.kuuhaku.handlers.games.tabletop.framework.Game;
import com.kuuhaku.model.common.MatchMaking;
import com.kuuhaku.model.common.drop.Prize;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.enums.SupportTier;
import com.kuuhaku.model.enums.Version;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.model.persistent.PixelCanvas;
import com.kuuhaku.model.persistent.RaidInfo;
import com.kuuhaku.model.persistent.RaidMember;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.records.RaidData;
import com.sun.management.OperatingSystemMXBean;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHeaders;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.discordbots.api.client.DiscordBotListAPI;

import javax.websocket.DeploymentException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("localvariable")
public class ShiroInfo {
	public static final boolean USE_BUTTONS = true;

	//PUBLIC CONSTANTS
	public static final String RESOURCES_URL = "https://raw.githubusercontent.com/OtagamerZ/ShiroJBot/master/src/main/resources";
	public static final String GIFS_URL = "https://raw.githubusercontent.com/OtagamerZ/ShoukanAssets/master/gifs";
	public static final String SHIRO_AVATAR = RESOURCES_URL + "/avatar/shiro/%s.png";
	public static final String JIBRIL_AVATAR = RESOURCES_URL + "/avatar/jibril/%s.png";
	public static final String TET_AVATAR = RESOURCES_URL + "/avatar/tet/%s.png";
	public static final String STEPHANIE_AVATAR = RESOURCES_URL + "/avatar/stephanie/%s.png";
	public static final String NERO_AVATAR = RESOURCES_URL + "/avatar/nero/%s.png";
	public static final String USATAN_AVATAR = RESOURCES_URL + "/avatar/usa-tan/%s.png";

	public static final String SITE_ROOT = "https://" + System.getenv("SERVER_URL");
	public static final String API_ROOT = "https://api." + System.getenv("SERVER_URL");
	public static final String SOCKET_ROOT = "wss://socket." + System.getenv("SERVER_URL");
	public static final String IMAGE_ENDPOINT = API_ROOT + "/image?id=%s";
	public static final String COLLECTION_ENDPOINT = API_ROOT + "/collection.jpg?id=%s";

	//PRIVATE CONSTANTS
	private static final OperatingSystemMXBean systemInfo = ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean());
	private static final ThreadPoolExecutor compilationPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
	private static final String botToken = System.getenv("BOT_TOKEN");
	private static final String dblToken;
	private static final String name = "Shiro J. Bot";
	private static final String version = VersionDAO.getBuildVersion(Version.V3);
	private static final String supportServerName = "Shiro Support";
	private static final String supportServerID = "421495229594730496";
	private static final String announcementChannelID = "597587565809369089";
	private static final String default_prefix = "s!";
	private static final String nomeDB = "shiro.sqlite";
	private static final String shiro = "572413282653306901";
	private static final String niichan = "350836145921327115"; //KuuHaKu
	private static final List<String> developers = List.of(
			niichan, //KuuHaKu
			"321665807988031495" //Reydux
	);
	private static final List<String> editors = List.of(

	);
	private static final Map<String, SupportTier> supports = Map.of(
			"656542716108472340", SupportTier.NORMAL, //Lazuli
			"553244700258336825", SupportTier.SENIOR, //The Caos
			"860121300110671902", SupportTier.NORMAL, //Hidekin
			"435229114132201493", SupportTier.NORMAL, //Megu
			"774526344708620298", SupportTier.NORMAL  //Mask
	);
	private static final List<String> emoteRepo = List.of(
			"666619034103447642", //Shiro Emote Repository 1
			"726171298044313694", //Shiro Emote Repository 2
			"732300321673576498", //Shiro Emote Repository 3
			"763775306095788033"  //Shiro Emote Repository 4
	);
	private static final List<String> levelEmoteRepo = List.of(
			"806891504442277969", //Low level emotes
			"806891669345271849", //Medium level emotes
			"806891903990628362", //High level emotes
			"806892045327007794"  //Top level emotes
	);
	private static final Map<String, Map<String, String>> polls = new HashMap<>();
	private static final ShiroEvents shiroEvents = new ShiroEvents();
	private static final CloseableHttpClient http = HttpClients.custom().setDefaultHeaders(List.of(
			new BasicHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0")
	)).build();
	private static final Map<String, String> emoteLookup = new HashMap<>();
	private static final Set<String> pruneQueue = new HashSet<>();

	//STATIC CONSTRUCTOR
	static {
		if (System.getenv().containsKey("TOPGG_TOKEN")) dblToken = System.getenv("TOPGG_TOKEN");
		else dblToken = null;
	}

	private long startTime = 0;
	private String winner = "";
	private WebSocketConfig sockets;
	private EncoderClient encoderClient;
	private final DiscordBotListAPI dblApi = dblToken == null ? null : new DiscordBotListAPI.Builder()
			.token(dblToken)
			.botId("572413282653306901")
			.build();
	private final Map<String, Game> games = new HashMap<>();
	private final Set<String> gameLock = new HashSet<>();
	private final MatchMaking matchMaking = new MatchMaking();
	private final File collectionsFolder = new File(System.getenv("COLLECTIONS_PATH"));
	private final File temporaryFolder = new File(System.getenv("TEMPORARY_PATH"));
	private final Set<String> ignore = new HashSet<>();
	private final ConcurrentMap<String, RaidData> antiRaidStreak = ExpiringMap.builder()
			.expirationListener((k, v) -> {
				Guild guild = Main.getShiroShards().getGuildById((String) k);
				if (guild == null) return;

				RaidData data = (RaidData) v;
				GuildConfig gc = GuildDAO.getGuildById(guild.getId());
				TextChannel chn = gc.getGeneralChannel();

				long duration = System.currentTimeMillis() - data.start();
				Set<String> ids = data.ids();
				if (chn != null) {
					EmbedBuilder eb = new EmbedBuilder()
							.setColor(Color.green)
							.setTitle("**RELATÓRIO DO SISTEMA R.A.ID**")
							.setDescription("""
									Detectado fim da raid, usuários podem voltar à rotina normal.
									          
									Duração da raid: %s
									Usuários banidos: %s
									
									O relatório completo pode ser encontrado no comando `raids`.
									""".formatted(Helper.toStringDuration(duration), ids.size())
							);

					chn.sendMessageEmbeds(eb.build()).queue(null, Helper::doNothing);
				}

				for (TextChannel tc : guild.getTextChannels()) {
					if (guild.getPublicRole().hasPermission(tc, Permission.MESSAGE_WRITE)) {
						tc.getManager().setSlowmode(0).queue(null, Helper::doNothing);
					}
				}

				RaidInfo info = new RaidInfo(guild.getId(), duration);
				for (String id : ids) {
					info.getMembers().add(new RaidMember(id, guild.getId()));
				}
				RaidDAO.saveInfo(info);
			})
			.expirationPolicy(ExpirationPolicy.ACCESSED)
			.expiration(10, TimeUnit.SECONDS)
			.build();

	//CACHES
	private final ConcurrentMap<String, ExpiringMap<String, Message>> messageCache = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, ExpiringMap<Long, String>> antiRaidCache = new ConcurrentHashMap<>();
	private final ExpiringMap<String, Boolean> ratelimit = ExpiringMap.builder().variableExpiration().build();
	private final ExpiringMap<String, Boolean> confirmationPending = ExpiringMap.builder().expiration(1, TimeUnit.MINUTES).build();
	private final ExpiringMap<String, Boolean> shoukanSlot = ExpiringMap.builder().expiration(200, TimeUnit.SECONDS).build();
	private final ExpiringMap<String, Boolean> specialEvent = ExpiringMap.builder().expiration(30, TimeUnit.MINUTES).build();
	private final ExpiringMap<String, KawaiponCard> currentCard = ExpiringMap.builder().expiration(1, TimeUnit.MINUTES).build();
	private final ExpiringMap<String, Prize<?>> currentDrop = ExpiringMap.builder().expiration(1, TimeUnit.MINUTES).build();
	private final ExpiringMap<String, byte[]> cardCache = ExpiringMap.builder().expiration(30, TimeUnit.MINUTES).build();
	private final ExpiringMap<String, byte[]> resourceCache = ExpiringMap.builder().expiration(30, TimeUnit.MINUTES).build();

	private boolean isLive = false;

	public ShiroInfo() {
		try {
			encoderClient = new EncoderClient(ShiroInfo.SOCKET_ROOT + "/encoder");
		} catch (URISyntaxException | DeploymentException | IOException e) {
			encoderClient = null;
		}
	}

	//CONSTANTS
	//STATIC
	public static OperatingSystemMXBean getSystemInfo() {
		return systemInfo;
	}

	public static ThreadPoolExecutor getCompilationPool() {
		return compilationPool;
	}

	public static String getBotToken() {
		return botToken;
	}

	public static String getDblToken() {
		return dblToken;
	}

	public static String getName() {
		return name;
	}

	public static String getFullName() {
		return I18n.getString("str_version", name, version);
	}

	public static String getVersion() {
		return version;
	}

	public static String getDefaultPrefix() {
		return default_prefix;
	}

	public static String getDBFileName() {
		return nomeDB;
	}

	public static Map<String, Map<String, String>> getPolls() {
		return polls;
	}

	public static CloseableHttpClient getHttp() {
		return http;
	}

	public static ShiroEvents getShiroEvents() {
		return shiroEvents;
	}

	public static ResourceBundle getLocale(I18n lang, String prefix) {
		return ResourceBundle.getBundle("i18n/" + prefix + "/locale", lang.getLocale());
	}

	public static String getSupportServerName() {
		return supportServerName;
	}

	public static String getSupportServerID() {
		return supportServerID;
	}

	public static String getAnnouncementChannelID() {
		return announcementChannelID;
	}

	public static Map<String, String> getEmoteLookup() {
		return emoteLookup;
	}

	public static Set<String> getPruneQueue() {
		return pruneQueue;
	}

	public static String getShiro() {
		return shiro;
	}

	public static String getNiiChan() {
		return niichan;
	}

	public static List<String> getDevelopers() {
		return developers;
	}

	public static List<String> getEditors() {
		return editors;
	}

	public static Map<String, SupportTier> getSupports() {
		return supports;
	}

	public static List<String> getEmoteRepo() {
		return emoteRepo;
	}

	public static List<String> getLevelEmoteRepo() {
		return levelEmoteRepo;
	}

	public static List<String> getStaff() {
		return Stream.concat(developers.stream(), supports.keySet().stream()).distinct().collect(Collectors.toList());
	}

	//NON-STATIC
	public DiscordBotListAPI getDblApi() {
		return dblApi;
	}

	public ScheduledExecutorService getScheduler() {
		return Executors.newSingleThreadScheduledExecutor();
	}

	public Map<String, Game> getGames() {
		return games;
	}

	public boolean gameInProgress(String id) {
		return gameLock.stream().anyMatch(s -> Helper.equalsAny(id, s.split(Pattern.quote(".")))) || games.keySet().stream().anyMatch(s -> Helper.equalsAny(id, s.split(Pattern.quote("."))));
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public File getCollectionsFolder() {
		if (!collectionsFolder.exists())
			collectionsFolder.mkdir();
		return collectionsFolder;
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public File getTemporaryFolder() {
		if (!temporaryFolder.exists())
			temporaryFolder.mkdir();
		return temporaryFolder;
	}

	public MatchMaking getMatchMaking() {
		return matchMaking;
	}

	public Set<String> getIgnore() {
		return ignore;
	}

	public ConcurrentMap<String, RaidData> getAntiRaidStreak() {
		return antiRaidStreak;
	}

	//VARIABLES
	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public User getUserByID(String userID) {
		if (userID == null || userID.isBlank()) return null;
		return Main.getShiroShards().getUserById(userID);
	}

	public User[] getUsersByID(String... userIDs) {
		return Arrays.stream(userIDs)
				.map(this::getUserByID)
				.toArray(User[]::new);
	}

	public Member getMemberByID(String userID) {
		User u = getUserByID(userID);
		return u.getMutualGuilds().get(0).getMember(u);
	}

	public List<Member> getMembersByID(String userID) {
		User u = getUserByID(userID);
		return u.getMutualGuilds().stream().map(g -> g.getMemberById(userID)).collect(Collectors.toList());
	}

	public Role getRoleByID(String roleID) {
		return Main.getShiroShards().getRoleById(roleID);
	}

	public Guild getGuildByID(String guildID) {
		return Main.getShiroShards().getGuildById(guildID);
	}

	public String getWinner() {
		return winner;
	}

	public void setWinner(String winner) {
		this.winner = winner;
	}

	public PixelCanvas getCanvas() {
		return CanvasDAO.getCanvas();
	}

	public WebSocketConfig getSockets() {
		return sockets;
	}

	public void setSockets(WebSocketConfig server) {
		this.sockets = server;
	}

	public EncoderClient getEncoderClient() {
		return encoderClient;
	}

	public boolean isEncoderConnected() {
		return encoderClient != null && encoderClient.getSession() != null && encoderClient.getSession().isOpen();
	}

	public void setEncoderClient(EncoderClient encoderClient) {
		this.encoderClient = encoderClient;
	}

	public void cache(Guild guild, Message message) {
		messageCache.computeIfAbsent(guild.getId(), k -> ExpiringMap.builder()
				.maxSize(64)
				.expiration(1, TimeUnit.DAYS)
				.build()
		)
				.put(message.getId(), message);
	}

	public Message retrieveCachedMessage(Guild guild, String id) {
		return messageCache.getOrDefault(
				guild.getId(),
				ExpiringMap.builder()
						.maxSize(64)
						.expiration(1, TimeUnit.DAYS)
						.build()
		).get(id);
	}

	public ExpiringMap<String, Message> retrieveCache(Guild guild) {
		return messageCache.getOrDefault(
				guild.getId(),
				ExpiringMap.builder()
						.maxSize(64)
						.expiration(1, TimeUnit.DAYS)
						.build()
		);
	}

	public ConcurrentMap<String, ExpiringMap<Long, String>> getAntiRaidCache() {
		return antiRaidCache;
	}

	public ExpiringMap<String, KawaiponCard> getCurrentCard() {
		return currentCard;
	}

	public ExpiringMap<String, Prize<?>> getCurrentDrop() {
		return currentDrop;
	}

	public ExpiringMap<String, byte[]> getCardCache() {
		return cardCache;
	}

	public ExpiringMap<String, byte[]> getResourceCache() {
		return resourceCache;
	}

	public ExpiringMap<String, Boolean> getRatelimit() {
		return ratelimit;
	}

	public ExpiringMap<String, Boolean> getSpecialEvent() {
		return specialEvent;
	}

	public ExpiringMap<String, Boolean> getConfirmationPending() {
		return confirmationPending;
	}

	public ExpiringMap<String, Boolean> getShoukanSlot() {
		return shoukanSlot;
	}

	public Set<String> getGameLock() {
		return gameLock;
	}

	public boolean isLive() {
		return isLive;
	}

	public void setLive(boolean live) {
		this.isLive = live;
	}
}
