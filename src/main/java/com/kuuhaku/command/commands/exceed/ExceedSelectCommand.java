package com.kuuhaku.command.commands.exceed;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.Exceed;
import com.kuuhaku.model.Profile;
import com.kuuhaku.utils.ExceedEnums;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static com.kuuhaku.model.Profile.*;

public class ExceedSelectCommand extends Command {
	public ExceedSelectCommand() {
		super("exceedselect", new String[]{"exselect", "sou"}, "Escolhe seu exceed, esta escolha é permanente.", Category.EXCEED);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		channel.sendMessage("<a:Loading:598500653215645697> Gerando placares...").queue(m -> {
			com.kuuhaku.model.Member u = SQLite.getMemberByMid(author.getId());

			if (u.getExceed().isEmpty()) {
				channel.sendMessage("Foi.").queue();
			} else {
				channel.sendMessage(":x: | Você já pertence à um exceed, não é possível trocá-lo.").queue();
			}
		});
	}
}
