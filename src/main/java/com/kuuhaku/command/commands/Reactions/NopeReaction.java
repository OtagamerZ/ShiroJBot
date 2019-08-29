/*
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.command.commands.Reactions;

import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

public class NopeReaction extends Reaction {
	public NopeReaction() {
		super("nope", new String[]{"sqn", "hojenão", "esquiva"}, "Evita a tentativa de alguém.");
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		this.setReaction(new String[]{
				"Hoje não!",
				"Fluido como a água!",
				"Ha ah, errou!"
		});

		Helper.sendReaction(getUrl("nope"), channel, author.getAsMention() + " esquivou! - " + this.getReaction()[this.getReactionLength()], false);
	}
}
