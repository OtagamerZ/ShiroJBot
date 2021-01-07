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

package com.kuuhaku.command.commands.discord.moderation;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.BackupDAO;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.model.persistent.Backup;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NonNls;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class BackupCommand extends Command {

    public BackupCommand(String name, String description, Category category, boolean requiresMM) {
        super(name, description, category, requiresMM);
    }

    public BackupCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
        super(name, aliases, description, category, requiresMM);
    }

    public BackupCommand(String name, String usage, String description, Category category, boolean requiresMM) {
        super(name, usage, description, category, requiresMM);
    }

    public BackupCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
        super(name, aliases, usage, description, category, requiresMM);
    }


    @Override
    public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
        if (Main.getInfo().getConfirmationPending().getIfPresent(author.getId()) != null) {
            channel.sendMessage("❌ | Você possui um comando com confirmação pendente, por favor resolva-o antes de usar este comando novamente.").queue();
            return;
        }

        Backup data = BackupDAO.getGuildBackup(guild);

        if (args.length < 1) {
            channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_backup-no-mode")).queue();
            return;
        } else if (!Helper.equalsAny(args[0], "salvar", "recuperar")) {
            channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_backup-invalid-mode")).queue();
            return;
        } else if (!guild.getSelfMember().hasPermission(Permission.ADMINISTRATOR)) {
            channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_backup-permission-required")).queue();
            return;
        } else if (!member.hasPermission(Permission.ADMINISTRATOR) && !member.isOwner()) {
            channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_backup-role-not-high-enough")).queue();
            return;
        }

        if (args[0].equalsIgnoreCase("salvar")) {
            data.setGuild(guild.getId());
            data.saveServerData(guild);
            channel.sendMessage("✅ | Backup feito com sucesso, utilize `" + prefix + "backup recuperar` para recuperar para este estado do servidor. (ISSO IRÁ REESCREVER O SERVIDOR, TODAS AS MENSAGENS SERÃO PERDIDAS)").queue();
        } else if (data.getGuild() == null || data.getGuild().isEmpty()) {
            channel.sendMessage(MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("err_backup-not-exists"), prefix)).queue();
        } else if (data.getLastRestored().toLocalDateTime().plusDays(7).until(LocalDateTime.now(), ChronoUnit.DAYS) < 7) {
            channel.sendMessage(MessageFormat.format(ShiroInfo.getLocale(I18n.PT).getString("err_backup-ratelimit"), 7 - data.getLastRestored().toLocalDateTime().plusDays(7).until(LocalDateTime.now(), ChronoUnit.DAYS))).queue();
        } else {
            String hash = Helper.generateHash(guild, author);
            ShiroInfo.getHashes().add(hash);
            Main.getInfo().getConfirmationPending().put(author.getId(), true);
            channel.sendMessage("**Restaurar um backup irá limpar todas as mensagens do servidor, inclusive aquelas fixadas**\n\nPor favor, confirme esta operação clicando no botão abaixo.")
                    .queue(s -> Pages.buttonize(s, Collections.singletonMap(Helper.ACCEPT, (mb, ms) -> {
                                if (!ShiroInfo.getHashes().remove(hash)) return;
                                Main.getInfo().getConfirmationPending().invalidate(author.getId());
                                data.restore(guild);
                            }), true, 30, TimeUnit.SECONDS,
                            u -> u.getId().equals(author.getId()),
                            ms -> {
                                ShiroInfo.getHashes().remove(hash);
                                Main.getInfo().getConfirmationPending().invalidate(author.getId());
                            })
                    );
        }
    }
}
