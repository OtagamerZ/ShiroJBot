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

package com.kuuhaku.command.commands.information;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.utils.ExceedEnums;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.TagIcons;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import javax.persistence.NoResultException;
import java.awt.*;
import java.io.IOException;

public class MyTagsCommand extends Command {

    public MyTagsCommand() {
        super("eu", new String[]{"meusemblemas", "mytags"}, "Mostra suas tags.", Category.PARTNER);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        EmbedBuilder eb = new EmbedBuilder();
        String exceed = SQLite.getMemberByMid(author.getId()).getExceed();

        eb.setTitle(":label: Emblemas de " + author.getName());
        try {
            eb.setColor(Helper.colorThief(author.getAvatarUrl()));
        } catch (IOException e) {
            eb.setColor(Helper.getRandomColor());
        }

        StringBuilder badges = new StringBuilder();

        if (!exceed.isEmpty()) {
            badges.append(TagIcons.getExceed(ExceedEnums.getByName(exceed)));
        }

        if (author.getId().equals(Main.getInfo().getNiiChan()) || Main.getInfo().getDevelopers().contains(author.getId()))
            badges.append(TagIcons.getTag(TagIcons.DEV));

        if (Main.getInfo().getSheriffs().contains(author.getId())) {
            badges.append(TagIcons.getTag(TagIcons.SHERIFF));
        }

        if (Main.getInfo().getEditors().contains(author.getId()))
            badges.append(TagIcons.getTag(TagIcons.EDITOR));

        try {
            if (MySQL.getTagById(author.getId()).isReader())
                badges.append(TagIcons.getTag(TagIcons.READER));
        } catch (NoResultException ignore) {
        }

        if (member.hasPermission(Permission.MANAGE_CHANNEL))
            badges.append(TagIcons.getTag(TagIcons.MODERATOR));

        try {
            if (MySQL.getChampionBeyblade().getId().equals(author.getId()))
                badges.append(TagIcons.getTag(TagIcons.CHAMPION));
        } catch (NoResultException ignore) {
        }

        try {
            if (SQLite.getMemberById(author.getId() + guild.getId()).getLevel() >= 70)
                badges.append(TagIcons.getTag(TagIcons.LVL70));
            else if (SQLite.getMemberById(author.getId() + guild.getId()).getLevel() >= 60)
                badges.append(TagIcons.getTag(TagIcons.LVL60));
            else if (SQLite.getMemberById(author.getId() + guild.getId()).getLevel() >= 50)
                badges.append(TagIcons.getTag(TagIcons.LVL50));
            else if (SQLite.getMemberById(author.getId() + guild.getId()).getLevel() >= 40)
                badges.append(TagIcons.getTag(TagIcons.LVL40));
            else if (SQLite.getMemberById(author.getId() + guild.getId()).getLevel() >= 30)
                badges.append(TagIcons.getTag(TagIcons.LVL30));
            else if (SQLite.getMemberById(author.getId() + guild.getId()).getLevel() >= 20)
                badges.append(TagIcons.getTag(TagIcons.LVL20));
        } catch (NoResultException ignore) {
        }

        try {
            if (MySQL.getTagById(author.getId()).isVerified())
                badges.append(TagIcons.getTag(TagIcons.VERIFIED));
        } catch (NoResultException ignore) {
        }

        try {
            if (MySQL.getTagById(author.getId()).isToxic())
                badges.append(TagIcons.getTag(TagIcons.TOXIC));
        } catch (NoResultException ignore) {
        }

        try {
            if (!SQLite.getMemberById(author.getId() + guild.getId()).getWaifu().isEmpty())
                badges.append(TagIcons.getTag(TagIcons.MARRIED));
        } catch (NoResultException ignore) {
        }

        eb.addField("Emblemas:", badges.toString(), false);

        channel.sendMessage(eb.build()).queue();
    }
}
