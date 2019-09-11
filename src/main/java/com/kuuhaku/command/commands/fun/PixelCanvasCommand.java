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

		try {
			if (opts.length == 1) {
				channel.sendMessage(":x: | É preciso especificar a coordenada e a cor neste formato: `X;Y;#cor`.\nPara ver um chunk, digite apenas as coordenadas X e Y.").queue();
				return;
			} else if (Integer.parseInt(opts[0]) > 256 || Integer.parseInt(opts[1]) > 256 || Integer.parseInt(opts[0]) < -257 || Integer.parseInt(opts[1]) < -257) {
				channel.sendMessage(":x: | As coordenadas devem estar dentro da grade de 512px X 512px.").queue();
				return;
			}
		} catch (NumberFormatException e) {
			channel.sendMessage(":x: | As coordenadas devem ser numéricas.").queue();
			return;
		}

		try {
			int[] coords = new int[]{Integer.parseInt(opts[0]), Integer.parseInt(opts[1])};

			if (StringUtils.isNumeric(opts[2])) {
				if (coords[0] < 256 && coords[0] > -257 && coords[1] < 256 && coords[1] > -257) {
					Main.getInfo().getCanvas().viewChunk(message.getTextChannel(), coords, Integer.parseInt(opts[2])).queue();
					return;
				} else {
					channel.sendMessage(":x: | A coordenada do chunk deve estar dentro da grade de 512px X 512px e o zoom deve ser maior que 0.").queue();
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
