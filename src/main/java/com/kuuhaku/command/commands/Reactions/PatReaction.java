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
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

public class PatReaction extends Reaction {
	private static boolean answer = false;

	public PatReaction(boolean isAnswer) {
		super("cafuné", new String[]{"cafunhé", "pat", "cafu"}, "Faz cafuné em alguém.");
		answer = isAnswer;
	}

	private static boolean isAnswer() {
		return answer;
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (message.getMentionedUsers().size() > 0) {
			this.setReaction(new String[]{
					"Fuu~~",
					"Parece até um gato!",
					"Quem não gosta de um cafuné?"
			});

			this.setSelfTarget(new String[]{
					"Não, desgrudaa!",
					"Não sou um gato!",
					"Nem tenta!"
			});

			if (message.getMentionedUsers().get(0) == Main.getInfo().getAPI().getSelfUser()) {
				Helper.sendReaction(getUrl("pat"), channel, author.getAsMention() + " tentou fazer cafuné na " + Main.getInfo().getAPI().getSelfUser().getAsMention() + " - " + this.getSelfTarget()[this.getSelfTargetLength()], false);
				return;
			}

			if (!isAnswer())
				Helper.sendReaction(getUrl("pat"), channel, author.getAsMention() + " fez cafuné em " + message.getMentionedUsers().get(0).getAsMention() + " - " + this.getReaction()[this.getReactionLength()], true);
			else
				Helper.sendReaction(getUrl("pat"), channel, message.getMentionedUsers().get(1).getAsMention() + " fez outro cafuné em " + author.getAsMention() + " - " + this.getReaction()[this.getReactionLength()], false);
		} else {
			Helper.typeMessage(channel, ":x: | Epa, você precisa mencionar alguém para fazer cafuné!");
		}
	}
}
