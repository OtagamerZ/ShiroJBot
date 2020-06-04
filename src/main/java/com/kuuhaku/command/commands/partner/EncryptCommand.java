/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.commands.partner;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NonNls;
import org.python.util.PythonInterpreter;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public class EncryptCommand extends Command {

	public EncryptCommand(@NonNls String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public EncryptCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public EncryptCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public EncryptCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (message.getAttachments().size() < 1) {
			channel.sendMessage(":x: | Você precisa adicionar um arquivo para criptografar.").queue();
			return;
		} else if (args.length < 1) {
			channel.sendMessage(":x: | Você precisa digitar uma chave para ser usada na criptografia do arquivo.").queue();
			return;
		}

		try {
			File file = File.createTempFile(Base64.getEncoder().encodeToString(author.getId().getBytes(StandardCharsets.UTF_8)), "shr");
			Message.Attachment att = message.getAttachments().get(0);
			att.downloadToFile(file).thenAcceptAsync(f -> {
				try {
					File encode = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("shirocryptor/encrypt.py")).toURI());
					String fileContent = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
					PythonInterpreter py = new PythonInterpreter();

					py.set("target", fileContent);
					py.set("key", args[0]);

					py.exec(FileUtils.readFileToString(encode, StandardCharsets.UTF_8));
					String encodedContent = py.get("encFile", String.class);
					String hash = py.get("hashFile", String.class);

					channel.sendMessage("")
							.addFile(encodedContent.getBytes(StandardCharsets.UTF_8), att.getFileName() + att.getFileExtension() + ".sbd")
							.addFile(hash.getBytes(StandardCharsets.UTF_8), att.getFileName() + att.getFileExtension() + ".hash")
							.queue();
				} catch (IOException | URISyntaxException e) {
					Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
				}

			});
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}

}
