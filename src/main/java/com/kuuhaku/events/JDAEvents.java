/*
 * This file is part of Shiro J Bot.
 *
 *     Shiro J Bot is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Shiro J Bot is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.events;

import com.kuuhaku.Main;
import com.kuuhaku.command.commands.Reactions.HugReaction;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.Beyblade;
import com.kuuhaku.model.DuelData;
import com.kuuhaku.model.guildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.*;
import java.util.function.Consumer;

public class JDAEvents extends ListenerAdapter {


    private static List<DuelData> dd = new ArrayList<>();
    public static Map<String, DuelData> duels = new HashMap<>();

    @Override
    public void onReady(ReadyEvent event) {
        try {
            System.out.println("Estou pronta!");
        } catch (Exception e) {
            System.out.println("Erro ao inicializar bot: " + e);
        }
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        Message message = event.getChannel().getMessageById(event.getMessageId()).complete();
        if (message.getAuthor() == Main.getInfo().getSelfUser()) {
            if (message.getContentRaw().contains("abraçou")) {
                User author = message.getMentionedUsers().get(0);
                MessageChannel channel = message.getChannel();

                new HugReaction(true).execute(author, null, null, null, message, channel, null, null, null);
            }

            if (duels.containsKey(event.getMessageId())) {
                dd.add(duels.get(event.getMessageId()));
                duels.remove(event.getMessageId());
                event.getChannel().sendMessage("O duelo começou!\nUsem `atacar` para atacar, `defender` para defender ou `especial` para tentar utilizar seu poder especial de alinhamento.\n\n**O desafiante começa primeiro!**").queue();
            }
        }
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (dd.stream().noneMatch(d -> d.getP1() == event.getAuthor() || d.getP2() == event.getAuthor())) {
            return;
        }
        @SuppressWarnings("OptionalGetWithoutIsPresent") DuelData duel = dd.stream().filter(d -> d.getP1() == event.getAuthor() || d.getP2() == event.getAuthor()).findFirst().get();
        boolean player1Turn = duel.isP1turn();
        EmbedBuilder eb = new EmbedBuilder();

        if (event.getMessage().getContentRaw().equalsIgnoreCase("atacar")) {
            if (player1Turn && event.getMessage().getAuthor() == duel.getP1()) {
                duel.setP1turn(false);
                duel.setD1(false);
                if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
                int damage = Math.round(duel.getB1().getStrength() * duel.getB1().getSpeed() / (duel.getB2().getStability() * Helper.getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 50);
                duel.getB2().setLife(duel.getB2().getLife() - damage);
                System.out.println(damage + " -> " + duel.getB2().getLife());
                event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getB1().getName() + " ataca, agora é a vez de " + duel.getB2().getName()).queue());
            } else if (!player1Turn && event.getMessage().getAuthor() == duel.getP2()) {
                duel.setP1turn(true);
                duel.setD2(false);
                if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
                int damage = Math.round(duel.getB2().getStrength() * duel.getB2().getSpeed() / (duel.getB1().getStability() * Helper.getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 50);
                duel.getB1().setLife(duel.getB1().getLife() - damage);
                System.out.println(damage + " -> " + duel.getB1().getLife());
                event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getB2().getName() + " ataca, agora é a vez de " + duel.getB1().getName()).queue());
            }
        } else if (event.getMessage().getContentRaw().equalsIgnoreCase("defender")) {
            if (player1Turn && event.getMessage().getAuthor() == duel.getP1()) {
                duel.setP1turn(false);
                duel.setD1(true);
                event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getB1().getName() + " assumiu uma postura defensiva, é a vez de " + duel.getB2().getName()).queue());
            } else if (!player1Turn && event.getMessage().getAuthor() == duel.getP2()) {
                duel.setP1turn(true);
                duel.setD2(true);
                event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getB2().getName() + " assumiu uma postura defensiva, é a vez de " + duel.getB1().getName()).queue());
            }
        } else if (event.getMessage().getContentRaw().equalsIgnoreCase("especial")) {
            if (duel.getB1().getS() != null) {
                if (!duel.isS1()) {
                    if (player1Turn && event.getMessage().getAuthor() == duel.getP1()) {
                        duel.setP1turn(false);
                        int chance = new Random().nextInt(100);
                        duel.setD1(false);
                        final Consumer<Void> Miss = tm -> event.getMessage().getChannel().sendMessage("Quase! " + duel.getB1().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) + ")").queue();
                        switch (duel.getB1().getSpecial()) {
                            case 11:
                                if (chance > duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) {
                                    duel.getB2().setLife(duel.getB2().getLife() - Math.round(duel.getB1().getStrength() * duel.getB1().getSpeed() / (duel.getB2().getStability() * Helper.getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 50));
                                    event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("O-O que?? " + duel.getB1().getName() + " desapareceu? Ah, lá está ela, com um movimento digno dos tigres ela executa o golpe especial " + duel.getB1().getS().getName() + "! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue());
                                    duel.setS1(true);
                                    if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
                                } else {
                                    event.getMessage().getChannel().sendTyping().queue(Miss);
                                }
                                break;
                            case 12:
                                if (chance > duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) {
                                    duel.getB2().setLife(duel.getB2().getLife() - Math.round(duel.getB1().getSpeed() * 2 / (duel.getB2().getStability() * Helper.getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 50));
                                    event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("Isso foi incrível!! " + duel.getB1().getName() + " executou com perfeição em " + duel.getB1().getS().getName() + " um dos golpes mais difíceis já conhecidos! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue());
                                    duel.setP1turn(true);
                                    event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getB2().getName() + " está atordoada, será que teremos uma reviravolta aqui?").queue());
                                    duel.setS1(true);
                                    if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
                                } else {
                                    event.getMessage().getChannel().sendTyping().queue(Miss);
                                }
                                break;
                            case 21:
                                if (chance > duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) {
                                    duel.getB2().setLife(duel.getB2().getLife() - Math.round(duel.getB1().getStrength() * duel.getB2().getStability() / (duel.getB2().getStability() * Helper.getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 50));
                                    event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("O que foi isso!? " + duel.getB1().getName() + " lançou " + duel.getB1().getS().getName() + " ao ar, depois o lançou utilizando sua própria defesa! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue());
                                    duel.setS1(true);
                                    if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
                                } else {
                                    event.getMessage().getChannel().sendTyping().queue(Miss);
                                }
                                break;
                            case 22:
                                if (chance > duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) {
                                    duel.getB2().setLife(duel.getB2().getLife() - Math.round(duel.getB1().getStrength() * duel.getB1().getStrength() / (duel.getB2().getStability() * Helper.getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 150));
                                    event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("Não é possível!! Eu jamais acreditaria se alguém me dissesse que era possível executar este golpe, mas " + duel.getB1().getName() + " provou que é possivel!!! " + duel.getB1().getS().getName() + " mal consegue se manter em pé! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue());
                                    duel.setS1(true);
                                    if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
                                } else {
                                    event.getMessage().getChannel().sendTyping().queue(Miss);
                                }
                                break;
                            case 31:
                                if (chance > duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) {
                                    duel.getB1().getS().setBear(true);
                                    duel.setD1(true);
                                    event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("Mais alguém está sentindo isso? " + duel.getB1().getName() + " acaba de executar a assinatura dos ursos!! Essa aura poderá virar o fluxo da partida! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue());
                                    duel.setS1(true);
                                } else {
                                    event.getMessage().getChannel().sendTyping().queue(Miss);
                                }
                                break;
                        }
                    } else {
                        event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("O poder mágico de " + duel.getB1().getName() + " já está no limite, não me parece que conseguirá realizar mais um golpe especial!").queue());
                    }
                } else {
                    event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getB1().getName() + " ainda não possui um alinhamento!").queue());
                }
            } else if (!player1Turn && event.getMessage().getAuthor() == duel.getP2()) {
                if (duel.getB2().getS() != null) {
                    if (!duel.isS2()) {
                        duel.setP1turn(true);
                        int chance = new Random().nextInt(100);
                        duel.setD2(false);
                        final Consumer<Void> Miss = tm -> event.getMessage().getChannel().sendMessage("Quase! " + duel.getB2().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) + ")").queue();
                        switch (duel.getB2().getSpecial()) {
                            case 11:
                                if (chance > duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) {
                                    duel.getB1().setLife(duel.getB1().getLife() - Math.round(duel.getB2().getStrength() * duel.getB2().getSpeed() / (duel.getB1().getStability() * Helper.getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 50));
                                    event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("O-O que?? " + duel.getB2().getName() + " desapareceu? Ah, lá está ele, com um movimento digno dos tigres ele executa o golpe especial " + duel.getB2().getS().getName() + "! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue());
                                    duel.setS2(true);
                                    if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
                                } else {
                                    event.getMessage().getChannel().sendTyping().queue(Miss);
                                }
                                break;
                            case 12:
                                if (chance > duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) {
                                    duel.getB1().setLife(duel.getB1().getLife() - Math.round(duel.getB2().getSpeed() * 2 / (duel.getB1().getStability() * Helper.getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 50));
                                    event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("Isso foi incrível!! " + duel.getB2().getName() + " executou com perfeição em " + duel.getB2().getS().getName() + " um dos golpes mais difíceis já conhecidos! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue());
                                    duel.setP1turn(false);
                                    event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getB1().getName() + " está atordoada, será que teremos uma reviravolta aqui?").queue());
                                    duel.setS2(true);
                                    if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
                                } else {
                                    event.getMessage().getChannel().sendTyping().queue(Miss);
                                }
                                break;
                            case 21:
                                if (chance > duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) {
                                    duel.getB1().setLife(duel.getB1().getLife() - Math.round(duel.getB2().getStrength() * duel.getB1().getStability() / (duel.getB1().getStability() * Helper.getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 50));
                                    event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("O que foi isso!? " + duel.getB2().getName() + " lançou " + duel.getB2().getS().getName() + " ao ar, depois o lançou utilizando sua própria defesa! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue());
                                    duel.setS2(true);
                                    if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
                                } else {
                                    event.getMessage().getChannel().sendTyping().queue(Miss);
                                }
                                break;
                            case 22:
                                if (chance > duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) {
                                    duel.getB1().setLife(duel.getB1().getLife() - Math.round(duel.getB2().getStrength() * duel.getB2().getStrength() / (duel.getB1().getStability() * Helper.getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 150));
                                    event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("Não é possível!! Eu jamais acreditaria se alguém me dissesse que era possível executar este golpe, mas " + duel.getB2().getName() + " provou que é possivel!!! " + duel.getB2().getS().getName() + " mal consegue se manter em pé! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue());
                                    duel.setS2(true);
                                    if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
                                } else {
                                    event.getMessage().getChannel().sendTyping().queue(Miss);
                                }
                                break;
                            case 31:
                                if (chance > duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) {
                                    duel.getB2().getS().setBear(true);
                                    duel.setD2(true);
                                    event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("Mais alguém está sentindo isso? " + duel.getB2().getName() + " acaba de executar a assinatura dos ursos!! Essa aura poderá virar o fluxo da partida! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue());
                                    duel.setS2(true);
                                } else {
                                    event.getMessage().getChannel().sendTyping().queue(Miss);
                                }
                                break;
                        }
                    } else {
                        event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage("O poder mágico de " + duel.getB2().getName() + " já está no limite, não me parece que conseguirá realizar mais um golpe especial!").queue());
                    }
                } else {
                    event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getB2().getName() + " ainda não possui um alinhamento!").queue());
                }
            }
        } else if (event.getMessage().getContentRaw().equalsIgnoreCase("desistir")) {
            if (event.getMessage().getAuthor() == duel.getP1()) {
                event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getP1().getAsMention() + " desistiu. A vitória é de " + duel.getP2().getAsMention() + "!").queue());
                Beyblade bl = MySQL.getBeybladeById(duel.getP1().getId());
                assert bl != null;
                bl.addLoses();
                MySQL.sendBeybladeToDB(bl);

                Beyblade bb = MySQL.getBeybladeById(duel.getP2().getId());
                assert bb != null;
                bb.addWins();
                MySQL.sendBeybladeToDB(bb);
                dd.removeIf(d -> d.getP1() == event.getMessage().getAuthor() || d.getP2() == event.getMessage().getAuthor());
            } else {
                event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getP2().getAsMention() + " desistiu. A vitória é de " + duel.getP1().getAsMention() + "!").queue());
                Beyblade bl = MySQL.getBeybladeById(duel.getP2().getId());
                assert bl != null;
                bl.addLoses();
                MySQL.sendBeybladeToDB(bl);

                Beyblade bb = MySQL.getBeybladeById(duel.getP1().getId());
                assert bb != null;
                bb.addWins();
                MySQL.sendBeybladeToDB(bb);
                dd.removeIf(d -> d.getP1() == event.getMessage().getAuthor() || d.getP2() == event.getMessage().getAuthor());
            }
        }
        if (duel.getB2().getLife() <= 0) {
            int pointWin = new Random().nextInt(Math.round(duel.getB2().getStrength() + duel.getB2().getSpeed() + duel.getB2().getStability() + (float) duel.getB1().getWins() / (duel.getB1().getLoses() == 0 ? 1 : duel.getB1().getLoses())));
            event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getP1().getAsMention() + " triunfou sobre " + duel.getP2().getAsMention() + ". Temos um vencedor!\n\n" + duel.getB1().getName() + " ganhou **" + pointWin + "** pontos de combate!").queue());
            Beyblade bl = MySQL.getBeybladeById(duel.getP2().getId());
            assert bl != null;
            bl.addLoses();
            MySQL.sendBeybladeToDB(bl);

            Beyblade bb = MySQL.getBeybladeById(duel.getP1().getId());
            assert bb != null;
            bb.addWins();
            bb.addPoints(pointWin);
            MySQL.sendBeybladeToDB(bb);
            dd.removeIf(d -> d.getP1() == event.getMessage().getAuthor() || d.getP2() == event.getMessage().getAuthor());
        } else if (duel.getB1().getLife() <= 0) {
            int pointWin = new Random().nextInt(Math.round(duel.getB1().getStrength() + duel.getB1().getSpeed() + duel.getB1().getStability() + (float) duel.getB1().getWins() / (duel.getB1().getLoses() == 0 ? 1 : duel.getB1().getLoses())));
            event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(duel.getP2().getAsMention() + " triunfou sobre " + duel.getP1().getAsMention() + ". Temos um vencedor!\n\n" + duel.getB2().getName() + " ganhou **" + pointWin + "** pontos de combate!").queue());
            Beyblade bl = MySQL.getBeybladeById(duel.getP1().getId());
            assert bl != null;
            bl.addLoses();
            MySQL.sendBeybladeToDB(bl);

            Beyblade bb = MySQL.getBeybladeById(duel.getP2().getId());
            assert bb != null;
            bb.addWins();
            bb.addPoints(pointWin);
            MySQL.sendBeybladeToDB(bb);
            dd.removeIf(d -> d.getP1() == event.getMessage().getAuthor() || d.getP2() == event.getMessage().getAuthor());
        } else if (event.getMessage().getContentRaw().equalsIgnoreCase("atacar") || event.getMessage().getContentRaw().equalsIgnoreCase("especial") || event.getMessage().getContentRaw().equalsIgnoreCase("defender")) {
            eb.setTitle("Dados do duelo:");
            eb.setDescription(duel.getB1().getName() + " *VS* " + duel.getB2().getName());
            eb.addField(duel.getB1().getName(), "Vida: " + duel.getB1().getLife(), true);
            eb.addField(duel.getB2().getName(), "Vida: " + duel.getB2().getLife(), true);
            event.getMessage().getChannel().sendTyping().queue(tm -> event.getMessage().getChannel().sendMessage(eb.build()).queue());
            dd.stream().filter(d -> d.getP1() == event.getMessage().getAuthor() || d.getP2() == event.getMessage().getAuthor()).findFirst().ifPresent(m -> {
                m.getB1().setLife(duel.getB1().getLife());
                m.getB2().setLife(duel.getB2().getLife());
            });
        }
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        guildConfig gc = new guildConfig();
        gc.setGuildId(event.getGuild().getId());
        SQLite.addGuildToDB(event.getGuild());
        try {
            Helper.sendPM(event.getGuild().getOwner().getUser(), "Obrigada por me adicionar ao seu servidor!");
        } catch (Exception err) {
            TextChannel dch = event.getGuild().getDefaultChannel();
            if (dch != null) {
                if (dch.canTalk()) {
                    dch.sendMessage("Obrigada por me adicionar ao seu servidor!").queue();
                }
            }
        }
    }

    /*@Override
	public void onReconnect(ReconnectedEvent event) {
		MainANT.getInfo().getLogChannel().sendMessage(DiscordHelper.getCustomEmoteMention(MainANT.getInfo().getGuild(), "kawaii") + " | Fui desparalizada!").queue();
	}*/

    @Override
    public void onShutdown(ShutdownEvent event) {
        //com.kuuhaku.MainANT.getInfo().getLogChannel().sendMessage(DiscordHelper.getCustomEmoteMention(com.kuuhaku.MainANT.getInfo().getGuild(), "choro") + " | Nunca vos esquecerei... Faleci! " + DiscordHelper.getCustomEmoteMention(com.kuuhaku.MainANT.getInfo().getGuild(), "bruh")).queue();
        System.out.println();
    }
	
	/*@Override
	public void onDisconnect(DisconnectEvent event) {
		com.kuuhaku.MainANT.getInfo().getLogChannel().sendMessage(DiscordHelper.getCustomEmoteMention(com.kuuhaku.MainANT.getInfo().getGuild(), "kms") + " | Fui paraliz-... " + DiscordHelper.getCustomEmoteMention(com.kuuhaku.MainANT.getInfo().getGuild(), "yeetus")).queue();
	}*/
}
