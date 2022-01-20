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

package com.kuuhaku.model.annotations;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import org.intellij.lang.annotations.Language;

import javax.annotation.RegEx;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Signature {
	String[] value();

	enum Type {
		ANY("\\S+"),
		WORD("\\w+"),
		NUMBER("\\d+"),
		TEXT(".+"),

		USER(Message.MentionType.USER.getPattern().pattern()),
		ROLE(Message.MentionType.ROLE.getPattern().pattern()),
		CHANNEL(Message.MentionType.CHANNEL.getPattern().pattern()),
		EMOTE(Message.MentionType.EMOTE.getPattern().pattern());

		private final String regex;

		Type(@Language("RegExp") String regex) {
			this.regex = regex;
		}

		public boolean validate(String value) {
			return value.matches(regex);
		}
	}
}
