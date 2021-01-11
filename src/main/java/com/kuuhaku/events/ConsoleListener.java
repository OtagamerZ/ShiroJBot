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

package com.kuuhaku.events;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.BackupDAO;
import com.kuuhaku.model.common.DataDump;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class ConsoleListener extends BufferedReader {
    private final Thread thread = new Thread(this::exec);
    private boolean interrupted = false;

    public ConsoleListener() {
        super(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    }

    public void start() {
        thread.start();
    }

    private void exec() {
        while (!interrupted) {
            try {
                handleCommand(readLine());
            } catch (IOException e) {
                Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
            }
        }
    }

    @Override
    public void close() {
        try {
            super.close();
            interrupted = true;
        } catch (IOException e) {
            Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
        }
    }

    private void handleCommand(String command) {
        switch (command) {
            case "test" -> System.out.println("No need to test, I'm working!");
            case "kill" -> {
                System.out.println("Sayonara, Nii-chan <3");
                Executors.newSingleThreadExecutor().execute(() ->
						BackupDAO.dumpData(new DataDump(
								com.kuuhaku.controller.sqlite.BackupDAO.getCADump(),
								com.kuuhaku.controller.sqlite.BackupDAO.getMemberDump(),
								com.kuuhaku.controller.sqlite.BackupDAO.getGuildDump(),
								com.kuuhaku.controller.sqlite.BackupDAO.getKawaigotchiDump(),
								com.kuuhaku.controller.sqlite.BackupDAO.getPoliticalStateDump(),
								null
						), true)
				);
			}
			case "notifyNoMemo" -> {
				for (String s : ShiroInfo.getDevelopers()) {
					Main.getInfo().getUserByID(s).openPrivateChannel()
							.flatMap(ch -> ch.sendMessage("Alerta, minha memória esgotou, causando um erro OutOfMemory. Por favor leia o arquivo de profile gerado na pasta de logs para mais informações."))
							.queue();
				}
			}
		}
    }
}
