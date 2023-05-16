/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.common.special;

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.guild.GuildConfig;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.DynamicProperty;
import com.kuuhaku.model.records.GuildBuff;
import com.kuuhaku.model.records.PseudoUser;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class PadoruEvent extends SpecialEvent {
	private final Set<String> users = new HashSet<>();
	private int stage = 0;

	public PadoruEvent(I18N locale) {
		super(locale);
	}

	@Override
	public void start(GuildMessageChannel channel) {
		addEvent(channel.getGuild(), this);

		PseudoUser pu = new PseudoUser("Nero Claudius", Constants.ORIGIN_RESOURCES + "avatar/nero/1.png", channel);
		pu.send(null, "***Hashire sori yo***");

		Utils.awaitMessage(channel, this::onRun);
		EXEC.schedule(() -> onTimeout(channel), 3, TimeUnit.MINUTES);
	}

	@Override
	public boolean onRun(Message msg) {
		if (isComplete()) return true;

		String content = msg.getContentRaw();
		String phrase = switch (stage) {
			case 0 -> "Kaze no you ni";
			case 1 -> "Tsukimihara wo";
			default -> "PADORU PADORU";
		};

		if (content.equalsIgnoreCase(phrase) && users.add(msg.getAuthor().getId())) {
			stage++;

			Emoji e = msg.getJDA().getEmojiById("787012642501689344");
			if (e != null) {
				msg.addReaction(e).queue(null, Utils::doNothing);
			}

			if (isComplete()) {
				onCompletion(msg.getGuildChannel());
			}
		}

		return isComplete();
	}

	@Override
	public void onCompletion(GuildMessageChannel channel) {
		EXEC.shutdownNow();
		File gif = IO.getResourceAsFile("assets/padoru_padoru.gif");
		if (gif != null) {
			channel.sendMessage(getLocale().get("success/padoru_complete")).addFiles(FileUpload.fromData(gif)).queue();
		} else {
			channel.sendMessage(getLocale().get("success/padoru_complete")).queue();
		}

		for (String id : users) {
			Account acc = DAO.find(Account.class, id);
			DynamicProperty dp = acc.getDynamicProperty("padoru");
			dp.setValue(NumberUtils.toInt(dp.getValue()) + 1);
			dp.save();
		}

		DAO.apply(GuildConfig.class, channel.getGuild().getId(), gc -> {
			GuildBuff gb = new GuildBuff("padoru", 1, TimeUnit.HOURS, 0.5, 0.5, 0.5, 0.5);
			gc.addBuff(gb);
		});
	}

	@Override
	public void onTimeout(GuildMessageChannel channel) {
		stage = -1;
		channel.sendMessage(getLocale().get("str/padoru_fail")).queue();
	}

	@Override
	public boolean isComplete() {
		return stage >= 3 || stage == -1;
	}
}
