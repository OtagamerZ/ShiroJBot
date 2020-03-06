/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.handlers.api.endpoint;

import com.kuuhaku.controller.mysql.TokenDAO;
import com.kuuhaku.handlers.api.exception.InternalErrorException;
import com.kuuhaku.handlers.api.exception.NotEnoughArgsException;
import com.kuuhaku.handlers.api.exception.UnauthorizedException;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.utils.Helper;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

@RestController
public class MemeRequest {
	@RequestMapping(value = "/meme/twobuttons", method = RequestMethod.POST)
	public @ResponseBody
	byte[] getTwoButtonsMeme(@RequestHeader(value = "token") String token,
							 @RequestHeader(value = "field-A") String fieldA,
							 @RequestHeader(value = "field-B") String fieldB) {
		if (!TokenDAO.validateToken(token)) throw new UnauthorizedException();
		else if (fieldA.isEmpty() || fieldB.isEmpty()) {
			throw new NotEnoughArgsException();
		}

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			BufferedImage bi = ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("Two-Buttons.jpg")));
			Graphics2D g2d = bi.createGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g2d.setColor(Color.BLACK);
			g2d.setFont(new Font("Arial", Font.BOLD, 25));
			if (g2d.getFontMetrics().stringWidth(fieldA + fieldB) > 270) {
				Profile.drawStringMultiLine(g2d, fieldA, 215, 55, 135);
				Profile.drawStringMultiLine(g2d, fieldB, 215, 255, 100);
			} else {
				Profile.printCenteredString(fieldA, 215, 55, 135, g2d);
				Profile.printCenteredString(fieldB, 215, 255, 100, g2d);
			}

			g2d.dispose();

			ImageIO.write(bi, "png", baos);
			return baos.toByteArray();
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			throw new InternalErrorException();
		}
	}
}
