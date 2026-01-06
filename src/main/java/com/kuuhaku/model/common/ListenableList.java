package com.kuuhaku.model.common;

import org.jspecify.annotations.NonNull;

import java.util.*;

public class ListenableList<T> implements List<T> {
	public interface ListEvent<T> {
		default boolean beforeAdd(T elem) {
			return true;
		}

		default void afterAdd(T elem) {
		}

		default boolean beforeRemove(T elem) {
			return true;
		}

		default void afterRemove(T elem) {
		}
	}

	private final Set<ListEvent<T>> listeners = new HashSet<>();
	private final List<T> internal = new ArrayList<>();

	@SafeVarargs
	public ListenableList(ListEvent<T>... listeners) {
		for (ListEvent<T> l : listeners) {
			addListener(l);
		}
	}

	@SuppressWarnings("UnusedReturnValue")
	public @NonNull ListenableList<T> addListener(@NonNull ListEvent<T> listener) {
		listeners.add(listener);
		return this;
	}

	@SuppressWarnings("UnusedReturnValue")
	public @NonNull ListenableList<T> removeListener(@NonNull ListEvent<T> listener) {
		listeners.remove(listener);
		return this;
	}

	@Override
	public int size() {
		return internal.size();
	}

	@Override
	public boolean isEmpty() {
		return internal.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return internal.contains(o);
	}

	@Override
	public @NonNull Iterator<T> iterator() {
		return listIterator();
	}

	@Override
	public Object @NonNull [] toArray() {
		return internal.toArray();
	}

	@Override
	public <T1> T1 @NonNull [] toArray(T1 @NonNull [] a) {
		return internal.toArray(a);
	}

	@Override
	public boolean add(T t) {
		if (listeners.stream().allMatch(l -> l.beforeAdd(t))) {
			internal.add(t);
			for (ListEvent<T> l : listeners) {
				l.afterAdd(t);
			}

			return true;
		}

		return false;
	}

	@Override
	public boolean remove(Object o) {
		return remove(indexOf(o)) != null;
	}

	@Override
	public boolean containsAll(@NonNull Collection<?> c) {
		return new HashSet<>(internal).containsAll(c);
	}

	@Override
	public boolean addAll(@NonNull Collection<? extends T> c) {
		return internal.addAll(c);
	}

	@Override
	public boolean addAll(int index, @NonNull Collection<? extends T> c) {
		return internal.addAll(index, c);
	}

	@Override
	public boolean removeAll(@NonNull Collection<?> c) {
		int hash = hashCode();
		removeIf(c::contains);
		return hash != hashCode();
	}

	@Override
	public boolean retainAll(@NonNull Collection<?> c) {
		int hash = hashCode();
		removeIf(t -> !c.contains(t));
		return hash != hashCode();
	}

	@Override
	public void clear() {
		Iterator<T> it = iterator();
		while (it.hasNext()) {
			it.next();
			it.remove();
		}
	}

	@Override
	public T get(int index) {
		return internal.get(index);
	}

	@Override
	public T set(int index, T element) {
		if (remove(index) != null) {
			add(index, element);
		}

		return null;
	}

	@Override
	public void add(int index, T element) {
		if (listeners.stream().allMatch(l -> l.beforeAdd(element))) {
			internal.add(index, element);
			for (ListEvent<T> l : listeners) {
				l.afterAdd(element);
			}
		}
	}

	@Override
	public T remove(int index) {
		if (index < 0 || index >= internal.size()) return null;

		T t = internal.get(index);
		if (listeners.stream().allMatch(l -> l.beforeRemove(t))) {
			internal.remove(index);
			for (ListEvent<T> l : listeners) {
				l.afterRemove(t);
			}

			return t;
		}

		return null;
	}

	@Override
	public int indexOf(Object o) {
		return internal.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return internal.lastIndexOf(o);
	}

	@Override
	public @NonNull ListIterator<T> listIterator() {
		return listIterator(-1);
	}

	@Override
	public @NonNull ListIterator<T> listIterator(int index) {
		return new ListIterator<>() {
			private int idx = index;

			@Override
			public boolean hasNext() {
				return idx < internal.size() - 1;
			}

			@Override
			public T next() {
				return get(++idx);
			}

			@Override
			public boolean hasPrevious() {
				return idx > 0;
			}

			@Override
			public T previous() {
				return get(--idx);
			}

			@Override
			public int nextIndex() {
				return idx + 1;
			}

			@Override
			public int previousIndex() {
				return idx - 1;
			}

			@Override
			public void remove() {
				ListenableList.this.remove(idx--);
			}

			@Override
			public void set(T t) {
				ListenableList.this.set(idx, t);
			}

			@Override
			public void add(T t) {
				ListenableList.this.add(idx++, t);
			}
		};
	}

	@Override
	public @NonNull List<T> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		ListenableList<?> that = (ListenableList<?>) o;
		return Objects.equals(internal, that.internal);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(internal);
	}
}
