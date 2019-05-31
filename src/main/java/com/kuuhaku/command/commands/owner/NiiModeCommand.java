package com.kuuhaku.command.commands.owner;

import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;

public class NiiModeCommand extends Command {

    public NiiModeCommand() {
        super("switch", new String[] {"kuuhaku"}, "Troca de lugar com a Shiro!", Category.NIICHAN);
    }

    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, Event event, String prefix) {
        if (Main.getInfo().isNiimode()) {
            Main.getInfo().switchNiimode();
            channel.sendMessage("Mas já? Então tá, sua vez agora Nii-chan!").queue();
        } else {
            Main.getInfo().switchNiimode();
            channel.sendMessage("Ok Nii-chan, agora é a minha vez de ser KuuHaKu!").queue();
        }
    }
}
