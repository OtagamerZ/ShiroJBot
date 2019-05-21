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

package com.kuuhaku.commands;

import com.kuuhaku.controller.Database;
import com.kuuhaku.model.Beyblade;
import com.kuuhaku.model.DuelData;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class Arena {
    public static void start(MessageReceivedEvent message, String[] cmd) {
        if (Database.getBeyblade(message.getAuthor().getId()) == null) {
            if (cmd.length > 1) {
                Beyblade bb = new Beyblade();
                bb.setId(message.getAuthor().getId());
                bb.setName(message.getMessage().getContentRaw().replace(cmd[0], "").trim());
                Database.sendBeyblade(bb);
                message.getChannel().sendMessage("Parabéns, você acabou de obter sua mais nova Beyblade!").queue();
            } else {
                message.getChannel().sendMessage("Você precisa dar um nome a sua beyblade!").queue();
            }
        } else {
            message.getChannel().sendMessage("Você já possui uma beyblade!").queue();
        }
    }

    public static void setCor(MessageReceivedEvent message, String[] cmd) {
        Beyblade bb = Database.getBeyblade(message.getAuthor().getId());
        if (bb == null) message.getChannel().sendMessage("Você não possui uma beyblade!").queue();
        else if (cmd.length > 1) {
            if (cmd[1].contains("#") && cmd[1].length() == 7) {
                bb.setColor(cmd[1]);
                Database.sendBeyblade(bb);
                message.getChannel().sendMessage("Cor setada com sucesso!").queue();
            } else {
                message.getChannel().sendMessage("A cor precisa estar neste formato: `#RRGGBB`").queue();
            }
        } else {
            message.getChannel().sendMessage("Você precisa especificar uma cor!").queue();
        }
    }

    public static void setName(MessageReceivedEvent message, String[] cmd) {
        Beyblade bb = Database.getBeyblade(message.getAuthor().getId());
        if (bb == null) message.getChannel().sendMessage("Você não possui uma beyblade!").queue();
        else if (bb.getPoints() < 50)
            message.getChannel().sendMessage("Você não possui pontos de combate suficiente!").queue();
        else if (cmd.length > 1) {
            if (message.getMessage().getContentRaw().replace(cmd[0], "").trim().length() < 20) {
                bb.setName(message.getMessage().getContentRaw().replace(cmd[0], "").trim());
                bb.takePoints(50);
                message.getChannel().sendMessage("Nome trocado com sucesso!").queue();
            } else {
                message.getChannel().sendMessage("O nome que você escolheu possui mais de 20 caracteres, por favor escolha um nome mais curto.").queue();
            }
        } else {
            message.getChannel().sendMessage("Você não me disse um nome!").queue();
        }
    }

    public static void upgrade(MessageReceivedEvent message, String[] cmd) {
        Beyblade bb = Database.getBeyblade(message.getAuthor().getId());
        switch (cmd[1]) {
            case "força":
                if (bb == null) message.getChannel().sendMessage("Você não possui uma beyblade!").queue();
                else if (bb.getPoints() < Math.round(15 * bb.getStrength() + bb.getStrength() + bb.getSpeed() + bb.getStability()))
                    message.getChannel().sendMessage("Você não possui pontos de combate suficiente!").queue();
                else {
                    bb.takePoints(Math.round(15 * bb.getStrength() + bb.getStrength() + bb.getSpeed() + bb.getStability()));
                    bb.addStrength();
                    message.getChannel().sendMessage("Poder aumentado com sucesso!").queue();
                }
                break;
            case "velocidade":
                if (bb == null) message.getChannel().sendMessage("Você não possui uma beyblade!").queue();
                else if (bb.getPoints() < Math.round(15 * bb.getSpeed() + bb.getStrength() + bb.getSpeed() + bb.getStability()))
                    message.getChannel().sendMessage("Você não possui pontos de combate suficiente!").queue();
                else {
                    bb.takePoints(Math.round(15 * bb.getSpeed() + bb.getStrength() + bb.getSpeed() + bb.getStability()));
                    bb.addSpeed();
                    message.getChannel().sendMessage("Velocidade aumentada com sucesso!").queue();
                }
                break;
            case "estabilidade":
                if (bb == null) message.getChannel().sendMessage("Você não possui uma beyblade!").queue();
                else if (bb.getPoints() < Math.round(15 * bb.getStability() + bb.getStrength() + bb.getSpeed() + bb.getStability()))
                    message.getChannel().sendMessage("Você não possui pontos de combate suficiente!").queue();
                else {
                    bb.takePoints(Math.round(15 * bb.getStability() + bb.getStrength() + bb.getSpeed() + bb.getStability()));
                    bb.addStability();
                    message.getChannel().sendMessage("Estabilidade aumentada com sucesso!").queue();
                }
                break;
            case "vida":
                if (bb == null) message.getChannel().sendMessage("Você não possui uma beyblade!").queue();
                else if (bb.getPoints() < Math.round(bb.getLife() / 2))
                    message.getChannel().sendMessage("Você não possui pontos de combate suficiente!").queue();
                else {
                    bb.takePoints(Math.round(bb.getLife() / 2));
                    bb.addLife();
                    message.getChannel().sendMessage("Vida aumentada com sucesso!").queue();
                }
                break;
                default: message.getChannel().sendMessage("O atributo especificado não é válido!").queue();
        }
    }

    public static void duel(MessageReceivedEvent message, Map<Long, DuelData> duels) {
        if (message.getMessage().getMentionedUsers().size() > 0) {
            if (Database.getBeyblade(message.getMessage().getMentionedUsers().get(0).getId()) != null) {
                message.getChannel().sendMessage(message.getMessage().getMentionedMembers().get(0).getAsMention() + ", você foi desafiado a um duelo de beyblades por " + message.getAuthor().getAsMention() + ". Se deseja aceitar, clique no botão abaixo:").queue(m -> {
                            m.addReaction("\u21aa").queue();
                            DuelData dd = new DuelData(message.getAuthor(), message.getMessage().getMentionedUsers().get(0));
                            duels.put(m.getIdLong(), dd);
                        }
                );
            } else {
                message.getChannel().sendMessage("Este usuário ainda não possui uma beyblade.").queue();
            }
        }
    }

    public static void battle(List<DuelData> accDuels, MessageReceivedEvent message) {
        @SuppressWarnings("OptionalGetWithoutIsPresent") DuelData duel = accDuels.stream().filter(d -> d.getP1() == message.getAuthor() || d.getP2() == message.getAuthor()).findFirst().get();
        boolean player1Turn = duel.isP1turn();

        if (message.getMessage().getContentRaw().equalsIgnoreCase("atacar")) {
            if (player1Turn && message.getAuthor() == duel.getP1()) {
                duel.setP1turn(false);
                duel.setD1(false);
                int damage = Math.round(duel.getB1().getStrength() * duel.getB1().getSpeed() / (duel.getB1().getStrength() + duel.getB2().getStability()) * (new Random().nextInt(Math.round(100 / (duel.isD2() ? duel.getB2().getStability() : 1)))));
                duel.getB2().setLife(duel.getB2().getLife() - damage);
                System.out.println(damage + " -> " + duel.getB2().getLife());
                message.getChannel().sendMessage(duel.getB1().getName() + " ataca, agora é a vez de " + duel.getB2().getName()).queue();
            } else if (!player1Turn && message.getAuthor() == duel.getP2()) {
                duel.setP1turn(true);
                duel.setD2(false);
                int damage = Math.round(duel.getB2().getStrength() * duel.getB2().getSpeed() / (duel.getB2().getStrength() + duel.getB1().getStability()) * (new Random().nextInt(Math.round(100 / (duel.isD1() ? duel.getB1().getStability() : 1)))));
                duel.getB1().setLife(duel.getB1().getLife() - damage);
                System.out.println(damage + " -> " + duel.getB1().getLife());
                message.getChannel().sendMessage(duel.getB2().getName() + " ataca, agora é a vez de " + duel.getB1().getName()).queue();
            }
        } else if (message.getMessage().getContentRaw().equalsIgnoreCase("defender")) {
            if (player1Turn && message.getAuthor() == duel.getP1()) {
                duel.setP1turn(false);
                duel.setD1(true);
                message.getChannel().sendMessage(duel.getB1().getName() + " assumiu uma postura defensiva, é sua vez " + duel.getB2().getName()).queue();
            } else if (!player1Turn && message.getAuthor() == duel.getP2()) {
                duel.setP1turn(true);
                duel.setD2(true);
                message.getChannel().sendMessage(duel.getB2().getName() + " assumiu uma postura defensiva, é sua vez " + duel.getB1().getName()).queue();
            }
        } else if (message.getMessage().getContentRaw().equalsIgnoreCase("especial")) {
            if (player1Turn && message.getAuthor() == duel.getP1()) {
                duel.setP1turn(false);
                int chance = new Random().nextInt(100);
                duel.setD1(false);
                if (chance > (71 - Math.round(duel.getB1().getSpeed()))) {
                    duel.getB2().setLife(duel.getB2().getLife() - Math.round(duel.getB1().getStrength() * duel.getB1().getSpeed() / (duel.getB1().getStrength() + duel.getB2().getStability()) * (new Random().nextInt(Math.round(100 / (duel.isD2() ? duel.getB2().getStability() : 1)))) * 2));
                    message.getChannel().sendMessage("Em uma manobra espetacular, " + duel.getB1().getName() + " acerta um golpe especial e causa o dobro do dano comum!! (" + chance + ")").queue();
                } else
                    message.getChannel().sendMessage("Você errou o especial e perdeu o turno! (" + chance + " < " + (71 - Math.round(duel.getB1().getSpeed())) + ")").queue();
            } else if (!player1Turn && message.getAuthor() == duel.getP2()) {
                duel.setP1turn(true);
                int chance = new Random().nextInt(100);
                duel.setD2(false);
                if (chance > (71 - Math.round(duel.getB2().getSpeed()))) {
                    duel.getB1().setLife(duel.getB1().getLife() - Math.round(duel.getB2().getStrength() * duel.getB2().getSpeed() / (duel.getB2().getStrength() + duel.getB1().getStability()) * (new Random().nextInt(Math.round(100 / (duel.isD1() ? duel.getB1().getStability() : 1)))) * 2));
                    message.getChannel().sendMessage("Em uma manobra espetacular, " + duel.getB2().getName() + " acerta um golpe especial e causa o dobro do dano comum!! (" + chance + ")").queue();
                } else
                    message.getChannel().sendMessage("Você errou o especial e perdeu o turno! (" + chance + " < " + (71 - Math.round(duel.getB1().getSpeed())) + ")").queue();
            }
        } else if (message.getMessage().getContentRaw().equalsIgnoreCase("desistir")) {
            if (message.getAuthor() == duel.getP1()) {
                message.getChannel().sendMessage(duel.getP1().getAsMention() + " desistiu. A vitória é de " + duel.getP2().getAsMention() + "!").queue();
                Beyblade bl = Database.getBeyblade(duel.getP1().getId());
                assert bl != null;
                bl.addLoses();
                Database.sendBeyblade(bl);

                Beyblade bb = Database.getBeyblade(duel.getP2().getId());
                assert bb != null;
                bb.addWins();
                Database.sendBeyblade(bb);
                accDuels.removeIf(d -> d.getP1() == message.getAuthor() || d.getP2() == message.getAuthor());
            } else {
                message.getChannel().sendMessage(duel.getP2().getAsMention() + " desistiu. A vitória é de " + duel.getP1().getAsMention() + "!").queue();
                Beyblade bl = Database.getBeyblade(duel.getP2().getId());
                assert bl != null;
                bl.addLoses();
                Database.sendBeyblade(bl);

                Beyblade bb = Database.getBeyblade(duel.getP1().getId());
                assert bb != null;
                bb.addWins();
                Database.sendBeyblade(bb);
                accDuels.removeIf(d -> d.getP1() == message.getAuthor() || d.getP2() == message.getAuthor());
            }
        }
        if (duel.getB2().getLife() <= 0) {
            int pointWin = new Random().nextInt(Math.round(duel.getB2().getStrength() + duel.getB2().getSpeed() + duel.getB2().getStability() + Math.round(duel.getB2().getWins() / duel.getB2().getLoses())));
            message.getChannel().sendMessage(duel.getP1().getAsMention() + " triunfou sobre " + duel.getP2().getAsMention() + ". Temos um vencedor!\n\n" + duel.getP1().getAsMention() + " ganhou **" + pointWin + "** pontos de combate!").queue();
            Beyblade bl = Database.getBeyblade(duel.getP2().getId());
            assert bl != null;
            bl.addLoses();
            Database.sendBeyblade(bl);

            Beyblade bb = Database.getBeyblade(duel.getP1().getId());
            assert bb != null;
            bb.addWins();
            bb.addPoints(pointWin);
            Database.sendBeyblade(bb);
            accDuels.removeIf(d -> d.getP1() == message.getAuthor() || d.getP2() == message.getAuthor());
        } else if (duel.getB1().getLife() <= 0) {
            int pointWin = new Random().nextInt(Math.round(duel.getB1().getStrength() + duel.getB1().getSpeed() + duel.getB1().getStability() + Math.round(duel.getB1().getWins() / duel.getB1().getLoses())));
            message.getChannel().sendMessage(duel.getP2().getAsMention() + " triunfou sobre " + duel.getP1().getAsMention() + ". Temos um vencedor!\n\n" + duel.getP2().getAsMention() + " ganhou **" + pointWin + "** pontos de combate!").queue();
            Beyblade bl = Database.getBeyblade(duel.getP1().getId());
            assert bl != null;
            bl.addLoses();
            Database.sendBeyblade(bl);

            Beyblade bb = Database.getBeyblade(duel.getP2().getId());
            assert bb != null;
            bb.addWins();
            bb.addPoints(pointWin);
            Database.sendBeyblade(bb);
            accDuels.removeIf(d -> d.getP1() == message.getAuthor() || d.getP2() == message.getAuthor());
        } else if (message.getMessage().getContentRaw().equals("atacar") || message.getMessage().getContentRaw().equals("especial") || message.getMessage().getContentRaw().equals("defender")) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Vez de " + (player1Turn ? duel.getB1().getName() : duel.getB2().getName()));
            eb.setDescription(duel.getB1().getName() + " *VS* " + duel.getB2().getName());
            eb.addField(duel.getB1().getName(), "Vida: " + duel.getB1().getLife(), true);
            eb.addField(duel.getB2().getName(), "Vida: " + duel.getB2().getLife(), true);
            message.getChannel().sendMessage(eb.build()).queue();
            accDuels.stream().filter(d -> d.getP1() == message.getAuthor() || d.getP2() == message.getAuthor()).findFirst().ifPresent(m -> {
                m.getB1().setLife(duel.getB1().getLife());
                m.getB2().setLife(duel.getB2().getLife());
            });
        }
    }
}
