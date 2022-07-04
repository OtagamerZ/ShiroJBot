/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.command.commands.discord.information;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Class;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.KawaiponBook;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.AddedAnime;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.awt.image.BufferedImage;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Command(
        name = "kawaipons",
        aliases = {"kps"},
        usage = "req_kawaipon-args",
        category = Category.INFO
)
@Requires({Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS})
public class KawaiponsCommand implements Executable {

    @Override
    public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
        channel.sendMessage(I18n.getString("str_generating-collection")).queue(m -> {
            try {
                Account acc = AccountDAO.getAccount(author.getId());
                Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

                if (kp.getCards().isEmpty()) {
                    m.editMessage("❌ | Você ainda não coletou nenhum Kawaipon.").queue();
                    return;
                } else if (args.length == 0) {
                    Set<KawaiponCard> collection = new HashSet<>();
                    Set<AddedAnime> animes = CardDAO.getValidAnime();
                    for (AddedAnime anime : animes) {
                        if (acc.getCompletion(anime).any())
                            collection.add(new KawaiponCard(CardDAO.getUltimate(anime.getName()), false));
                    }

                    KawaiponBook kb = new KawaiponBook();
                    BufferedImage cards = kb.view(author.getId(), author.getName());

                    EmbedBuilder eb = new ColorlessEmbedBuilder();
                    int count = collection.size();
                    int foil = kp.getFoilCards().size();
                    int common = kp.getCards().size() - foil;

                    eb.setTitle("\uD83C\uDFB4 | Kawaipons de " + author.getName());
                    eb.addField(":books: | Coleções completas:", count + " de " + CardDAO.getValidAnime().size() + " (" + Helper.prcntToInt(count, CardDAO.getValidAnime().size(), RoundingMode.DOWN) + "%)", true);
                    eb.addField(":red_envelope: | Total de cartas normais:", common + " de " + CardDAO.getTotalCards() + " (" + Helper.prcntToInt(common, CardDAO.getTotalCards(), RoundingMode.DOWN) + "%)", true);
                    eb.addField(":star2: | Total de cartas cromadas:", foil + " de " + CardDAO.getTotalCards() + " (" + Helper.prcntToInt(foil, CardDAO.getTotalCards(), RoundingMode.DOWN) + "%)", true);
                    eb.setImage("attachment://cards.jpg");
                    eb.setFooter("Total coletado (normais + cromadas): " + Helper.prcntToInt(kp.getCards().size(), CardDAO.getTotalCards() * 2, RoundingMode.DOWN) + "%");

                    m.delete().queue();
                    channel.sendMessageEmbeds(eb.build()).addFile(Helper.writeAndGet(cards, "cards", "jpg")).queue();
                    return;
                }

                Class c = Class.getByName(args[0]);
                Race r = Race.getByName(args[0]);
                KawaiponRarity rr = KawaiponRarity.getByName(args[0]);

                if (rr == null) {
                    if (c == null) {
                        if (r == null) {
                           if (Helper.equalsAny(args[0], "elegivel", "elegiveis", "campeoes", "senshi")) {
                                List<Drawable> cardList = CardDAO.getAllChampions(false).stream().map(d -> (Drawable) d).collect(Collectors.toList());

                                KawaiponBook kb = new KawaiponBook();
                                BufferedImage cards = kb.view(cardList, AccountDAO.getAccount(author.getId()), "Cartas elegíveis");

                                send(author, channel, m, cards, "Cartas elegíveis", null);
                                return;
                            } else if (Helper.equalsAny(args[0], "item", "itens", "equips", "equipamentos", "equipments", "evogear")) {
                                List<Drawable> cardList = CardDAO.getAllEquipments().stream().map(d -> (Drawable) d).collect(Collectors.toList());

                                KawaiponBook kb = new KawaiponBook();
                                BufferedImage cards = kb.view(cardList, AccountDAO.getAccount(author.getId()), "Equipamentos evogear");

                                send(author, channel, m, cards, "Equipamentos evogear", null);
                                return;
                            } else if (Helper.equalsAny(args[0], "campo", "campos", "field", "fields")) {
                                List<Drawable> cardList = CardDAO.getAllFields().stream().map(d -> (Drawable) d).collect(Collectors.toList());

                                KawaiponBook kb = new KawaiponBook();
                                BufferedImage cards = kb.view(cardList, AccountDAO.getAccount(author.getId()), "Campos Shoukan");

                                send(author, channel, m, cards, "Equipamentos evogear", null);
                                return;
                            } else if (Helper.equalsAny(args[0], "fusao", "fusion", "fusions", "fusoes", "evolucao", "evolution", "evolucoes", "evolutions")) {
                                List<Drawable> cardList = CardDAO.getFusions().stream().map(d -> (Drawable) d).collect(Collectors.toList());

                                KawaiponBook kb = new KawaiponBook();
                                BufferedImage cards = kb.view(cardList, AccountDAO.getAccount(author.getId()), "Fusões Senshi");

                                send(author, channel, m, cards, "Fusões Senshi", null);
                                return;
                            }

                            boolean foil = args.length > 1 && args[1].equalsIgnoreCase("C");
                            AddedAnime anime = CardDAO.verifyAnime(args[0].toUpperCase(Locale.ROOT));

                            if (anime == null) {
                                m.editMessage("❌ | Anime inválido ou ainda não adicionado, você não quis dizer `" + Helper.didYouMean(args[0], CardDAO.getValidAnime().stream().map(AddedAnime::getName).toArray(String[]::new)) + "`? (colocar `_` no lugar de espaços)").queue();
                                return;
                            }

                            Set<KawaiponCard> collection = kp.getCards().stream().filter(k -> k.getCard().getAnime().equals(anime)).collect(Collectors.toSet());

                            KawaiponBook kb = new KawaiponBook();
                            BufferedImage cards = kb.view(author.getId(), anime, foil);

                            send(author, channel, m, collection, cards, anime.toString(), CardDAO.getTotalCards(anime.getName()));
                            return;
                        }

                        List<Drawable> cardList = CardDAO.getChampions(r).stream().map(d -> (Drawable) d).collect(Collectors.toList());

                        KawaiponBook kb = new KawaiponBook();
                        BufferedImage cards = kb.view(cardList, AccountDAO.getAccount(author.getId()), r.getName());

                        send(author, channel, m, cards, r, r.getName());
                        return;
                    }

                    List<Drawable> cardList = CardDAO.getChampions(c).stream().map(d -> (Drawable) d).collect(Collectors.toList());

                    KawaiponBook kb = new KawaiponBook();
                    BufferedImage cards = kb.view(cardList, AccountDAO.getAccount(author.getId()), c.getName());

                    send(author, channel, m, cards, c.getName(), c);
                } else {
                    boolean foil = args.length > 1 && args[1].equalsIgnoreCase("C");
                    Set<KawaiponCard> collection = kp.getCards().stream().filter(k -> k.getCard().getRarity().equals(rr)).collect(Collectors.toSet());

                    KawaiponBook kb = new KawaiponBook();
                    BufferedImage cards = kb.view(author.getId(), rr, foil);

                    send(author, channel, m, collection, cards, rr.toString(), CardDAO.getTotalCards(rr));
                }
            } catch (InterruptedException e) {
                m.editMessage(I18n.getString("err_collection-generation-error")).queue();
                Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
            }
        });
    }

    private void send(User author, MessageChannel channel, Message m, Set<KawaiponCard> collection, BufferedImage cards, String s, long l) {
        String hash = Helper.hash(author.getId() + System.currentTimeMillis(), "SHA-1");
        Helper.writeAndGet(Helper.removeAlpha(cards), hash, "jpg", Main.getInfo().getCollectionsFolder());
        Helper.keepMaximumNFiles(Main.getInfo().getCollectionsFolder(), 20);

        EmbedBuilder eb = new ColorlessEmbedBuilder();
        int foil = (int) collection.stream().filter(KawaiponCard::isFoil).count();
        int common = collection.size() - foil;

        String url = ShiroInfo.COLLECTION_ENDPOINT.formatted(hash);
        eb.setTitle("\uD83C\uDFB4 | Kawaipons de " + author.getName() + " (" + s + ")")
                .setDescription("[Clique aqui](%s) para abrir a imagem no navegador\nSe estiver borrada, [clique aqui](%s)".formatted(url, url + "&m=file"))
                .addField(":red_envelope: | Cartas normais:", common + " de " + l + " (" + Helper.prcntToInt(common, l, RoundingMode.DOWN) + "%)", true)
                .addField(":star2: | Cartas cromadas:", foil + " de " + l + " (" + Helper.prcntToInt(foil, l, RoundingMode.DOWN) + "%)", true)
                .setFooter("Total coletado (normais + cromadas): " + Helper.prcntToInt(collection.size(), l * 2, RoundingMode.DOWN) + "%")
                .setImage(url);
        m.delete().queue();

        channel.sendMessageEmbeds(eb.build()).queue();
    }

    private void send(User author, MessageChannel channel, Message m, BufferedImage cards, String s, Class c) {
        String hash = Helper.hash(author.getId() + System.currentTimeMillis(), "SHA-1");
        Helper.writeAndGet(Helper.removeAlpha(cards), hash, "jpg", Main.getInfo().getCollectionsFolder());
        Helper.keepMaximumNFiles(Main.getInfo().getCollectionsFolder(), 20);

        EmbedBuilder eb = new ColorlessEmbedBuilder();

        String url = ShiroInfo.COLLECTION_ENDPOINT.formatted(hash);
        eb.setTitle("\uD83C\uDFB4 | Cartas Senshi (" + s + ")")
                .setDescription("%s[Clique aqui](%s) para abrir a imagem no navegador\nSe estiver borrada, [clique aqui](%s)".formatted(c == null ? "" : c.getDescription() + "\n\n", url, url + "&m=file"))
                .setImage(url);
        m.delete().queue();

        channel.sendMessageEmbeds(eb.build()).queue();
    }

    private void send(User author, MessageChannel channel, Message m, BufferedImage cards, Race r, String s) {
        String hash = Helper.hash(author.getId() + System.currentTimeMillis(), "SHA-1");
        Helper.writeAndGet(Helper.removeAlpha(cards), hash, "jpg", Main.getInfo().getCollectionsFolder());
        Helper.keepMaximumNFiles(Main.getInfo().getCollectionsFolder(), 20);

        EmbedBuilder eb = new ColorlessEmbedBuilder();

        String url = ShiroInfo.COLLECTION_ENDPOINT.formatted(hash);
        eb.setTitle("\uD83C\uDFB4 | Cartas Senshi (" + s + ")")
                .setDescription("%s[Clique aqui](%s) para abrir a imagem no navegador\nSe estiver borrada, [clique aqui](%s)".formatted(r == null ? "" : r.getDescription() + "\n\n", url, url + "&m=file"))
                .setImage(url)
                .setThumbnail(ShiroInfo.RESOURCES_URL + "/shoukan/" + (r == null ? "shoukan.png" : "race/" + r.name().toLowerCase(Locale.ROOT) + ".png"));

		if (r != null) {
			eb.addField("Efeito primário", r.getMajorDesc(), true)
					.addField("Efeito secundário", r.getMinorDesc(), true);
		}
		m.delete().queue();

        channel.sendMessageEmbeds(eb.build()).queue();
	}
}