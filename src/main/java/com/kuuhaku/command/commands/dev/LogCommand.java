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
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NonNls;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

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
			HttpURLConnection hastebin = (HttpURLConnection) new URL("https://hastebin.com/documents").openConnection();
			hastebin.setRequestMethod("POST");
			hastebin.setRequestProperty("User-Agent", "Mozilla/5.0");
			hastebin.setDoOutput(true);
			hastebin.connect();

			try (OutputStream os = hastebin.getOutputStream()) {
				//noinspection ImplicitDefaultCharsetUsage
				os.write(IOUtils.toString(new FileReader(log)).getBytes(StandardCharsets.UTF_8));
			}

			String key;

			try (BufferedReader br = new BufferedReader(new InputStreamReader(hastebin.getInputStream(), StandardCharsets.UTF_8))) {
				String response;
				StringBuilder sb = new StringBuilder();

				while ((response = br.readLine()) != null) {
					sb.append(response.trim());
				}

				key = new JSONObject(sb.toString()).getString("key");
			}

			if (log.exists()) channel.sendMessage("Aqui está!\n" + "https://hastebin.com/" + key).queue();
			else channel.sendMessage(":x: | Arquivo de log não encontrado.").queue();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
