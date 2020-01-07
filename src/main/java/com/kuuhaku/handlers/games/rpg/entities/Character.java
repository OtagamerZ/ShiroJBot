package com.kuuhaku.handlers.games.rpg.entities;

import com.kuuhaku.handlers.games.rpg.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.RestAction;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.Collectors;

@SuppressWarnings("ALL")
public class Character {
	private final String name;
	private final String image;
	private final String bio;
	private final Status status;
	private final Equipped inventory = new Equipped();

	public Character(String name, String image, String bio, int strength, int perception, int endurance, int charisma, int intelligence, int agility, int luck) {
		this.name = name;
		this.image = image;
		this.bio = bio;
		this.status = new Status(strength, perception, endurance, charisma, intelligence, agility, luck);
	}

	public Character(String[] ssvData) {
		this(ssvData[0], ssvData[1], ssvData[2],
				Integer.parseInt(ssvData[3]),
				Integer.parseInt(ssvData[4]),
				Integer.parseInt(ssvData[5]),
				Integer.parseInt(ssvData[6]),
				Integer.parseInt(ssvData[7]),
				Integer.parseInt(ssvData[8]),
				Integer.parseInt(ssvData[9])
		);
	}

	public String getName() {
		return name;
	}

	public String getImage() {
		return image;
	}

	public String getBio() {
		return bio;
	}

	public Status getStatus() {
		return status;
	}

	public Equipped getInventory() {
		return inventory;
	}

	public void updateStats() {
		int[] modifiedStats = new int[8];
		for (int i = 0; i < modifiedStats.length; i++) {
			modifiedStats[i] = getInventory().getStatModifiers()[i] + getStatus().getBaseStats()[i];
		}

		getStatus().setStats(modifiedStats);
	}

	@SuppressWarnings("rawtypes")
	public RestAction openProfile(TextChannel channel) {
		EmbedBuilder eb = new EmbedBuilder();

		eb.setTitle("(" + status.getLevel() + ") Perfil de " + name + (status.isAlive() ? " (vivo)" : " (morto)"));
		eb.setThumbnail(image);
		eb.setDescription(bio);
		eb.addField("Vida", String.valueOf(status.getLife()), false);
		eb.addField("Defesa", String.valueOf(status.getDefense()), true);
		eb.addField("Força", String.valueOf(status.getStrength()), true);
		eb.addField("Percepção", String.valueOf(status.getPerception()), true);
		eb.addField("Resistência", String.valueOf(status.getEndurance()), true);
		eb.addField("Carisma", String.valueOf(status.getCharisma()), true);
		eb.addField("Inteligência", String.valueOf(status.getIntelligence()), true);
		eb.addField("Agilidade", String.valueOf(status.getAgility()), true);
		eb.addField("Sorte", String.valueOf(status.getLuck()), true);

		return channel.sendMessage(eb.build());
	}

	@SuppressWarnings("rawtypes")
	public RestAction openInventory(TextChannel channel) {
		EmbedBuilder eb = new EmbedBuilder();

		eb.setTitle("Inventário de " + name);
		eb.setThumbnail("https://image.flaticon.com/icons/png/512/183/183724.png");
		eb.setFooter("Ouro: " + inventory.getGold(), null);

		if (inventory.getItems().size() > 0)
			eb.addField("Itens", inventory.getItems().stream().map(i -> "(" + i.getType().getName() + ") " + i.getName() + "\n").collect(Collectors.joining()), false);
		return channel.sendMessage(eb.build());
	}

	public RestAction openNiceInventory(TextChannel channel) throws IOException, FontFormatException {
		//TODO Max. 24 caractéres
		BufferedImage bi = new BufferedImage(1240, 905, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Utils.makeItem(inventory.getHead() != null ? inventory.getHead().getName() : "", bi,"inventory/visored-helm.png", 64, 64, 0, 0, 600);
		Utils.makeItem(inventory.getChest() != null ? inventory.getChest().getName() : "", bi,"inventory/chest-armor.png", 64, 64, 0, 100, 600);
		Utils.makeItem(inventory.getLeg() != null ? inventory.getLeg().getName() : "", bi,"inventory/metal-skirt.png", 64, 64, 0, 200, 600);
		Utils.makeItem(inventory.getBoot() != null ? inventory.getBoot().getName() : "", bi,"inventory/leg-armor.png", 64, 64, 0, 300, 600);
		Utils.makeItem(inventory.getGlove() != null ? inventory.getGlove().getName() : "", bi,"inventory/gauntlet.png", 64, 64, 0, 400, 600);
		Utils.makeItem(inventory.getBag() != null ? inventory.getBag().getName() : "", bi,"inventory/backpack.png", 64, 64, 0, 500, 600);
		Utils.makeItem(inventory.getNecklace() != null ? inventory.getNecklace().getName() : "", bi,"inventory/gem-chain.png", 64, 64, 0, 600, 600);
		Utils.makeItem(inventory.getWeapon() != null ? inventory.getWeapon().getName() : "", bi,"inventory/battered-axe.png", 64, 64, 0, 700, 600);
		Utils.makeItem(inventory.getOffhand() != null ? inventory.getOffhand().getName() : "", bi,"inventory/templar-shield.png", 64, 64, 0, 800, 600);

		Utils.makeItem(inventory.getRings().size() > 0 ? inventory.getRings().get(0).getName() : "", bi,"inventory/ring.png", 64, 64, 620, 0, 600);
		Utils.makeItem(inventory.getRings().size() > 1 ? inventory.getRings().get(1).getName() : "", bi,"inventory/ring.png", 64, 64, 620, 100, 600);
		Utils.makeItem(inventory.getRings().size() > 2 ? inventory.getRings().get(2).getName() : "", bi,"inventory/ring.png", 64, 64, 620, 200, 600);
		Utils.makeItem(inventory.getRings().size() > 3 ? inventory.getRings().get(3).getName() : "", bi,"inventory/ring.png", 64, 64, 620, 300, 600);
		Utils.makeItem(inventory.getRings().size() > 4 ? inventory.getRings().get(4).getName() : "", bi,"inventory/ring.png", 64, 64, 620, 400, 600);

		Utils.makeInfoBox(this, bi, 600, 384, 620, 500);

		BufferedImage bi2 = new BufferedImage(bi.getWidth(), bi.getHeight() + 120, BufferedImage.TYPE_INT_RGB);
		g2d = bi2.createGraphics();
		g2d.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2d.setColor(Color.WHITE);
		g2d.setFont(new Font("Arial", Font.PLAIN, 60));

		g2d.drawImage(bi, 0, 120, null);
		g2d.drawRoundRect(10, 10, bi.getWidth() - 20, 100, 64, 64);
		g2d.drawString("Equipamentos de " + getName(), bi.getWidth() / 2 - (int) (g2d.getFontMetrics().getStringBounds("Equipamento de " + getName(), g2d).getWidth() / 2), 80);

		g2d.dispose();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(bi2, "png", baos);
		return channel.sendFile(baos.toByteArray(), "inventory.png");
	}

	public MessageEmbed openInventory() {
		EmbedBuilder eb = new EmbedBuilder();

		eb.setTitle("Inventário de " + name);
		eb.setThumbnail("https://image.flaticon.com/icons/png/512/183/183724.png");
		if (inventory.getWeapon() != null)
			eb.addField("Arma principal equipada", inventory.getWeapon().getName(), false);
		if (inventory.getOffhand() != null)
			eb.addField("Arma secundária equipada", inventory.getOffhand().getName(), false);
		if (inventory.getHead() != null) eb.addField("Elmo equipado", inventory.getHead().getName(), false);
		if (inventory.getChest() != null) eb.addField("Peitoral equipado", inventory.getChest().getName(), false);
		if (inventory.getLeg() != null) eb.addField("Calça equipada", inventory.getLeg().getName(), false);
		if (inventory.getBoot() != null) eb.addField("Botas equipadas", inventory.getBoot().getName(), false);
		if (inventory.getGlove() != null) eb.addField("Luvas equipadas", inventory.getGlove().getName(), false);
		if (inventory.getNecklace() != null) eb.addField("Colar equipado", inventory.getNecklace().getName(), false);
		if (inventory.getRings().size() > 0)
			eb.addField("Aneis equipados", inventory.getRings().stream().map(r -> r.getName() + "\n").collect(Collectors.joining()), false);
		if (inventory.getBag() != null) eb.addField("Bolsa equipada", inventory.getBag().getName(), false);

		if (inventory.getItems().size() > 0)
			eb.addField("Itens", inventory.getItems().stream().map(i -> "(" + i.getType().getName() + ") " + i.getName() + "\n").collect(Collectors.joining()), false);
		return eb.build();
	}
}

