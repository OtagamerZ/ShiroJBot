package com.rdx.events.guild;

import net.dv8tion.jda.core.hooks.ListenerAdapter;
/*
import com.kuuhaku.utils.Embeds;
import com.kuuhaku.model.guildConfig;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kuuhaku.controller.Database;
*/



public class GuildMemberEvents extends ListenerAdapter{

	/*
	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent user) {
		try {
			if (gcMap.get(user.getGuild().getId()).getCanalbv() != null) {
				Embeds.welcomeEmbed(user, gcMap.get(user.getGuild().getId()).getMsgBoasVindas(), user.getGuild().getTextChannelById(gcMap.get(user.getGuild().getId()).getCanalbv()));
				Map<String, Object> roles = gcMap.get(user.getGuild().getId()).getCargoNew();
				List<Role> list = new ArrayList<>();
				roles.values().forEach(r -> list.add(user.getGuild().getRoleById(r.toString())));
				if (gcMap.get(user.getGuild().getId()).getCargoNew().size() > 0)
					user.getGuild().getController().addRolesToMember(user.getMember(), list).queue();
			}
		} catch (Exception e) { e.printStackTrace(); }
	}

	@Override
	public void onGuildMemberLeave(GuildMemberLeaveEvent user) {
		try {
			if (gcMap.get(user.getGuild().getId()).getCanalbv() != null) {
				Embeds.byeEmbed(user, gcMap.get(user.getGuild().getId()).getMsgAdeus(), user.getGuild().getTextChannelById(gcMap.get(user.getGuild().getId()).getCanalbv()));
				if (memberMap.get(user.getUser().getId() + user.getGuild().getId()) != null)
					memberMap.remove(user.getUser().getId() + user.getGuild().getId());
			}
		} catch (Exception ignored) {}
	}
	*/
}