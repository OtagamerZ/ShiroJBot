/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.command.commands.reactions.answerable;

import com.kuuhaku.Main;
import com.kuuhaku.command.commands.reactions.Reaction;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;

public class SlapReaction extends Reaction {

	public SlapReaction() {
		super("estapear", new String[]{"tapa", "slap", "baka"}, "Dá um tapa em alguém.", true, "slap");
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		setInteraction(new User[]{author, message.getMentionedUsers().get(0)});

		if (message.getMentionedUsers().size() > 0) {
			this.setReaction(new String[]{
					"Kono BAKA!",
					"Eu faria o mesmo!",
					"Bem que mereceu!"
			});

			this.setSelfTarget(new String[]{
					"Fui treinada por meu Nii-chan, POR MEU NII-CHAN!",
					"Você não pode acertar quem você não vê!",
					"O que você achou que ia acontecer?!"
			});

			if (message.getMentionedUsers().get(0) == Main.getInfo().getAPI().getSelfUser()) {
				sendReaction(getType(), (TextChannel) channel, getInteraction()[0].getAsMention() + " tentou estapear a " + Main.getInfo().getAPI().getSelfUser().getAsMention() + " - " + this.getSelfTarget(), false);
				return;
			}

			sendReaction(getType(), (TextChannel) channel, getInteraction()[0].getAsMention() + " estapeou " + getInteraction()[1].getAsMention() + " - " + this.getReaction(), true);
		} else {
			Helper.typeMessage(channel, ":x: | Epa, você precisa mencionar alguém para dar um tapa!");
		}
	}

	@Override
	public void answer(TextChannel chn) {
		sendReaction(getType(), chn, getInteraction()[1].getAsMention() + " devolveu o tapa de " + getInteraction()[0].getAsMention() + " - " + this.getReaction(), false);
	}
}
