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

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;

import java.util.ArrayList;

@SuppressWarnings("localvariable")
public class ShiroInfo {

	//TODO Alternador do modo desenvolvimento (true quando utilizar em IDEs, false quando for dar push para o master)
    private static final boolean DEV = true;

	//CONSTANTS
	private static final String BotToken = DEV ? "NTcyNzg0MzA1MTM5NDgyNjg2.XPZgPA.qCnxnU1bvukDLcZZKT_LhQWgKNY" : System.getenv("BOT_TOKEN");
	private static final String AnilistToken = DEV ? "client_credentials&Client_id=2107&Client_secret=4xJiVDdfa61xu1SOfSspNcPHfqoAh3PzpubDBrtH" : System.getenv("ANILIST_TOKEN");
	private static final String YandexToken = DEV ? "trnsl.1.1.20190604T123034Z.fbf5dbf78b4e7a52.5b560d01ee0357074266d549f24361d956761a56" : System.getenv("YANDEX_TOKEN");
	private static final String apiVersion = "3.8.3_463";
	private static final String name = "Shiro";
	private static final String version = "2.0";
	private static final String default_prefix = DEV ? "dev!" : "s!";
	private static final String nomeDB = "shiro.sqlite";
	private static final String niichan = "350836145921327115"; //KuuHaKu
	private static final ArrayList<String> developers = new ArrayList<String>() {{
		add("321665807988031495"); //Reydux
		add("350836145921327115"); //KuuHaKu
	}};

	private JDA api;
	private long startTime;
	private boolean ready = false;

	public ShiroInfo() {

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

	public Role getRoleByID(String roleID) {
		return api.getRoleById(roleID);
	}

	public Guild getGuildByID(String guildID) {
		return api.getGuildById(guildID);
	}
}
