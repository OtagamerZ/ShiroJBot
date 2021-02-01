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
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.KawaiponBook;
import com.kuuhaku.model.enums.AnimeName;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Command(
        name = "kawaipons",
        aliases = {"kps"},
        usage = "req_kawaipon-args",
        category = Category.INFO
)
public class KawaiponsCommand implements Executable {

    @Override
    public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
        channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("str_generating-collection")).queue(m -> {
            try {
                Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

                if (kp.getCards().size() == 0) {
                    m.editMessage("❌ | Você ainda não coletou nenhum Kawaipon.").queue();
                    return;
                } else if (args.length == 0) {
                    Set<KawaiponCard> collection = new HashSet<>();
                    for (AnimeName anime : AnimeName.validValues()) {
                        if (CardDAO.totalCards(anime) == kp.getCards().stream().filter(k -> k.getCard().getAnime().equals(anime) && !k.isFoil()).count())
                            collection.add(new KawaiponCard(CardDAO.getUltimate(anime), false));
                    }

                    KawaiponBook kb = new KawaiponBook(collection);
                    BufferedImage cards = kb.view(CardDAO.getCardsByRarity(KawaiponRarity.ULTIMATE), "Coleção Kawaipon", false);

                    EmbedBuilder eb = new ColorlessEmbedBuilder();
                    int count = collection.size();
                    int foil = (int) kp.getCards().stream().filter(KawaiponCard::isFoil).count();
                    int common = kp.getCards().size() - foil;

                    eb.setTitle("\uD83C\uDFB4 | Kawaipons de " + author.getName());
                    eb.addField(":books: | Coleções completas:", count + " de " + AnimeName.validValues().length + " (" + Helper.prcntToInt(count, AnimeName.validValues().length) + "%)", true);
                    eb.addField(":red_envelope: | Total de cartas normais:", common + " de " + CardDAO.totalCards() + " (" + Helper.prcntToInt(common, CardDAO.totalCards()) + "%)", true);
                    eb.addField(":star2: | Total de cartas cromadas:", foil + " de " + CardDAO.totalCards() + " (" + Helper.prcntToInt(foil, CardDAO.totalCards()) + "%)", true);
                    eb.setImage("attachment://cards.png");
                    eb.setFooter("Total coletado (normais + cromadas): " + Helper.prcntToInt(kp.getCards().size(), CardDAO.totalCards() * 2) + "%");

                    m.delete().queue();
                    channel.sendMessage(eb.build()).addFile(Helper.getBytes(cards, "png"), "cards.png").queue();
                    return;
                }

                Class c = Class.getByName(args[0]);
                Race r = Race.getByName(args[0]);
                KawaiponRarity rr = KawaiponRarity.getByName(args[0]);

                if (rr == null) {
                    if (c == null) {
                        if (r == null) {
                            if (args[0].equalsIgnoreCase("total")) {
                                boolean foil = args.length > 1 && args[1].equalsIgnoreCase("C");
                                Set<KawaiponCard> collection = kp.getCards();
                                Set<KawaiponCard> toRender = collection.stream().filter(k -> k.isFoil() == foil).collect(Collectors.toSet());

                                KawaiponBook kb = new KawaiponBook(toRender);
                                BufferedImage cards = kb.view(CardDAO.getCards(), "Todas as cartas", foil);

                                send(author, channel, m, collection, cards, "Todas as cartas", CardDAO.totalCards());
                                return;
                            } else if (Helper.equalsAny(args[0], "elegivel", "elegiveis", "campeoes", "senshi")) {
                                List<Drawable> cardList = CardDAO.getAllChampions(false).stream().map(d -> (Drawable) d).collect(Collectors.toList());

                                KawaiponBook kb = new KawaiponBook();
                                BufferedImage cards = kb.view(cardList, AccountDAO.getAccount(author.getId()), "Cartas elegíveis", true);

                                send(author, channel, m, cards, "Cartas elegíveis", null);
                                return;
                            } else if (Helper.equalsAny(args[0], "item", "itens", "equips", "equipamentos", "equipments", "evogear")) {
                                List<Drawable> cardList = CardDAO.getAllEquipments().stream().map(d -> (Drawable) d).collect(Collectors.toList());

                                KawaiponBook kb = new KawaiponBook();
                                BufferedImage cards = kb.view(cardList, AccountDAO.getAccount(author.getId()), "Equipamentos EvoGear", false);

                                send(author, channel, m, cards, "Equipamentos EvoGear", null);
                                return;
                            } else if (Arrays.stream(AnimeName.validValues()).noneMatch(a -> a.name().equals(args[0].toUpperCase()))) {
                                m.editMessage("❌ | Anime inválido ou ainda não adicionado, você não quis dizer `" + Helper.didYouMean(args[0], Arrays.stream(AnimeName.validValues()).map(AnimeName::name).toArray(String[]::new)) + "`? (colocar `_` no lugar de espaços)").queue();
                                return;
                            }

                            boolean foil = args.length > 1 && args[1].equalsIgnoreCase("C");
                            AnimeName anime = AnimeName.valueOf(args[0].toUpperCase());
                            Set<KawaiponCard> collection = kp.getCards().stream().filter(k -> k.getCard().getAnime().equals(anime)).collect(Collectors.toSet());
                            Set<KawaiponCard> toRender = collection.stream().filter(k -> k.isFoil() == foil).collect(Collectors.toSet());

                            KawaiponBook kb = new KawaiponBook(toRender);
                            BufferedImage cards = kb.view(CardDAO.getCardsByAnime(anime), anime.toString(), foil);

                            send(author, channel, m, collection, cards, anime.toString(), CardDAO.totalCards(anime));
                            return;
                        }

                        List<Drawable> cardList = CardDAO.getChampions(r).stream().map(d -> (Drawable) d).collect(Collectors.toList());

                        KawaiponBook kb = new KawaiponBook();
                        BufferedImage cards = kb.view(cardList, AccountDAO.getAccount(author.getId()), r.getName(), true);

                        send(author, channel, m, cards, r, r.getName());
                        return;
                    }

                    List<Drawable> cardList = CardDAO.getChampions(c).stream().map(d -> (Drawable) d).collect(Collectors.toList());

                    KawaiponBook kb = new KawaiponBook();
                    BufferedImage cards = kb.view(cardList, AccountDAO.getAccount(author.getId()), c.getName(), true);

                    send(author, channel, m, cards, c.getName(), c);
                } else {
                    boolean foil = args.length > 1 && args[1].equalsIgnoreCase("C");
                    Set<KawaiponCard> collection = kp.getCards().stream().filter(k -> k.getCard().getRarity().equals(rr)).collect(Collectors.toSet());
                    Set<KawaiponCard> toRender = collection.stream().filter(k -> k.isFoil() == foil).collect(Collectors.toSet());

                    KawaiponBook kb = new KawaiponBook(toRender);
                    BufferedImage cards = kb.view(CardDAO.getCardsByRarity(rr), rr.toString(), foil);


                    send(author, channel, m, collection, cards, rr.toString(), CardDAO.totalCards(rr));
                }
            } catch (IOException | InterruptedException e) {
                m.editMessage(ShiroInfo.getLocale(I18n.PT).getString("err_collection-generation-error")).queue();
                Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
            }
        });
    }

    private void send(User author, MessageChannel channel, Message m, Set<KawaiponCard> collection, BufferedImage cards, String s, long l) throws IOException {
        String hash = Helper.hash((author.getId() + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8), "SHA-1");
        File f = new File(Main.getInfo().getCollectionsFolder(), hash + ".jpg");
        byte[] bytes = Helper.getBytes(Helper.removeAlpha(cards), "jpg", 0.5f);
        //byte[] bytes = Helper.getBytes(Helper.removeAlpha(cards), "jpg");
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(bytes);
        }

        Helper.keepMaximumNFiles(Main.getInfo().getCollectionsFolder(), 20);

        EmbedBuilder eb = new ColorlessEmbedBuilder();
        int foil = (int) collection.stream().filter(KawaiponCard::isFoil).count();
        int common = collection.size() - foil;

        String url = "https://api." + System.getenv("SERVER_URL") + "/collection?id=" + hash;
        eb.setTitle("\uD83C\uDFB4 | Kawaipons de " + author.getName() + " (" + s + ")")
                .setDescription("[Clique para abrir a imagem no navegador](" + url + ")")
                .addField(":red_envelope: | Cartas normais:", common + " de " + l + " (" + Helper.prcntToInt(common, l) + "%)", true)
                .addField(":star2: | Cartas cromadas:", foil + " de " + l + " (" + Helper.prcntToInt(foil, l) + "%)", true)
                .setFooter("Total coletado (normais + cromadas): " + Helper.prcntToInt(collection.size(), l * 2) + "%")
                .setImage(url);
        m.delete().queue();

        channel.sendMessage(eb.build()).queue();
    }

    private void send(User author, MessageChannel channel, Message m, BufferedImage cards, String s, Class c) throws IOException {
        String hash = Helper.hash((author.getId() + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8), "SHA-1");
        File f = new File(Main.getInfo().getCollectionsFolder(), hash + ".jpg");
        byte[] bytes = Helper.getBytes(Helper.removeAlpha(cards), "jpg", 0.5f);
        //byte[] bytes = Helper.getBytes(Helper.removeAlpha(cards), "jpg");
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(bytes);
        }

        Helper.keepMaximumNFiles(Main.getInfo().getCollectionsFolder(), 20);

        EmbedBuilder eb = new ColorlessEmbedBuilder();

        String url = "https://api." + System.getenv("SERVER_URL") + "/collection?id=" + hash;
        eb.setTitle("\uD83C\uDFB4 | Cartas Senshi (" + s + ")")
                .setDescription((c == null ? "" : c.getDescription() + "\n\n") + "[Clique para abrir a imagem no navegador](" + url + ")")
                .setImage(url);
        m.delete().queue();

        channel.sendMessage(eb.build()).queue();
    }

    private void send(User author, MessageChannel channel, Message m, BufferedImage cards, Race r, String s) throws IOException {
        String hash = Helper.hash((author.getId() + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8), "SHA-1");
        File f = new File(Main.getInfo().getCollectionsFolder(), hash + ".jpg");
        byte[] bytes = Helper.getBytes(Helper.removeAlpha(cards), "jpg", 0.5f);
        //byte[] bytes = Helper.getBytes(Helper.removeAlpha(cards), "jpg");
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(bytes);
        }

        Helper.keepMaximumNFiles(Main.getInfo().getCollectionsFolder(), 20);

        EmbedBuilder eb = new ColorlessEmbedBuilder();

        String url = "https://api." + System.getenv("SERVER_URL") + "/collection?id=" + hash;
        eb.setTitle("\uD83C\uDFB4 | Cartas Senshi (" + s + ")")
                .setDescription((r == null ? "" : r.getDescription() + "\n\n") + "[Clique para abrir a imagem no navegador](" + url + ")")
                .setImage(url);
        m.delete().queue();

        channel.sendMessage(eb.build()).queue();
    }
}