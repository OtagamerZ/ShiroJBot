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

public class LaughReaction extends Reaction {
	public LaughReaction() {
		super("rir", new String[]{"kkk", "laugh", "aiai"}, "Ri.");
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		this.setReaction(new String[]{
				"Kkkkkkkk!",
				"Rsrsrsrs!",
				"Asdjasdnlad."
		});

		Helper.sendReaction(getUrl("laugh"), channel, author.getAsMention() + " está rindo! - " + this.getReaction()[this.getReactionLength()], false);
	}
}
