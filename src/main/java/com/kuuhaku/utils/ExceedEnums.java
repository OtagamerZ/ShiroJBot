package com.kuuhaku.utils;

import com.kuuhaku.model.Exceed;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public enum ExceedEnums {
	IMANITY("Imanity"), SEIREN("Seiren"), WEREBEAST("Werebeast"), LUMAMANA("Lumamana"), EXMACHINA("Ex-Machina"), FLUGEL("Flügel");

	private String name;

	ExceedEnums(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public static ExceedEnums getByName(String name) {
		return Arrays.stream(ExceedEnums.values()).filter(e -> e.getName().equalsIgnoreCase(name)).collect(Collectors.toList()).get(0);
	}

	public static String getExceedEmote(ExceedEnums ex) {
		switch (ex) {
			case IMANITY:
				return "";
			case SEIREN:
				return "";
			case WEREBEAST:
				return "";
			case LUMAMANA:
				return "";
			case EXMACHINA:
				return "";
			case FLUGEL:
				return "";
		}
		return "";
	}

	public static BufferedImage getExceedImage(ExceedEnums ex) throws IOException {
		String path = "";
		switch (ex) {
			case IMANITY:
				path = "exceed/imanity.png";
				break;
			case SEIREN:
				path = "exceed/seiren.png";
				break;
			case WEREBEAST:
				path = "exceed/werebeast.png";
				break;
			case LUMAMANA:
				path = "exceed/lumamana.png";
				break;
			case EXMACHINA:
				path = "exceed/exmachina.png";
				break;
			case FLUGEL:
				path = "exceed/flugel.png";
				break;
		}

		return ImageIO.read(Objects.requireNonNull(ExceedEnums.class.getClassLoader().getResourceAsStream(path)));
	}
}
