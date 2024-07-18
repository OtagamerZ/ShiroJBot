/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.common;

import com.kuuhaku.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public class MultiProcessor<In, Out> {
	private final List<In> tasks;
	private final ExecutorService exec;

	private MultiProcessor(List<In> tasks, ExecutorService executor) {
		this.tasks = tasks;
		this.exec = executor;
	}

	public static <In> MultiProcessor<In, ?> with(ExecutorService executor, List<In> tasks) {
		return new MultiProcessor<>(tasks, executor);
	}

	public <R> MultiProcessor<In, R> forResult(Class<R> klass) {
		return new MultiProcessor<>(tasks, exec);
	}

	public MultiProcessor<In, Out> addTask(In task) {
		tasks.add(task);
		return this;
	}

	public List<Out> process(Function<In, Out> task) {
		List<In> all = List.copyOf(tasks);

		List<CompletableFuture<Out>> tasks = new ArrayList<>();
		for (int i = 0; i < all.size(); i++) {
			int index = i;
			tasks.add(CompletableFuture.supplyAsync(() -> task.apply(all.get(index)), exec));
		}

		try {
			List<Out> finished = new ArrayList<>();
			for (CompletableFuture<Out> t : tasks) {
				finished.add(t.get());
			}

			exec.shutdownNow();
			exec.close();
			return finished;
		} catch (InterruptedException ignore) {
		} catch (ExecutionException e) {
			Constants.LOGGER.error(e, e);
		}

		return null;
	}

	public Out process(Function<In, Out> task, Function<List<Out>, Out> merger) {
		return merger.apply(process(task));
	}
}
