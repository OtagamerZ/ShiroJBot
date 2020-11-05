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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.GsonBuilder;
import com.kuuhaku.controller.postgresql.CanvasDAO;
import com.kuuhaku.controller.postgresql.VersionDAO;
import com.kuuhaku.events.JDAEvents;
import com.kuuhaku.handlers.api.websocket.WebSocketConfig;
import com.kuuhaku.handlers.games.tabletop.framework.Game;
import com.kuuhaku.handlers.music.GuildMusicManager;
import com.kuuhaku.model.common.drop.Prize;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.enums.Version;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.model.persistent.PixelCanvas;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.spaceprogram.kittycache.KittyCache;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import org.apache.http.impl.client.HttpClientBuilder;
import org.discordbots.api.client.DiscordBotListAPI;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("localvariable")
public class ShiroInfo {

    //TODO Alternador do modo desenvolvimento (true quando utilizar em IDEs, false quando for dar push para o master)
    private static final boolean DEV = false;

    //CONSTANTS
    private static final ThreadMXBean tBean = ManagementFactory.getThreadMXBean();
    private static final ThreadPoolExecutor compilationPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
    private static final String BotToken = System.getenv("BOT_TOKEN");
    private static final String YoutubeToken = System.getenv("YOUTUBE_TOKEN");
    private static final String DBLToken;
    private static final String name = "Shiro J. Bot";
    private static final String version = VersionDAO.getBuildVersion(Version.V3);
    private static final String supportServerName = "Shiro Support";
    private static final String supportServerID = "421495229594730496";
    private static final String twitchChannelID = "743479145618472960";
    private static final String announcementChannelID = "597587565809369089";
    private static final String default_prefix = DEV ? "dev!" : "s!";
	private static final String nomeDB = "shiro.sqlite";
	private static final String shiro = "572413282653306901";
	private static final String niichan = "350836145921327115"; //KuuHaKu
	private static final List<String> developers = List.of(
			niichan, //KuuHaKu
			"321665807988031495", //Reydux
			"694652893571055746" //HeyCarlosz
	);
	private static final List<String> editors = List.of(

	);
	private static final List<String> supports = List.of(
			"581550063327903744", //Riki Yamada
			"656542716108472340", //Fenyx
			"666488799835979786", //Lucas
			"633774453876326410" //Kurome
	);
	private static final List<String> emoteRepo = List.of(
			"666619034103447642", //Shiro Emote Repository 1
			"726171298044313694", //Shiro Emote Repository 2
			"732300321673576498" //Shiro Emote Repository 3
	);
	private static final Map<String, Map<String, String>> polls = new HashMap<>();
	private static final Map<Long, GuildMusicManager> gmms = new HashMap<>();
	private static final AudioPlayerManager apm = new DefaultAudioPlayerManager();
	private static final JDAEvents shiroEvents = new JDAEvents();
	private static final GsonBuilder JSONFactory = new GsonBuilder();
	private static final HttpClientBuilder httpBuilder = HttpClientBuilder.create();
	private static final HashSet<String> hashes = new HashSet<>();

	//STATIC CONSTRUCTOR
	static {
		if (System.getenv().containsKey("TOPGG_TOKEN")) DBLToken = System.getenv("TOPGG_TOKEN");
		else DBLToken = null;
	}

	private JDA api = null;
	private long startTime = 0;
	private String winner = "";
	private WebSocketConfig sockets;
	private final DiscordBotListAPI dblApi = DBLToken == null ? null : new DiscordBotListAPI.Builder()
			.token(DBLToken)
			.botId("572413282653306901")
			.build();
	private final Map<String, KittyCache<String, Message>> messageCache = new HashMap<>();
	private final Cache<String, Boolean> ratelimit = CacheBuilder.newBuilder().expireAfterWrite(3, TimeUnit.SECONDS).build();
	private final Cache<String, Boolean> confirmationPending = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();
	private final Cache<String, Integer> pendingJoin = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();
	private final Map<String, Game> games = new HashMap<>();
	private final Set<String> requests = new HashSet<>();
	private final Cache<String, KawaiponCard> currentCard = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();
	private final Cache<String, Prize> currentDrop = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();
	private final Cache<String, byte[]> cardCache = CacheBuilder.newBuilder().expireAfterWrite(60, TimeUnit.MINUTES).build();
	private final Set<String> gameLock = new HashSet<>();
	private final File collectionsFolder = new File(System.getenv("COLLECTIONS_PATH"));
	private boolean isLive = false;

	//CONSTANTS
	//STATIC
	public static ResourceBundle getLocale(I18n lang) {
		return ResourceBundle.getBundle("locale", lang.getLocale());
	}

	public static GsonBuilder getJSONFactory() {
		return JSONFactory;
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

	public static HashSet<String> getHashes() {
		return hashes;
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

	public static List<String> getSupports() {
		return supports;
	}

	public static List<String> getEmoteRepo() {
		return emoteRepo;
	}

    public static List<String> getStaff() {
        return Stream.concat(developers.stream(), supports.stream()).distinct().collect(Collectors.toList());
    }

    //NON-STATIC
    public float getCPULoad() {
        return (float) Helper.round(tBean.getCurrentThreadCpuTime() * 100, 2);
    }

    public ThreadPoolExecutor getCompilationPool() {
        return compilationPool;
    }

    public boolean isDev() {
        return DEV;
    }

    public String getBotToken() {
        return BotToken;
    }

	public String getYoutubeToken() {
		return YoutubeToken;
	}

	public static String getDBLToken() {
		return DBLToken;
	}

	public DiscordBotListAPI getDblApi() {
		return dblApi;
	}

	public String getName() {
		return name;
	}

	public String getFullName() {
		return MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("str_version"), name, version);
	}

	public String getVersion() {
		return version;
	}

	public String getDefaultPrefix() {
		return default_prefix;
	}

	public String getDBFileName() {
		return nomeDB;
	}

	public Map<String, Map<String, String>> getPolls() {
		return polls;
	}

	public ScheduledExecutorService getScheduler() {
		return Executors.newSingleThreadScheduledExecutor();
	}

	public AudioPlayerManager getApm() {
		return apm;
	}

	Map<Long, GuildMusicManager> getGmms() {
		return gmms;
	}

	void addGmm(long id, GuildMusicManager gmm) {
		gmms.put(id, gmm);
	}

	public JDAEvents getShiroEvents() {
		return shiroEvents;
	}

	public Map<String, Game> getGames() {
		return games;
	}

	public boolean gameInProgress(String id) {
		return gameLock.stream().anyMatch(s -> Helper.equalsAny(id, s.split(Pattern.quote(".")))) || games.keySet().stream().anyMatch(s -> Helper.equalsAny(id, s.split(Pattern.quote("."))));
	}

	public File getCollectionsFolder() {
		if (!collectionsFolder.exists())
			collectionsFolder.mkdir();
		return collectionsFolder;
	}

	//VARIABLES
	public JDA getAPI() {
		return api;
	}

	public void setAPI(JDA api) {
		this.api = api;
	}

	public SelfUser getSelfUser() {
		return api.getSelfUser();
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public User getUserByID(String userID) {
		return api.getUserById(userID);
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
		return api.getRoleById(roleID);
	}

	public Guild getGuildByID(String guildID) {
		return api.getGuildById(guildID);
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

	public void cache(Guild guild, Message message) {
		KittyCache<String, Message> cache = messageCache.getOrDefault(guild.getId(), new KittyCache<>(64));
		cache.put(message.getId(), message, (int) TimeUnit.DAYS.toSeconds(1));
		messageCache.put(guild.getId(), cache);
	}

	public Message retrieveCachedMessage(Guild guild, String id) {
		return messageCache.getOrDefault(guild.getId(), new KittyCache<>(64)).get(id);
	}

	public KittyCache<String, Message> retrieveCache(Guild guild) {
		return messageCache.getOrDefault(guild.getId(), new KittyCache<>(64));
	}

	public Set<String> getRequests() {
		return requests;
	}

	public Cache<String, KawaiponCard> getCurrentCard() {
		return currentCard;
	}

	public Cache<String, Prize> getCurrentDrop() {
		return currentDrop;
	}

	public Cache<String, byte[]> getCardCache() {
		return cardCache;
	}

	public Cache<String, Integer> getPendingJoin() {
		return pendingJoin;
	}

	public Cache<String, Boolean> getRatelimit() {
		return ratelimit;
	}

	public Cache<String, Boolean> getConfirmationPending() {
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
