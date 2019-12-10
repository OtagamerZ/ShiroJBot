package com.kuuhaku.command.commands.fun;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL.CanvasDAO;
import com.kuuhaku.model.PixelCanvas;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;

import static com.kuuhaku.utils.Helper.CANVAS_SIZE;

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
