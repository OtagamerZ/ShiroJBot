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

package com.kuuhaku.managers;

import com.kuuhaku.command.Command;
import com.kuuhaku.command.commands.Reactions.HugReaction;
import com.kuuhaku.command.commands.fun.OhNoCommand;
import com.kuuhaku.command.commands.fun.PPTCommand;
import com.kuuhaku.command.commands.information.BackgroundCommand;
import com.kuuhaku.command.commands.information.ComandosCommand;
import com.kuuhaku.command.commands.information.ProfileCommand;
import com.kuuhaku.command.commands.information.ReportBugCommand;
import com.kuuhaku.command.commands.misc.*;
import com.kuuhaku.command.commands.owner.KillCommand;
import com.kuuhaku.command.commands.owner.LeaveCommand;
import com.kuuhaku.command.commands.owner.PartnerTagCommand;
import com.kuuhaku.command.commands.owner.ToxicTagCommand;

import java.util.ArrayList;
import java.util.List;

//import com.kuuhaku.command.commands.moderation.*;

public class CommandManager {

    private List<Command> commands;

    public CommandManager() {
        commands = new ArrayList<Command>() {{
            //EXEMPLO: commands.add(new KillCommand());

            //OWNER
            add(new KillCommand());
            add(new LeaveCommand());
            add(new ToxicTagCommand());
            add(new PartnerTagCommand());

            //MODERATION

            //INFORMATION
            add(new ComandosCommand());
            add(new ProfileCommand());
            add(new ReportBugCommand());
            add(new BackgroundCommand());

            //MISC
            add(new AsciiCommand());
            add(new AvatarCommand());
            add(new FlipCoinCommand());
            add(new PingCommand());
            add(new ReverseCommand());
            add(new SayCommand());
            add(new UptimeCommand());

            //FUN
            add(new OhNoCommand());
            add(new PPTCommand());
            add(new HugReaction(false));
        }};
    }

    public List<Command> getCommands() {
        return commands;
    }
}
