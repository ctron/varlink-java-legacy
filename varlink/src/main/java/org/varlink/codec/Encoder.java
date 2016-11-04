package org.varlink.codec;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
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

	private ByteBuffer buffer;
	private final IntFunction<ByteBuffer> allocator;

	public Encoder() {
		this.buffer = ByteBuffer.allocate(1);
		this.buffer.order(ByteOrder.LITTLE_ENDIAN);
		this.allocator = this.buffer.isDirect() ? ByteBuffer::allocateDirect : ByteBuffer::allocate;
	}

	public void encodeInt64(final long value) {
		ensureMore(8);
		this.buffer.putLong(value);
	}

	public void encodeString(final String value) {
		final byte[] data = value.getBytes(StandardCharsets.UTF_8);
		ensureMore(data.length + 1);
		this.buffer.put(data);
		this.buffer.put((byte) 0);
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
		final ByteBuffer result = this.buffer.asReadOnlyBuffer();
		result.flip();
		return result;
	}
}
