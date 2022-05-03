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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class MultiProcessor<In, Out> {
	private final Supplier<In> supplier;
	private final ExecutorService exec;
	private final int threads;

	private MultiProcessor(Supplier<In> supplier, ExecutorService exec, int threads) {
		this.supplier = supplier;
		this.exec = exec;
		this.threads = threads;
	}

	public static <In, Out> MultiProcessor<In, Out> with(int threads, Supplier<In> supplier) {
		return new MultiProcessor<>(supplier, Executors.newWorkStealingPool(threads), threads);
	}

	public <R> MultiProcessor<In, R> forResult(Class<R> klass) {
		return new MultiProcessor<>(supplier, exec, threads);
	}

	public List<CompletableFuture<Out>> process(BiFunction<Integer, In, Out> task) {
		List<CompletableFuture<Out>> tasks = new ArrayList<>();
		for (int i = 0; i < threads; i++) {
			int index = i;
			tasks.add(CompletableFuture.supplyAsync(() -> task.apply(index, supplier.get()), exec));
		}

		return tasks;
	}

	public Out process(BiFunction<Integer, In, Out> task, Function<List<Out>, Out> merger) {
		try {
			List<Out> finished = new ArrayList<>();
			List<CompletableFuture<Out>> tasks = process(task);
			for (CompletableFuture<Out> t : tasks) {
				finished.add(t.get());
			}

			return merger.apply(finished);
		} catch (ExecutionException | InterruptedException e) {
			return null;
		}
	}
}
