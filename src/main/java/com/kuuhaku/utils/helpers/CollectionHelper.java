package com.kuuhaku.utils.helpers;

import net.jodah.expiringmap.ExpiringMap;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class CollectionHelper {
	@SuppressWarnings("unchecked")
	public static <T> T getOr(Object get, T or) {
		if (get instanceof String s && s.isBlank()) return or;
		else return get == null ? or : (T) get;
	}

	@SafeVarargs
	public static <T> T getOrMany(Object get, T... or) {
		T out = null;

		for (T t : or) {
			out = getOr(get, t);
			if (out != null && !(out instanceof String s && s.isBlank())) break;
		}

		return out;
	}

	public static <T> List<List<T>> chunkify(Collection<T> col, int chunkSize) {
		List<T> list = List.copyOf(col);
		int overflow = list.size() % chunkSize;
		List<List<T>> chunks = new ArrayList<>();

		for (int i = 0; i < (list.size() - overflow) / chunkSize; i++) {
			chunks.add(list.subList(i * chunkSize, (i * chunkSize) + chunkSize));
		}

		if (overflow > 0)
			chunks.add(list.subList(list.size() - overflow, list.size()));

		return chunks;
	}

	public static <K, V> List<List<Map.Entry<K, V>>> chunkify(Map<K, V> map, int chunkSize) {
		List<Map.Entry<K, V>> list = List.copyOf(map.entrySet());
		int overflow = list.size() % chunkSize;
		List<List<Map.Entry<K, V>>> chunks = new ArrayList<>();

		for (int i = 0; i < (list.size() - overflow) / chunkSize; i++) {
			chunks.add(list.subList(i * chunkSize, (i * chunkSize) + chunkSize));
		}

		if (overflow > 0)
			chunks.add(list.subList(list.size() - overflow, list.size()));

		return chunks;
	}

	public static <T> T getRandomEntry(Collection<T> col) {
		if (col.isEmpty()) throw new IllegalArgumentException("Collection must not be empty");
		List<T> list = List.copyOf(col);

		return list.get(MathHelper.rng(list.size() - 1));
	}

	@SafeVarargs
	public static <T> T getRandomEntry(T... array) {
		if (array.length == 0) throw new IllegalArgumentException("Array must not be empty");
		List<T> list = List.of(array);

		return list.get(MathHelper.rng(list.size() - 1));
	}

	public static <T> T getRandomEntry(Random random, Collection<T> col) {
		if (col.isEmpty()) throw new IllegalArgumentException("Collection must not be empty");
		List<T> list = List.copyOf(col);

		return list.get(MathHelper.rng(list.size() - 1, random));
	}

	@SafeVarargs
	public static <T> T getRandomEntry(Random random, T... array) {
		if (array.length == 0) throw new IllegalArgumentException("Array must not be empty");
		List<T> list = List.of(array);

		return list.get(MathHelper.rng(list.size() - 1, random));
	}

	public static <T> List<T> getRandomN(List<T> array, int elements) {
		List<T> aux = new ArrayList<>(array);
		List<T> out = new ArrayList<>();
		Random random = new Random(System.currentTimeMillis());

		for (int i = 0; i < elements && aux.size() > 0; i++) {
			int index = MathHelper.rng(aux.size() - 1, random);

			out.add(aux.get(index));
			Collections.shuffle(aux, random);
		}

		return out;
	}

	public static <T> List<T> getRandomN(List<T> array, int elements, int maxInstances) {
		List<T> aux = new ArrayList<>(array);
		List<T> out = new ArrayList<>();
		Random random = new Random(System.currentTimeMillis());

		for (int i = 0; i < elements && aux.size() > 0; i++) {
			int index = MathHelper.rng(aux.size() - 1, random);

			T inst = aux.get(index);
			if (Collections.frequency(out, inst) < maxInstances)
				out.add(inst);
			else {
				aux.remove(index);
				Collections.shuffle(aux, random);
				i--;
			}
		}

		return out;
	}

	public static <T> List<T> getRandomN(List<T> array, int elements, int maxInstances, long seed) {
		List<T> aux = new ArrayList<>(array);
		List<T> out = new ArrayList<>();
		Random random = new Random(seed);

		for (int i = 0; i < elements && aux.size() > 0; i++) {
			int index = MathHelper.rng(aux.size() - 1, random);

			T inst = aux.get(index);
			if (Collections.frequency(out, inst) < maxInstances)
				out.add(inst);
			else {
				aux.remove(index);
				Collections.shuffle(aux, random);
				i--;
			}
		}

		return out;
	}

	public static <T> T getNext(T current, List<T> sequence) {
		int index = sequence.indexOf(current);
		return index == -1 ? null : sequence.get(Math.min(index + 1, sequence.size() - 1));
	}

	@SafeVarargs
	public static <T> T getNext(T current, T... sequence) {
		int index = ArrayUtils.indexOf(sequence, current);
		return index == -1 ? null : sequence[Math.min(index + 1, sequence.length - 1)];
	}

	public static <T> T getPrevious(T current, List<T> sequence) {
		int index = sequence.indexOf(current);
		return index == -1 ? null : sequence.get(Math.max(index - 1, 0));
	}

	@SafeVarargs
	public static <T> T getPrevious(T current, T... sequence) {
		int index = ArrayUtils.indexOf(sequence, current);
		return index == -1 ? null : sequence[Math.max(index - 1, 0)];
	}

	public static <T> List<T> getIndexes(List<T> list, int... indexes) {
		List<T> out = new ArrayList<>();
		for (int index : indexes) {
			if (index < list.size()) out.add(list.get(index));
		}

		return out;
	}

	public static List<Integer> getNumericList(int min, int max) {
		List<Integer> values = new ArrayList<>();
		for (int i = min; i < max; i++) {
			values.add(i);
		}
		return values;
	}

	public static <T extends Collection<String>> Function<T, String> properlyJoin() {
		return objs -> {
			List<String> ls = List.copyOf(objs);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < ls.size(); i++) {
				if (i == ls.size() - 1 && ls.size() > 1) sb.append(" e ");
				else if (i > 0) sb.append(", ");

				sb.append(ls.get(i));
			}

			return sb.toString();
		};
	}

	public static <C, T extends Collection<C>> String parseAndJoin(T col, Function<C, String> mapper) {
		return col.stream().map(mapper).collect(Collectors.collectingAndThen(Collectors.toList(), properlyJoin()));
	}

	@SuppressWarnings("unchecked")
	public static <T> T map(Class<T> type, Object[] tuple) {
		try {
			List<Constructor<?>> constructors = Arrays.stream(type.getConstructors())
					.filter(c -> c.getParameterCount() == tuple.length)
					.toList();

			for (Constructor<?> ctor : constructors) {
				try {
					return (T) ctor.newInstance(tuple);
				} catch (IllegalArgumentException ignore) {
				}
			}

			throw new IllegalStateException("No matching constructor found.");
		} catch (Exception e) {
			if (e instanceof InvocationTargetException ex) {
				throw new RuntimeException(ex.getCause());
			}

			throw new RuntimeException(e);
		}
	}

	public static <T> List<T> map(Class<T> type, List<Object[]> records) {
		List<T> result = new LinkedList<>();
		for (Object[] record : records) {
			result.add(map(type, record));
		}
		return result;
	}

	public static <T> T safeCast(Object obj, Class<T> klass) {
		return klass != null && klass.isInstance(obj) ? klass.cast(obj) : null;
	}

	public static <T> void replaceContent(Collection<T> from, Collection<T> to) {
		to.removeAll(from);
		to.addAll(from);
	}

	public static <K, V> void replaceContent(Map<K, V> from, Map<K, V> to) {
		for (K key : from.keySet()) {
			from.remove(key);
		}
		to.putAll(from);
	}

	public static <T> List<T> removeIf(Collection<T> col, Function<T, Boolean> cond) {
		List<T> removed = new ArrayList<>();

		Iterator<T> i = col.iterator();
		while (i.hasNext()) {
			T obj = i.next();

			if (cond.apply(obj)) {
				removed.add(obj);
				i.remove();
			}
		}

		return removed;
	}

	public static <K, V> Map<K, V> removeIf(Map<K, V> map, BiFunction<K, V, Boolean> cond) {
		Map<K, V> removed = new HashMap<>();

		Iterator<Map.Entry<K, V>> i = map.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<K, V> entry = i.next();

			if (cond.apply(entry.getKey(), entry.getValue())) {
				removed.put(entry.getKey(), entry.getValue());
				i.remove();
			}
		}

		return removed;
	}

	public static Integer[] mergeArrays(Integer[]... arr) {
		List<Integer[]> in = new ArrayList<>(List.of(arr));
		int max = in.stream().map(a -> a.length).max(Integer::compareTo).orElse(0);

		return IntStream.range(0, max)
				.mapToObj(i -> {
					int n = 0;

					for (Integer[] ts : in) {
						if (i < ts.length) n += ts[i];
					}

					return n;
				}).toArray(Integer[]::new);
	}

	public static int safeGet(int[] arr, int index) {
		try {
			return arr[index];
		} catch (IndexOutOfBoundsException e) {
			return -1;
		}
	}

	public static long safeGet(long[] arr, int index) {
		try {
			return arr[index];
		} catch (IndexOutOfBoundsException e) {
			return -1;
		}
	}

	public static float safeGet(float[] arr, int index) {
		try {
			return arr[index];
		} catch (IndexOutOfBoundsException e) {
			return -1;
		}
	}

	public static double safeGet(double[] arr, int index) {
		try {
			return arr[index];
		} catch (IndexOutOfBoundsException e) {
			return -1;
		}
	}

	public static <T> T safeGet(T[] arr, int index) {
		try {
			return arr[index];
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	public static <T> T safeGet(List<T> lst, int index) {
		try {
			return lst.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	public static <T, O> O castGet(T[] arr, int index, Function<T, O> converter) {
		try {
			return converter.apply(arr[index]);
		} catch (RuntimeException e) {
			return null;
		}
	}

	public static <T, O> O castGet(List<T> lst, int index, Function<T, O> converter) {
		try {
			return converter.apply(lst.get(index));
		} catch (RuntimeException e) {
			return null;
		}
	}

	public static <K, V> ExpiringMap.Builder<? super K, ? super V> makeExpMap() {
		return ExpiringMap.builder().variableExpiration();
	}

	public static <K, V> ExpiringMap.Builder<? super K, ? super V> makeExpMap(int time, TimeUnit unit) {
		return ExpiringMap.<K, V>builder().expiration(time, unit);
	}
}
