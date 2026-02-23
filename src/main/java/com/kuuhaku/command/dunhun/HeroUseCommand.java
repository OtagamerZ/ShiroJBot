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

package com.kuuhaku.command.dunhun;

import com.github.ygimenez.model.Page;
import com.kuuhaku.interfaces.Executable;
import com.kuuhaku.interfaces.annotations.Command;
import com.kuuhaku.interfaces.annotations.Requires;
import com.kuuhaku.interfaces.annotations.Syntax;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.enums.Category;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.AccountSettings;
import com.kuuhaku.model.records.EventData;
import com.kuuhaku.model.records.FieldMimic;
import com.kuuhaku.model.records.MessageData;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.util.List;

@Command(
		name = "hero",
		path = "use",
		category = Category.INFO
)
@Syntax(allowEmpty = true, value = {
		"<hero:word:r>"
})
@Requires(Permission.MESSAGE_EMBED_LINKS)
public class HeroUseCommand implements Executable {
	@Override
	public void execute(JDA bot, I18N locale, EventData data, MessageData.Guild event, JSONObject args) {
		Account acc = data.profile().getAccount();
		AccountSettings settings = acc.getSettings();

		if (args.isEmpty()) {
			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle(locale.get("str/heroes"));

			List<Page> pages = Utils.generatePages(eb, acc.getHeroes(locale), 9, 3,
					hero -> new FieldMimic(
							(hero.isCurrent() ? "✅ " : "") + "`" + hero.getId() + "` " + hero.getName(),
							locale.get("str/level", hero.getLevel()) + (
									hero.isRetired() ? "**" + locale.get("str/retired") + "**" : ""
							)
					).toString()
			);

			Utils.paginate(pages, event.channel(), event.user());
			return;
		}

		List<Hero> heroes = acc.getHeroes(locale);
		String hero = args.getString("hero");
		for (Hero h : heroes) {
			if (h.getName().equalsIgnoreCase(hero)) {
				settings.setCurrentHero(h.getId());
				settings.save();

				event.channel().sendMessage(locale.get("success/switch", h.getName())).queue();
				return;
			}
		}

		event.channel().sendMessage(locale.get("error/unknown_hero")).queue();
	}
}