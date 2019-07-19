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
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.time.format.DateTimeFormatter;

public class BotInfoCommand extends Command {

    public BotInfoCommand() {
        super("info", new String[]{"botinfo", "bot"}, "Mostra dados sobre a Shiro.", Category.INFO);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle(":dividers: Dados sobre a Shiro J. Bot");
        eb.setThumbnail(Main.getInfo().getAPI().getSelfUser().getAvatarUrl());
        eb.addField(":triangular_flag_on_post: Projeto inicial por:", Main.getInfo().getUserByID(Main.getInfo().getNiiChan()).getAsTag(), true);
        StringBuilder sb = new StringBuilder();
        Main.getInfo().getDevelopers().forEach(d -> sb.append(Main.getInfo().getUserByID(d).getAsTag()).append(", "));
        eb.addField(":tools: Desenvolvida por:", sb.toString(), true);
        eb.addField(":calendar_spiral: Criada em:", Main.getInfo().getSelfUser().getCreationTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy - hh:mm")), true);
        eb.addField(":handshake: Apoiada por:", MySQL.getPartnerAmount() + " servidores", true);
        eb.addField(":earth_americas: Estou em:", Main.getInfo().getAPI().getGuilds().size() + " servidores", true);
        eb.addField(":speech_balloon: Conheço:", Main.getInfo().getAPI().getUsers().size() + " usuários", true);
        eb.addField(":envelope: Link de convite:", "https://discordbots.org/bot/572413282653306901", true);

        channel.sendMessage(eb.build()).queue();
    }
}
