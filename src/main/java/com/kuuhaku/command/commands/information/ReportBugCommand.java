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
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.time.format.DateTimeFormatter;

public class ReportBugCommand extends Command {

    public ReportBugCommand() {
        super("bug", new String[]{"sendbug", "feedback"}, "<mensagem>", "Envia um relatório de bug para os devs.", Category.INFO);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {

        if (args.length == 0) { channel.sendMessage(":x: | Você precisa definir uma mensagem.").queue(); return; }

        String mensagem = String.join(" ", args).trim();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy - hh:mm");

        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle("Relatório de bug");
        eb.addField("Enviador por:", author.getAsTag() + " (" + guild.getName() + " | " + channel.getName() + ")", true);
        eb.addField("Enviado em:", df.format(message.getCreationTime()), true);
        eb.addField("Relatório:", "```" + mensagem + "```", false);

        Main.getInfo().getDevelopers().forEach(dev -> Main.getInfo().getAPI().getUserById(dev).openPrivateChannel().queue(m -> m.sendMessage(eb.build()).queue()));
        channel.sendMessage("✅ | Bug reportado com sucesso.").queue();
    }
}
