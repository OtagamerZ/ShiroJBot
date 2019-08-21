package com.kuuhaku.command.commands.exceed;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.model.Exceed;
import com.kuuhaku.model.Profile;
import com.kuuhaku.utils.ExceedEnums;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static com.kuuhaku.model.Profile.HEIGTH;
import static com.kuuhaku.model.Profile.WIDTH;
import static com.kuuhaku.model.Profile.FONT;

public class ExceedRankCommand extends Command {
	public ExceedRankCommand() {
		super("exceedrank", new String[]{"exrank", "topexceed", "topex"}, "Mostra o ranking dos exceeds.", Category.EXCEED);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		channel.sendMessage("<a:Loading:598500653215645697> Gerando placares...").queue(m -> {
			try {
				BufferedImage bi = new BufferedImage(WIDTH, HEIGTH, BufferedImage.TYPE_INT_ARGB);
				List<Exceed> exceeds = new ArrayList<>();
				for (ExceedEnums ex : ExceedEnums.values()) {
					exceeds.add(MySQL.getExceed(ex));
				}

				BufferedImage bg = Profile.scaleImage(ImageIO.read(Helper.getImage("http://snagfilms-a.akamaihd.net/08/bd/a9131d1c48089e81990bdeafc0c4/1426-lec3-1536x865.jpg")), WIDTH, HEIGTH);
				BufferedImage fg = ImageIO.read(Helper.getImage("https://i.imgur.com/eGLuRMb.png"));

				List<BufferedImage> bars = new ArrayList<BufferedImage>() {{
					add(Profile.scaleImage(ImageIO.read(Helper.getImage("https://i.imgur.com/1USPoLD.jpg")), 68, 350));
					add(Profile.scaleImage(ImageIO.read(Helper.getImage("https://i.imgur.com/HNX66NB.jpg")), 68, 350));
					add(Profile.scaleImage(ImageIO.read(Helper.getImage("https://i.imgur.com/uCtc2Jr.jpg")), 68, 350));
					add(Profile.scaleImage(ImageIO.read(Helper.getImage("https://i.imgur.com/otuZup3.jpg")), 68, 350));
					add(Profile.scaleImage(ImageIO.read(Helper.getImage("https://i.imgur.com/nDIQ4ln.jpg")), 68, 350));
					add(Profile.scaleImage(ImageIO.read(Helper.getImage("https://i.imgur.com/H725kN3.jpg")), 68, 350));
				}};

				List<String> names = new ArrayList<String>() {{
					add(ExceedEnums.IMANITY.getName());
					add(ExceedEnums.SEIREN.getName());
					add(ExceedEnums.WEREBEAST.getName());
					add(ExceedEnums.LUMAMANA.getName());
					add(ExceedEnums.EXMACHINA.getName());
					add(ExceedEnums.FLUGEL.getName());
				}};

				Graphics2D g2d = (Graphics2D) bi.getGraphics();
				g2d.setFont(new Font(FONT.getName(), Font.PLAIN, 30));
				g2d.drawImage(bg, null, 0, 0);

				for (int i = 0; i < 6; i++) {
					int h = (int) (10 + (10 * 100 / 90)) * 350 / 100;
					Profile.printCenteredString(names.get(i), 68, 152 + (113 * i), h - 10, g2d);
					g2d.setClip(new Rectangle2D.Float(152 + (113 * i), 580 - h, 68, h));
					g2d.drawImage(bars.get(i), null, 152 + (113 * i), 230);
					g2d.setClip(null);
				}

				g2d.drawImage(fg, null, 0, 0);
				g2d.dispose();

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(Profile.clipRoundEdges(bi), "png", baos);
				channel.sendFile(baos.toByteArray(), "ranking.png").queue();
				m.delete().queue();
			} catch (Exception e) {
				m.editMessage(":x: | Epa, teve um erro ao gerar o placar, meus criadores já foram notificados!").queue();
				Helper.log(this.getClass(), LogLevel.ERROR, e + " | " + e.getStackTrace()[0]);
			}
		});
	}
}
