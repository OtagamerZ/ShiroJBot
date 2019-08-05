package com.kuuhaku.events.cron;

import com.kuuhaku.Main;
import com.kuuhaku.handlers.music.GuildMusicManager;
import com.kuuhaku.utils.Music;
import net.dv8tion.jda.core.entities.Guild;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

public class ClearEvent implements Job {
	public static JobDetail clear;

	@Override
	public void execute(JobExecutionContext context) {
		for (Guild g : Main.getInfo().getAPI().getGuilds()) {
			GuildMusicManager gmm = Music.getGuildAudioPlayer(g);
			if (g.getAudioManager().isConnected() && (g.getAudioManager().getConnectedChannel().getMembers().size() < 2 || gmm.scheduler.queue().size() == 0)) {
				g.getAudioManager().closeAudioConnection();
				gmm.scheduler.clear();
				gmm.player.destroy();
			}
		}
	}
}
