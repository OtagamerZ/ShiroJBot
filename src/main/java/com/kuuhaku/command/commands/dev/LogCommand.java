/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.command.commands.dev;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.I18n;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class LogCommand extends Command {

	public LogCommand(@NonNls String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public LogCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public LogCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public LogCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		File log = new File("logs/stacktrace.log");
		try {
			String stringLog = IOUtils.toString(new FileReader(log));

			stringLog = StringUtils.reverse(stringLog);
			stringLog = StringUtils.reverse(stringLog.substring(0, Math.min(stringLog.length(), 5242880)));

			File croppedLog = File.createTempFile("log_" + System.currentTimeMillis(), ".txt");

			PrintWriter writer = new PrintWriter(croppedLog);
			writer.print(stringLog);

			if (log.exists())
				channel.sendMessage("Aqui está!").addFile(croppedLog, "stacktrace.txt").queue(s -> croppedLog.delete());
			else channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_log-not-found")).queue();
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}
}
