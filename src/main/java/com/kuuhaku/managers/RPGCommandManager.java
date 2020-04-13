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

package com.kuuhaku.managers;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.command.commands.rpg.*;

import java.util.ArrayList;
import java.util.List;

import static com.kuuhaku.utils.I18n.PT;
import static com.kuuhaku.utils.ShiroInfo.getLocale;

public class RPGCommandManager {

    private static final String REQ_ITEM = "req_item";
    private static final String REQ_MENTION = "req_mention";
    private final List<Command> commands;

    public RPGCommandManager() {
        commands = new ArrayList<Command>() {{
            add(new NewCampaignCommand(
                    "rnovacampanha", new String[]{"rnewcampaign", "rnewgame"}, getLocale(PT).getString("rpg_new-campaign"), Category.RPG, false
            ));
            add(new EndCampaignCommand(
                    "rfimdejogo", new String[]{"rendcampaign", "rgameover"}, getLocale(PT).getString("rpg_end-campaign"), Category.RPG, false
            ));
            add(new AttackCommand(
                    "ratacar", new String[]{"rattack", "rdamage", "rdmg"}, getLocale(PT).getString(REQ_MENTION), getLocale(PT).getString("rpg_cure"), Category.RPG, false
            ));
            add(new BagCommand(
                    "rbolsa", new String[]{"rbag"}, getLocale(PT).getString("rpg_bag"), Category.RPG, false
            ));
            add(new EquipCommand(
                    "requipar", new String[]{"requip"}, getLocale(PT).getString(REQ_ITEM), getLocale(PT).getString("rpg_equip"), Category.RPG, false
            ));
            add(new EquippedCommand(
                    "requipados", new String[]{"requipped"}, getLocale(PT).getString("rpg_gear"), Category.RPG, false
            ));
            add(new GiveCommand(
                    "rdar", new String[]{"rgive"}, getLocale(PT).getString("req_mention-item-gold-xp-qtd"), getLocale(PT).getString("rpg_give"), Category.RPG, false
            ));
            add(new MapCommand(
                    "rmapa", new String[]{"rmap"}, getLocale(PT).getString("rpg_map"), Category.RPG, false
            ));
            add(new MoveCommand(
                    "rmover", new String[]{"rmove"}, getLocale(PT).getString("req_x-y"), getLocale(PT).getString("rpg_move"), Category.RPG, false
            ));
            add(new NewItemCommand(
                    "rnovoitem", new String[]{"rnewitem"}, getLocale(PT).getString("rpg_new-item"), Category.RPG, true
            ));
            add(new NewMapCommand(
                    "rnovomapa", new String[]{"rnewmap"}, getLocale(PT).getString("rpg_new-map"), Category.RPG, true
            ));
            add(new NewMobCommand(
                    "rnovomonstro", new String[]{"rnewmob"}, getLocale(PT).getString("rpg_new-mob"), Category.RPG, true
            ));
            add(new NewPlayerCommand(
                    "rnovo", new String[]{"rnew"}, getLocale(PT).getString("rpg_new-player"), Category.RPG, true
            ));
            add(new RemoveCommand(
                    "rremover", new String[]{"rremove"}, getLocale(PT).getString("req_type-mention-name"), getLocale(PT).getString("rpg_remove-entity"), Category.RPG, false
            ));
            add(new RollCommand(
                    "rrolar", new String[]{"rdado"}, getLocale(PT).getString("req_dice"), getLocale(PT).getString("rpg_dice"), Category.RPG, false
            ));
            add(new StatusCommand(
                    "rperfil", new String[]{"rprofile", "rstatus"}, getLocale(PT).getString("rpg_profile"), Category.RPG, false
            ));
            add(new SwitchMapCommand(
                    "raomapa", new String[]{"rtrocarmapa", "rtomap"}, getLocale(PT).getString("req_index"), getLocale(PT).getString("rpg_change-map"), Category.RPG, false
            ));
            add(new TakeCommand(
                    "rtirar", new String[]{"rpegar"}, getLocale(PT).getString("req_mention-item-gold-qtd"), getLocale(PT).getString("rpg_take"), Category.RPG, false
            ));
            add(new UnequipCommand(
                    "rdesequipar", new String[]{"runequip"}, getLocale(PT).getString(REQ_ITEM), getLocale(PT).getString("rpg_unequip"), Category.RPG, false
            ));
            add(new ViewCommand(
                    "rver", new String[]{"rinfo"}, getLocale(PT).getString(REQ_ITEM), getLocale(PT).getString("rpg_info"), Category.RPG, false
            ));
            add(new WorldListCommand(
                    "rlista", new String[]{"rlist"}, getLocale(PT).getString("rpg_list"), Category.RPG, false
            ));
            add(new ChestCommand(
					"rbau", new String[]{"rchest"}, getLocale(PT).getString("req_mention-chest"), getLocale(PT).getString("rpg_chest"), Category.RPG, false
			));
            add(new NewChestCommand(
                    "rnovobau", new String[]{"rnewchest"}, getLocale(PT).getString("rpg_new-chest"), Category.RPG, true
            ));
            add(new UseCommand(
                    "rusar", new String[]{"ruse"}, getLocale(PT).getString("req_item-gold-qtd"), getLocale(PT).getString("rpg_use"), Category.RPG, false
            ));
            add(new SetMaxPointsCommand(
                    "rmaxpts", getLocale(PT).getString("req_qtd"), getLocale(PT).getString("rpg_max-points"), Category.RPG, false
            ));
        }};
    }

    public List<Command> getCommands() {
        return commands;
    }
}
