package com.kuuhaku.command.commands.fun;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import javax.imageio.ImageIO;
import javax.persistence.NoResultException;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MarryCommand extends Command {
	public MarryCommand() {
		super("casar", new String[]{"declarar", "marry"}, "<usuário>", "Pede um usuário em casamento.", Category.FUN);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
		try {
			if (message.getMentionedUsers().size() < 1) {
				channel.sendMessage(":x: | Você precisa mencionar um usuário!").queue();
				return;
			} else if (message.getMentionedUsers().get(0) == author) {
				channel.sendMessage(":x: | Por mais que eu respeite seu lado otaku, você não pode se casar com sí mesmo!").queue();
				return;
			} else if (!SQLite.getMemberById(author.getId() + guild.getId()).getWaifu().isEmpty() || !SQLite.getMemberById(message.getMentionedUsers().get(0).getId() + guild.getId()).getWaifu().isEmpty()) {
				channel.sendMessage(":x: | Essa pessoa já está casada, hora de passar pra frente!").queue();
				return;
			}

			channel.sendMessage(message.getMentionedUsers().get(0).getAsMention() + ", deseja casar-se com " + author.getAsMention() + ", por toda eternidade ou até que meu Nii-chan crie um comando de divórcio?" +
					"\nDigite `SIM` para aceitar ou `NÃO` para negar.").queue();
			Helper.queue.add(new User[]{author, message.getMentionedUsers().get(0)});
		} catch (NoResultException ignore) {
		}
	}
}
