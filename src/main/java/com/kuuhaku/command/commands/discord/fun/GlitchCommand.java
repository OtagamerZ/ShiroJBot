/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.command.commands.discord.fun;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.GifFrame;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ImageFilters;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.imaging.ImageReadException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Command(
		name = "bugar",
		aliases = {"glitch"},
		usage = "req_intensity",
		category = Category.FUN
)
@Requires({Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_HISTORY})
public class GlitchCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		AtomicReference<Message> ms = new AtomicReference<>();
		channel.sendMessage("<a:loading:697879726630502401> Bugando a imagem...")
				.flatMap(s -> {
					ms.set(s);
					return channel.getHistory().retrievePast(25);
				})
				.queue(s -> {
					Message msg;
					if (Helper.getImageFrom(message) != null)
						msg = message;
					else
						msg = s.stream()
								.filter(m -> Helper.getImageFrom(m) != null && !m.getId().equals(ms.get().getId()))
								.max(Comparator.comparing(ISnowflake::getTimeCreated))
								.orElse(null);

					if (msg == null) {
						ms.get().delete().queue(null, Helper::doNothing);
						channel.sendMessage("❌ | Não encontrei nenhum imagem nas últimas 25 mensagens, por favor envie uma.").queue();
						return;
					}

					try {
						String url = Helper.getImageFrom(msg);
						File f;

						int pow = 4;
						if (args.length > 0) {
							pow = Integer.parseInt(args[0]);
							if (!Helper.between(pow, 1, 21)) {
								ms.get().delete().queue(null, Helper::doNothing);
								channel.sendMessage("❌ | A intensidade deve ser um valor entre 1 e 20.").queue();
								return;
							}
						}

						assert url != null;
						if (false && url.contains(".gif")) {
							int finalPow = pow;
							f = File.createTempFile("glitched", ".gif");
							List<GifFrame> frames = Helper.readGif(url, true);
							frames.replaceAll(frame -> new GifFrame(
									ImageFilters.glitch(frame.getAdjustedFrame(), finalPow),
									frame.getDisposal(),
									frame.getWidth(),
									frame.getHeight(),
									frame.getOffsetX(),
									frame.getOffsetY(),
									frame.getDelay()
							));

							Helper.makeGIF(f, frames);
						} else {
							BufferedImage bi = ImageIO.read(Helper.getImage(url));

							f = Helper.writeAndGet(ImageFilters.glitch(bi, pow), "glitched", "png");
						}

						ms.get().delete().queue(null, Helper::doNothing);
						channel.sendMessage("Aqui está sua imagem!")
								.addFile(f)
								.queue();
					} catch (NullPointerException | IOException | ImageReadException e) {
						ms.get().delete().queue(null, Helper::doNothing);
						channel.sendMessage("❌ | Houve um erro ao baixar a imagem, tente com outra.").queue();
					} catch (NumberFormatException e) {
						ms.get().delete().queue(null, Helper::doNothing);
						channel.sendMessage("❌ | A intensidade deve ser um valor numérico.").queue();
					} catch (IllegalArgumentException e) {
						channel.sendMessage("❌ | A imagem final ficou muito grande, que tal tentar com uma imagem menor?").queue();
					}
				});
	}
}
