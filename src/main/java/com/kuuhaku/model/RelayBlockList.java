package com.kuuhaku.model;

import com.kuuhaku.Main;
import com.kuuhaku.controller.MySQL;
import net.dv8tion.jda.core.EmbedBuilder;

import java.awt.*;
import java.util.List;

public class RelayBlockList {
	private static List<String> blockedIDs = MySQL.blockedList();

	public static void blockID(String id, String reason) {
		EmbedBuilder eb = new EmbedBuilder();
		blockedIDs.add(id);

		eb.setTitle("Você foi bloqueado de utilizar o chat global pela seguinte razão:");
		eb.setDescription(reason + "\n\nVocê poderá voltar a utilizá-lo após o reset diário da Shiro. Caso seja bloqueado muitas vezes, poderá perder acesso permanentemente ao chat global!");
		eb.setColor(Color.orange);
		eb.setThumbnail("https://image.flaticon.com/icons/png/512/718/718672.png");
		Main.getInfo().getUserByID(id).openPrivateChannel().queue(c -> c.sendMessage(eb.build()).queue());
	}

	public static void permaBlockID(String id) {
		EmbedBuilder eb = new EmbedBuilder();
		blockedIDs.add(id);

		PermaBlock pb = new PermaBlock();
		pb.block(id);
		MySQL.permaBlock(pb);

		eb.setTitle("Você foi bloqueado permanentemente de utilizar o chat global");
		eb.setDescription("Este bloqueio **NÃO** será removido em momento algum, por melhor que seja a explicação para isto ter ocorrido!");
		eb.setColor(Color.red);
		eb.setThumbnail("https://cdn.pixabay.com/photo/2013/07/12/12/40/abort-146072_640.png");
		Main.getInfo().getUserByID(id).openPrivateChannel().queue(c -> c.sendMessage(eb.build()).queue());
	}

	public static boolean check(String id) {
		return blockedIDs.contains(id);
	}
}
