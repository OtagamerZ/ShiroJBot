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

package com.kuuhaku.model.persistent;

import com.kuuhaku.utils.Helper;

import javax.imageio.ImageIO;
import javax.persistence.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Entity
@Table(name = "matchround")
public class MatchRound {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "TEXT")
	private String script = "";

	@Column(columnDefinition = "TEXT")
	private String snippet = null;

	public int getId() {
		return id;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String command) {
		this.script = command;
	}

	public void appendScript(String command) {
		this.script += script.isBlank() ? command : "\n" + command;
	}

	public BufferedImage getSnippet() {
		byte[] bytes = Base64.getDecoder().decode(snippet);
		try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
			return ImageIO.read(bais);
		} catch (IOException e) {
			return null;
		}
	}

	public void setSnippet(BufferedImage img) {
		BufferedImage bi = Helper.toColorSpace(Helper.scaleImage(img, 256, 256), BufferedImage.TYPE_INT_RGB);

		this.snippet = Base64.getEncoder().encodeToString(Helper.getBytes(bi, "jpg", 0.5f));
		;
	}
}
