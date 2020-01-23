package com.kuuhaku.events;

import com.kuuhaku.controller.sqlite.MemberDAO;
import com.kuuhaku.model.Member;
import com.kuuhaku.model.jda.Guild;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.events.guild.member.update.GenericGuildMemberUpdateEvent;
import net.dv8tion.jda.api.events.guild.update.GenericGuildUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

public class UpdateEvents extends ListenerAdapter {
	@Override
	public void onGenericGuildUpdate(@Nonnull GenericGuildUpdateEvent event) {
		List<String> mbs = MemberDAO.getRegisteredUsers().stream().map(Member::getMid).collect(Collectors.toList());
		List<String> ids = event.getGuild().getMembers().stream().filter(m -> mbs.contains(m.getId())).map(net.dv8tion.jda.api.entities.Member::getId).collect(Collectors.toList());

		Helper.notifyDashboard(ids, event.getGuild().getId(), new Guild(event.getGuild().getId(), event.getGuild().getName(), event.getGuild().getIconUrl(), event.getGuild().getOwnerId()));
	}

	@Override
	public void onGenericGuildMemberUpdate(@Nonnull GenericGuildMemberUpdateEvent event) {
		com.kuuhaku.model.jda.Member m = new com.kuuhaku.model.jda.Member(event.getUser().getId(), event.getGuild().getId(), event.getUser().getName(), event.getMember().getNickname(), event.getUser().getAvatarUrl());

		Helper.notifyDashboard(event.getUser().getId(), event.getUser().getId(), m);
	}
}
