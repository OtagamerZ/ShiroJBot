package com.kuuhaku.model.common;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.internal.entities.ReceivedMessage;

import java.util.List;

public class FakeMessage extends ReceivedMessage {
	public FakeMessage(Guild guild, MessageChannel channel, Member member, String content) {
		super(0,
				channel.getIdLong(),
				guild.getIdLong(),
				guild.getJDA(),
				guild,
				channel,
				MessageType.DEFAULT,
				null,
				false,
				guild.getJDA().getSelfUser().getApplicationIdLong(),
				false,
				false,
				content,
				null,
				member.getUser(),
				member,
				null,
				null,
				null,
				null,
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				0,
				null,
				null,
				null,
				0
		);
	}
}
