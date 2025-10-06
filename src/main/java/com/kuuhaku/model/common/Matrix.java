package com.kuuhaku.model.common;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;

public class Matrix<T> implements Iterable<Matrix.MatrixView<T>> {
	public record MatrixView<T>(Matrix<T> parent, T element, int x, int y) {
		public boolean isValidPos() {
			boolean isInsideVert = y >= 0 && y < parent.height;
			boolean isInsideHori = x >= 0 && x < parent.width;
			return isInsideVert && isInsideHori;
		}

		public MatrixView<T> peek(int x, int y) {
			boolean isInsideVert = y >= 0 && y < parent.height;
			boolean isInsideHori = x >= 0 && x < parent.width;

			if (!isInsideVert || !isInsideHori) {
				return new MatrixView<>(parent, null, x, y);
			}

			return new MatrixView<>(parent, parent.matrix[y * parent.width + x], x, y);
		}

		@SuppressWarnings("unchecked")
		public MatrixView<T>[][] expand(int size) {
			MatrixView<T>[][] view = new MatrixView[size][size];

			for (int vy = 0; vy < size; vy++) {
				MatrixView<T>[] row = view[vy];
				for (int vx = 0; vx < size; vx++) {
					int dx = y + (-1 + vy);
					int dy = x + (-1 + vx);
					row[vx] = new MatrixView<>(parent, peek(dx, dy).element, dx, dy);
				}
			}

			return view;
		}

		public MatrixView<T> replace(T value) {
			if (!isValidPos()) return this;

			return new MatrixView<>(parent, parent.matrix[y * parent.width + x] = value, x, y);
		}
	}

	protected final T[] matrix;
	private final int height;
	private final int width;

	@SuppressWarnings("unchecked")
	public Matrix(T[][] matrix) {
		this.matrix = (T[]) Arrays.stream(matrix).flatMap(Arrays::stream).toArray();
		this.height = matrix.length;
		this.width = matrix[0].length;
	}

	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	@Override
	public @NotNull Iterator<MatrixView<T>> iterator() {
		return new Iterator<>() {
			private int i = -1;

			@Override
			public boolean hasNext() {
				return i < matrix.length;
			}

			@Override
			public MatrixView<T> next() {
				i++;
				return new MatrixView<>(Matrix.this, matrix[i], i / width, i % width);
			}
		};
	}
}
