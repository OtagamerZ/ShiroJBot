/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.controller.postgresql.TagDAO;
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
