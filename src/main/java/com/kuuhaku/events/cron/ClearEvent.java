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

package com.kuuhaku.events.cron;

import com.kuuhaku.Main;
import com.kuuhaku.handlers.music.GuildMusicManager;
import com.kuuhaku.utils.Music;
import net.dv8tion.jda.api.entities.Guild;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.util.Objects;

public class ClearEvent implements Job {
	public static JobDetail clear;

	@Override
	public void execute(JobExecutionContext context) {
		for (Guild g : Main.getInfo().getAPI().getGuilds()) {
			GuildMusicManager gmm = Music.getGuildAudioPlayer(g);
			if (g.getAudioManager().isConnected() && (Objects.requireNonNull(g.getAudioManager().getConnectedChannel()).getMembers().size() < 1 || gmm.scheduler.queue().size() == 0)) {
				g.getAudioManager().closeAudioConnection();
				gmm.scheduler.clear();
				gmm.player.destroy();
			}
		}
	}
}
