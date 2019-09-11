package com.kuuhaku.command.commands.fun;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.model.PixelCanvas;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;

public class PixelCanvasCommand extends Command {

	public PixelCanvasCommand() {
		super("canvas", new String[]{"pixel", "pixelcanvas"}, "[<X>;<Y>;<#cor>]", "Adiciona um pixel da cor selecionada no canvas.", Category.FUN);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (args.length < 1) {
			Main.getInfo().getCanvas().viewCanvas(message.getTextChannel()).queue();
			return;
		}

		String[] opts = args[0].split(";");

		if (opts.length == 1) {
			channel.sendMessage(":x: | É preciso especificar a coordenada e a cor neste formato: `X;Y;#cor`.\nPara ver um chunk, digite apenas as coordenadas X e Y.").queue();
			return;
		} else if (!StringUtils.isNumeric(opts[0]) || !StringUtils.isNumeric(opts[1]) || Integer.parseInt(opts[0]) > 1024 || Integer.parseInt(opts[1]) > 1024 || Integer.parseInt(opts[0]) < -1024 || Integer.parseInt(opts[1]) < -1024) {
			channel.sendMessage(":x: | As coordenadas devem ser numéricas, e estar dentro da grade de 2048px x 2048px.").queue();
			return;
		}

		try {
			int[] coords = new int[]{Integer.parseInt(opts[0]), Integer.parseInt(opts[1])};

			if (opts.length == 2) {
				if (coords[0] < 8 && coords[0] >= 0 && coords[1] < 8 && coords[1] >= 0) {
					Main.getInfo().getCanvas().viewChunk(message.getTextChannel(), coords).queue();
					return;
				} else {
					channel.sendMessage(":x: | A coordenada do chunk deve estar dentro da grade de 7 x 7.").queue();
					return;
				}
			}

			Color color = Color.decode(opts[2]);

			PixelCanvas canvas = Main.getInfo().getCanvas();
			canvas.addPixel(message.getTextChannel(), coords, color).queue();

			MySQL.saveCanvas(canvas);
			Main.getInfo().reloadCanvas();
		} catch (NumberFormatException e) {
			channel.sendMessage(":x: | Cor no formato incorreto, ela deve seguir o padrão hexadecimal (#RRGGBB).").queue();
		}
	}
}
