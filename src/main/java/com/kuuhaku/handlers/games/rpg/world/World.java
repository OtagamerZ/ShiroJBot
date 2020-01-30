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

package com.kuuhaku.handlers.games.rpg.world;

import com.kuuhaku.handlers.games.rpg.Utils;
import com.kuuhaku.handlers.games.rpg.actors.Actor;
import com.kuuhaku.handlers.games.rpg.entities.Chest;
import com.kuuhaku.handlers.games.rpg.entities.Item;
import com.kuuhaku.handlers.games.rpg.exceptions.UnknownItemException;
import com.kuuhaku.model.common.Profile;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.RestAction;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("rawtypes")
public class World {
	private com.kuuhaku.handlers.games.rpg.world.Map currentMap;
	private final ArrayList<com.kuuhaku.handlers.games.rpg.world.Map> maps = new ArrayList<>();
	private final String master;
	private final Map<String, Actor.Player> players = new HashMap<>();
	private final Map<String, Actor.Monster> monsters = new HashMap<>();
	private final Map<String, Chest> chests = new HashMap<>();
	private final List<Item> items = new ArrayList<>();
	private boolean locked = false;

	public World(User master) {
		this.master = master.getId();
		System.out.println(master.getId() + " | " + this.master);
	}

	public void addMap(com.kuuhaku.handlers.games.rpg.world.Map map) {
		maps.add(map);
	}

	public com.kuuhaku.handlers.games.rpg.world.Map getCurrentMap() {
		return currentMap;
	}

	public ArrayList<com.kuuhaku.handlers.games.rpg.world.Map> getMaps() {
		return maps;
	}

	public void switchMap(int map) {
		currentMap = maps.get(map);
	}

	public String getMaster() {
		return master;
	}

	public void addPlayer(Actor.Player player) {
		players.put(String.valueOf(player.getId()), player);
	}

	public void removePlayer(String id) {
		players.remove(id);
	}

	public void addMonster(Actor.Monster monster) {
		monsters.put(monster.getMob().getName(), monster);
	}

	public void removeMonster(String name) {
		monsters.remove(name);
	}

	public void addItem(Item item) {
		items.add(item);
	}

	public void removeItem(String name) throws UnknownItemException {
		items.remove(getItem(name));
	}

	public void addChest(Chest cst) {
		chests.put(cst.getName(), cst);
	}

	public void removeChest(String name) {
		chests.remove(name);
	}

	public Map<String, Actor.Player> getPlayers() {
		return players;
	}

	public Map<String, Actor.Monster> getMonsters() {
		return monsters;
	}

	public List<Item> getItems() {
		return items;
	}

	private byte[] getMap() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(positionPlayers(), "png", baos);
		getPlayers().values().forEach(p -> p.move(currentMap, p.getPos()));
		return baos.toByteArray();
	}

	public RestAction render(TextChannel channel) throws IOException {
		return channel.sendFile(getMap(), "world.png");
	}

	private BufferedImage positionPlayers() {
		BufferedImage playerMap = getModifiableMap();
		Graphics2D g2d = playerMap.createGraphics();
		g2d.setFont(new Font("Arial", Font.PLAIN, 50));
		g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2d.setColor(Color.GREEN);

		g2d.drawImage(currentMap.getMap(), 0, 0, null);
		final int[] offset = {61, 2};
		List<Actor.Player> playerList = new ArrayList<Actor.Player>() {{
			addAll(players.values());
		}};
		playerList.removeIf(p -> p.currentMap() != currentMap);
		Collections.reverse(playerList);

		if (playerList.size() == 0) return playerMap;
		for (int i = 0; i < playerList.size(); i++) {
			g2d.drawLine(97 + Utils.toDotPos(playerList.get(i).wasAt())[0], 97 + Utils.toDotPos(playerList.get(i).wasAt())[1], 97 + Utils.toDotPos(playerList.get(i).getPos())[0], 97 + Utils.toDotPos(playerList.get(i).getPos())[1]);
			g2d.drawImage(Profile.scaleImage(playerList.get(i).getPin(), 72, 95), Utils.toPinPos(playerList.get(i).getPos())[0] + offset[0], Utils.toPinPos(playerList.get(i).getPos())[1] + offset[1], null);
			g2d.drawString(playerList.get(i).getCharacter().getName() + " -> " + Arrays.toString(Utils.arrayToCoord(playerList.get(i).getPos())), 15, currentMap.getMap().getHeight() + 50 * (i + 1));
		}

		return playerMap;
	}

	private BufferedImage getModifiableMap() {
		return new BufferedImage(currentMap.getMap().getWidth(), currentMap.getMap().getHeight() + 70 * Math.max(players.values().size(), monsters.values().size()), BufferedImage.TYPE_INT_RGB);
	}

	public RestAction listItems(TextChannel channel) {
		EmbedBuilder eb = new EmbedBuilder();

		eb.setTitle("Itens cadastrados");
		eb.setThumbnail("https://www.worldlandtrust.org/wp-content/uploads/2018/04/globe-icon.png");

		if (items.size() > 0)
			eb.addField("Itens", items.stream().map(i -> "(" + i.getType().getName() + ") " + i.getName() + "\n").collect(Collectors.joining()), false);
		return channel.sendMessage(eb.build());
	}

	public RestAction listMonsters(TextChannel channel) {
		EmbedBuilder eb = new EmbedBuilder();

		eb.setTitle("Monstros cadastrados");
		eb.setThumbnail("https://www.worldlandtrust.org/wp-content/uploads/2018/04/globe-icon.png");

		if (monsters.size() > 0)
			eb.addField("Monstros", monsters.values().stream().map(m -> m.getMob().getName() + "\n").collect(Collectors.joining()), false);
		return channel.sendMessage(eb.build());
	}

	public RestAction listPlayers(TextChannel channel) {
		EmbedBuilder eb = new EmbedBuilder();

		eb.setTitle("Jogadores cadastrados");
		eb.setThumbnail("https://www.worldlandtrust.org/wp-content/uploads/2018/04/globe-icon.png");

		if (players.size() > 0)
			eb.addField("Jogadores", players.values().stream().map(p -> p.getCharacter().getName() + "\n").collect(Collectors.joining()), false);
		return channel.sendMessage(eb.build());
	}

	public RestAction listChests(TextChannel channel) {
		EmbedBuilder eb = new EmbedBuilder();

		eb.setTitle("Baús cadastrados");
		eb.setThumbnail("https://www.worldlandtrust.org/wp-content/uploads/2018/04/globe-icon.png");

		if (chests.size() > 0)
			eb.addField("Baús", chests.values().stream().map(p -> p.getName() + "\n").collect(Collectors.joining()), false);
		return channel.sendMessage(eb.build());
	}

	public Item getItem(String name) throws UnknownItemException {
		List<Item> its = items.stream().filter(i -> StringUtils.containsIgnoreCase(i.getName(), name)).collect(Collectors.toList());
		if (its.size() > 0) return its.get(0).getThis();
		else throw new UnknownItemException();
	}

	public Chest getChest(String name) {
		List<Chest> cst = chests.values().stream().filter(c -> StringUtils.containsIgnoreCase(c.getName(), name)).collect(Collectors.toList());
		if (cst.size() > 0) return cst.get(0);
		else throw new UnknownItemException();
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public String getAsJSON() {
		return ShiroInfo.getJSONFactory().toJson(this);
	}
}

