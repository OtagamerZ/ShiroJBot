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

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang.WordUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
        name = "invocarheroi",
        aliases = {"summonhero", "isekai"},
        usage = "req_race-name",
        category = Category.SUPPORT
)
public class SummonHeroCommand implements Executable {

    @Override
    public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
        Hero h = CardDAO.getHero(author.getId());

        if (h == null) {
            channel.sendMessage("❌ | Você já possui um herói.").queue();
            return;
        } else if (args.length < 2) {
            channel.sendMessage("❌ | Você precisa informar uma raça e um nome para seu novo herói.").queue();
            return;
        }

        Race r = Race.getByName(args[0]);
        if (r == null) {
            channel.sendMessage("❌ | Raça inválida.").queue();
            return;
        }

        String name = WordUtils.capitalizeFully(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        if (name.isBlank()) {
            channel.sendMessage("❌ | Você precisa digitar um nome.").queue();
            return;
        }

        channel.sendMessage("Você está prestes a invocar " + name + ", campeão da raça " + r.toString().toLowerCase(Locale.ROOT) + ", deseja confirmar?.")
                .queue(s -> Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
                            CardDAO.saveHero(new Hero(author, name, r));

                            s.delete().flatMap(d -> channel.sendMessage("✅ | Herói invocado com sucesso!")).queue();
                        }), true, 1, TimeUnit.MINUTES,
                        u -> u.getId().equals(author.getId()),
                        ms -> Main.getInfo().getConfirmationPending().remove(author.getId())
                ));
    }
}
