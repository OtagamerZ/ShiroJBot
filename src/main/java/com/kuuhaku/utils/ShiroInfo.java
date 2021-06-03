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

package com.kuuhaku.utils;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.CanvasDAO;
import com.kuuhaku.controller.postgresql.VersionDAO;
import com.kuuhaku.events.ShiroEvents;
import com.kuuhaku.handlers.api.websocket.EncoderClient;
import com.kuuhaku.handlers.api.websocket.WebSocketConfig;
import com.kuuhaku.handlers.games.tabletop.framework.Game;
import com.kuuhaku.handlers.music.GuildMusicManager;
import com.kuuhaku.model.common.MatchMaking;
import com.kuuhaku.model.common.TempCache;
import com.kuuhaku.model.common.drop.Prize;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.enums.SupportTier;
import com.kuuhaku.model.enums.Version;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.model.persistent.PixelCanvas;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sun.management.OperatingSystemMXBean;
import net.dv8tion.jda.api.entities.*;
import org.apache.http.impl.client.HttpClientBuilder;
import org.discordbots.api.client.DiscordBotListAPI;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("localvariable")
public class ShiroInfo {
	//PUBLIC CONSTANTS
	public static final String RESOURCES_URL = "https://raw.githubusercontent.com/OtagamerZ/ShiroJBot/master/src/main/resources";
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
	public static final String COLLECTION_ENDPOINT = API_ROOT + "/collection?id=%s";

	//PRIVATE CONSTANTS
	private static final OperatingSystemMXBean systemInfo = ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean());
	private static final ThreadPoolExecutor compilationPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
	private static final String botToken = System.getenv("BOT_TOKEN");
	private static final String youtubeToken = System.getenv("YOUTUBE_TOKEN");
	private static final String dblToken;
	private static final String name = "Shiro J. Bot";
	private static final String version = VersionDAO.getBuildVersion(Version.V3);
	private static final String supportServerName = "Shiro Support";
	private static final String supportServerID = "421495229594730496";
	private static final String twitchChannelID = "743479145618472960";
	private static final String announcementChannelID = "597587565809369089";
	private static final String default_prefix = "s!";
	private static final String nomeDB = "shiro.sqlite";
	private static final String shiro = "572413282653306901";
	private static final String niichan = "350836145921327115"; //KuuHaKu
	private static final List<String> developers = List.of(
			niichan, //KuuHaKu
			"321665807988031495" //Reydux
			//"694652893571055746" //HeyCarlosz
	);
	private static final List<String> editors = List.of(

	);
	private static final Map<String, SupportTier> supports = Map.of(
			"656542716108472340", SupportTier.SENIOR, //Fenyx
			"666488799835979786", SupportTier.SENIOR, //Lucas
			"619214753839185930", SupportTier.NORMAL, //Botzera
			"553244700258336825", SupportTier.NORMAL  //Caos
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
	private static final Map<Long, GuildMusicManager> gmms = new HashMap<>();
	private static final AudioPlayerManager apm = new DefaultAudioPlayerManager();
	private static final ShiroEvents shiroEvents = new ShiroEvents();
	private static final HttpClientBuilder httpBuilder = HttpClientBuilder.create();
	private static final HashMap<String, String> emoteLookup = new HashMap<>();

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
	private final ConcurrentMap<String, TempCache<String, Message>> messageCache = new ConcurrentHashMap<>();
	private final Map<String, Game> games = new HashMap<>();
	private final Map<String, Invite> requests = new HashMap<>();
	private final Set<String> gameLock = new HashSet<>();
	private final MatchMaking matchMaking = new MatchMaking();
	private final File collectionsFolder = new File(System.getenv("COLLECTIONS_PATH"));
	private final File temporaryFolder = new File(System.getenv("TEMPORARY_PATH"));

	//CACHES
	private final TempCache<String, Boolean> ratelimit = new TempCache<>(3, TimeUnit.SECONDS);
	private final TempCache<String, Boolean> confirmationPending = new TempCache<>(1, TimeUnit.MINUTES);
	private final TempCache<String, Boolean> specialEvent = new TempCache<>(30, TimeUnit.MINUTES);
	private final TempCache<String, KawaiponCard> currentCard = new TempCache<>(1, TimeUnit.MINUTES);
	private final TempCache<String, Prize<?>> currentDrop = new TempCache<>(1, TimeUnit.MINUTES);
	private final TempCache<String, byte[]> cardCache = new TempCache<>(30, TimeUnit.MINUTES);
	private final TempCache<String, byte[]> resourceCache = new TempCache<>(30, TimeUnit.MINUTES);

	private boolean isLive = false;

	public ShiroInfo() {
		try {
			encoderClient = new EncoderClient(ShiroInfo.SOCKET_ROOT + "/encoder");
		} catch (URISyntaxException e) {
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

	public static String getYoutubeToken() {
		return youtubeToken;
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

	public static AudioPlayerManager getApm() {
		return apm;
	}

	public static Map<Long, GuildMusicManager> getGmms() {
		return gmms;
	}

	public static void addGmm(long id, GuildMusicManager gmm) {
		gmms.put(id, gmm);
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

	public static String getTwitchChannelID() {
		return twitchChannelID;
	}

	public static String getAnnouncementChannelID() {
		return announcementChannelID;
	}

	public static HttpClientBuilder getHttpBuilder() {
		return httpBuilder;
	}

	public static HashMap<String, String> getEmoteLookup() {
		return emoteLookup;
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

	//VARIABLES
	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public User getUserByID(String userID) {
		return Main.getShiroShards().getUserById(userID);
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

	public void setEncoderClient(EncoderClient encoderClient) {
		this.encoderClient = encoderClient;
	}

	public void cache(Guild guild, Message message) {
		messageCache.computeIfAbsent(guild.getId(), k -> new TempCache<>(64, 1, TimeUnit.DAYS))
				.put(message.getId(), message);
	}

	public Message retrieveCachedMessage(Guild guild, String id) {
		return messageCache.getOrDefault(
				guild.getId(),
				new TempCache<>(64, 1, TimeUnit.DAYS)
		).get(id);
	}

	public TempCache<String, Message> retrieveCache(Guild guild) {
		return messageCache.getOrDefault(
				guild.getId(),
				new TempCache<>(64, 1, TimeUnit.DAYS)
		);
	}

	public Map<String, Invite> getRequests() {
		return requests;
	}

	public TempCache<String, KawaiponCard> getCurrentCard() {
		return currentCard;
	}

	public TempCache<String, Prize<?>> getCurrentDrop() {
		return currentDrop;
	}

	public TempCache<String, byte[]> getCardCache() {
		return cardCache;
	}

	public TempCache<String, byte[]> getResourceCache() {
		return resourceCache;
	}

	public TempCache<String, Boolean> getRatelimit() {
		return ratelimit;
	}

	public TempCache<String, Boolean> getSpecialEvent() {
		return specialEvent;
	}

	public TempCache<String, Boolean> getConfirmationPending() {
		return confirmationPending;
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
