package com.kuuhaku.command.commands.misc;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class PixelzoneCommand extends Command {

	public PixelzoneCommand() {
		super("pixelzone", new String[]{"pz"}, "Pega uma print das coordenadas informadas.", Category.MISC);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		if (args.length < 1 || !args[0].contains(",")) {
			channel.sendMessage(":x: | Você necessita de fornecer uma coordenada válida (separada por vírgula)").queue();
			return;
		} else if (!StringUtils.isNumeric(args[0].split(",")[0]) || !StringUtils.isNumeric(args[0].split(",")[1])) {
			channel.sendMessage(":x: | Você necessita de fornecer uma coordenada numérica").queue();
			return;
		}

		try {
			HttpURLConnection con = (HttpURLConnection) new URL("https://pixelzone.io/?p=" + args[0].split(",")[0] + "," + args[0].split(",")[1]).openConnection();
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			BufferedImage canvas = ImageIO.read(con.getInputStream());

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(canvas, "png", baos);

			channel.sendFile(baos.toByteArray(), "pixelzone.png").queue();
		} catch (IOException e) {
			Helper.log(this.getClass(), LogLevel.ERROR, e + " | " + e.getStackTrace()[0]);
		}
	}
}
