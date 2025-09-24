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

import com.kuuhaku.controller.DAO;
import com.kuuhaku.manager.CacheManager;
import com.kuuhaku.manager.CommandManager;
import com.kuuhaku.manager.ScheduleManager;
import com.kuuhaku.model.common.ExecChain;
import com.kuuhaku.model.persistent.shiro.GlobalProperty;
import com.sun.management.OperatingSystemMXBean;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.StopWatch;

import javax.imageio.ImageIO;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.time.LocalDate;

public class Main {
	public static final ExecChain READY = new ExecChain();
	private static final OperatingSystemMXBean info = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
	protected static final StopWatch boot = new StopWatch();

	private static final CacheManager cacheManager = new CacheManager();
	private static final CommandManager commandManager = new CommandManager();
	private static final ScheduleManager scheduleManager = new ScheduleManager();

	private static Application app;

	public static void main(String[] args) {
		boot.start();
		Constants.LOGGER.info("""
						
						----------------------------------------------------------
						Shiro J. Bot  Copyright (C) 2019-{} Yago Gimenez (KuuHaKu)
						This program comes with ABSOLUTELY NO WARRANTY
						This is free software, and you are welcome to redistribute it under certain conditions
						See license for more information regarding redistribution conditions
						----------------------------------------------------------
						SYSTEM INFO
						----------------------------------------------------------
						Name: {}
						Version: {}
						Architecture: {}
						CPU: {} cores
						Memory: {} GiB (Allocated: {} GiB)
						Charset: {}
						Java runtime: {} {}
						----------------------------------------------------------
						END OF SUMMARY
						----------------------------------------------------------
						""",
				LocalDate.now().getYear(),
				info.getName(),
				info.getVersion(),
				info.getArch(),
				Runtime.getRuntime().availableProcessors(),
				Math.round(info.getTotalMemorySize() / Math.pow(1024, 3)),
				Math.round(Runtime.getRuntime().maxMemory() / Math.pow(1024, 3)),
				Charset.defaultCharset(),
				System.getProperty("java.vm.name"),
				System.getProperty("java.vendor.version")
		);

		GlobalProperty ver = DAO.find(GlobalProperty.class, "build_number");
		if (ver == null) {
			ver = new GlobalProperty("build_number", "0");
		}

		ver.setValue(NumberUtils.toInt(ver.getValue()) + 1);
		ver.save();

		ImageIO.setUseCache(false);
		Thread.setDefaultUncaughtExceptionHandler(app = new Application());
		READY.run();
	}

	public static CacheManager getCacheManager() {
		return cacheManager;
	}

	public static CommandManager getCommandManager() {
		return commandManager;
	}

	public static ScheduleManager getScheduleManager() {
		return scheduleManager;
	}

	public static Application getApp() {
		return app;
	}
}
