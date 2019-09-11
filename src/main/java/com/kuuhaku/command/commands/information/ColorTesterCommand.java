package com.kuuhaku.command.commands.information;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ColorTesterCommand extends Command {

	public ColorTesterCommand() {
		super("cor", new String[]{"color"}, "<#cor>", "Vê o tom da cor informada.", Category.INFO);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (args.length < 1) {
			channel.sendMessage(":x: | Você tem que especificar uma cor no seguinte formato: `#RRGGBB`").queue();
			return;
		}

		try {
			BufferedImage bi = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = bi.createGraphics();
			g2d.setColor(Color.decode(args[0]));

			g2d.fillRect(0, 0, 128, 128);
			g2d.dispose();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(bi, "png", baos);

			EmbedBuilder eb = new EmbedBuilder();
			eb.setColor(Color.decode(args[0]));
			eb.setTitle("Cor " + args[0]);
			eb.setThumbnail("attachment://color.png");

			channel.sendMessage(eb.build()).addFile(baos.toByteArray(), "color.png").queue();

		} catch (NumberFormatException e) {
			channel.sendMessage(":x: | Cor no formato incorreto, ela deve seguir o padrão hexadecimal (#RRGGBB).").queue();
		} catch (IOException e) {
			Helper.log(this.getClass(), LogLevel.ERROR, e + " | " + e.getStackTrace()[0]);
		}
	}
}
