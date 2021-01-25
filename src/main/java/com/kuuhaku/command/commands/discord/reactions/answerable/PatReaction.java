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

package com.kuuhaku.command.commands.discord.reactions.answerable;

import com.kuuhaku.Main;
import com.kuuhaku.command.commands.discord.reactions.Reaction;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

public class PatReaction extends Reaction {

	public PatReaction(@NonNls String name, @NonNls String[] aliases, String description, boolean answerable, @NonNls String type) {
		super(name, aliases, description, answerable, type);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (message.getMentionedUsers().size() == 0) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_no-user")).queue();
			return;
		}
		setInteraction(new User[]{author, message.getMentionedUsers().get(0)});

		if (message.getMentionedUsers().size() > 0) {
			this.setReaction(new String[]{
					"Fuu~~",
					"Purr purr~~",
					"Quem não gosta de um cafuné?"
			});

			this.setSelfTarget(new String[]{
					"Não, desgrudaa!",
					"Não sou um gato!",
					"Nem tenta!"
			});

			if (message.getMentionedUsers().get(0) == Main.getSelfUser()) {
				sendReaction(getType(), (TextChannel) channel, getInteraction()[1], getInteraction()[0].getAsMention() + " tentou acariciar a " + Main.getSelfUser().getAsMention() + " - " + this.getSelfTarget(), false);
				return;
			}

			sendReaction(getType(), (TextChannel) channel, getInteraction()[1], getInteraction()[0].getAsMention() + " acariciou " + getInteraction()[1].getAsMention() + " - " + this.getReaction(), true);
		} else {
			Helper.typeMessage(channel, "❌ | Epa, você precisa mencionar alguém para fazer cafuné!");
		}
	}

	@Override
	public void answer(TextChannel chn) {
		sendReaction(getType(), chn, null, getInteraction()[1].getAsMention() + " devolveu o cafuné de " + getInteraction()[0].getAsMention() + " - " + this.getReaction(), false);
	}
}
