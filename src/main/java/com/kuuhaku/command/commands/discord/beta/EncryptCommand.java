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

package com.kuuhaku.command.commands.discord.beta;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.io.FileUtils;
import org.jasypt.util.binary.StrongBinaryEncryptor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.Executors;

@Command(
		name = "criptografar",
		aliases = {"crypt", "crpt"},
		usage = "req_encrypt",
		category = Category.BETA
)
public class EncryptCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (message.getAttachments().size() < 1) {
			channel.sendMessage("❌ | Você precisa adicionar um arquivo para criptografar.").queue();
			return;
		} else if (args.length < 1) {
			channel.sendMessage("❌ | Você precisa digitar uma chave para ser usada na criptografia do arquivo.").queue();
			return;
		}

		StrongBinaryEncryptor ste = new StrongBinaryEncryptor();
		Message.Attachment att = message.getAttachments().get(0);

		Executors.newSingleThreadExecutor().execute(() -> {
			try {
				MessageDigest md = MessageDigest.getInstance("SHA-256");
				ste.setPassword(Arrays.toString(md.digest(args[0].getBytes(StandardCharsets.UTF_8))));
				att.downloadToFile(File.createTempFile(Base64.getEncoder().encodeToString(author.getId().getBytes(StandardCharsets.UTF_8)), "shr")).thenAcceptAsync(f -> {
					try {
						byte[] data = FileUtils.readFileToByteArray(f);
						byte[] encData = ste.encrypt(data);

						channel.sendMessage("Aqui está seu arquivo criptografado com a chave `" + args[0] + "`")
								.addFile(encData, att.getFileName() + ".shr")
								.flatMap(s -> message.delete())
								.queue(null, Helper::doNothing);
						message.delete().queue(null, Helper::doNothing);
					} catch (IOException e) {
						Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
					}
				});
			} catch (IOException | NoSuchAlgorithmException e) {
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		});
	}

}
