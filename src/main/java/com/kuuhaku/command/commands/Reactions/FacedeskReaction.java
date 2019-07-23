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

import com.kuuhaku.model.ReactionsList;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

public class FacedeskReaction extends Reaction {
    public FacedeskReaction() {
		super("facedesk", new String[]{"mds", "ahnão", "nss"}, "Reage a algo quem não é possível que alguém tenha feito.");
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
            this.setReaction(new String[]{
                    "Nuss.",
                    "Bah.",
                    "Meeeeee."
            });

            Helper.sendReaction(ReactionsList.facedesk(), channel, author.getAsMention() + " não ta acreditando nisso! - " + this.getReaction()[this.getReactionLength()], false);
    }
}
