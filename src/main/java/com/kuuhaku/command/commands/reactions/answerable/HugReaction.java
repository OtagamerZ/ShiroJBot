/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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
import org.jetbrains.annotations.NonNls;

public class HugReaction extends Reaction {

	public HugReaction(@NonNls String name, @NonNls String[] aliases, String description, boolean answerable, @NonNls String type) {
		super(name, aliases, description, answerable, type);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (message.getMentionedUsers().size() > 0) {
			setInteraction(new User[]{author, message.getMentionedUsers().get(0)});

			this.setReaction(new String[]{
					"Awnn...ai qualquer um shippa!",
					"Ai sim ein, vai pra cima garoto(a)!",
			});

			this.setSelfTarget(new String[]{
					"Ow ow ow, sem pegação!",
					"Meu Nii-chan vai ficar bravo com isso!",
					"Moshi moshi, FBI-sama?"
			});

			if (getInteraction()[1] == Main.getInfo().getAPI().getSelfUser()) {
				sendReaction(getType(), (TextChannel) channel, getInteraction()[0].getAsMention() + " tentou abraçar a " + Main.getInfo().getAPI().getSelfUser().getAsMention() + " - " + this.getSelfTarget(), false);
				return;
			}

			sendReaction(getType(), (TextChannel) channel, getInteraction()[0].getAsMention() + " abraçou " + getInteraction()[1].getAsMention() + " - " + this.getReaction(), true);
		} else {
			Helper.typeMessage(channel, ":x: | Epa, você precisa mencionar alguém para abraçar!");
		}
	}

	@Override
	public void answer(TextChannel chn) {
		sendReaction(getType(), chn, getInteraction()[1].getAsMention() + " devolveu o abraço de " + getInteraction()[0].getAsMention() + " - " + this.getReaction(), false);
	}
}
