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

	@Test
	public void testStruct1() {
		final Encoder encoder = new Encoder();

		encoder.encodeString("foo");
		encoder.encodeBoolean(true);
		encoder.encodeInt64(42);

		final ByteBuffer result = encoder.close();

		assertByteBuffer(new byte[] { //
				0x66, 0x6f, 0x6f, 0x00, // "foo"
				0x01, // true = 0x01
				0x00, 0x00, 0x00, // 3x padding
				0x2a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // the answer
				0x04 // offset
		}, result);
	}

	@Test
	public void testStruct2() {

		final Encoder encoder = new Encoder();

		encoder.encodeBoolean(true);
		encoder.encodeString("foo");
		encoder.encodeUnsignedInt8(42);
		encoder.encodeString("");
		encoder.encodeInt32(-42);
		encoder.encodeString("foobar");

		final ByteBuffer result = encoder.close();

		assertByteBuffer(new byte[] { //
				0x01, // true = 0x01
				0x66, 0x6f, 0x6f, 0x00, // "foo"
				0x2a, // the answer = uint8_t
				0x00, // ""
				0x00, // 1x padding
				// the negative answer = int32_t
				(byte) 0xd6, (byte) 0xff, (byte) 0xff, (byte) 0xff, //
				0x66, 0x6f, 0x6f, 0x62, 0x61, 0x72, 0x00, // "foobar"
				0x07, 0x05 // offset
		}, result);
	}

	private static void assertByteBuffer(final byte[] expected, final ByteBuffer actual) {
		final byte[] data = new byte[actual.remaining()];
		actual.get(data);
		Assert.assertArrayEquals(expected, data);
	}
}
