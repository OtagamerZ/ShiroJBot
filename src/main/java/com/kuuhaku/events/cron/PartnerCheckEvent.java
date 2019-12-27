package com.kuuhaku.events.cron;

import com.kuuhaku.Main;
import com.kuuhaku.controller.MySQL.TagDAO;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.Guild;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import javax.persistence.NoResultException;
import java.util.Objects;

public class PartnerCheckEvent implements Job {
	public static JobDetail check;

	@Override
	public void execute(JobExecutionContext context) {
		Main.getJibril().getGuilds().forEach(PartnerCheckEvent::notif);
		Main.getTet().getGuilds().forEach(PartnerCheckEvent::notif);
	}

	private static void notif(Guild g) {
		try {
			if (!TagDAO.getTagById(g.getOwnerId()).isPartner() && !Main.getInfo().getDevelopers().contains(g.getOwnerId())) {
				g.leave().queue();
				Helper.logger(PartnerCheckEvent.class).info("Saí do servidor " + g.getName() + " por " + Objects.requireNonNull(g.getOwner()).getUser().getAsTag() + " não estar na lista de parceiros.");
			}
		} catch (NoResultException e) {
			g.leave().queue();
			Helper.logger(PartnerCheckEvent.class).info("Saí do servidor " + g.getName() + " por " + Objects.requireNonNull(g.getOwner()).getUser().getAsTag() + " não possuir tags.");
		}
	}
}
