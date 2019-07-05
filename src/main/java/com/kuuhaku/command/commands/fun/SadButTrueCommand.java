package com.kuuhaku.command.commands.fun;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.model.Profile;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

public class SadButTrueCommand extends Command {

	public SadButTrueCommand() {
		super("tmv", new String[]{"tristemasverdade", "sadbuttrue", "sbt"}, "<verdade>", "Gera um meme no formato \"Triste mas verdade\"", Category.FUN);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {

		if (args.length < 1) {
			channel.sendMessage(":x: | Você tem que escrever a mensagem que deseja que apareca no meme.").queue();
			return;
		}

		try {
			BufferedImage bi = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("sadbuttrue.png")));
			Graphics2D g2d = bi.createGraphics();

			g2d.setColor(Color.BLACK);
			g2d.setFont(new Font("Impact", Font.BOLD, 20));
			Profile.printCenteredString(String.join(" ", args), 267, 66, 554, g2d);
			g2d.dispose();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(bi, "png", baos);

			channel.sendMessage("Aqui está seu meme " + author.getAsMention() + "!").addFile(baos.toByteArray(), "tmv.jpg").queue();
		} catch (IOException e) {
			Helper.log(this.getClass(), LogLevel.ERROR, e.toString());
		}
	}

}
