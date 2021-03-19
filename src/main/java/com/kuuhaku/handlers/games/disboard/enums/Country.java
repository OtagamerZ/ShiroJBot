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

package com.kuuhaku.handlers.games.disboard.enums;

import com.kuuhaku.utils.Helper;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


public enum Country {
	AFLAR("Aflar", new Point(3792, 374)),
	AFLIL("Aflil", new Point(1004, 1196)),
	ASCEN("Ascen", new Point(3095, 29)),
	ASCYE("Ascye", new Point(3104, 0)),
	ASHAIT("Ashait", new Point(1138, 1206)),
	ASHEA("Ashea", new Point(0, 0)),
	ASTRA("Astra", new Point(1418, 564)),
	ASWEN("Aswen", new Point(654, 1704)),
	ASWIA("Aswia", new Point(1605, 618)),
	CLAEZE("Claeze", new Point(1183, 854)),
	CLOU_FLA("Clou Fla", new Point(1216, 1126)),
	DESTAIT("Destait", new Point(1326, 356)),
	ELCHEA("Elchea", new Point(1668, 942)),
	ELVEN_GARD("Elven Gard", new Point(0, 0)),
	ESHA("Esha", new Point(2331, 1219)),
	ESHAIN("Eshain", new Point(2139, 2121)),
	ESTYA("Estya", new Point(1985, 1107)),
	ESWAIN("Eswain", new Point(1763, 810)),
	FAFLYE("Faflye", new Point(1108, 1378)),
	FLIOH_TRISTAN("Flioh Tristan", new Point(1962, 750)),
	FLOYRUS("Floyrus", new Point(2012, 817)),
	FLUYCIA("Fluycia", new Point(1731, 954)),
	FRIYCA("Friyca", new Point(1858, 580)),
	GREYBURG("Greyburg", new Point(1376, 0)),
	HARDEN_FELL("Harden Fell", new Point(2136, 112)),
	HEDRYA("Hedrya", new Point(2407, 521)),
	KOSTEN("Kosten", new Point(1195, 836)),
	LUSHEN("Lushen", new Point(2475, 1376)),
	PLUL_CRESH("Plul Cresh", new Point(1571, 854)),
	PRIELAND("Prieland", new Point(2179, 777)),
	PROILES("Proiles", new Point(2162, 582)),
	SCIAH_STRELA("Sciah Strela", new Point(1357, 729)),
	SCOI_STAD("Scoi Stad", new Point(1514, 1084)),
	SHABIA("Shabia", new Point(1710, 1317)),
	SHIER_CRON("Shier Cron", new Point(1391, 989)),
	SHIOSAU("Shiosau", new Point(2992, 1675)),
	SPUYI("Spuyi", new Point(1189, 751)),
	SHIJAN("Shijan", new Point(1252, 751)),
	STAIDAL("Staidal", new Point(1118, 1294)),
	STAUBIA("Staubia", new Point(1474, 1216)),
	STEYLES("Steyles", new Point(3797, 669)),
	STUII_SHUWA("Stuii Shuwa", new Point(1713, 941));

	private final String name;
	private final Point coords;
	public static final List<Country> availableCountries = new ArrayList<>() {{
		addAll(List.of(values()));
	}};

	Country(String name, Point coords) {
		this.name = name;
		this.coords = coords;
	}

	public String getName() {
		return name;
	}

	public Point getCoords() {
		return coords;
	}

	public String getFilepath() {
		return this.name().toLowerCase(Locale.ROOT) + ".png";
	}

	public int getSize() {
		int size = 0;
		try {
			BufferedImage bi = scale(ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResource("countries/" + getFilepath()))), 0.1);
			byte[] pixels = ((DataBufferByte) bi.getAlphaRaster().getDataBuffer()).getData();
			for (byte pixel : pixels) {
				if (pixel == 0) size++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return size;
	}

	private BufferedImage scale(BufferedImage source, double ratio) {
		int w = (int) (source.getWidth() * ratio);
		int h = (int) (source.getHeight() * ratio);
		BufferedImage bi = new BufferedImage(w, h, source.getType());
		Graphics2D g2d = bi.createGraphics();
		double xScale = (double) w / source.getWidth();
		double yScale = (double) h / source.getHeight();
		AffineTransform at = AffineTransform.getScaleInstance(xScale, yScale);
		g2d.drawRenderedImage(source, at);
		g2d.dispose();
		return bi;
	}

	public static List<Country> getStartingCountries() {
		List<Country> out = new ArrayList<>();

		for (int i = 0; i < 7; i++) {
			int index = Helper.rng(availableCountries.size(), true);

			out.add(availableCountries.get(index));
			availableCountries.remove(index);
		}

		return out;
	}
}
