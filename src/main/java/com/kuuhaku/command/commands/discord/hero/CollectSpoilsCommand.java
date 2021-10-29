/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.commands.discord.hero;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.enums.Reward;
import com.kuuhaku.model.persistent.Expedition;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.awt.*;
import java.util.Map;

@Command(
        name = "espolios",
        aliases = {"spoils", "loot"},
        category = Category.MISC
)
public class CollectSpoilsCommand implements Executable {

    @Override
    public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
        Hero h = KawaiponDAO.getHero(author.getId());

        if (h == null) {
            channel.sendMessage("❌ | Você não possui ou não selecionou um herói.").queue();
            return;
        } else if (h.getExpedition() == null || !h.hasArrived()) {
            channel.sendMessage("❌ | Seu herói não retornou de uma expedição ainda.").queue();
            return;
        }

        Expedition e = h.getExpedition();

        boolean died = false;
        int chance = e.getSuccessChance(h);
        if (Helper.chance(chance)) {
            EmbedBuilder eb = new EmbedBuilder()
                    .setColor(Color.green)
                    .setTitle("Espólios da expedição para " + e);

            for (Map.Entry<String, Object> entry : e.getRewards().entrySet()) {
                Reward rew = Reward.valueOf(entry.getKey());
                int val = (int) (double) entry.getValue();

                eb.addField(rew.toString(),
                        switch (rew) {
                            case XP -> Helper.separate(rew.reward(h, val)) + " XP";
                            case HP -> Helper.separate(rew.reward(h, val)) + " HP";
                            case EP -> Helper.separate(rew.reward(h, val)) + " EP";
                            case CREDIT -> Helper.separate(rew.reward(h, val)) + " CR";
                            case GEM -> Helper.separate(rew.reward(h, val)) + " gemas";
                            case EQUIPMENT -> String.valueOf(rew.reward(h, val));
                        }, true);
            }

            h = KawaiponDAO.getHero(author.getId());
            assert h != null;

            channel.sendMessage("\uD83E\uDDED | Seja bem-vindo(a) de volta " + h.getName() + "!")
                    .setEmbeds(eb.build())
                    .queue();
        } else {
            EmbedBuilder eb = new EmbedBuilder()
                    .setColor(Color.red)
                    .setTitle("A expedição para " + e + " fracassou");

            int expXp = e.getRewards().getInt("XP") / 10;
            if (expXp > 0 && Helper.chance(66)) {
                expXp = Helper.rng(expXp);
                h.setXp(h.getXp() + expXp);
                eb.addField("Bônus de exploração", "+" + expXp + " XP", true);
            }

            if (chance < 15 && Helper.chance(50)) {
                Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());
                kp.getHeroes().remove(h);
                KawaiponDAO.saveKawaipon(kp);
                eb.addField("Morte", "Seu herói morreu durante a expedição", true);
                died = true;
            }
            if (chance < 33 && Helper.chance(50)) {
                int max = h.getXp();
                int penalty = Helper.rng(max / 10, max / 8);
                h.setXp(h.getXp() - penalty);
                eb.addField("Penalidade de XP", "-" + penalty + " XP", true);
            }
            if (chance < 66 && Helper.chance(50)) {
                int max = h.getMaxHp();
                int penalty = Helper.rng(max / 5, max / 3);
                h.setHp(h.getHp() - penalty);
                eb.addField("Penalidade de HP", "-" + penalty + " HP", true);
            }

            channel.sendMessage("\uD83E\uDDED | Seja bem-vindo(a) de volta " + h.getName() + "!")
                    .setEmbeds(eb.build())
                    .queue();
        }

        h.arrive();
        if (!died) KawaiponDAO.saveHero(h);
    }
}
