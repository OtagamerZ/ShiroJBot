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
import com.kuuhaku.command.commands.Reactions.*;
import com.kuuhaku.command.commands.beyblade.*;
import com.kuuhaku.command.commands.fun.OhNoCommand;
import com.kuuhaku.command.commands.fun.PPTCommand;
import com.kuuhaku.command.commands.information.*;
import com.kuuhaku.command.commands.misc.*;
import com.kuuhaku.command.commands.moderation.AllowCommunityCommand;
import com.kuuhaku.command.commands.moderation.RemoveAnswerCommand;
import com.kuuhaku.command.commands.moderation.SettingsCommand;
import com.kuuhaku.command.commands.dev.*;

import java.util.ArrayList;
import java.util.List;

public class CommandManager {

    private List<Command> commands;

    public CommandManager() {
        commands = new ArrayList<Command>() {{
            //DEV
            add(new KillCommand());
            add(new LeaveCommand());
            add(new ToxicTagCommand());
            add(new PartnerTagCommand());

            //MODERATION
            add(new RemoveAnswerCommand());
            add(new SettingsCommand());
            add(new AllowCommunityCommand());

            //INFORMATION
            add(new ComandosCommand());
            add(new ProfileCommand());
            add(new ReportBugCommand());
            add(new BackgroundCommand());
            add(new RelayCommand());

            //MISC
            add(new AsciiCommand());
            add(new AvatarCommand());
            add(new FlipCoinCommand());
            add(new PingCommand());
            add(new ReverseCommand());
            add(new SayCommand());
            add(new UptimeCommand());
            add(new CustomAnswerCommand());
            add(new AnimeCommand());
            add(new ImageCommand());

            //FUN
            add(new OhNoCommand());
            add(new PPTCommand());
            //RECIPROCOS
            add(new HugReaction(false));
            add(new KissReaction(false));
            add(new PatReaction(false));
            add(new StareReaction(false));
            add(new SlapReaction(false));
            add(new PunchReaction(false));
            add(new BiteReaction(false));
            //NÃO RECIPROCOS
            add(new BlushReaction());
            add(new CryReaction());
            add(new DanceReaction());
            add(new FacedeskReaction());
            add(new LaughReaction());
            add(new NopeReaction());
            add(new RunReaction());

            //BEYBLADE
            add(new StartCommand());
            add(new ColorCommand());
            add(new DuelCommand());
            add(new ShopCommand());
            add(new AlignmentCommand());
            add(new InfoCommand());
            add(new RankCommand());
            add(new SlotsCommand());
            add(new RedeemCommand());
        }};
    }

    public List<Command> getCommands() {
        return commands;
    }
}
