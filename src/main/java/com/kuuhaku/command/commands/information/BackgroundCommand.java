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

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.SQLite;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class BackgroundCommand extends Command {

    public BackgroundCommand() {
        super("background", new String[]{"fundo", "bg"}, "<link>", "Muda o fundo do seu perfil (a imagem especificada será redimensionada, então utilize imagens de resolução 1080p ou similar).", Category.MISC);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        if (args.length == 0) {
            channel.sendMessage(":x: | Você precisa definir uma imagem.").queue();
            return;
        }

        try {
            HttpURLConnection con = (HttpURLConnection) new URL(args[0]).openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            BufferedImage bi = ImageIO.read(con.getInputStream());
            con.disconnect();
            bi.flush();

            com.kuuhaku.model.Member m = SQLite.getMemberById(author.getId() + guild.getId());
            m.setBg(args[0]);
            SQLite.saveMemberToDB(m);
            channel.sendMessage("Imagem de fundo trocada com sucesso!").queue();
        } catch (IOException e) {
            channel.sendMessage(":x: | O link da imagem não me parece correto.").queue();
        }
    }
}
