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
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CanvasDAO;
import com.kuuhaku.controller.postgresql.TokenDAO;
import com.kuuhaku.handlers.api.exception.UnauthorizedException;
import com.kuuhaku.model.enums.DailyTask;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.PixelCanvas;
import com.kuuhaku.model.persistent.PixelOperation;
import com.kuuhaku.model.persistent.Token;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.awt.*;
import java.util.Map;

import static com.kuuhaku.utils.Helper.CANVAS_SIZE;

public class PixelChunkCommand extends Command {

	public PixelChunkCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public PixelChunkCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public PixelChunkCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public PixelChunkCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_canvas-no-chunk")).queue();
			return;
		}

		String[] opts = args.length < 2 ? new String[]{} : args[1].split(";");
		int[] offset;

		assert StringUtils.isNumeric(args[0]);
		if (!StringUtils.isNumeric(args[0]) || Integer.parseInt(args[0]) < 1 || Integer.parseInt(args[0]) > 4) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_canvas-invalid-chunk")).queue();
			return;
		}

		offset = switch (Integer.parseInt(args[0])) {
			case 1 -> new int[]{0, 0};
			case 2 -> new int[]{512, 0};
			case 3 -> new int[]{0, 512};
			case 4 -> new int[]{512, 512};
			default -> throw new IllegalStateException("Unexpected value: " + opts[0]);
		};

		if (args.length < 2) {
			Main.getInfo().getCanvas().viewSection(message.getTextChannel(), Integer.parseInt(args[0])).queue();
			return;
		}

		try {
			if (opts.length == 2) {
				channel.sendMessage("❌ | É preciso especificar a coordenada e a cor neste formato: `zona X;Y;#cor`.\nPara dar zoom, digite apenas o número do chunk, as coordenadas X e Y e o nível do zoom.").queue();
				return;
			} else if (Integer.parseInt(opts[0]) > CANVAS_SIZE / 4 || Integer.parseInt(opts[0]) < -CANVAS_SIZE / 4 || Integer.parseInt(opts[1]) > CANVAS_SIZE / 4 || Integer.parseInt(opts[1]) < -CANVAS_SIZE / 4) {
				channel.sendMessage("❌ | As coordenadas não podem ser menores que -" + (CANVAS_SIZE / 4) + "px ou maiores que " + (CANVAS_SIZE / 4) + "px.").queue();
				return;
			}
		} catch (NumberFormatException e) {
			channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_canvas-invalid-coordinates")).queue();
			return;
		}

		try {
			int[] coords = new int[]{Integer.parseInt(opts[0]) + offset[0], Integer.parseInt(opts[1]) + offset[1]};

			if (StringUtils.isNumeric(opts[2])) {
				if (Integer.parseInt(opts[2]) <= 0 || Integer.parseInt(opts[2]) > 10) {
					channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_canvas-invalid-zoom")).queue();
					return;
				}
				Main.getInfo().getCanvas().viewChunk(message.getTextChannel(), coords, Integer.parseInt(opts[2]), true).queue();
				return;
			}

			Token t = TokenDAO.getTokenById(author.getId());

			if (t == null) {
				channel.sendMessage("❌ | Você ainda não possui um token de acesso ao ShiroCanvas, por favor faça login em https://shirojbot.site para gerar um automaticamente.").queue();
				return;
			} else if (t.isDisabled()) {
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

				Account acc = AccountDAO.getAccount(t.getUid());
				if (!acc.hasCompletedQuests()) {
					Map<DailyTask, Integer> pg = acc.getDailyProgress();
					pg.compute(DailyTask.CANVAS_TASK, (k, v) -> Helper.getOr(v, 0) + 1);
					acc.setDailyProgress(pg);
					AccountDAO.saveAccount(acc);
				}

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
