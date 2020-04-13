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

package com.kuuhaku.handlers.games.rpg.actors;

import com.kuuhaku.handlers.games.rpg.Utils;
import com.kuuhaku.handlers.games.rpg.entities.Character;
import com.kuuhaku.handlers.games.rpg.entities.Mob;
import com.kuuhaku.handlers.games.rpg.world.Map;
import net.dv8tion.jda.api.entities.User;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Objects;

public abstract class Actor {
	private final long id;
	private final java.util.Map<Map, Integer[]> pos = new HashMap<>();
	private java.util.Map<Map, Integer[]> oldPos = new HashMap<>();
	private Map atMap;
	private final String pin;

	private Actor(User user, Map map, String avatar) throws IOException {
		this.id = user.getIdLong();
		this.pin = makePlayerPin(user);
		this.atMap = map;
	}

	private Actor() {
		this.id = 0;
		this.pin = null;
	}

	public static class Player extends Actor {
		private final Character character;

		public Player(Map map, User user, Character character) throws IOException {
			super(user, map, character.getImage());
			this.character = character;
		}

		public Character getCharacter() {
			return character;
		}
	}

	public static class Monster extends Actor {
		private final Mob mob;

		public Monster(Mob mob) {
			this.mob = mob;
		}

		public Mob getMob() {
			return mob;
		}
	}

	public long getId() {
		return id;
	}

	public void toMap(Map map) {
		this.atMap = map;
	}

	public Map currentMap() {
		return atMap;
	}

	public void move(Map map, Integer[] pos) {
		this.oldPos = this.pos;
		this.pos.put(map, pos);
	}

	public Integer[] wasAt() {
		return oldPos.getOrDefault(atMap, new Integer[]{atMap.getDefaultPos()[0], atMap.getDefaultPos()[1]});
	}

	public Integer[] getPos() {
		return pos.getOrDefault(atMap, new Integer[]{atMap.getDefaultPos()[0], atMap.getDefaultPos()[1]});
	}

	public BufferedImage getPin() {
		return Utils.decodeBase64(pin);
	}

	private String makePlayerPin(User user) throws IOException {
		BufferedImage pin = ImageIO.read(Objects.requireNonNull(Actor.class.getClassLoader().getResourceAsStream("allypin.png")));
		BufferedImage canvas = new BufferedImage(pin.getWidth(), pin.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = canvas.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		HttpURLConnection con = (HttpURLConnection) new URL(Objects.requireNonNull(user.getAvatarUrl())).openConnection();
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		Image avatar = ImageIO.read(con.getInputStream()).getScaledInstance(128, 128, 0);

		g2d.setClip(new Ellipse2D.Float(6, 6, 128, 128));
		g2d.drawImage(avatar, 6, 6, null);

		g2d.setClip(null);
		g2d.drawImage(pin, 0, 0, null);

		return Utils.encodeToBase64(canvas);
	}
}
