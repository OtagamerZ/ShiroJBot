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
import com.kuuhaku.command.commands.fun.*;
import com.kuuhaku.command.commands.information.*;
import com.kuuhaku.command.commands.misc.*;
import com.kuuhaku.command.commands.moderation.*;
import com.kuuhaku.command.commands.dev.*;
import com.kuuhaku.command.commands.partner.AiCommand;
import com.kuuhaku.command.commands.partner.JibrilCommand;

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
            add(new VerifiedTagCommand());
            add(new RelaysCommand());
            add(new BlockCommand());
            add(new LogCommand());

            //PARTNER
            add(new JibrilCommand());
            add(new AiCommand());

            //MODERATION
            add(new RemoveAnswerCommand());
            add(new SettingsCommand());
            add(new AllowCommunityCommand());
            add(new KickMemberCommand());
            add(new BanMemberCommand());
            add(new NoLinkCommand());

            //INFORMATION
            add(new ComandosCommand());
            add(new ProfileCommand());
            add(new ReportBugCommand());
            add(new ReportUserCommand());
            add(new BackgroundCommand());
            add(new BiographyCommand());
            add(new RelayCommand());
            add(new TagsCommand());
            add(new MyTagsCommand());
            add(new BotInfoCommand());
            add(new URankCommand());

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
            add(new ValidateGIFCommand());
            add(new LocalEmoteListCommand());
            add(new ShiroEmoteListCommand());
            add(new JibrilEmoteListCommand());

            //FUN
            add(new SadButTrueCommand());
            add(new HardDecisionCommand());
            add(new ExpandingBrainCommand());
            add(new PPTCommand());
            add(new ShipCommand());
            //REAÇÕES
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
