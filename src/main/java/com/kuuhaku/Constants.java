/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.enums.Role;
import com.kuuhaku.model.persistent.user.Account;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public abstract class Constants {
	protected static final String BOT_TOKEN = System.getenv("BOT_TOKEN");
	public static final String OWNER = "350836145921327115";
	public static final String DEFAULT_PREFIX = "x!"; // TODO Revert to s!
	public static final Logger LOGGER = LogManager.getLogger("shiro");

	public static final double P_HOURS_IN_DAY = 23 + (56d / 60) + (4d / 3600);

	public static final long MILLIS_IN_DAY = TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS);
	public static final long MILLIS_IN_HOUR = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);
	public static final long MILLIS_IN_MINUTE = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
	public static final long MILLIS_IN_SECOND = TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS);
	public static final double GOLDEN_RATIO = (1 + Math.sqrt(5)) / 2;

	public static final String TIMESTAMP = "<t:%s:R>";
	public static final String TIMESTAMP_R = "<t:%s:R>";
	public static final String VOID = "\u200B";
	public static final String ACCEPT = "✅";

	public static final Random DEFAULT_RNG = new Random();
	public static final Random DEFAULT_SECURE_RNG = new SecureRandom();

	//public static final File COLLECTIONS_FOLDER = new File(System.getenv("COLLECTIONS_PATH"));
	//public static final File TEMPORARY_FOLDER = new File(System.getenv("TEMPORARY_PATH"));

	public static final Function<Member, Boolean> DEV_PRIVILEGE = m -> DAO.find(Account.class, m.getId()).getRole().allowed(Role.DEVELOPER);
	public static final Function<Member, Boolean> SUP_PRIVILEGE = m -> DAO.find(Account.class, m.getId()).getRole().allowed(Role.SUPPORT);
	public static final Function<Member, Boolean> TST_PRIVILEGE = m -> DAO.find(Account.class, m.getId()).getRole().allowed(Role.TESTER);
	public static final Function<Member, Boolean> REV_PRIVILEGE = m -> DAO.find(Account.class, m.getId()).getRole().allowed(Role.REVIEWER);
	public static final Function<Member, Boolean> STF_PRIVILEGE = m -> SUP_PRIVILEGE.apply(m) || TST_PRIVILEGE.apply(m) || REV_PRIVILEGE.apply(m);
	public static final Function<Member, Boolean> MOD_PRIVILEGE = m -> SUP_PRIVILEGE.apply(m) || m.hasPermission(Permission.KICK_MEMBERS);
	public static final Function<Member, Boolean> USER_PRIVILEGE = m -> true;

	public static final Map<RenderingHints.Key, Object> HD_HINTS = Map.of(
			RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
			RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY,
			RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON,
			RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC
	);
	public static final Map<RenderingHints.Key, Object> SD_HINTS = Map.of(
			RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
			RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED,
			RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF,
			RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR
	);
}
