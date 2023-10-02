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

package com.kuuhaku.command.commands.discord.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.command.Slashed;
import com.kuuhaku.controller.postgresql.RaidDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.annotations.SlashCommand;
import com.kuuhaku.model.annotations.SlashGroup;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.exceptions.ValidationException;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.apache.commons.lang3.StringUtils;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Command(
        name = "usuario",
        aliases = {"user", "us"},
        usage = "req_id-mention-opt",
        category = Category.INFO
)
@Requires({Permission.MESSAGE_EMBED_LINKS})
@SlashGroup("info")
@SlashCommand(name = "usuario", args = {
        "{\"name\": \"usuario\", \"description\": \"Usuário a ser analisado\", \"type\": \"USER\", \"required\": false}"
})
public class UserInfoCommand implements Executable, Slashed {

    @Override
    public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
        Member m;
        if (args.length > 0 && StringUtils.isNumeric(args[0]))
            m = guild.getMemberById(args[0]);
        else if (!message.getMentionedMembers().isEmpty())
            m = message.getMentionedMembers().get(0);
        else
            m = member;

        if (m == null) {
            channel.sendMessage("❌ | Não achei esse usuário aqui, talvez ele ja tenha saído.").queue();
            return;
        }

        String type = "";
        if (m.isOwner())
            type = " <:Owner:852289952632799302>";
        else if (m.getUser().isBot()) {
            if (m.getUser().getFlags().contains(User.UserFlag.VERIFIED_BOT))
                type = " <:VerifiedBot:852289820307095592>";
            else
                type = " <:Bot:852289902573781003>";
        }

        StringBuilder sb = new StringBuilder();
        for (User.UserFlag flag : m.getUser().getFlags()) {
            sb.append(
                    switch (flag) {
                        case STAFF -> "<:DiscordStaff:852288722565201930> ";
                        case PARTNER -> "<:DiscordPartner:852288518314786817> ";
                        case HYPESQUAD -> "<:HypesquadEvents:852288043774771201> ";
                        case HYPESQUAD_BRAVERY -> "<:HypesquadBravery:852288048180101151> ";
                        case HYPESQUAD_BRILLIANCE -> "<:HypesquadBrilliance:852288180384563221> ";
                        case HYPESQUAD_BALANCE -> "<:HypesquadBalance:852288047545319424> ";
                        case EARLY_SUPPORTER -> "<:EarlySupporter:852288574790565948> ";
                        case BUG_HUNTER_LEVEL_1 -> "<:BugHunter1:852288420545429525> ";
                        case BUG_HUNTER_LEVEL_2 -> "<:BugHunter2:852288421157797908> ";
                        case VERIFIED_DEVELOPER -> "<:EarlyBotDeveloper:852288651706892298> ";
                        case CERTIFIED_MODERATOR -> "<:CertifiedModerator:852538983333363732> ";
                        default -> "";
                    }
            );
        }

        boolean booster = m.getTimeBoosted() != null;
        if (booster) {
            int time = (int) m.getTimeBoosted().until(OffsetDateTime.now(), ChronoUnit.MONTHS);
            if (time >= 18)
                sb.append("<:Booster9:852288241104322651> ");
            else if (time >= 15)
                sb.append("<:Booster8:852288241461624852> ");
            else if (time >= 12)
                sb.append("<:Booster7:852288241499635712> ");
            else if (time >= 9)
                sb.append("<:Booster6:852288241469096016> ");
            else if (time >= 6)
                sb.append("<:Booster5:852288241364238366> ");
            else if (time >= 3)
                sb.append("<:Booster4:852288241276157973> ");
            else if (time >= 2)
                sb.append("<:Booster3:852288241159634945> ");
            else if (time >= 1)
                sb.append("<:Booster2:852288241386127371> ");
            else
                sb.append("<:Booster1:852288241193189407> ");
        }

        EmbedBuilder eb = new ColorlessEmbedBuilder();
        eb.setTitle(":dividers: | Dados sobre " + m.getUser().getName() + type)
                .setThumbnail(m.getUser().getEffectiveAvatarUrl())
                .addField(":man_detective: | Nome real", "`" + m.getUser().getAsTag() + "`", true)
                .addField(":medal: | Emblemas", sb.toString(), true)
                .addField(":1234: | ID", "`" + m.getId() + "`", true)
                .addField(":calendar: | Conta criada em", m.getTimeCreated().format(Helper.DATE_FORMAT) + "\n(" + Helper.TIMESTAMP.formatted(m.getTimeCreated().toEpochSecond()) + ")", true)
                .addField(":calendar: | Membro desde", m.hasTimeJoined() ? m.getTimeJoined().format(Helper.DATE_FORMAT) + "\n(" + Helper.TIMESTAMP.formatted(m.getTimeJoined().toEpochSecond()) + ")" : "Não sei", true);
        if (booster)
            eb.addField(":calendar: | Booster desde", m.getTimeBoosted().format(Helper.DATE_FORMAT) + "\n(" + Helper.TIMESTAMP.formatted(m.getTimeBoosted().toEpochSecond()) + ")", true);
        else
            eb.addBlankField(true);

        List<String> roles = m.getRoles().stream().map(Role::getAsMention).collect(Collectors.toList());
        int x = 0;
        while (String.join(" ").length() > 1000) {
            roles.remove(roles.size() - 1);
            x++;
        }

        if (!m.getRoles().isEmpty())
            eb.addField(":beginner: | Cargos", x > 0 ? String.join(" ") + "... e mais xx cargos" : String.join(" "), false);

        int raids = RaidDAO.getUserRaids(m.getId());
        if (raids > 0) {
            eb.addField(":warning: | ALERTA | :warning:", "Este usuário foi banido " + raids + (raids == 1 ? " vez " : " vezes ") + "pelo sistema R.A.ID.", false);
        }

        channel.sendMessageEmbeds(eb.build()).queue();
    }

    @Override
    public String toCommand(SlashCommandEvent evt) throws ValidationException {
        OptionMapping user = evt.getOption("usuario");

        return user == null ? "" : user.getAsUser().getId();
    }
}
