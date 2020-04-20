/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.commands.fun;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.CanvasDAO;
import com.kuuhaku.model.persistent.PixelCanvas;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.awt.*;

import static com.kuuhaku.utils.Helper.CANVAS_SIZE;

public class PixelCanvasCommand extends Command {

	public PixelCanvasCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public PixelCanvasCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public PixelCanvasCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public PixelCanvasCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			Main.getInfo().getCanvas().viewCanvas(message.getTextChannel()).queue();
			return;
		}

		String[] opts = args[0].split(";");

		try {
			if (opts.length == 1) {
				channel.sendMessage(":x: | É preciso especificar a coordenada e a cor neste formato: `X;Y;#cor`.\nPara dar zoom, digite apenas as coordenadas X e Y e o nível do zoom.").queue();
				return;
			} else if (Integer.parseInt(opts[0]) > CANVAS_SIZE / 2 || Integer.parseInt(opts[0]) < -CANVAS_SIZE / 2 || Integer.parseInt(opts[1]) > CANVAS_SIZE / 2 || Integer.parseInt(opts[1]) < -CANVAS_SIZE / 2) {
				channel.sendMessage(":x: | As coordenadas não podem ser menores que -" + (CANVAS_SIZE / 2) + "px ou maiores que " + (CANVAS_SIZE / 2) + "px.").queue();
				return;
			}
		} catch (NumberFormatException e) {
			channel.sendMessage(":x: | As coordenadas devem ser numéricas.").queue();
			return;
		}

		try {
			int[] coords = new int[]{Integer.parseInt(opts[0]), Integer.parseInt(opts[1])};

			if (StringUtils.isNumeric(opts[2])) {
				if (Integer.parseInt(opts[2]) <= 0 || Integer.parseInt(opts[2]) > 10) {
					channel.sendMessage(":x: | O zoom não pode ser menor ou igual à 0, nem maior que 10").queue();
					return;
				}
				Main.getInfo().getCanvas().viewChunk(message.getTextChannel(), coords, Integer.parseInt(opts[2]), false).queue();
				return;
			}

			Color color = Color.decode(opts[2]);

			PixelCanvas canvas = Main.getInfo().getCanvas();
			canvas.addPixel(message.getTextChannel(), coords, color).queue();

			CanvasDAO.saveCanvas(canvas);
		} catch (NumberFormatException e) {
			channel.sendMessage(":x: | Cor no formato incorreto, ela deve seguir o padrão hexadecimal (#RRGGBB).").queue();
		}
	}
}
