/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.controller.postgresql.BotStatsDAO;
import com.kuuhaku.controller.postgresql.MemberDAO;
import com.kuuhaku.controller.postgresql.VoiceTimeDAO;
import com.kuuhaku.handlers.api.websocket.EncoderClient;
import com.kuuhaku.model.persistent.MutedMember;
import com.kuuhaku.model.persistent.VoiceTime;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import javax.websocket.DeploymentException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MinuteEvent implements Job {
	public static JobDetail minute;

	@Override
	public void execute(JobExecutionContext context) {
		BotStatsDAO.register();

		if (!Main.getInfo().isEncoderConnected()) {
			try {
				Main.getInfo().setEncoderClient(new EncoderClient(ShiroInfo.SOCKET_ROOT + "/encoder"));
			} catch (URISyntaxException | DeploymentException | IOException ignore) {
			}
		}

		Collection<VoiceTime> voiceTimes = ShiroInfo.getShiroEvents().getVoiceTimes().values();
		for (VoiceTime vt : voiceTimes) {
			vt.update();
			VoiceTimeDAO.saveVoiceTime(vt);
		}

		for (MutedMember m : MemberDAO.getMutedMembers()) {
			Guild g = Main.getInfo().getGuildByID(m.getGuild());

			if (g == null) {
				MemberDAO.removeMutedMember(m);
			} else if (!m.isMuted()) {
				try {
					if (!g.getSelfMember().hasPermission(Permission.MANAGE_ROLES, Permission.MANAGE_CHANNEL, Permission.MANAGE_PERMISSIONS))
						continue;

					Member mb = g.getMemberById(m.getUid());
					if (mb == null) {
						MemberDAO.removeMutedMember(m);
						continue;
					}

					List<AuditableRestAction<Void>> act = new ArrayList<>();
					for (GuildChannel gc : g.getChannels()) {
						PermissionOverride po = gc.getPermissionOverride(mb);
						if (po != null)
							act.add(po.delete());
					}

					RestAction.allOf(act)
							.queue(s -> {
								Helper.logToChannel(g.getSelfMember().getUser(), false, null, mb.getAsMention() + " foi dessilenciado por " + g.getSelfMember().getAsMention(), g);
								MemberDAO.removeMutedMember(m);
							}, Helper::doNothing);
				} catch (HierarchyException ignore) {
				} catch (IllegalArgumentException | NullPointerException e) {
					MemberDAO.removeMutedMember(m);
				}
			}
		}
	}
}
