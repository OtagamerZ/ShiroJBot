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

package com.kuuhaku.command.commands.discord.moderation;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.GuildDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.BuffType;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.guild.Buff;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.utils.XStringBuilder;
import com.kuuhaku.utils.helpers.MathHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Command(
        name = "melhorar",
        aliases = {"upgrade", "up"},
        usage = "req_type-tier",
        category = Category.MODERATION
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
public class PurchaseBuffCommand implements Executable {

    @Override
    public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
        Account acc = Account.find(Account.class, author.getId());

        if (args.length < 2) {
            EmbedBuilder eb = new ColorlessEmbedBuilder()
                    .setTitle(":level_slider: | Melhorias de servidor")
                    .setDescription("Melhorias são aplicadas a todos os membros do servidor por um certo período, use-as para oferecer vantagens aos seus membros.")
                    .setFooter("Seus CR: " + StringHelper.separate(acc.getBalance()), "https://i.imgur.com/U0nPjLx.gif");

            for (BuffType type : BuffType.values()) {
                XStringBuilder sb = new XStringBuilder();
                for (int i = 1; i < 4; i++) {
                    sb.appendNewLine("**Tier %s** (%s CR): `+%s%% %s` (%s dias)".formatted(
                            i,
                            type.getPrice(i),
                            MathHelper.roundToString(type.getPowerMult() * i * 100, 2),
                            switch (type) {
                                case XP -> "ganho de XP";
                                case CARD -> "chance de aparecer cartas";
                                case DROP -> "chance de aparecer drops";
                                case FOIL -> "chance de cartas serem cromadas";
                            },
                            switch (i) {
                                case 1 -> 15;
                                case 2 -> 11;
                                case 3 -> 7;
                                default -> throw new IllegalStateException("Unexpected value: " + i);
                            }
                    ));
                }

                eb.addField(type + " (`" + prefix + "up " + type.name() + " 1/2/3`)", sb.toString(), false);
            }

            channel.sendMessageEmbeds(eb.build()).queue();
            return;
        } else if (!StringUtils.isNumeric(args[1])) {
            channel.sendMessage("❌ | O tier da melhoria deve ser um valor entre 1 e 3.").queue();
            return;
        }

        try {
            int tier = Integer.parseInt(args[1]);
            if (!MathHelper.between(tier, 1, 4)) {
                channel.sendMessage("❌ | O tier da melhoria deve ser um valor entre 1 e 3.").queue();
                return;
            }

            Buff sb = new Buff(BuffType.valueOf(args[0].toUpperCase(Locale.ROOT)), tier);
            if (acc.getTotalBalance() < sb.getPrice()) {
                channel.sendMessage(I18n.getString("err_insufficient-credits-user")).queue();
                return;
            }

            acc.consumeCredit(sb.getPrice(), this.getClass());

            GuildConfig gc = GuildDAO.getGuildById(guild.getId());
            if (!gc.addBuff(sb.getType(), sb.getTier())) {
                channel.sendMessage("❌ | Este servidor já possui uma melhoria de tier superior nessa categoria.").queue();
                return;
            }

            GuildDAO.updateGuildSettings(gc);
            acc.save();
            channel.sendMessage("✅ | Melhoria aplicada com sucesso! (" + TimeUnit.DAYS.convert(sb.getTime(), TimeUnit.MILLISECONDS) + " dias).").queue();
        } catch (IllegalArgumentException e) {
            channel.sendMessage("❌ | O tipo da melhoria deve ser `xp`, `card`, `drop` ou `foil`.").queue();
        }
    }
}
