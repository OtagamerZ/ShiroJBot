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

public class ShiroInfo {

    private String token;
    private String apiVersion;
    private String name;
    private String version;
    private String default_prefix;
    private String nomeDB;
    private JDA api;
    private ArrayList<String> developers;
    private String niichan;
    private boolean niimode;
    private long startTime;

    public ShiroInfo() {
        token = "NTcyNzg0MzA1MTM5NDgyNjg2.XOxx0A.gvatPLOCKLTYVsXXsid1V_mW0H8";

        apiVersion = "3.8.3_463";

        name = "Shiro";
        version = "2.0";

        default_prefix = "s!";

        nomeDB = "shiro.sqlite";

        developers = new ArrayList<String>() {{
            add("321665807988031495"); //Reydux
            add("350836145921327115"); //KuuHaKu
        }};

        niichan = "350836145921327115"; //KuuHaKu
    }

    public JDA getAPI() {
        return api;
    }

    public void setAPI(JDA api) {
        this.api = api;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getToken() {
        return token;
    }

    public String getDefaultPrefix() {
        return default_prefix;
    }

    public String getDBFileName() {
        return nomeDB;
    }

    public ArrayList<String> getDevelopers() {
        return developers;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getFullName() {
        return name + " v" + version;
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

    public User getUserByID(String userID) { return api.getUserById(userID); }

    Role getRoleByID(String roleID) { return api.getRoleById(roleID); }

    public String getNiiChan() {
        return niichan;
    }

    public boolean isNiimode() {
        return niimode;
    }

    public void switchNiimode() {
        niimode = !niimode;
    }
}
