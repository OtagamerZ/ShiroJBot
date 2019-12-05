package com.kuuhaku.command.commands.fun;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.model.PixelCanvas;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;

import static com.kuuhaku.utils.Helper.CANVAS_SIZE;

public class PixelChunkCommand extends Command {

	public PixelChunkCommand() {
		super("chunk", new String[]{"zone", "pixelchunk"}, "<zona> [<X>;<Y>;<#cor>]", "Adiciona um pixel da cor selecionada no chunk do canvas.", Category.FUN);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(":x: | É necessário ao menos especificar o número do chunk").queue();
			return;
		}

		String[] opts = args.length < 2 ? new String[]{} : args[1].split(";");
		int[] offset;

		assert StringUtils.isNumeric(args[0]);
		if (Integer.parseInt(args[0]) < 1 || Integer.parseInt(args[0]) > 4) {
			channel.sendMessage(":x: | O número do chunk deve ser de 1 à 4").queue();
			return;
		}

		switch (Integer.parseInt(args[0])) {
			case 1:
				offset = new int[]{0, 0};
				break;
			case 2:
				offset = new int[]{512, 0};
				break;
			case 3:
				offset = new int[]{0, 512};
				break;
			case 4:
				offset = new int[]{512, 512};
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + opts[0]);
		}

		if (args.length < 2) {
			Main.getInfo().getCanvas().viewSection(message.getTextChannel(), Integer.parseInt(args[0])).queue();
			return;
		}

		try {
			if (opts.length == 2) {
				channel.sendMessage(":x: | É preciso especificar a coordenada e a cor neste formato: `zona X;Y;#cor`.\nPara dar zoom, digite apenas o número do chunk, as coordenadas X e Y e o nível do zoom.").queue();
				return;
			} else if (Integer.parseInt(opts[0]) > CANVAS_SIZE / 4 || Integer.parseInt(opts[0]) < -CANVAS_SIZE / 4 || Integer.parseInt(opts[1]) > CANVAS_SIZE / 4 || Integer.parseInt(opts[1]) < -CANVAS_SIZE / 4) {
				channel.sendMessage(":x: | As coordenadas não podem ser menores que -" + (CANVAS_SIZE / 4) + "px ou maiores que " + (CANVAS_SIZE / 4) + "px.").queue();
				return;
			}
		} catch (NumberFormatException e) {
			channel.sendMessage(":x: | As coordenadas devem ser numéricas.").queue();
			return;
		}

		try {
			int[] coords = new int[]{Integer.parseInt(opts[0]) + offset[0], Integer.parseInt(opts[1]) + offset[1]};

			if (StringUtils.isNumeric(opts[2])) {
				if (Integer.parseInt(opts[2]) <= 0 || Integer.parseInt(opts[2]) > 10) {
					channel.sendMessage(":x: | O zoom não pode ser menor ou igual à 0, nem maior que 10").queue();
					return;
				}
				Main.getInfo().getCanvas().viewChunk(message.getTextChannel(), coords, Integer.parseInt(opts[2]), true).queue();
				return;
			}

			Color color = Color.decode(opts[2]);

			PixelCanvas canvas = Main.getInfo().getCanvas();
			canvas.addPixel(message.getTextChannel(), coords, color).queue();

			com.kuuhaku.controller.MySQL.Canvas.saveCanvas(canvas);
		} catch (NumberFormatException e) {
			channel.sendMessage(":x: | Cor no formato incorreto, ela deve seguir o padrão hexadecimal (#RRGGBB).").queue();
		}
	}
}
