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

import com.kuuhaku.manager.CacheManager;
import com.kuuhaku.manager.CommandManager;
import com.sun.management.OperatingSystemMXBean;
import org.apache.commons.lang3.time.StopWatch;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.time.LocalDate;

public class Main {
	private static final OperatingSystemMXBean info = ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean());
	protected static final StopWatch boot = new StopWatch();

	private static final CacheManager cacheManager = new CacheManager();
	private static final CommandManager commandManager = new CommandManager();
	private static Application app;

	public static void main(String[] args) {
		boot.start();
		Constants.LOGGER.info("""

				----------------------------------------------------------
				Shiro J. Bot  Copyright (C) 2019-%s Yago Gimenez (KuuHaKu)
				This program comes with ABSOLUTELY NO WARRANTY
				This is free software, and you are welcome to redistribute it under certain conditions
				See license for more information regarding redistribution conditions
				----------------------------------------------------------
				SYSTEM INFO
				----------------------------------------------------------
				Name: %s
				Version: %s
				Architecture: %s
				CPU: %s cores
				Memory: %s GiB (Allocated: %s GiB)
				Charset: %s
				----------------------------------------------------------
				END OF SUMMARY
				----------------------------------------------------------
				"""
				.formatted(
						LocalDate.now().getYear(),
						info.getName(),
						info.getVersion(),
						info.getArch(),
						Runtime.getRuntime().availableProcessors(),
						Math.round(info.getTotalMemorySize() / Math.pow(1024, 3)),
						Math.round(Runtime.getRuntime().maxMemory() / Math.pow(1024, 3)),
						Charset.defaultCharset()
				)
		);

		ImageIO.setUseCache(false);
		Thread.setDefaultUncaughtExceptionHandler(app = new Application());
	}

	public static CacheManager getCacheManager() {
		return cacheManager;
	}

	public static CommandManager getCommandManager() {
		return commandManager;
	}

	@Nonnull
	public static Application getApp() {
		return app;
	}
}
