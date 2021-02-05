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

package com.kuuhaku.command.commands.discord.fun;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.CanvasDAO;
import com.kuuhaku.controller.postgresql.TokenDAO;
import com.kuuhaku.handlers.api.exception.UnauthorizedException;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.PixelCanvas;
import com.kuuhaku.model.persistent.PixelOperation;
import com.kuuhaku.model.persistent.Token;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.text.MessageFormat;

import static com.kuuhaku.utils.Helper.CANVAS_SIZE;

@Command(
		name = "canvas",
		aliases = {"pixel"},
		usage = "req_x-y-color",
		category = Category.FUN
)
@Requires({Permission.MESSAGE_ATTACH_FILES})
public class PixelCanvasCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			Main.getInfo().getCanvas().viewCanvas(message.getTextChannel()).queue();
			return;
		}

		String[] opts = args[0].split(";");

		try {
			if (opts.length <= 1) {
				channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_canvas-invalid-arguments")).queue();
				return;
			} else if (Integer.parseInt(opts[0]) > CANVAS_SIZE / 2 || Integer.parseInt(opts[0]) < -CANVAS_SIZE / 2 || Integer.parseInt(opts[1]) > CANVAS_SIZE / 2 || Integer.parseInt(opts[1]) < -CANVAS_SIZE / 2) {
				channel.sendMessage(MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("err_canvas-coordinates-out-of-bounds"), CANVAS_SIZE / 2, CANVAS_SIZE / 2)).queue();
				return;
			}
		} catch (NumberFormatException e) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_canvas-invalid-coordinates")).queue();
			return;
		}

		try {
			int[] coords = new int[]{Integer.parseInt(opts[0]), Integer.parseInt(opts[1])};

			if (StringUtils.isNumeric(opts[2])) {
				if (Integer.parseInt(opts[2]) <= 0 || Integer.parseInt(opts[2]) > 10) {
					channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_canvas-invalid-zoom")).queue();
					return;
				}
				Main.getInfo().getCanvas().viewChunk(message.getTextChannel(), coords, Integer.parseInt(opts[2]), false).queue();
				return;
			}

			Token t = TokenDAO.getTokenById(author.getId());

			if (t == null) {
				channel.sendMessage("❌ | Você ainda não possui um token de acesso ao ShiroCanvas, por favor faça login em https://shirojbot.site para gerar um automaticamente.").queue();
				return;
			}
			if (t.isDisabled()) {
				channel.sendMessage("❌ | Seu token foi proibido de interagir com o canvas.").queue();
				return;
			}

			try {
				PixelOperation op = new PixelOperation(
						t.getToken(),
						t.getHolder(),
						coords[0],
						coords[1],
						opts[2]
				);

				CanvasDAO.saveOperation(op);
			} catch (NullPointerException e) {
				throw new UnauthorizedException();
			}

			Color color = Color.decode(opts[2]);

			PixelCanvas canvas = Main.getInfo().getCanvas();
			canvas.addPixel(message.getTextChannel(), coords, color).queue();

			CanvasDAO.saveCanvas(canvas);
		} catch (NumberFormatException e) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-color")).queue();
		}
	}
}
