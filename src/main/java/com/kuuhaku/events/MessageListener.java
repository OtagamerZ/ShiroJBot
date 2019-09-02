package com.kuuhaku.events;

import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;

public abstract class MessageListener extends ListenerAdapter {
	@Override
	public abstract void onGenericMessageReaction(@Nonnull GenericMessageReactionEvent event);
}
