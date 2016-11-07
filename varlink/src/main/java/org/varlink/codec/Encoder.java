package org.varlink.codec;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.function.IntFunction;

/**
 * A simple varlink encoder
 * <p>
 * <strong>Note:</strong> This class is not thread safe
 * </p>
 *
 * @author jreimann
 */
public final class Encoder {

	private static final byte BOOLEAN_FALSE = (byte) 0x00;
	private static final byte BOOLEAN_TRUE = (byte) 0x01;

	private static enum OffsetSize {
		SIZE_1(1) {
			@Override
			public void encodeOffset(final ByteBuffer buffer, final Integer value) {
				buffer.put(value.byteValue());
			}

			@Override
			public OffsetSize ensureSize(final long size) {
				if (size <= 0xFF) {
					return this;
				}
				return SIZE_2.ensureSize(size);
			}
		},
		SIZE_2(2) {
			@Override
			public void encodeOffset(final ByteBuffer buffer, final Integer value) {
				buffer.putShort(value.shortValue());
			}

			@Override
			public OffsetSize ensureSize(final long size) {
				if (size <= 0xFFFF) {
					return this;
				}
				return SIZE_4.ensureSize(size);
			}
		},
		SIZE_4(4) {
			@Override
			public void encodeOffset(final ByteBuffer buffer, final Integer value) {
				buffer.putInt(value.intValue());
			}

			@Override
			public OffsetSize ensureSize(final long size) {
				if (size <= 0xFFFFFFFF) {
					return this;
				}
				return SIZE_8.ensureSize(size);
			}
		},
		SIZE_8(8) {
			@Override
			public void encodeOffset(final ByteBuffer buffer, final Integer value) {
				buffer.putLong(value.longValue()); // yes ... I know!
			}

			@Override
			public OffsetSize ensureSize(final long size) {
				if (size <= Long.MAX_VALUE) {
					return this;
				}

				throw new IllegalStateException("Unable to record offset at that size");
			}
		};

		public abstract void encodeOffset(ByteBuffer buffer, Integer value);

		public abstract OffsetSize ensureSize(long size);

		private final int size;

		private OffsetSize(final int size) {
			this.size = size;
		}

		public int getSize() {
			return this.size;
		}
	}

	private ByteBuffer buffer;
	private final IntFunction<ByteBuffer> allocator;
	private final LinkedList<Integer> offsets = new LinkedList<>();
	private boolean lastWasVariable;

	public Encoder() {
		this.buffer = ByteBuffer.allocate(1);
		this.buffer.order(ByteOrder.LITTLE_ENDIAN);
		this.allocator = this.buffer.isDirect() ? ByteBuffer::allocateDirect : ByteBuffer::allocate;
	}

	public void encodeBoolean(final boolean value) {
		ensureMore(1);
		this.buffer.put(value ? BOOLEAN_TRUE : BOOLEAN_FALSE);
		recordFixed();
	}

	public void encodeUnsignedInt8(final int value) {
		checkRange(0, 0xFF, value);

		ensureMore(1);
		this.buffer.put((byte) (value & 0xFF));
		recordFixed();
	}

	private void checkRange(final int min, final int max, final int value) {
		if (value < min || value > max) {
			throw new IllegalArgumentException(
					String.format("Value out of range - min: %s, max: %s, value: %s", min, max, value));
		}
	}

	public void encodeInt8(final int value) {
		checkRange(Byte.MIN_VALUE, Byte.MAX_VALUE, value);
		encodeInt8((byte) value);
	}

	public void encodeInt8(final byte value) {
		ensureMore(1);
		this.buffer.put(value);
		recordFixed();
	}

	public void encodeInt16(final int value) {
		checkRange(Short.MIN_VALUE, Short.MAX_VALUE, value);
		encodeInt16((short) value);
	}

	public void encodeInt16(final short value) {
		align(2);
		ensureMore(2);
		this.buffer.putShort(value);
		recordFixed();

	}

	public void encodeInt32(final int value) {
		align(4);
		ensureMore(4);
		this.buffer.putInt(value);
		recordFixed();
	}

	public void encodeInt64(final long value) {
		align(8);
		ensureMore(8);
		this.buffer.putLong(value);
		recordFixed();
	}

	public void encodeString(final String value) {
		final byte[] data = value.getBytes(StandardCharsets.UTF_8);
		ensureMore(data.length + 1);
		this.buffer.put(data);
		this.buffer.put((byte) 0);

		recordVariable();
	}

	private void recordFixed() {
		this.lastWasVariable = false;
	}

	private void recordVariable() {
		this.lastWasVariable = true;
		final int pos = this.buffer.position(); // already at next position
		this.offsets.add(pos);
	}

	/**
	 * Align current buffer position to a multiplier of 8
	 */
	private void align(final int alignment) {
		final int pos = this.buffer.position();
		if (pos % alignment != 0) {
			final int padding = alignment - pos % alignment;
			ensureMore(padding);
			this.buffer.position(this.buffer.position() + padding);
		}
	}

	private void ensureMore(final int bytes) {
		if (this.buffer.remaining() > bytes) {
			return;
		}

		final ByteBuffer newBuffer = this.allocator.apply(nextCapacity(this.buffer, bytes));
		newBuffer.order(ByteOrder.LITTLE_ENDIAN);

		// flip first, we are reading now
		this.buffer.flip();
		// copy current data
		newBuffer.put(this.buffer);
		// assign new buffer
		this.buffer = newBuffer;
	}

	private static int nextCapacity(final ByteBuffer buffer, final int bytes) {
		return buffer.capacity() + bytes; // FIXME: should be block based
	}

	public ByteBuffer close() {

		// don't encode last offsets

		if (this.lastWasVariable) {
			this.offsets.removeLast();
		}

		// encode offsets

		if (!this.offsets.isEmpty()) {

			// eval offset size

			OffsetSize offsetSize = OffsetSize.SIZE_1;

			for (final Integer offset : this.offsets) {
				offsetSize = offsetSize.ensureSize(offset);
			}

			final int space = this.offsets.size() * offsetSize.getSize();
			ensureMore(space);
			while (!this.offsets.isEmpty()) {
				offsetSize.encodeOffset(this.buffer, this.offsets.pollLast());
			}
		}

		final ByteBuffer result = this.buffer.asReadOnlyBuffer();
		result.flip();
		return result;
	}
}
