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
import com.kuuhaku.command.Reactions.HugReaction;
import com.kuuhaku.command.commands.fun.OhNoCommand;
import com.kuuhaku.command.commands.fun.PPTCommand;
import com.kuuhaku.command.commands.information.ComandosCommand;
import com.kuuhaku.command.commands.misc.*;
import com.kuuhaku.command.commands.owner.KillCommand;
import com.kuuhaku.command.commands.owner.LeaveCommand;

import java.util.ArrayList;
import java.util.List;

//import com.kuuhaku.command.commands.moderation.*;

public class CommandManager {

    private List<Command> commands;

    public CommandManager() {
        commands = new ArrayList<Command>() {{
            add(new HugReaction(false));
        }};

        //EXEMPLO: commands.add(new KillCommand());

        //OWNER
        commands.add(new KillCommand());
        commands.add(new LeaveCommand());

        //MODERATION

        //INFORMATION
        commands.add(new ComandosCommand());

        //MISC
        commands.add(new AsciiCommand());
        commands.add(new AvatarCommand());
        commands.add(new FlipCoinCommand());
        commands.add(new PingCommand());
        commands.add(new ReverseCommand());
        commands.add(new SayCommand());
        commands.add(new UptimeCommand());

        //FUN
        commands.add(new OhNoCommand());
        commands.add(new PPTCommand());
    }

    public List<Command> getCommands() {
        return commands;
    }
}
