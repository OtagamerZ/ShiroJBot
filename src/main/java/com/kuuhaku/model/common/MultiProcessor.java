/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class MultiProcessor<In, Out> {
	private final Function<Integer, Collection<In>> supplier;
	private final ExecutorService exec;
	private final int threads;

	private MultiProcessor(Function<Integer, Collection<In>> supplier, int threads) {
		this.supplier = supplier;
		this.exec = Executors.newWorkStealingPool(threads);
		this.threads = threads;
	}

	public static <In> MultiProcessor<In, ?> with(int threads, Function<Integer, Collection<In>> supplier) {
		return new MultiProcessor<>(supplier, threads);
	}

	public <R> MultiProcessor<In, R> forResult(Class<R> klass) {
		return new MultiProcessor<>(supplier, threads);
	}

	public List<Out> process(Function<In, Out> task) {
		List<In> all = List.copyOf(supplier.apply(threads));

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

			return finished;
		} catch (ExecutionException | InterruptedException e) {
			Constants.LOGGER.error(e, e);
			return null;
		}
	}

	public Out process(Function<In, Out> task, Function<List<Out>, Out> merger) {
		return merger.apply(process(task));
	}
}
