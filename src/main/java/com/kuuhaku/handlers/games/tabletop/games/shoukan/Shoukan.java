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

package com.kuuhaku.handlers.games.tabletop.games.shoukan;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.framework.Board;
import com.kuuhaku.handlers.games.tabletop.framework.Game;
import com.kuuhaku.handlers.games.tabletop.framework.enums.BoardSize;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.EffectTrigger;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Phase;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.enums.KawaiponRarity;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Shoukan extends Game {
    private final Map<Side, Hand> hands;
    private final TextChannel channel;
    private final Arena arena = new Arena();
    private final ListenerAdapter listener = new ListenerAdapter() {
        @Override
        public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
            if (canInteract(event)) play(event);
        }
    };
    private Message message = null;
    private Phase phase = Phase.PLAN;
    private final List<Champion> ultimates = CardDAO.getFusions();
    private final boolean[] changed = {false, false, false, false, false};
    private final boolean daily;
    private boolean draw = false;

    public Shoukan(JDA handler, TextChannel channel, int bet, JSONObject custom, boolean daily, User... players) {
        super(handler, new Board(BoardSize.S_NONE, bet, Arrays.stream(players).map(User::getId).toArray(String[]::new)), channel, custom);
        this.channel = channel;
        this.daily = daily;

        Kawaipon p1 = daily ? Helper.getDailyDeck() : KawaiponDAO.getKawaipon(players[0].getId());
        Kawaipon p2 = daily ? Helper.getDailyDeck() : KawaiponDAO.getKawaipon(players[1].getId());

        this.hands = Map.of(
                Side.TOP, new Hand(this, players[0], p1.getDrawables(), Side.TOP),
                Side.BOTTOM, new Hand(this, players[1], p2.getDrawables(), Side.BOTTOM)
        );

        setActions(
                s -> close(),
                s -> {
                    if (custom == null) getBoard().awardWinner(this, daily, getBoard().getPlayers().get(1).getId());
                }
        );
    }

    @Override
    public void start() {
        Hand h = getHandById(getCurrent().getId());
        h.addMana(h.getManaPerTurn());
        channel.sendMessage(getCurrent().getAsMention() + " você começa! (Olhe as mensagens privadas)")
                .addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg")
                .queue(s -> {
                    this.message = s;
                    getHandler().addEventListener(listener);
                    h.showHand();
                    Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
                });
    }

    @Override
    public boolean canInteract(GuildMessageReceivedEvent evt) {
        Predicate<GuildMessageReceivedEvent> condition = e -> e.getChannel().getId().equals(channel.getId());

        return condition
                .and(e -> e.getAuthor().getId().equals(getCurrent().getId()))
                .and(e -> StringUtils.isNumeric(e.getMessage().getContentRaw().split(",")[0]) || e.getMessage().getContentRaw().equalsIgnoreCase("reload"))
                .test(evt);
    }

    @Override
    public void play(GuildMessageReceivedEvent evt) {
        Message message = evt.getMessage();
        String cmd = message.getContentRaw();
        Hand h = getHandById(getCurrent().getId());

        if (cmd.equalsIgnoreCase("reload")) {
            channel.sendMessage(message.getAuthor().getAsMention() + " recriou a mensagem do jogo.")
                    .addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg")
                    .queue(s -> {
                        if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
                        this.message = s;
                        Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
                    });
            resetTimerKeepTurn();
            return;
        }

        String[] args = cmd.split(",");
        if (!StringUtils.isNumeric(args[0])) {
            channel.sendMessage("❌ | Índice inválido.").queue(null, Helper::doNothing);
            return;
        }
        int index = Integer.parseInt(args[0]) - 1;

        if (phase == Phase.PLAN) {
            try {
                List<SlotColumn<Drawable, Drawable>> slots = arena.getSlots().get(h.getSide());
                if (args.length == 1) {
                    if (index < 0 || index >= slots.size()) {
                        channel.sendMessage("❌ | Índice inválido.").queue(null, Helper::doNothing);
                        return;
                    }

                    Champion c = (Champion) slots.get(index).getTop();

                    if (c == null) {
                        channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue(null, Helper::doNothing);
                        return;
                    } else if (changed[index]) {
                        channel.sendMessage("❌ | Você já mudou a postura dessa carta neste turno.").queue(null, Helper::doNothing);
                        return;
                    }

                    c.setDefending(c.isFlipped() || !c.isDefending());

                    if (c.hasEffect() && !c.isFlipped()) {
                        c.getEffect(new EffectParameters(phase, EffectTrigger.ON_SWITCH, this, index, h.getSide(), Duelists.of(c, index, null, -1), channel));
                        postCombat();
                    }

                    MessageAction act;
                    if (c.isFlipped()) {
                        c.setFlipped(false);

                        act = channel.sendMessage("Carta virada para cima em modo de defesa.");
                    } else if (!c.isDefending())
                        act = channel.sendMessage("Carta trocada para modo de ataque.");
                    else
                        act = channel.sendMessage("Carta trocada para modo de defesa.");

                    act.addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg")
                            .queue(s -> {
                                if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
                                this.message = s;
                                Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
                            });
                    changed[index] = true;
                    resetTimerKeepTurn();
                    return;
                }

                Drawable d = h.getCards().get(index);

                if (!d.isAvailable()) {
                    channel.sendMessage("❌ | Essa carta já foi jogada neste turno.").queue(null, Helper::doNothing);
                    return;
                }

                if (d instanceof Equipment) {
                    if (args.length < 3) {
                        channel.sendMessage("❌ | O terceiro argumento deve ser o número da casa da carta à equipar este equipamento.").queue(null, Helper::doNothing);
                        return;
                    }

                    if (!StringUtils.isNumeric(args[1])) {
                        channel.sendMessage("❌ | Índice inválido, escolha uma casa para colocar essa carta.").queue(null, Helper::doNothing);
                        return;
                    }

                    int dest = Integer.parseInt(args[1]) - 1;
                    SlotColumn<Drawable, Drawable> slot = slots.get(dest);

                    if (slot.getBottom() != null) {
                        channel.sendMessage("❌ | Já existe uma carta nessa casa.").queue(null, Helper::doNothing);
                        return;
                    }

                    if (!StringUtils.isNumeric(args[2])) {
                        channel.sendMessage("❌ | Índice inválido, escolha uma carta para equipar esse equipamento.").queue(null, Helper::doNothing);
                        return;
                    }
                    int toEquip = Integer.parseInt(args[2]) - 1;

                    SlotColumn<Drawable, Drawable> target = slots.get(toEquip);

                    if (target.getTop() == null) {
                        channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue(null, Helper::doNothing);
                        return;
                    }

                    Drawable tp = d.copy();
                    tp.setAcc(AccountDAO.getAccount(h.getUser().getId()));
                    slot.setBottom(tp);
                    Champion t = (Champion) target.getTop();
                    if (t.isFlipped()) {
                        t.setFlipped(false);
                        t.setDefending(true);
                    }
                    t.addLinkedTo((Equipment) tp);
                    ((Equipment) tp).setLinkedTo(Pair.of(toEquip, t));
                    if (t.hasEffect() && !t.isFlipped()) {
                        t.getEffect(new EffectParameters(phase, EffectTrigger.ON_EQUIP, this, dest, h.getSide(), Duelists.of(t, dest, null, -1), channel));
                        postCombat();
                    }
                } else if (d instanceof Champion) {
                    if (args.length < 3) {
                        channel.sendMessage("❌ | O terceiro argumento deve ser `A`, `D` ou `B` para definir se a carta será posicionada em modo de ataque, defesa ou virada para baixo.").queue(null, Helper::doNothing);
                        return;
                    } else if (h.getMana() < ((Champion) d).getMana()) {
                        channel.sendMessage("❌ | Você não tem mana suficiente para invocar essa carta, encerre o turno reagindo com :arrow_forward: ou jogue cartas de equipamento.").queue(null, Helper::doNothing);
                        return;
                    }

                    if (!StringUtils.isNumeric(args[1])) {
                        channel.sendMessage("❌ | Índice inválido, escolha uma casa para colocar essa carta.").queue(null, Helper::doNothing);
                        return;
                    }
                    int dest = Integer.parseInt(args[1]) - 1;

                    SlotColumn<Drawable, Drawable> slot = slots.get(dest);

                    if (slot.getTop() != null) {
                        channel.sendMessage("❌ | Já existe uma carta nessa casa.").queue(null, Helper::doNothing);
                        return;
                    }

                    Champion tp = (Champion) d.copy();

                    switch (args[2].toLowerCase()) {
                        case "a" -> tp.setFlipped(false);
                        case "d" -> {
                            tp.setFlipped(false);
                            tp.setDefending(true);
                        }
                        case "b" -> tp.setFlipped(true);
                        default -> {
                            channel.sendMessage("❌ | O terceiro argumento deve ser `A`, `D` ou `B` para definir se a carta será posicionada em modo de ataque, defesa ou virada para baixo.").queue(null, Helper::doNothing);
                            return;
                        }
                    }

                    tp.setAcc(AccountDAO.getAccount(h.getUser().getId()));
                    slot.setTop(tp);
                    if (tp.hasEffect() && !tp.isFlipped()) {
                        tp.getEffect(new EffectParameters(phase, EffectTrigger.ON_SUMMON, this, dest, h.getSide(), Duelists.of(tp, dest, null, -1), channel));
                        postCombat();
                    }
                } else {
                    if (!args[1].equalsIgnoreCase("f")) {
                        channel.sendMessage("❌ | O segundo argumento precisa ser `F` se deseja jogar uma carta de campo.").queue(null, Helper::doNothing);
                        return;
                    }

                    Field f = (Field) d.copy();
                    f.setAcc(AccountDAO.getAccount(h.getUser().getId()));
                    arena.setField(f);
                }

                d.setAvailable(false);
                if (d instanceof Champion)
                    h.removeMana(((Champion) d).getMana());

                List<Drawable> champsInField = arena.getSlots().get(h.getSide())
                        .stream()
                        .map(SlotColumn::getTop)
                        .collect(Collectors.toList());

                List<Drawable> equipsInField = arena.getSlots().get(h.getSide())
                        .stream()
                        .map(SlotColumn::getBottom)
                        .collect(Collectors.toList());

                List<String> allCards = new ArrayList<>() {{
                    addAll(Stream.of(champsInField, equipsInField)
                            .flatMap(List::stream)
                            .filter(Objects::nonNull)
                            .map(dr -> dr.getCard().getId())
                            .collect(Collectors.toList())
                    );
                }};

                Champion aFusion = ultimates
                        .stream()
                        .filter(f ->
                                f.getRequiredCards().size() > 0 &&
                                        allCards.containsAll(f.getRequiredCards()) &&
                                        h.getMana() >= f.getMana()
                        )
                        .findFirst()
                        .orElse(null);

                if (aFusion != null) {
                    List<SlotColumn<Drawable, Drawable>> slts = arena.getSlots().get(h.getSide());

                    for (String requiredCard : aFusion.getRequiredCards()) {
                        for (int i = 0; i < slts.size(); i++) {
                            SlotColumn<Drawable, Drawable> column = slts.get(i);
                            if (column.getTop() != null && column.getTop().getCard().getId().equals(requiredCard)) {
                                banishCard(h.getSide(), i, getArena().getSlots().get(h.getSide()));
                                break;
                            } else if (column.getBottom() != null && column.getBottom().getCard().getId().equals(requiredCard)) {
                                unequipCard(h.getSide(), i, getArena().getSlots().get(h.getSide()));
                                break;
                            }
                        }
                    }

                    for (SlotColumn<Drawable, Drawable> slt : slts) {
                        if (slt.getTop() == null) {
                            aFusion.setAcc(AccountDAO.getAccount(h.getUser().getId()));
                            slt.setTop(aFusion.copy());
                            if (aFusion.hasEffect() && !aFusion.isFlipped()) {
                                aFusion.getEffect(new EffectParameters(phase, EffectTrigger.ON_SUMMON, this, Integer.parseInt(args[1]) - 1, h.getSide(), Duelists.of(aFusion, Integer.parseInt(args[1]) - 1, null, -1), channel));
                                postCombat();
                            }
                            h.removeMana(aFusion.getMana());
                            break;
                        }
                    }
                }

                channel.sendFile(Helper.getBytes(arena.render(hands), "jpg"), "board.jpg")
                        .queue(s -> {
                            if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
                            this.message = s;
                            Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
                            h.showHand();
                        });
                resetTimerKeepTurn();
            } catch (IndexOutOfBoundsException e) {
                channel.sendMessage("❌ | Índice inválido, verifique a mensagem enviada por mim no privado para ver as cartas na sua mão.").queue(null, Helper::doNothing);
            } catch (NumberFormatException e) {
                channel.sendMessage("❌ | Índice inválido, o primeiro argumento deve ser um valor inteiro que represente uma carta na sua mão e o segundo deve ser uma casa vazia no tabuleiro.").queue(null, Helper::doNothing);
            }
        } else {
            try {
                if (args.length > 1 && !StringUtils.isNumeric(args[1])) {
                    channel.sendMessage("❌ | Índice inválido, escolha uma carta para ser atacada.").queue(null, Helper::doNothing);
                    return;
                }

                int[] is = {index, args.length == 1 ? 0 : Integer.parseInt(args[1]) - 1};

                List<SlotColumn<Drawable, Drawable>> yourSide = arena.getSlots().get(h.getSide());
                List<SlotColumn<Drawable, Drawable>> hisSide = arena.getSlots().get(h.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP);

                if (args.length == 1) {
                    if (is[0] < 0 || is[0] >= yourSide.size()) {
                        channel.sendMessage("❌ | Índice inválido.").queue(null, Helper::doNothing);
                        return;
                    }

                    Champion c = (Champion) yourSide.get(is[0]).getTop();

                    if (c == null) {
                        channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue(null, Helper::doNothing);
                        return;
                    } else if (hisSide.stream().anyMatch(s -> s.getTop() != null)) {
                        channel.sendMessage("❌ | Ainda existem campeões no campo inimigo.").queue(null, Helper::doNothing);
                        return;
                    } else if (!c.isAvailable()) {
                        channel.sendMessage("❌ | Essa carta já atacou neste turno.").queue(null, Helper::doNothing);
                        return;
                    } else if (c.isFlipped()) {
                        channel.sendMessage("❌ | Você não pode atacar com cartas viradas para baixo.").queue(null, Helper::doNothing);
                        return;
                    } else if (c.isDefending()) {
                        channel.sendMessage("❌ | Você não pode atacar com cartas em modo de defesa.").queue(null, Helper::doNothing);
                        return;
                    }

                    Hand enemy = getHandById(getBoard().getPlayers().get(1).getId());

                    int yPower = Math.round(
                            c.getAtk() +
                                    c.getLinkedTo().stream().mapToInt(Equipment::getAtk).sum() *
                                            (arena.getField() == null ? 1 : arena.getField().getModifiers().optFloat(c.getRace().name(), 1f))
                    );

                    enemy.removeHp(yPower);
                    c.setAvailable(false);

                    if (!postCombat()) {
                        channel.sendMessage("Você atacou diretamente o inimigo.")
                                .addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg")
                                .queue(s -> {
                                    if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
                                    this.message = s;
                                    Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
                                });
                        resetTimerKeepTurn();
                    }
                    return;
                }

                Champion yours = (Champion) yourSide.get(is[0]).getTop();
                Champion his = (Champion) hisSide.get(is[1]).getTop();

                if (yours == null || his == null) {
                    channel.sendMessage("❌ | Não existe uma carta nessa casa.").queue(null, Helper::doNothing);
                    return;
                } else if (!yours.isAvailable()) {
                    channel.sendMessage("❌ | Essa carta já atacou neste turno.").queue(null, Helper::doNothing);
                    return;
                } else if (yours.isFlipped()) {
                    channel.sendMessage("❌ | Você não pode atacar com cartas viradas para baixo.").queue(null, Helper::doNothing);
                    return;
                } else if (yours.isDefending()) {
                    channel.sendMessage("❌ | Você não pode atacar com cartas em modo de defesa.").queue(null, Helper::doNothing);
                    return;
                }

                if (yours.hasEffect()) {
                    yours.getEffect(new EffectParameters(phase, EffectTrigger.ON_ATTACK, this, is[0], h.getSide(), Duelists.of(yours, is[0], his, is[1]), channel));
                    postCombat();
                }

                if (his.hasEffect()) {
                    his.getEffect(new EffectParameters(phase, EffectTrigger.ON_DEFEND, this, is[1], h.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP, Duelists.of(yours, is[0], his, is[1]), channel));
                    postCombat();
                }

                int yPower = Math.round(
                        yours.getEAtk() +
                                yours.getLinkedTo().stream().mapToInt(Equipment::getAtk).sum() *
                                        (arena.getField() == null ? 1 : arena.getField().getModifiers().optFloat(yours.getRace().name(), 1f))
                );

                int hPower;
                if (his.isDefending() || his.isFlipped()) {
                    if (his.isFlipped()) {
                        his.setFlipped(false);
                        his.setDefending(true);
                        if (his.hasEffect()) {
                            his.getEffect(new EffectParameters(phase, EffectTrigger.ON_FLIP, this, is[1], h.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP, Duelists.of(yours, is[0], his, is[1]), channel));
                            postCombat();
                        }
                    }
                    hPower = Math.round(
                            his.getEDef() +
                                    his.getLinkedTo().stream().mapToInt(Equipment::getDef).sum() *
                                            (arena.getField() == null ? 1 : arena.getField().getModifiers().optFloat(his.getRace().name(), 1f))
                    );
                } else
                    hPower = Math.round(
                            his.getEAtk() +
                                    his.getLinkedTo().stream().mapToInt(Equipment::getAtk).sum() *
                                            (arena.getField() == null ? 1 : arena.getField().getModifiers().optFloat(his.getRace().name(), 1f))
                    );

                if (yPower > hPower) {
                    yours.setAvailable(false);
                    yours.resetAttribs();
                    if (yours.hasEffect()) {
                        yours.getEffect(new EffectParameters(phase, EffectTrigger.POST_ATTACK, this, is[0], h.getSide(), Duelists.of(yours, is[0], his, is[1]), channel));
                        postCombat();
                    }
                    if (his.hasEffect()) {
                        his.getEffect(new EffectParameters(phase, EffectTrigger.ON_DEATH, this, is[1], h.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP, Duelists.of(yours, is[0], his, is[1]), channel));
                        postCombat();
                    }
                    killCard(h.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP, is[1], hisSide);

                    if (!postCombat()) {
                        channel.sendMessage("Sua carta derrotou a carta inimiga! (" + yPower + " > " + hPower + ")")
                                .addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg")
                                .queue(s -> {
                                    if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
                                    this.message = s;
                                    Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
                                });
                        resetTimerKeepTurn();
                    }
                } else if (yPower < hPower) {
                    his.resetAttribs();
                    if (yours.hasEffect()) {
                        yours.getEffect(new EffectParameters(phase, EffectTrigger.ON_SUICIDE, this, is[0], h.getSide(), Duelists.of(yours, is[0], his, is[1]), channel));
                        postCombat();
                    }
                    if (his.hasEffect()) {
                        his.getEffect(new EffectParameters(phase, EffectTrigger.POST_DEFENSE, this, is[1], h.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP, Duelists.of(yours, is[0], his, is[1]), channel));
                        postCombat();
                    }
                    killCard(h.getSide(), is[0], yourSide);

                    if (!postCombat()) {
                        channel.sendMessage("Sua carta foi derrotada pela carta inimiga! (" + yPower + " < " + hPower + ")")
                                .addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg")
                                .queue(s -> {
                                    if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
                                    this.message = s;
                                    Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
                                });
                        resetTimerKeepTurn();
                    }
                } else {
                    killCard(h.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP, is[1], hisSide);
                    killCard(h.getSide(), is[0], yourSide);

                    if (yours.hasEffect()) {
                        yours.getEffect(new EffectParameters(phase, EffectTrigger.ON_SUICIDE, this, is[0], h.getSide(), Duelists.of(yours, is[0], his, is[1]), channel));
                        postCombat();
                    }
                    if (his.hasEffect()) {
                        his.getEffect(new EffectParameters(phase, EffectTrigger.ON_DEATH, this, is[1], h.getSide() == Side.TOP ? Side.BOTTOM : Side.TOP, Duelists.of(yours, is[0], his, is[1]), channel));
                        postCombat();
                    }

                    if (!postCombat()) {
                        channel.sendMessage("As duas cartas foram destruidas! (" + yPower + " = " + hPower + ")")
                                .addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg")
                                .queue(s -> {
                                    if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
                                    this.message = s;
                                    Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
                                });
                        resetTimerKeepTurn();
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                channel.sendMessage("❌ | Índice inválido, escolha uma carta para usar no ataque e uma para ser atacada.").queue(null, Helper::doNothing);
            } catch (NumberFormatException e) {
                channel.sendMessage("❌ | Índice inválido, o primeiro argumento deve ser uma casa com uma carta no seu lado do tabuleiro e o segundo deve ser uma casa com uma carta no lado do inimigo.").queue(null, Helper::doNothing);
            }
        }
    }

    public void killCard(Side s, int index, List<SlotColumn<Drawable, Drawable>> side) {
        Champion ch = (Champion) side.get(index).getTop();
        if (ch == null || ch.getBonus().getSpecialData().optBoolean("preventDeath", false)) return;
        ch.reset();
        if (ch.getCard().getRarity() != KawaiponRarity.FUSION)
            arena.getGraveyard().get(s).add(ch);
        side.get(index).setTop(null);
        side.forEach(sd -> {
            if (sd.getBottom() != null && ((Equipment) sd.getBottom()).getLinkedTo().getLeft() == index) {
                Equipment eq = (Equipment) sd.getBottom();
                eq.setLinkedTo(null);
                if (eq.getTier() >= 4) arena.getBanished().add(eq);
                else arena.getGraveyard().get(s).add(eq);
                sd.setBottom(null);
            }
        });
    }

    public void banishCard(Side s, int index, List<SlotColumn<Drawable, Drawable>> side) {
        Champion ch = (Champion) side.get(index).getTop();
        if (ch == null || ch.getBonus().getSpecialData().optBoolean("preventDeath", false)) return;
        ch.reset();
        if (ch.getCard().getRarity() != KawaiponRarity.FUSION)
            arena.getBanished().add(ch);
        side.get(index).setTop(null);
        side.forEach(sd -> {
            if (sd.getBottom() != null && ((Equipment) sd.getBottom()).getLinkedTo().getLeft() == index) {
                Equipment eq = (Equipment) sd.getBottom();
                eq.setLinkedTo(null);
                if (eq.getTier() >= 4) arena.getBanished().add(eq);
                else arena.getGraveyard().get(s).add(eq);
                sd.setBottom(null);
            }
        });
    }

    public void unequipCard(Side s, int index, List<SlotColumn<Drawable, Drawable>> side) {
        Equipment eq = (Equipment) side.get(index).getBottom();
        if (eq == null) return;

        if (side.get(eq.getLinkedTo().getLeft()).getTop() != null)
            ((Champion) side.get(eq.getLinkedTo().getLeft()).getTop()).removeLinkedTo(eq);
        eq.setLinkedTo(null);

        SlotColumn<Drawable, Drawable> sd = side.get(index);
        arena.getGraveyard().get(s).add(eq);
        sd.setBottom(null);
    }

    public Arena getArena() {
        return arena;
    }

    public Map<Side, Hand> getHands() {
        return hands;
    }

    public Hand getHandById(String id) {
        return hands.values().stream().filter(h -> h.getUser().getId().equals(id)).findFirst().orElseThrow();
    }

    public SlotColumn<Drawable, Drawable> getFirstAvailableSlot(Side s, boolean top) {
        for (SlotColumn<Drawable, Drawable> slot : arena.getSlots().get(s)) {
            if (top ? slot.getTop() == null : slot.getBottom() == null)
                return slot;
        }
        return null;
    }

    public void convertCard(Side side, int index) {
        Side his = side == Side.TOP ? Side.BOTTOM : Side.TOP;
        Champion ch = (Champion) getArena().getSlots().get(his).get(index).getTop();
        if (ch == null || ch.getBonus().getSpecialData().optBoolean("preventConvert", false)) return;
        SlotColumn<Drawable, Drawable> sc = getFirstAvailableSlot(side, true);
        if (sc != null) {
            ch.clearLinkedTo();
            ch.setAcc(AccountDAO.getAccount(getHands().get(side).getUser().getId()));
            sc.setTop(ch);
            List<SlotColumn<Drawable, Drawable>> slts = getArena().getSlots().get(his);
            slts.get(index).setTop(null);
            for (int i = 0; i < slts.size(); i++) {
                if (slts.get(i).getBottom() != null && ((Equipment) slts.get(i).getBottom()).getLinkedTo().getLeft() == index)
                    unequipCard(his, i, slts);
            }
        }
    }

    public void convertEquipments(Champion target, int pos, Side side, int index) {
        Side his = side == Side.TOP ? Side.BOTTOM : Side.TOP;
        Champion ch = (Champion) getArena().getSlots().get(his).get(index).getTop();
        if (ch == null) return;
        List<SlotColumn<Drawable, Drawable>> slts = getArena().getSlots().get(his);
        for (int i = 0; i < 5; i++) {
            Equipment eq = (Equipment) slts.get(i).getBottom();
            if (eq != null && eq.getLinkedTo().getLeft() == index) {
                SlotColumn<Drawable, Drawable> sc = getFirstAvailableSlot(side, false);
                if (sc != null) {
                    ch.removeLinkedTo(eq);
                    slts.get(i).setBottom(null);

                    target.addLinkedTo(eq);
                    eq.setLinkedTo(Pair.of(pos, target));
                    eq.setAcc(AccountDAO.getAccount(getHands().get(side).getUser().getId()));
                    sc.setBottom(eq);
                } else return;
            }
        }
    }

    public boolean postCombat() {
        AtomicBoolean finished = new AtomicBoolean(false);
        getHands().forEach((s, h) -> {
            if (!finished.get()) {
                Hand op = getHands().get(s == Side.TOP ? Side.BOTTOM : Side.TOP);
                if (h.getHp() == 0) {
                    channel.sendMessage(op.getUser().getAsMention() + " zerou os pontos de vida de " + h.getUser().getAsMention() + ", temos um vencedor! (" + getRound() + " turnos)")
                            .addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg")
                            .queue(msg -> {
                                if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
                            });

                    if (getCustom() == null)
                        getBoard().awardWinner(this, daily, op.getUser().getId());
                    close();
                    finished.set(true);
                }
            }
        });
        return finished.get();
    }

    @Override
    public Map<String, BiConsumer<Member, Message>> getButtons() {
        AtomicReference<String> hash = new AtomicReference<>(Helper.generateHash(this));
        ShiroInfo.getHashes().add(hash.get());

        Map<String, BiConsumer<Member, Message>> buttons = new LinkedHashMap<>();
        buttons.put("▶️", (mb, ms) -> {
            if (!ShiroInfo.getHashes().remove(hash.get())) return;
            getHandById(getCurrent().getId()).getCards().removeIf(d -> !d.isAvailable());
            if (getRound() < 1 || phase == Phase.ATTACK) {
                User u = getCurrent();

                AtomicReference<Hand> h = new AtomicReference<>(getHandById(getCurrent().getId()));
                List<SlotColumn<Drawable, Drawable>> slots = arena.getSlots().get(h.get().getSide());
                for (int i = 0; i < slots.size(); i++) {
                    Champion c = (Champion) slots.get(i).getTop();
                    if (c != null) {
                        c.setAvailable(true);
                        c.resetAttribs();
                        if (c.hasEffect()) {
                            c.getEffect(new EffectParameters(phase, EffectTrigger.AFTER_TURN, this, i, h.get().getSide(), Duelists.of(c, i, null, -1), channel));
                            postCombat();
                        }
                    }
                }

                resetTimer();

                phase = Phase.PLAN;
                h.set(getHandById(getCurrent().getId()));
                slots = arena.getSlots().get(h.get().getSide());
                for (int i = 0; i < slots.size(); i++) {
                    Champion c = (Champion) slots.get(i).getTop();
                    if (c != null) {
                        if (c.hasEffect()) {
                            c.getEffect(new EffectParameters(phase, EffectTrigger.BEFORE_TURN, this, i, h.get().getSide(), Duelists.of(c, i, null, -1), channel));
                            postCombat();
                        }
                    }
                }
                h.get().addMana(h.get().getManaPerTurn());

                channel.sendMessage(u.getAsMention() + " encerrou o turno, agora é sua vez " + getCurrent().getAsMention())
                        .addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg")
                        .queue(s -> {
                            if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
                            this.message = s;
                            Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
                            h.get().showHand();
                            for (int i = 0; i < 5; i++) {
                                changed[i] = false;
                            }
                        });
                return;
            }

            hash.set(Helper.generateHash(this));
            ShiroInfo.getHashes().add(hash.get());
            channel.sendMessage("**FASE DE ATAQUE:** Escolha uma carta do seu lado e uma carta do lado inimigo para iniciar combate").queue(null, Helper::doNothing);
            phase = Phase.ATTACK;
            draw = false;
            resetTimerKeepTurn();
        });
        buttons.put("\uD83D\uDCE4", (mb, ms) -> {
            if (!ShiroInfo.getHashes().remove(hash.get())) return;
            if (phase != Phase.PLAN) {
                channel.sendMessage("❌ | Você só pode puxar cartas na fase de planejamento.").queue(null, Helper::doNothing);
                return;
            }

            Hand h = getHandById(getCurrent().getId());

            int remaining = 5 - h.getCards().size();

            if (remaining <= 0) {
                channel.sendMessage("❌ | Você não pode puxar mais cartas se tiver 5 cartas ou mais na sua mão.").queue(null, Helper::doNothing);
                return;
            }

            if (!h.draw()) {
                channel.sendMessage(getCurrent().getAsMention() + " não possui mais cartas no deck, " + getPlayerById(getBoard().getPlayers().get(1).getId()).getAsMention() + " venceu! (" + getRound() + " turnos)")
                        .addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg")
                        .queue(s -> {
                            if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
                        });

                if (getCustom() == null)
                    getBoard().awardWinner(this, daily, getBoard().getPlayers().get(1).getId());
                close();
                return;
            }

            remaining = 5 - h.getCards().size();
            channel.sendMessage(getCurrent().getAsMention() + " puxou uma carta (" + (remaining == 0 ? "não pode puxar mais cartas" : "pode puxar mais " + remaining + " carta" + (remaining == 1 ? "" : "s")) + ")")
                    .addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg")
                    .queue(s -> {
                        if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
                        this.message = s;
                        Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
                        h.showHand();
                    });
            resetTimerKeepTurn();
        });
        buttons.put("\uD83E\uDD1D", (mb, ms) -> {
            if (!ShiroInfo.getHashes().remove(hash.get())) return;
            if (draw) {
                channel.sendMessage("Por acordo mútuo, declaro empate! (" + getRound() + " turnos)")
                        .addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg")
                        .queue(s -> {
                            if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
                        });
                close();
            } else {
                User u = getCurrent();
                resetTimer();

                phase = Phase.PLAN;
                Hand h = getHandById(getCurrent().getId());
                arena.getSlots().get(getHandById(mb.getId()).getSide()).forEach(s -> {
                    if (s.getTop() != null) {
                        Champion c = (Champion) s.getTop();
                        c.setAvailable(true);
                        c.resetAttribs();
                    }
                });

                h.addMana(h.getManaPerTurn());

                draw = true;
                channel.sendMessage(u.getAsMention() + " deseja um acordo de empate, " + getCurrent().getAsMention() + " agora é sua vez, clique em \uD83E\uDD1D caso queira aceitar ou continue jogando normalmente.")
                        .addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg")
                        .queue(s -> {
                            if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
                            this.message = s;
                            Pages.buttonize(s, getButtons(), false, 3, TimeUnit.MINUTES, us -> us.getId().equals(getCurrent().getId()));
                            h.showHand();
                            for (int i = 0; i < 5; i++) {
                                changed[i] = false;
                            }
                        });
            }
        });
        buttons.put("\uD83C\uDFF3️", (mb, ms) -> {
            if (!ShiroInfo.getHashes().remove(hash.get())) return;
            channel.sendMessage(getCurrent().getAsMention() + " desistiu! (" + getRound() + " turnos)")
                    .addFile(Helper.getBytes(arena.render(hands), "jpg", 0.5f), "board.jpg")
                    .queue(s -> {
                        if (this.message != null) this.message.delete().queue(null, Helper::doNothing);
                    });
            if (getCustom() == null)
                getBoard().awardWinner(this, daily, getBoard().getPlayers().get(1).getId());
            close();
        });

        return buttons;
    }

    @Override
    public void close() {
        super.close();
        getHandler().removeEventListener(listener);
    }
}
