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
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class ValidateGIFCommand extends Command {

	public ValidateGIFCommand() {
		super("validate", new String[]{"testgif", "tgif"}, "<link>", "Testa se as dimensões da GIF são recomendadas para o uso em reações.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (args.length == 0) {
			channel.sendMessage(":x: | Você precisa definir uma imagem.").queue();
			return;
		}

		try {
			BufferedImage bi = ImageIO.read(Helper.getImage(args[0]));

			String w;
			if (bi.getWidth() >= 500) w = "EXCELENTE";
			else if (bi.getWidth() >= 400) w = "BOA";
			else if (bi.getWidth() >= 300) w = "RUIM";
			else w = "HORRÍVEL";

			String h;
			if (bi.getHeight() >= 220) h = "EXCELENTE";
			else if (bi.getHeight() >= 200) h = "BOA";
			else if (bi.getHeight() >= 180) h = "RUIM";
			else h = "HORRÍVEL";
			String s = "Propoções: " + bi.getWidth() + "x" + bi.getHeight() + "\nEssa GIF possui uma qualidade `" + w + "`x`" + h + "`!";

			channel.sendMessage(s).queue();
		} catch (IOException e) {
			channel.sendMessage(":x: | O link da imagem não me parece correto.").queue();
		} catch (NullPointerException npe) {
			channel.sendMessage(":x: | Houve um erro ao recuperar dados da imagem (O site Tenor costuma retornar este erro).").queue();
		}
	}
}
