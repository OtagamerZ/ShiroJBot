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

import com.kuuhaku.interfaces.SafeCallable;
import com.kuuhaku.model.enums.Role;
import com.kuuhaku.model.persistent.shiro.GlobalProperty;
import com.kuuhaku.model.persistent.user.Account;
import groovy.lang.GroovyShell;
import it.sauronsoftware.cron4j.Scheduler;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.security.SecureRandom;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

public abstract class Constants {
    protected static final String BOT_TOKEN = System.getenv("BOT_TOKEN");
    public static final String OWNER = "350836145921327115";
    public static final String SUPPORT_SERVER = "421495229594730496";
    public static final String DEFAULT_PREFIX = "x!"; // TODO Revert to s!
    public static final Logger LOGGER = Main.LOGGER;
    public static final GroovyShell GROOVY = new GroovyShell();
    public static final Scheduler SCHEDULER = new Scheduler();
    public static final String BOT_NAME = "Shiro J. Bot";
    public static final SafeCallable<String> BOT_VERSION = () -> "v4." + GlobalProperty.get("build_number", "0");

    public static final String EMOTE_REPO_1 = "666619034103447642";
    public static final String EMOTE_REPO_2 = "726171298044313694";
    public static final String EMOTE_REPO_3 = "732300321673576498";
    public static final String EMOTE_REPO_4 = "763775306095788033";

    public static final double P_HOURS_IN_DAY = 23 + (56d / 60) + (4d / 3600);

    public static final long MILLIS_IN_DAY = TimeUnit.DAYS.toMillis(1);
    public static final long MILLIS_IN_HOUR = TimeUnit.HOURS.toMillis(1);
    public static final long MILLIS_IN_MINUTE = TimeUnit.MINUTES.toMillis(1);
    public static final long MILLIS_IN_SECOND = TimeUnit.SECONDS.toMillis(1);
    public static final double GOLDEN_RATIO = (1 + Math.sqrt(5)) / 2;

    public static final String TIMESTAMP = "<t:%s:f>";
    public static final String TIMESTAMP_R = "<t:%s:R>";
    public static final String VOID = "\u200B";
    public static final String ACCEPT = "✅";

    public static final String SERVER_ROOT = System.getenv("SERVER_URL");
    public static final String API_ROOT = "https://api." + SERVER_ROOT + "/v2/";
    public static final String SOCKET_ROOT = "wss://socket." + SERVER_ROOT + "/v2/";
    public static final String ORIGIN_RESOURCES = System.getenv("GIT_ORIGIN")
            .replace("https://github.com/", "https://raw.githubusercontent.com/") +
            "/" + System.getenv("GIT_BRANCH") + "/src/main/resources/";

    public static final Function<Object, String> LOADING = o -> "<a:loading:697879726630502401> | " + o;

    private static final SplittableRandom SOURCE_RNG = new SplittableRandom();
    public static final Supplier<RandomGenerator> DEFAULT_RNG = SOURCE_RNG::split;
    public static final SecureRandom DEFAULT_SECURE_RNG = new SecureRandom();

    //public static final File COLLECTIONS_FOLDER = new File(System.getenv("COLLECTIONS_PATH"));
    //public static final File TEMPORARY_FOLDER = new File(System.getenv("TEMPORARY_PATH"));

    public static final Function<Member, Boolean> DEV_PRIVILEGE = m -> Account.hasRole(m.getId(), false, Role.DEVELOPER);
    public static final Function<Member, Boolean> SUP_PRIVILEGE = m -> Account.hasRole(m.getId(), false, Role.SUPPORT);
    public static final Function<Member, Boolean> TST_PRIVILEGE = m -> Account.hasRole(m.getId(), false, Role.TESTER);
    public static final Function<Member, Boolean> REV_PRIVILEGE = m -> Account.hasRole(m.getId(), false, Role.REVIEWER);
    public static final Function<Member, Boolean> STF_PRIVILEGE = m -> Account.hasRole(m.getId(), false, Role.SUPPORT, Role.TESTER, Role.REVIEWER);
    public static final Function<Member, Boolean> MOD_PRIVILEGE = m -> SUP_PRIVILEGE.apply(m) || m.hasPermission(Permission.KICK_MEMBERS);
    public static final Function<Member, Boolean> USER_PRIVILEGE = m -> true;

    public static final Map<RenderingHints.Key, Object> HD_HINTS = Map.of(
            RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB,
            RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY,
            RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY,
            RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY,
            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON,
            RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR,
            RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE,
            RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON
    );
    public static final Map<RenderingHints.Key, Object> SD_HINTS = Map.of(
            RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
            RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED,
            RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED,
            RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED,
            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF,
            RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
    );
}
