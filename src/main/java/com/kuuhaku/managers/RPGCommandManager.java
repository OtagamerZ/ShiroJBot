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
import com.kuuhaku.command.commands.rpg.*;

import java.util.ArrayList;
import java.util.List;

public class RPGCommandManager {

	private final List<Command> commands;

    public RPGCommandManager() {
        commands = new ArrayList<Command>() {{
            add(new NewCampaignCommand());
            add(new EndCampaignCommand());
            add(new AttackCommand());
            add(new BagCommand());
            add(new EquipCommand());
            add(new EquippedCommand());
            add(new GiveCommand());
            add(new MapCommand());
            add(new MoveCommand());
            add(new NewItemCommand());
            add(new NewMapCommand());
            add(new NewMobCommand());
            add(new NewPlayerCommand());
            add(new RemoveCommand());
            add(new RollCommand());
            add(new StatusCommand());
            add(new SwitchMapCommand());
            add(new TakeCommand());
            add(new UnequipCommand());
            add(new ViewCommand());
            add(new WorldListCommand());
            add(new ChestCommand());
            add(new NewChestCommand());
        }};
    }

    public List<Command> getCommands() {
        return commands;
    }
}
