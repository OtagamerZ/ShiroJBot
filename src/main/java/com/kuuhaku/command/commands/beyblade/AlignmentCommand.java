package com.kuuhaku.command.commands.beyblade;

import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.MySQL;
import com.kuuhaku.controller.SQLite;
import com.kuuhaku.model.Beyblade;
import com.kuuhaku.model.guildConfig;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

import java.util.Objects;

public class AlignmentCommand extends Command {

    public AlignmentCommand() {
        super("balign", new String[]{"balinhar", "bcasa"}, "<casa>", "Escolhe seu alinhamento.", Category.BEYBLADE);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        if (MySQL.getBeybladeById(author.getId()) == null) {
            channel.sendMessage(":x: | Você não possui uma Beyblade.").queue();
            return;
        } else if (args.length == 0) {
            guildConfig gc = SQLite.getGuildById(guild.getId());

            channel.sendMessage("__**Alinhamento é o que define seu estilo de combate:**__\n- Tigres são focados em uma **velocidade** extrema.\n- Dragões são focados em um **poder** incomparável.\n- Ursos são focados em uma **defesa** impenetrável.\n\n" +
                    "Cada alinhamento possui especiais diferentes, que poderão virar um duelo, a **primeira vez** que você escolher um alinhamento custará **150 pontos de combate**. " +
                    "Após, **qualquer troca de alinhamento custará 300 pontos de combate**.\n" +
                    "\nPara escolher tigre, digite `" + gc.getPrefix() + "balinhar tigre`" +
                    "\nPara escolher dragão, digite `" + gc.getPrefix() + "balinhar dragão`" +
                    "\nPara escolher urso, digite `" + gc.getPrefix() + "balinhar urso`").queue();
            return;
        }

        channel.sendMessage("<a:Loading:598500653215645697> Analizando...").queue(m -> {
            Beyblade bb = Objects.requireNonNull(MySQL.getBeybladeById(author.getId()));

            if (bb.getPoints() >= (bb.getS() == null ? 150 : 300)) {
                switch (args[0].trim().toLowerCase()) {
                    case "tigre":
                        bb.setSpecial(10 + Helper.rng(2));
                        bb.takePoints((bb.getS() == null ? 150 : 300));
                        break;
                    case "dragão":
                        bb.setSpecial(20 + Helper.rng(2));
                        bb.takePoints((bb.getS() == null ? 150 : 300));
                        break;
                    case "urso":
                        bb.setSpecial(30 + 1);
                        bb.takePoints((bb.getS() == null ? 150 : 300));
                        break;
                    default:
                        m.editMessage(":x: | alinhamento inválido, escolha entre **tigre**, **dragão** ou **urso**!").queue();
                        return;
                }
                m.editMessage("Seu alinhamento foi trocado para **" + Objects.requireNonNull(bb.getS()).getType() + "**, e o especial concedido a você foi: " + bb.getS().getName()).queue();
            } else {
                m.editMessage(":x: | Você não possui pontos de combate suficiente!").queue();
            }
        });
    }
}
