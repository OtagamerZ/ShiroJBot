package com.kuuhaku.model;

import com.kuuhaku.Main;
import com.kuuhaku.controller.mysql.RelayDAO;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RelayBlockList {
	private static final List<String> blockedIDs = RelayDAO.blockedList();
	private static final List<String> blockedThumbs = new ArrayList<>();

	public static void blockID(String id, String reason) {
		EmbedBuilder eb = new EmbedBuilder();
		blockedIDs.add(id);

		eb.setTitle("Você foi bloqueado de utilizar o chat global pela seguinte razão:");
		eb.setDescription(reason + "\n\nVocê poderá voltar a utilizá-lo após o reset diário da Shiro. Caso seja bloqueado muitas vezes, poderá perder acesso permanentemente ao chat global!");
		eb.setColor(Color.orange);
		eb.setThumbnail("https://image.flaticon.com/icons/png/512/718/718672.png");
		Main.getInfo().getUserByID(id).openPrivateChannel().queue(c -> c.sendMessage(eb.build()).queue());
		Main.getInfo().getDevelopers().forEach(d -> Objects.requireNonNull(Main.getJibril().getUserById(d)).openPrivateChannel().queue(c -> {
			String msg = "Usuário bloqueado do chat global.```Usuário: " + Main.getInfo().getUserByID(id).getAsTag() + "\n\nRazão: " + reason + "```";
			c.sendMessage(msg).queue();
		}));
	}

	public static void permaBlockID(String id, String reason) {
		EmbedBuilder eb = new EmbedBuilder();
		blockedIDs.add(id);

		PermaBlock pb = new PermaBlock();
		pb.block(id);
		RelayDAO.permaBlock(pb);

		eb.setTitle("Você foi bloqueado permanentemente de utilizar o chat global");
		eb.setDescription("Este bloqueio **NÃO** será removido em momento algum, por melhor que seja a explicação para isto ter ocorrido!\n\nRazão: " + reason);
		eb.setColor(Color.red);
		eb.setThumbnail("https://cdn.pixabay.com/photo/2013/07/12/12/40/abort-146072_640.png");
		Main.getInfo().getUserByID(id).openPrivateChannel().queue(c -> c.sendMessage(eb.build()).queue());
		Main.getInfo().getDevelopers().forEach(d -> Objects.requireNonNull(Main.getJibril().getUserById(d)).openPrivateChannel().queue(c -> {
			String msg = "Usuário bloqueado permanentemente do chat global.```Usuário: " + Main.getInfo().getUserByID(id).getAsTag() + "\n\nRazão: " + reason + "```";
			c.sendMessage(msg).queue();
		}));
	}

	public static void blockThumb(String id) {
		EmbedBuilder eb = new EmbedBuilder();
		blockedThumbs.add(id);

		eb.setTitle("Seu avatar foi bloqueado por ser considerado indecente:");
		eb.setDescription("Seu avatar foi considerado indecente ou explícito, por esta razão ele foi temporariamente censurado. Recomendo que troque-o assim que possível pois poderá resultar em bloqueio do uso do chat global caso você mantenha este avatar.");
		eb.setColor(Color.yellow);
		eb.setThumbnail("https://www.rhinocarhire.com/CorporateSite/media/Drive-Smart/Dash-Symbols/brake_warning_symbol_in_red.png");
		Main.getInfo().getUserByID(id).openPrivateChannel().queue(c -> c.sendMessage(eb.build()).queue());
	}

	public static void clearBlockedThumbs() {
		blockedThumbs.clear();
	}

	public static boolean check(String id) {
		return blockedIDs.contains(id);
	}

	public static boolean checkThumb(String id) {
		return blockedThumbs.contains(id);
	}

	public static void refresh(){
		blockedIDs.clear();
		blockedIDs.addAll(RelayDAO.blockedList());
	}
}
