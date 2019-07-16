/*
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.utils;

import com.ibm.cloud.sdk.core.service.security.IamOptions;
import com.ibm.watson.assistant.v1.Assistant;
import com.ibm.watson.assistant.v1.model.Context;
import com.ibm.watson.assistant.v1.model.MessageResponse;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.SelfUser;
import net.dv8tion.jda.core.entities.User;
import org.discordbots.api.client.DiscordBotListAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

@SuppressWarnings("localvariable")
public class ShiroInfo {

	//TODO Alternador do modo desenvolvimento (true quando utilizar em IDEs, false quando for dar push para o master)
	private static final boolean DEV = false;

	//CONSTANTS
	private static final String BotToken = System.getenv("BOT_TOKEN");
	private static final String AnilistToken = System.getenv("ANILIST_TOKEN");
	private static final String YandexToken = System.getenv("YANDEX_TOKEN");
	private static final String infoInstance = System.getenv("WORKSPACE_ID");
	private static final String apiVersion = "3.8.3_463";
	private static final String name = "Shiro";
	private static final String version = "2.0";
	private static final String default_prefix = DEV ? "dev!" : "s!";
	private static final String nomeDB = "shiro.sqlite";
	private static final String niichan = "350836145921327115"; //KuuHaKu
	private static final ArrayList<String> developers = new ArrayList<String>() {{
		add("350836145921327115"); //KuuHaKu
		add("321665807988031495"); //Reydux
	}};
	private static final ArrayList<String> editors = new ArrayList<String>() {{

	}};
	private static final DiscordBotListAPI dbl = new DiscordBotListAPI.Builder().token(System.getenv("DBL_TOKEN")).botId("572413282653306901").build();
	private static final IamOptions options = new IamOptions.Builder().apiKey(System.getenv("AI_TOKEN")).build();
	private static final Assistant ai = new Assistant("2019-06-27", options);
	public static final List<User[]> queue = new ArrayList<>();
	private static final Map<String, Integer[]> polls = new HashMap<>();
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

	private JDA api;
	private long startTime;
	private boolean ready = false;
	private Context context = new Context();

	public ShiroInfo() {
		ai.setEndPoint("https://gateway.watsonplatform.net/assistant/api");
	}

	//CONSTANTS
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

	public String getInfoInstance() {
		return infoInstance;
	}

	public String getApiVersion() {
		return apiVersion;
	}

	public String getName() {
		return name;
	}

	public String getFullName() {
		return name + " v" + version;
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

	public DiscordBotListAPI getDBL() {
		return dbl;
	}

	public Assistant getAi() {
		return ai;
	}

	public List<User[]> getQueue() {
		return queue;
	}

	public Map<String, Integer[]> getPolls() {
		return polls;
	}

	public ScheduledExecutorService getScheduler() {
		return scheduler;
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

	public long getPing() {
		return api.getPing();
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public boolean isReady() {
		return ready;
	}

	public void setReady(boolean ready) {
		this.ready = ready;
	}

	public User getUserByID(String userID) {
		return api.getUserById(userID);
	}

	Role getRoleByID(String roleID) {
		return api.getRoleById(roleID);
	}

	public Guild getGuildByID(String guildID) {
		return api.getGuildById(guildID);
	}

	public Context getContext() {
		return context;
	}

	public void updateContext(MessageResponse response) {
		context = response.getContext();
	}
}
