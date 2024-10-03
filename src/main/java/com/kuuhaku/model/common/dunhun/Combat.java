package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.Constants;
import com.kuuhaku.game.Dunhun;
import com.kuuhaku.game.engine.Renderer;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.XStringBuilder;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.dunhun.Hero;
import com.kuuhaku.model.persistent.dunhun.Monster;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Combat implements Renderer<BufferedImage> {
	private String lastAction = "";
	private Dunhun game;
	private Monster enemy;

	public Combat(Dunhun game) {
		this.game = game;
		enemy = Monster.getRandom();
	}

	@Override
	public BufferedImage render(I18N locale) {
		BufferedImage bi = new BufferedImage(255 * (game.getHeroes().size() + 1) + 64, 370, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();

		int offset = 0;
		for (Hero h : game.getHeroes().values()) {
			g2d.drawImage(h.render(locale), offset, 0, null);
			offset += 255;
		}

		BufferedImage cbIcon = IO.getResourceAsImage("dunhun/icons/combat.png");
		g2d.drawImage(cbIcon, offset, 153, null);
		offset += 64;

		g2d.drawImage(enemy.render(locale), offset, 0, null);
		g2d.dispose();

		return bi;
	}

	public MessageEmbed getEmbed() {
		I18N locale = game.getLocale();
		EmbedBuilder eb = new ColorlessEmbedBuilder();

		XStringBuilder sb = new XStringBuilder();
		for (Hero h : game.getHeroes().values()) {
			if (!sb.isEmpty()) sb.nextLine();

			sb.appendNewLine(h.getName() + "『" + h.getHp() + "/" + h.getMaxHp() + "』");
			sb.appendNewLine(Utils.makeProgressBar(h.getHp(), h.getMaxHp(), 10));
			sb.appendNewLine(Utils.makeProgressBar(h.getAp(), h.getMaxAp(), h.getMaxAp(), '◇', '◈'));
		}
		eb.addField(Constants.VOID, sb.toString(), true);

		sb.clear();
		sb.appendNewLine(enemy.getName(locale) + "『" + enemy.getHp() + "/" + enemy.getMaxHp() + "』");
		sb.appendNewLine(Utils.makeProgressBar(enemy.getHp(), enemy.getMaxHp(), 10));
		sb.appendNewLine(Utils.makeProgressBar(enemy.getAp(), enemy.getMaxAp(), enemy.getMaxAp(), '◇', '◈'));
		eb.addField(Constants.VOID, sb.toString(), true);

		eb.setImage("attachment://cards.png");

		return eb.build();
	}
}
