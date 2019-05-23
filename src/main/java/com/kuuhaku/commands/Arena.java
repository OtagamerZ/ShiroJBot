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
                message.getChannel().sendMessage("Você precisa dar um nome a sua Beyblade!").queue();
            }
        } else {
            message.getChannel().sendMessage("Você já possui uma Beyblade!").queue();
        }
    }

    public static void setCor(MessageReceivedEvent message, String[] cmd) {
        Beyblade bb = Database.getBeyblade(message.getAuthor().getId());
        if (bb == null) message.getChannel().sendMessage("Você não possui uma Beyblade!").queue();
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
        if (bb == null) message.getChannel().sendMessage("Você não possui uma Beyblade!").queue();
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

    public static void chooseHouse(MessageReceivedEvent message, String[] cmd, Beyblade bb) {
        if (bb != null) {
            if (bb.getPoints() < (bb.getS() == null ? 150 : 300)) {
                switch (cmd[1]) {
                    case "tigre":
                        bb.setSpecial(10 + new Random().nextInt(2));
                        bb.takePoints((bb.getS() == null ? 150 : 300));
                    case "dragão":
                        bb.setSpecial(20 + new Random().nextInt(2));
                        bb.takePoints((bb.getS() == null ? 150 : 300));
                    case "urso":
                        bb.setSpecial(30 + new Random().nextInt(2));
                        bb.takePoints((bb.getS() == null ? 150 : 300));
                }
                message.getChannel().sendMessage("Seu alinhamento foi trocado para **" + bb.getS().getType() + "**, e o especial concedido a você foi: " + bb.getS().getName()).queue();
            } else {
                message.getChannel().sendMessage("Você não possui pontos de combate suficiente!").queue();
            }
        } else {
            message.getChannel().sendMessage("Você não possui uma Beyblade!").queue();
        }
    }

    public static void upgrade(MessageReceivedEvent message, String[] cmd) {
        Beyblade bb = Database.getBeyblade(message.getAuthor().getId());
        switch (cmd[1]) {
            case "nome":
                if (bb == null) message.getChannel().sendMessage("Você não possui uma Beyblade!").queue();
                else if (bb.getPoints() < 50)
                    message.getChannel().sendMessage("Você não possui pontos de combate suficiente!").queue();
                else if (cmd.length > 2) {
                    bb.takePoints(50);
                    bb.setName(message.getMessage().getContentRaw().replace(cmd[0] + cmd[1], "").trim());
                    message.getChannel().sendMessage("Nome trocado com sucesso!").queue();
                }
                break;
            case "força":
                if (bb == null) message.getChannel().sendMessage("Você não possui uma Beyblade!").queue();
                else if (bb.getPoints() < Math.round(15 * bb.getStrength() + bb.getStrength() + bb.getSpeed() + bb.getStability()))
                    message.getChannel().sendMessage("Você não possui pontos de combate suficiente!").queue();
                else {
                    bb.takePoints(Math.round(15 * bb.getStrength() + bb.getStrength() + bb.getSpeed() + bb.getStability()));
                    bb.addStrength();
                    message.getChannel().sendMessage("Poder aumentado com sucesso!").queue();
                }
                break;
            case "velocidade":
                if (bb == null) message.getChannel().sendMessage("Você não possui uma Beyblade!").queue();
                else if (bb.getPoints() < Math.round(15 * bb.getSpeed() + bb.getStrength() + bb.getSpeed() + bb.getStability()))
                    message.getChannel().sendMessage("Você não possui pontos de combate suficiente!").queue();
                else {
                    bb.takePoints(Math.round(15 * bb.getSpeed() + bb.getStrength() + bb.getSpeed() + bb.getStability()));
                    bb.addSpeed();
                    message.getChannel().sendMessage("Velocidade aumentada com sucesso!").queue();
                }
                break;
            case "estabilidade":
                if (bb == null) message.getChannel().sendMessage("Você não possui uma Beyblade!").queue();
                else if (bb.getPoints() < Math.round(15 * bb.getStability() + bb.getStrength() + bb.getSpeed() + bb.getStability()))
                    message.getChannel().sendMessage("Você não possui pontos de combate suficiente!").queue();
                else {
                    bb.takePoints(Math.round(15 * bb.getStability() + bb.getStrength() + bb.getSpeed() + bb.getStability()));
                    bb.addStability();
                    message.getChannel().sendMessage("Estabilidade aumentada com sucesso!").queue();
                }
                break;
            case "vida":
                if (bb == null) message.getChannel().sendMessage("Você não possui uma Beyblade!").queue();
                else if (bb.getPoints() < Math.round(bb.getLife() / 2))
                    message.getChannel().sendMessage("Você não possui pontos de combate suficiente!").queue();
                else {
                    bb.takePoints(Math.round(bb.getLife() / 2));
                    bb.addLife();
                    message.getChannel().sendMessage("Vida aumentada com sucesso!").queue();
                }
                break;
            default:
                message.getChannel().sendMessage("O atributo especificado não é válido!").queue();
        }
    }

    public static void duel(MessageReceivedEvent message, Map<Long, DuelData> duels) {
        if (message.getMessage().getMentionedUsers().size() > 0) {
            if (Database.getBeyblade(message.getMessage().getMentionedUsers().get(0).getId()) != null) {
                DuelData dd = new DuelData(message.getAuthor(), message.getMessage().getMentionedUsers().get(0));
                if (duels.containsValue(dd)) message.getChannel().sendMessage("Você já possui um duelo pendente!").queue();
                else message.getChannel().sendMessage(message.getMessage().getMentionedMembers().get(0).getAsMention() + ", você foi desafiado a um duelo de beyblades por " + message.getAuthor().getAsMention() + ". Se deseja aceitar, clique no botão abaixo:").queue(m -> {
                            m.addReaction("\u21aa").queue();
                            duels.put(m.getIdLong(), dd);
                        }
                );
            } else {
                message.getChannel().sendMessage("Este usuário ainda não possui uma Beyblade.").queue();
            }
        }
    }

    public static void fakeDuel(MessageReceivedEvent message, Map<Long, DuelData> duels) {
        if (message.getMessage().getMentionedUsers().size() > 0) {
            if (Database.getBeyblade(message.getMessage().getMentionedUsers().get(0).getId()) != null) {
                DuelData dd = new DuelData(message.getAuthor(), message.getMessage().getMentionedUsers().get(0));
                if (duels.containsValue(dd)) message.getChannel().sendMessage("Você já possui um duelo pendente!").queue();
                else message.getChannel().sendMessage(message.getMessage().getMentionedMembers().get(0).getAsMention() + ", você foi desafiado a um duelo de beyblades por " + message.getAuthor().getAsMention() + ". Se deseja aceitar, clique no botão abaixo:").queue(m -> {
                            m.addReaction("\u21aa").queue();
                            duels.put(m.getIdLong(), dd);
                        }
                );
            } else {
                message.getChannel().sendMessage("Este usuário ainda não possui uma Beyblade.").queue();
            }
        }
    }

    public static void battle(List<DuelData> accDuels, MessageReceivedEvent message) {
        @SuppressWarnings("OptionalGetWithoutIsPresent") DuelData duel = accDuels.stream().filter(d -> d.getP1() == message.getAuthor() || d.getP2() == message.getAuthor()).findFirst().get();
        boolean player1Turn = duel.isP1turn();
        EmbedBuilder eb = new EmbedBuilder();

        if (message.getMessage().getContentRaw().equalsIgnoreCase("atacar")) {
            if (player1Turn && message.getAuthor() == duel.getP1()) {
                duel.setP1turn(false);
                duel.setD1(false);
                if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
                int damage = Math.round(duel.getB1().getStrength() * duel.getB1().getSpeed() / (duel.getB2().getStability() * getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 50);
                duel.getB2().setLife(duel.getB2().getLife() - damage);
                System.out.println(damage + " -> " + duel.getB2().getLife());
                message.getChannel().sendMessage(duel.getB1().getName() + " ataca, agora é a vez de " + duel.getB2().getName()).queue();
            } else if (!player1Turn && message.getAuthor() == duel.getP2()) {
                duel.setP1turn(true);
                duel.setD2(false);
                if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
                int damage = Math.round(duel.getB2().getStrength() * duel.getB2().getSpeed() / (duel.getB1().getStability() * getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 50);
                duel.getB1().setLife(duel.getB1().getLife() - damage);
                System.out.println(damage + " -> " + duel.getB1().getLife());
                message.getChannel().sendMessage(duel.getB2().getName() + " ataca, agora é a vez de " + duel.getB1().getName()).queue();
            }
        } else if (message.getMessage().getContentRaw().equalsIgnoreCase("defender")) {
            if (player1Turn && message.getAuthor() == duel.getP1()) {
                duel.setP1turn(false);
                duel.setD1(true);
                message.getChannel().sendMessage(duel.getB1().getName() + " assumiu uma postura defensiva, é a vez de " + duel.getB2().getName()).queue();
            } else if (!player1Turn && message.getAuthor() == duel.getP2()) {
                duel.setP1turn(true);
                duel.setD2(true);
                message.getChannel().sendMessage(duel.getB2().getName() + " assumiu uma postura defensiva, é a vez de " + duel.getB1().getName()).queue();
            }
        } else if (message.getMessage().getContentRaw().equalsIgnoreCase("especial")) {
            if (duel.getB1().getS() != null) {
                if (!duel.isS1()) {
                    if (player1Turn && message.getAuthor() == duel.getP1()) {
                        duel.setP1turn(false);
                        int chance = new Random().nextInt(100);
                        duel.setD1(false);
                        switch (duel.getB1().getSpecial()) {
                            case 11:
                                if (chance > duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) {
                                    duel.getB2().setLife(duel.getB2().getLife() - Math.round(duel.getB1().getStrength() * duel.getB1().getSpeed() / (duel.getB2().getStability() * getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 50));
                                    message.getChannel().sendMessage("O-O que?? " + duel.getB1().getName() + " desapareceu? Ah, lá está ela, com um movimento digno dos tigres ela executa o golpe especial " + duel.getB1().getS().getName() + "! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue();
                                    duel.setS1(true);
                                    if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
                                } else {
                                    message.getChannel().sendMessage("Quase! " + duel.getB1().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) + ")").queue();
                                }
                                break;
                            case 12:
                                if (chance > duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) {
                                    duel.getB2().setLife(duel.getB2().getLife() - Math.round(duel.getB1().getSpeed() * 2 / (duel.getB2().getStability() * getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 50));
                                    message.getChannel().sendMessage("Isso foi incrível!! " + duel.getB1().getName() + " executou com perfeição em " + duel.getB1().getS().getName() + " um dos golpes mais difíceis já conhecidos! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue();
                                    duel.setP1turn(true);
                                    message.getChannel().sendMessage(duel.getB2().getName() + " está atordoada, será que teremos uma reviravolta aqui?").queue();
                                    duel.setS1(true);
                                    if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
                                } else {
                                    message.getChannel().sendMessage("Quase! " + duel.getB1().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) + ")").queue();
                                }
                                break;
                            case 21:
                                if (chance > duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) {
                                    duel.getB2().setLife(duel.getB2().getLife() - Math.round(duel.getB1().getStrength() * duel.getB2().getStability() / (duel.getB2().getStability() * getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 50));
                                    message.getChannel().sendMessage("O que foi isso!? " + duel.getB1().getName() + " lançou " + duel.getB1().getS().getName() + " ao ar, depois o lançou utilizando sua própria defesa! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue();
                                    duel.setS1(true);
                                    if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
                                } else {
                                    message.getChannel().sendMessage("Quase! " + duel.getB1().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) + ")").queue();
                                }
                                break;
                            case 22:
                                if (chance > duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) {
                                    duel.getB2().setLife(duel.getB2().getLife() - Math.round(duel.getB1().getStrength() * duel.getB1().getStrength() / (duel.getB2().getStability() * getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 150));
                                    message.getChannel().sendMessage("Não é possível!! Eu jamais acreditaria se alguém me dissesse que era possível executar este golpe, mas " + duel.getB1().getName() + " provou que é possivel!!! " + duel.getB1().getS().getName() + " mal consegue se manter em pé! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue();
                                    duel.setS1(true);
                                    if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
                                } else {
                                    message.getChannel().sendMessage("Quase! " + duel.getB1().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) + ")").queue();
                                }
                                break;
                            case 31:
                                if (chance > duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) {
                                    duel.getB1().getS().setBear(true);
                                    duel.setD1(true);
                                    message.getChannel().sendMessage("Mais alguém está sentindo isso? " + duel.getB1().getName() + " acaba de executar a assinatura dos ursos!! Essa aura poderá virar o fluxo da partida! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue();
                                    duel.setS1(true);
                                } else {
                                    message.getChannel().sendMessage("Quase! " + duel.getB1().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) + ")").queue();
                                }
                                break;
                        }
                    } else {
                        message.getChannel().sendMessage("O poder mágico de " + duel.getB1().getName() + " já está no limite, não me parece que conseguirá realizar mais um golpe especial!").queue();
                    }
                } else {
                    message.getChannel().sendMessage(duel.getB1().getName() + " ainda não possui um alinhamento!").queue();
                }
            } else if (!player1Turn && message.getAuthor() == duel.getP2()) {
                if (duel.getB2().getS() != null) {
                    if (!duel.isS2()) {
                        duel.setP1turn(true);
                        int chance = new Random().nextInt(100);
                        duel.setD2(false);
                        switch (duel.getB2().getSpecial()) {
                            case 11:
                                if (chance > duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) {
                                    duel.getB1().setLife(duel.getB1().getLife() - Math.round(duel.getB2().getStrength() * duel.getB2().getSpeed() / (duel.getB1().getStability() * getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 50));
                                    message.getChannel().sendMessage("O-O que?? " + duel.getB2().getName() + " desapareceu? Ah, lá está ele, com um movimento digno dos tigres ele executa o golpe especial " + duel.getB2().getS().getName() + "! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue();
                                    duel.setS2(true);
                                    if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
                                } else {
                                    message.getChannel().sendMessage("Quase! " + duel.getB2().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) + ")").queue();
                                }
                                break;
                            case 12:
                                if (chance > duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) {
                                    duel.getB1().setLife(duel.getB1().getLife() - Math.round(duel.getB2().getSpeed() * 2 / (duel.getB1().getStability() * getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 50));
                                    message.getChannel().sendMessage("Isso foi incrível!! " + duel.getB2().getName() + " executou com perfeição em " + duel.getB2().getS().getName() + " um dos golpes mais difíceis já conhecidos! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue();
                                    duel.setP1turn(false);
                                    message.getChannel().sendMessage(duel.getB1().getName() + " está atordoada, será que teremos uma reviravolta aqui?").queue();
                                    duel.setS2(true);
                                    if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
                                } else {
                                    message.getChannel().sendMessage("Quase! " + duel.getB2().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) + ")").queue();
                                }
                                break;
                            case 21:
                                if (chance > duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) {
                                    duel.getB1().setLife(duel.getB1().getLife() - Math.round(duel.getB2().getStrength() * duel.getB1().getStability() / (duel.getB1().getStability() * getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 50));
                                    message.getChannel().sendMessage("O que foi isso!? " + duel.getB2().getName() + " lançou " + duel.getB2().getS().getName() + " ao ar, depois o lançou utilizando sua própria defesa! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue();
                                    duel.setS2(true);
                                    if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
                                } else {
                                    message.getChannel().sendMessage("Quase! " + duel.getB2().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) + ")").queue();
                                }
                                break;
                            case 22:
                                if (chance > duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) {
                                    duel.getB1().setLife(duel.getB1().getLife() - Math.round(duel.getB2().getStrength() * duel.getB2().getStrength() / (duel.getB1().getStability() * getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 150));
                                    message.getChannel().sendMessage("Não é possível!! Eu jamais acreditaria se alguém me dissesse que era possível executar este golpe, mas " + duel.getB2().getName() + " provou que é possivel!!! " + duel.getB2().getS().getName() + " mal consegue se manter em pé! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue();
                                    duel.setS2(true);
                                    if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
                                } else {
                                    message.getChannel().sendMessage("Quase! " + duel.getB2().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) + ")").queue();
                                }
                                break;
                            case 31:
                                if (chance > duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) {
                                    duel.getB2().getS().setBear(true);
                                    duel.setD2(true);
                                    message.getChannel().sendMessage("Mais alguém está sentindo isso? " + duel.getB2().getName() + " acaba de executar a assinatura dos ursos!! Essa aura poderá virar o fluxo da partida! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue();
                                    duel.setS2(true);
                                } else {
                                    message.getChannel().sendMessage("Quase! " + duel.getB2().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) + ")").queue();
                                }
                                break;
                        }
                    } else {
                        message.getChannel().sendMessage("O poder mágico de " + duel.getB2().getName() + " já está no limite, não me parece que conseguirá realizar mais um golpe especial!").queue();
                    }
                } else {
                    message.getChannel().sendMessage(duel.getB2().getName() + " ainda não possui um alinhamento!").queue();
                }
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
            int pointWin = new Random().nextInt(Math.round(duel.getB2().getStrength() + duel.getB2().getSpeed() + duel.getB2().getStability() + (float) duel.getB1().getWins() / (duel.getB1().getLoses() == 0 ? 1 : duel.getB1().getLoses())));
            message.getChannel().sendMessage(duel.getP1().getAsMention() + " triunfou sobre " + duel.getP2().getAsMention() + ". Temos um vencedor!\n\n" + duel.getB1().getName() + " ganhou **" + pointWin + "** pontos de combate!").queue();
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
            int pointWin = new Random().nextInt(Math.round(duel.getB1().getStrength() + duel.getB1().getSpeed() + duel.getB1().getStability() + (float) duel.getB1().getWins() / (duel.getB1().getLoses() == 0 ? 1 : duel.getB1().getLoses())));
            message.getChannel().sendMessage(duel.getP2().getAsMention() + " triunfou sobre " + duel.getP1().getAsMention() + ". Temos um vencedor!\n\n" + duel.getB2().getName() + " ganhou **" + pointWin + "** pontos de combate!").queue();
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
        } else if (message.getMessage().getContentRaw().equalsIgnoreCase("atacar") || message.getMessage().getContentRaw().equalsIgnoreCase("especial") || message.getMessage().getContentRaw().equalsIgnoreCase("defender")) {
            eb.setTitle("Dados do duelo:");
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

    public static void train(List<DuelData> accDuels, MessageReceivedEvent message) {
        @SuppressWarnings("OptionalGetWithoutIsPresent") DuelData duel = accDuels.stream().filter(d -> d.getP1() == message.getAuthor() || d.getP2() == message.getAuthor()).findFirst().get();
        boolean player1Turn = duel.isP1turn();
        EmbedBuilder eb = new EmbedBuilder();

        if (message.getMessage().getContentRaw().equalsIgnoreCase("atacar")) {
            if (player1Turn && message.getAuthor() == duel.getP1()) {
                duel.setP1turn(false);
                duel.setD1(false);
                if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
                int damage = Math.round(duel.getB1().getStrength() * duel.getB1().getSpeed() / (duel.getB2().getStability() * getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 50);
                duel.getB2().setLife(duel.getB2().getLife() - damage);
                System.out.println(damage + " -> " + duel.getB2().getLife());
                message.getChannel().sendMessage(duel.getB1().getName() + " ataca, agora é a vez de " + duel.getB2().getName()).queue();
            } else if (!player1Turn && message.getAuthor() == duel.getP2()) {
                duel.setP1turn(true);
                duel.setD2(false);
                if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
                int damage = Math.round(duel.getB2().getStrength() * duel.getB2().getSpeed() / (duel.getB1().getStability() * getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 50);
                duel.getB1().setLife(duel.getB1().getLife() - damage);
                System.out.println(damage + " -> " + duel.getB1().getLife());
                message.getChannel().sendMessage(duel.getB2().getName() + " ataca, agora é a vez de " + duel.getB1().getName()).queue();
            }
        } else if (message.getMessage().getContentRaw().equalsIgnoreCase("defender")) {
            if (player1Turn && message.getAuthor() == duel.getP1()) {
                duel.setP1turn(false);
                duel.setD1(true);
                message.getChannel().sendMessage(duel.getB1().getName() + " assumiu uma postura defensiva, é a vez de " + duel.getB2().getName()).queue();
            } else if (!player1Turn && message.getAuthor() == duel.getP2()) {
                duel.setP1turn(true);
                duel.setD2(true);
                message.getChannel().sendMessage(duel.getB2().getName() + " assumiu uma postura defensiva, é a vez de " + duel.getB1().getName()).queue();
            }
        } else if (message.getMessage().getContentRaw().equalsIgnoreCase("especial")) {
            if (duel.getB1().getS() != null) {
                if (!duel.isS1()) {
                    if (player1Turn && message.getAuthor() == duel.getP1()) {
                        duel.setP1turn(false);
                        int chance = new Random().nextInt(100);
                        duel.setD1(false);
                        switch (duel.getB1().getSpecial()) {
                            case 11:
                                if (chance > duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) {
                                    duel.getB2().setLife(duel.getB2().getLife() - Math.round(duel.getB1().getStrength() * duel.getB1().getSpeed() / (duel.getB2().getStability() * getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 50));
                                    message.getChannel().sendMessage("O-O que?? " + duel.getB1().getName() + " desapareceu? Ah, lá está ela, com um movimento digno dos tigres ela executa o golpe especial " + duel.getB1().getS().getName() + "! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue();
                                    duel.setS1(true);
                                    if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
                                } else {
                                    message.getChannel().sendMessage("Quase! " + duel.getB1().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) + ")").queue();
                                }
                                break;
                            case 12:
                                if (chance > duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) {
                                    duel.getB2().setLife(duel.getB2().getLife() - Math.round(duel.getB1().getSpeed() * 2 / (duel.getB2().getStability() * getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 50));
                                    message.getChannel().sendMessage("Isso foi incrível!! " + duel.getB1().getName() + " executou com perfeição em " + duel.getB1().getS().getName() + " um dos golpes mais difíceis já conhecidos! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue();
                                    duel.setP1turn(true);
                                    message.getChannel().sendMessage(duel.getB2().getName() + " está atordoada, será que teremos uma reviravolta aqui?").queue();
                                    duel.setS1(true);
                                    if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
                                } else {
                                    message.getChannel().sendMessage("Quase! " + duel.getB1().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) + ")").queue();
                                }
                                break;
                            case 21:
                                if (chance > duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) {
                                    duel.getB2().setLife(duel.getB2().getLife() - Math.round(duel.getB1().getStrength() * duel.getB2().getStability() / (duel.getB2().getStability() * getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 50));
                                    message.getChannel().sendMessage("O que foi isso!? " + duel.getB1().getName() + " lançou " + duel.getB1().getS().getName() + " ao ar, depois o lançou utilizando sua própria defesa! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue();
                                    duel.setS1(true);
                                    if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
                                } else {
                                    message.getChannel().sendMessage("Quase! " + duel.getB1().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) + ")").queue();
                                }
                                break;
                            case 22:
                                if (chance > duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) {
                                    duel.getB2().setLife(duel.getB2().getLife() - Math.round(duel.getB1().getStrength() * duel.getB1().getStrength() / (duel.getB2().getStability() * getDefFac(duel.isD2(), duel.getB2())) * (float) Math.random() * 150));
                                    message.getChannel().sendMessage("Não é possível!! Eu jamais acreditaria se alguém me dissesse que era possível executar este golpe, mas " + duel.getB1().getName() + " provou que é possivel!!! " + duel.getB1().getS().getName() + " mal consegue se manter em pé! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue();
                                    duel.setS1(true);
                                    if (duel.getB2().getS() != null) duel.getB2().getS().setBear(false);
                                } else {
                                    message.getChannel().sendMessage("Quase! " + duel.getB1().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) + ")").queue();
                                }
                                break;
                            case 31:
                                if (chance > duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) {
                                    duel.getB1().getS().setBear(true);
                                    duel.setD1(true);
                                    message.getChannel().sendMessage("Mais alguém está sentindo isso? " + duel.getB1().getName() + " acaba de executar a assinatura dos ursos!! Essa aura poderá virar o fluxo da partida! (" + chance + " > " + duel.getB1().getS().getDiff() + ")").queue();
                                    duel.setS1(true);
                                } else {
                                    message.getChannel().sendMessage("Quase! " + duel.getB1().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB1().getS().getDiff() - duel.getB1().getSpeed()) + ")").queue();
                                }
                                break;
                        }
                    } else {
                        message.getChannel().sendMessage("O poder mágico de " + duel.getB1().getName() + " já está no limite, não me parece que conseguirá realizar mais um golpe especial!").queue();
                    }
                } else {
                    message.getChannel().sendMessage(duel.getB1().getName() + " ainda não possui um alinhamento!").queue();
                }
            } else if (!player1Turn && message.getAuthor() == duel.getP2()) {
                if (duel.getB2().getS() != null) {
                    if (!duel.isS2()) {
                        duel.setP1turn(true);
                        int chance = new Random().nextInt(100);
                        duel.setD2(false);
                        switch (duel.getB2().getSpecial()) {
                            case 11:
                                if (chance > duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) {
                                    duel.getB1().setLife(duel.getB1().getLife() - Math.round(duel.getB2().getStrength() * duel.getB2().getSpeed() / (duel.getB1().getStability() * getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 50));
                                    message.getChannel().sendMessage("O-O que?? " + duel.getB2().getName() + " desapareceu? Ah, lá está ele, com um movimento digno dos tigres ele executa o golpe especial " + duel.getB2().getS().getName() + "! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue();
                                    duel.setS2(true);
                                    if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
                                } else {
                                    message.getChannel().sendMessage("Quase! " + duel.getB2().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) + ")").queue();
                                }
                                break;
                            case 12:
                                if (chance > duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) {
                                    duel.getB1().setLife(duel.getB1().getLife() - Math.round(duel.getB2().getSpeed() * 2 / (duel.getB1().getStability() * getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 50));
                                    message.getChannel().sendMessage("Isso foi incrível!! " + duel.getB2().getName() + " executou com perfeição em " + duel.getB2().getS().getName() + " um dos golpes mais difíceis já conhecidos! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue();
                                    duel.setP1turn(false);
                                    message.getChannel().sendMessage(duel.getB1().getName() + " está atordoada, será que teremos uma reviravolta aqui?").queue();
                                    duel.setS2(true);
                                    if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
                                } else {
                                    message.getChannel().sendMessage("Quase! " + duel.getB2().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) + ")").queue();
                                }
                                break;
                            case 21:
                                if (chance > duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) {
                                    duel.getB1().setLife(duel.getB1().getLife() - Math.round(duel.getB2().getStrength() * duel.getB1().getStability() / (duel.getB1().getStability() * getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 50));
                                    message.getChannel().sendMessage("O que foi isso!? " + duel.getB2().getName() + " lançou " + duel.getB2().getS().getName() + " ao ar, depois o lançou utilizando sua própria defesa! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue();
                                    duel.setS2(true);
                                    if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
                                } else {
                                    message.getChannel().sendMessage("Quase! " + duel.getB2().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) + ")").queue();
                                }
                                break;
                            case 22:
                                if (chance > duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) {
                                    duel.getB1().setLife(duel.getB1().getLife() - Math.round(duel.getB2().getStrength() * duel.getB2().getStrength() / (duel.getB1().getStability() * getDefFac(duel.isD1(), duel.getB1())) * (float) Math.random() * 150));
                                    message.getChannel().sendMessage("Não é possível!! Eu jamais acreditaria se alguém me dissesse que era possível executar este golpe, mas " + duel.getB2().getName() + " provou que é possivel!!! " + duel.getB2().getS().getName() + " mal consegue se manter em pé! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue();
                                    duel.setS2(true);
                                    if (duel.getB1().getS() != null) duel.getB1().getS().setBear(false);
                                } else {
                                    message.getChannel().sendMessage("Quase! " + duel.getB2().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) + ")").queue();
                                }
                                break;
                            case 31:
                                if (chance > duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) {
                                    duel.getB2().getS().setBear(true);
                                    duel.setD2(true);
                                    message.getChannel().sendMessage("Mais alguém está sentindo isso? " + duel.getB2().getName() + " acaba de executar a assinatura dos ursos!! Essa aura poderá virar o fluxo da partida! (" + chance + " > " + duel.getB2().getS().getDiff() + ")").queue();
                                    duel.setS2(true);
                                } else {
                                    message.getChannel().sendMessage("Quase! " + duel.getB2().getName() + " tenta executar um golpe especial mas falha! (" + chance + " < " + (duel.getB2().getS().getDiff() - duel.getB2().getSpeed()) + ")").queue();
                                }
                                break;
                        }
                    } else {
                        message.getChannel().sendMessage("O poder mágico de " + duel.getB2().getName() + " já está no limite, não me parece que conseguirá realizar mais um golpe especial!").queue();
                    }
                } else {
                    message.getChannel().sendMessage(duel.getB2().getName() + " ainda não possui um alinhamento!").queue();
                }
            }
        } else if (message.getMessage().getContentRaw().equalsIgnoreCase("desistir")) {
            if (message.getAuthor() == duel.getP1()) {
                message.getChannel().sendMessage(duel.getP1().getAsMention() + " desistiu. A vitória é de " + duel.getP2().getAsMention() + "!").queue();
                Beyblade bl = Database.getBeyblade(duel.getP1().getId());
                assert bl != null;
                Database.sendBeyblade(bl);

                Beyblade bb = Database.getBeyblade(duel.getP2().getId());
                assert bb != null;
                Database.sendBeyblade(bb);
                accDuels.removeIf(d -> d.getP1() == message.getAuthor() || d.getP2() == message.getAuthor());
            } else {
                message.getChannel().sendMessage(duel.getP2().getAsMention() + " desistiu. A vitória é de " + duel.getP1().getAsMention() + "!").queue();
                Beyblade bl = Database.getBeyblade(duel.getP2().getId());
                assert bl != null;
                Database.sendBeyblade(bl);

                Beyblade bb = Database.getBeyblade(duel.getP1().getId());
                assert bb != null;
                Database.sendBeyblade(bb);
                accDuels.removeIf(d -> d.getP1() == message.getAuthor() || d.getP2() == message.getAuthor());
            }
        }
        if (duel.getB2().getLife() <= 0) {
            int pointWin = new Random().nextInt(Math.round(duel.getB2().getStrength() + duel.getB2().getSpeed() + duel.getB2().getStability() + (float) duel.getB1().getWins() / (duel.getB1().getLoses() == 0 ? 1 : duel.getB1().getLoses())));
            message.getChannel().sendMessage(duel.getP1().getAsMention() + " triunfou sobre " + duel.getP2().getAsMention() + ". Temos um vencedor!\n\n" + duel.getB1().getName() + " ganhou **" + pointWin + "** pontos de combate!").queue();
            Beyblade bl = Database.getBeyblade(duel.getP2().getId());
            assert bl != null;
            Database.sendBeyblade(bl);

            Beyblade bb = Database.getBeyblade(duel.getP1().getId());
            assert bb != null;
            bb.addPoints(Math.round(pointWin / 2));
            Database.sendBeyblade(bb);
            accDuels.removeIf(d -> d.getP1() == message.getAuthor() || d.getP2() == message.getAuthor());
        } else if (duel.getB1().getLife() <= 0) {
            int pointWin = new Random().nextInt(Math.round(duel.getB1().getStrength() + duel.getB1().getSpeed() + duel.getB1().getStability() + (float) duel.getB1().getWins() / (duel.getB1().getLoses() == 0 ? 1 : duel.getB1().getLoses())));
            message.getChannel().sendMessage(duel.getP2().getAsMention() + " triunfou sobre " + duel.getP1().getAsMention() + ". Temos um vencedor!\n\n" + duel.getB2().getName() + " ganhou **" + pointWin + "** pontos de combate!").queue();
            Beyblade bl = Database.getBeyblade(duel.getP1().getId());
            assert bl != null;
            Database.sendBeyblade(bl);

            Beyblade bb = Database.getBeyblade(duel.getP2().getId());
            assert bb != null;
            bb.addPoints(Math.round(pointWin / 2));
            Database.sendBeyblade(bb);
            accDuels.removeIf(d -> d.getP1() == message.getAuthor() || d.getP2() == message.getAuthor());
        } else if (message.getMessage().getContentRaw().equalsIgnoreCase("atacar") || message.getMessage().getContentRaw().equalsIgnoreCase("especial") || message.getMessage().getContentRaw().equalsIgnoreCase("defender")) {
            eb.setTitle("Dados do duelo:");
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

    private static float getDefFac(boolean defending, Beyblade b) {
        if (defending) {
            if (b.getS() == null) {
                return b.getStability();
            } else {
                if (b.getS().isBear()) {
                    return 2.0f + (b.getStability() / 2);
                } else {
                    return b.getStability();
                }
            }
        } else {
            return 1.0f;
        }
    }
}
