package org.varlink.codec;

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

public class CodecTest {

	@Test
	public void testString1() {
		final Encoder encoder = new Encoder();
		encoder.encodeString("foo");
		final ByteBuffer result = encoder.close();

		assertByteBuffer(new byte[] { 0x66, 0x6f, 0x6f, 0x00 }, result);
	}

	@Test
	public void testLong1() {
		final Encoder encoder = new Encoder();
		encoder.encodeInt64(42);
		final ByteBuffer result = encoder.close();

		assertByteBuffer(new byte[] { 0x2a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }, result);
	}

	private static void assertByteBuffer(final byte[] expected, final ByteBuffer actual) {
		final byte[] data = new byte[actual.remaining()];
		actual.get(data);
		Assert.assertArrayEquals(expected, data);
	}
}
