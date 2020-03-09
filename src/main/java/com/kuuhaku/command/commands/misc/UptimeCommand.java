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

package com.kuuhaku.command.commands.misc;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class UptimeCommand extends Command {

	public UptimeCommand(@NonNls String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public UptimeCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public UptimeCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public UptimeCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {

		long uptimeSec = Instant.now().getEpochSecond() - Main.getInfo().getStartTime();

		int dias = (int) TimeUnit.SECONDS.toDays(uptimeSec);
		long horas = TimeUnit.SECONDS.toHours(uptimeSec) - (dias * 24);
		long minutos = TimeUnit.SECONDS.toMinutes(uptimeSec) - (TimeUnit.SECONDS.toHours(uptimeSec) * 60);
		long segundos = TimeUnit.SECONDS.toSeconds(uptimeSec) - (TimeUnit.SECONDS.toMinutes(uptimeSec) * 60);

		String uptime = dias + " dias, " + horas + " horas, " + minutos + " mins e " + segundos + " segundos.";

		channel.sendMessage("Hummm... acho que estou acordada há " + uptime).queue();
	}

}
