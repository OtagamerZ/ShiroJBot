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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.GsonBuilder;
import com.kuuhaku.controller.postgresql.CanvasDAO;
import com.kuuhaku.controller.postgresql.VersionDAO;
import com.kuuhaku.events.JDAEvents;
import com.kuuhaku.handlers.api.websocket.WebSocketConfig;
import com.kuuhaku.handlers.games.tabletop.entity.Tabletop;
import com.kuuhaku.handlers.music.GuildMusicManager;
import com.kuuhaku.model.persistent.PixelCanvas;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.spaceprogram.kittycache.KittyCache;
import io.socket.client.Socket;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import org.apache.http.impl.client.HttpClientBuilder;
import org.discordbots.api.client.DiscordBotListAPI;
import org.jetbrains.annotations.NotNull;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@SuppressWarnings("localvariable")
public class ShiroInfo {

	//TODO Alternador do modo desenvolvimento (true quando utilizar em IDEs, false quando for dar push para o master)
	private static final boolean DEV = false;

	//CONSTANTS
	private static final ThreadMXBean tBean = ManagementFactory.getThreadMXBean();
	private static final ThreadPoolExecutor compilationPools = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
	private static final String BotToken = System.getenv("BOT_TOKEN");
	private static final String AnilistToken = System.getenv("ANILIST_TOKEN");
	private static final String YandexToken = System.getenv("YANDEX_TOKEN");
	private static final String YoutubeToken = System.getenv("YOUTUBE_TOKEN");
	private static final String DBLToken;
	private static final String name = "Shiro J. Bot";
	private static final String version = VersionDAO.getBuildVersion(Version.V3);
	private static final String supportServer = "Shiro Support";
	private static final String supportServerID = "421495229594730496";
	private static final String default_prefix = DEV ? "dev!" : "s!";
	private static final String nomeDB = "shiro.sqlite";
	private static final String niichan = "350836145921327115"; //KuuHaKu
	private static final ArrayList<String> developers = new ArrayList<String>() {{
		add(niichan); //KuuHaKu
		add("321665807988031495"); //Reydux
		add("694652893571055746"); //HeyCarlosz
	}};
	private static final ArrayList<String> editors = new ArrayList<String>() {{

	}};
	private static final ArrayList<String> supports = new ArrayList<String>() {{

	}};
	private static final Map<String, Map<String, String>> polls = new HashMap<>();
	private static final Map<Long, GuildMusicManager> gmms = new HashMap<>();
	private static final AudioPlayerManager apm = new DefaultAudioPlayerManager();
	private static final JDAEvents shiroEvents = new JDAEvents();
	private static final Map<String, KittyCache<String, Message>> messageCache = new HashMap<>();
	private static final GsonBuilder JSONFactory = new GsonBuilder();
	private static final LoadingCache<User, Boolean> ratelimit = CacheBuilder.newBuilder().expireAfterWrite(2, TimeUnit.SECONDS).build(
			new CacheLoader<User, Boolean>() {
				@Override
				public Boolean load(@NotNull User key) {
					return false;
				}
			}
	);
	private static final HttpClientBuilder httpBuilder = HttpClientBuilder.create();
	private static final Map<String, Tabletop> games = new HashMap<>();

	//STATIC CONSTRUCTOR
	static {
		if (System.getenv().containsKey("TOPGG_TOKEN")) DBLToken = System.getenv("TOPGG_TOKEN");
		else DBLToken = null;
	}

	private JDA api = null;
	private long startTime = 0;
	private String winner = "";
	private WebSocketConfig server;
	private Socket client = null;
	private final DiscordBotListAPI dblApi = DBLToken == null ? null : new DiscordBotListAPI.Builder()
			.token(DBLToken)
			.botId("572413282653306901")
			.build();

	//CONSTANTS
	//STATIC
	public static ResourceBundle getLocale(I18n lang) {
		return ResourceBundle.getBundle("locale", lang.getLocale());
	}

	public static void cache(Guild guild, Message message) {
		KittyCache<String, Message> cache = messageCache.getOrDefault(guild.getId(), new KittyCache<>(64));
		cache.put(message.getId(), message, (int) TimeUnit.DAYS.toSeconds(1));
		messageCache.put(guild.getId(), cache);
	}

	public static Message retrieveCachedMessage(Guild guild, String id) {
		return messageCache.getOrDefault(guild.getId(), new KittyCache<>(64)).get(id);
	}

	public static KittyCache<String, Message> retrieveCache(Guild guild) {
		return messageCache.getOrDefault(guild.getId(), new KittyCache<>(64));
	}

	public static String getSupportServer() {
		return supportServer;
	}

	public static GsonBuilder getJSONFactory() {
		return JSONFactory;
	}

	public static String getSupportServerID() {
		return supportServerID;
	}

	public static HttpClientBuilder getHttpBuilder() {
		return httpBuilder;
	}

	public static Map<String, Tabletop> getGames() {
		return games;
	}

	public static boolean gameInProgress(String id) {
		return games.keySet().stream().anyMatch(s -> Helper.containsAny(id, s.split(Pattern.quote("."))));
	}

	//NON-STATIC
	public float getCPULoad() {
		return (float) Helper.round(tBean.getCurrentThreadCpuTime() * 100, 2);
	}

	public ThreadPoolExecutor getPool() {
		return compilationPools;
	}

	public boolean isDev() {
		return DEV;
	}

	public String getBotToken() {
		return BotToken;
	}

	public String getYandexToken() {
		return YandexToken;
	}

	public String getAnilistToken() {
		return AnilistToken;
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

	public String getNiiChan() {
		return niichan;
	}

	public ArrayList<String> getDevelopers() {
		return developers;
	}

	public ArrayList<String> getEditors() {
		return editors;
	}

	public ArrayList<String> getSupports() {
		return supports;
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

	public static LoadingCache<User, Boolean> getRatelimit() {
		return ratelimit;
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

	public WebSocketConfig getServer() {
		return server;
	}

	public void setServer(WebSocketConfig server) {
		this.server = server;
	}

	public Socket getClient() {
		return client;
	}

	public Socket setClient(Socket client) {
		this.client = client;
		return this.client;
	}
}
