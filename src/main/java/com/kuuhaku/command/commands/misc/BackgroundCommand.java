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

package com.kuuhaku.command.commands.misc;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.sqlite.MemberDAO;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class BackgroundCommand extends Command {

	public BackgroundCommand(String name, String description, Category category) {
		super(name, description, category);
	}

	public BackgroundCommand(String name, String[] aliases, String description, Category category) {
		super(name, aliases, description, category);
	}

	public BackgroundCommand(String name, String usage, String description, Category category) {
		super(name, usage, description, category);
	}

	public BackgroundCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category) {
		super(name, aliases, usage, description, category);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
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

            com.kuuhaku.model.persistent.Member m = MemberDAO.getMemberById(author.getId() + guild.getId());
            m.setBg(args[0]);
            MemberDAO.updateMemberConfigs(m);
            channel.sendMessage("Imagem de fundo trocada com sucesso!").queue();
        } catch (IOException e) {
            channel.sendMessage(":x: | O link da imagem não me parece correto.").queue();
        }
    }
}
