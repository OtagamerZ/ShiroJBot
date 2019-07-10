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

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.model.ReactionsList;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

public class BiteReaction extends Reaction {
	private static boolean answer = false;

	public BiteReaction(boolean isAnswer) {
		super("morder", new String[]{"moider", "bite", "moide"}, "Morde alguém.", Category.FUN);
		answer = isAnswer;
	}

	private static boolean isAnswer() {
		return answer;
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (message.getMentionedUsers().size() > 0) {
			this.setReaction(new String[]{
					"Snack!",
					"~~moide!",
					"Munch!"
			});

			this.setSelfTarget(new String[]{
					"Não, não, NÃO!",
					"Complicado ein!",
					"Não sou biscoito pra morder!"
			});

			if (message.getMentionedUsers().get(0) == Main.getInfo().getAPI().getSelfUser()) {
				Helper.sendReaction(ReactionsList.bite(), channel, author.getAsMention() + " tentou morder a " + Main.getInfo().getAPI().getSelfUser().getAsMention() + " - " + this.getSelfTarget()[this.getSelfTargetLength()], false);
				return;
			}

			if (!isAnswer())
				Helper.sendReaction(ReactionsList.bite(), channel, author.getAsMention() + " mordeu " + message.getMentionedUsers().get(0).getAsMention() + " - " + this.getReaction()[this.getReactionLength()], true);
			else
				Helper.sendReaction(ReactionsList.bite(), channel, message.getMentionedUsers().get(0).getAsMention() + " devolveu a mordida de " + author.getAsMention() + " - " + this.getReaction()[this.getReactionLength()], false);
		} else {
			Helper.typeMessage(channel, ":x: | Epa, você precisa mencionar alguém para morder!");
		}
	}
}
