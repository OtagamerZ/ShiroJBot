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

package com.kuuhaku.model.common.embed;

import com.kuuhaku.utils.Helper;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Embed {
	private Title title;
	private Author author;
	private String color;
	private String body;
	private String thumbnail;
	private Image image;
	private boolean showDate;
	private Footer footer;
	private List<Field> fields;

	public Title getTitle() {
		return Helper.getOr(title, new Title());
	}

	public void setTitle(Title value) {
		this.title = value;
	}

	public Author getAuthor() {
		return Helper.getOr(author, new Author());
	}

	public void setAuthor(Author value) {
		this.author = value;
	}

	public String getColor() {
		return color;
	}

	public Color getParsedColor() {
		try {
			return Color.decode(color);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public void setColor(String value) {
		this.color = value;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String value) {
		this.body = value;
	}

	public String getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(String value) {
		this.thumbnail = value;
	}

	public Image getImage() {
		return Helper.getOr(image, new Image());
	}

	public void setImage(Image value) {
		this.image = value;
	}

	public boolean getShowDate() {
		return showDate;
	}

	public void setShowDate(boolean value) {
		this.showDate = value;
	}

	public Footer getFooter() {
		return Helper.getOr(footer, new Footer());
	}

	public void setFooter(Footer value) {
		this.footer = value;
	}

	public List<Field> getFields() {
		return Helper.getOr(fields, new ArrayList<Field>()).subList(0, 25);
	}

	public void setFields(List<Field> value) {
		this.fields = value;
	}
}