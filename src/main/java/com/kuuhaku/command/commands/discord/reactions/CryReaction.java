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

package com.kuuhaku.command.commands.discord.reactions;

import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

public class CryReaction extends Reaction {

    public CryReaction(@NonNls String name, @NonNls String[] aliases, String description, boolean answerable, @NonNls String type) {
        super(name, aliases, description, answerable, type);
    }

    @Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		this.setReaction(new String[]{
				"Faça isso parar!",
				"Sadboy feels.",
				"Me deixa sozinho(a)."
		});

		sendReaction(getType(), (TextChannel) channel, null, author.getAsMention() + " está chorando! - " + this.getReaction(), false);
	}

    @Override
    public void answer(TextChannel chn) {

    }
}
