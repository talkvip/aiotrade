package org.aiotrade.lib.math.random

import org.aiotrade.lib.math.BitString

object BinaryUtils {
  // Mask for casting a byte to an int, bit-by-bit (with
  // bitwise AND) with no special consideration for the sign bit.
  private val BITWISE_BYTE_TO_INT = 0x000000FF
    
  private val HEX_CHARS = Array('0', '1', '2', '3', '4', '5', '6', '7',
                                '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
  /**
   * Converts an array of bytes in to a String of hexadecimal characters (0 - F).
   * @param data An array of bytes to convert to a String.
   * @return A hexadecimal String representation of the data.
   */
  def convertBytesToHexString(data: Array[Byte]): String = {
    val buffer = new StringBuilder(data.length * 2)
    var i = 0
    while (i < data.length) {
      val b = data(i) 
      buffer.append(HEX_CHARS((b >>> 4) & 0x0F))
      buffer.append(HEX_CHARS(b & 0x0F))
      i += 1
    }
    buffer.toString
  }

  /**
   * Converts a hexadecimal String (such as one generated by the
   * {@link #convertBytesToHexString(Array[Byte])} method) into an array of bytes.
   * @param hex The hexadecimal String to be converted into an array of bytes.
   * @return An array of bytes that.
   */
  def convertHexStringToBytes(hex: String): Array[Byte] = {
    if (hex.length % 2 != 0) {
      throw new IllegalArgumentException("Hex string must have even number of characters.")
    }
    
    var seed = new Array[Byte](hex.length / 2)
    var i = 0
    while (i < seed.length) {
      val index = i * 2
      seed(i) = java.lang.Integer.parseInt(hex.substring(index, index + 2), 16).toByte
      i += 1
    }
    seed
  }
    


  /**
   * Take four bytes from the specified position in the specified
   * block and convert them into a 32-bit int, using the big-endian
   * convention.
   * @param bytes The data to read from.
   * @param offset The position to start reading the 4-byte int from.
   * @return The 32-bit integer represented by the four bytes.
   */
  def convertBytesToInt(bytes: Array[Byte], offset: Int): Int =  {
    (BITWISE_BYTE_TO_INT & bytes(offset + 3)) | 
    ((BITWISE_BYTE_TO_INT & bytes(offset + 2)) << 8) |
    ((BITWISE_BYTE_TO_INT & bytes(offset + 1)) << 16) |
    ((BITWISE_BYTE_TO_INT & bytes(offset)) << 24)
  }


  /**
   * Convert an array of bytes into an array of ints.  4 bytes from the
   * input data map to a single int in the output data.
   * @param bytes The data to read from.
   * @return An array of 32-bit integers constructed from the data.
   * @since 1.1
   */
  def convertBytesToInts(bytes: Array[Byte]): Array[Int] = {
    if (bytes.length % 4 != 0) {
      throw new IllegalArgumentException("Number of input bytes must be a multiple of 4.")
    }
    
    val ints = new Array[Int](bytes.length / 4)
    var i = 0
    while (i < ints.length) {
      ints(i) = convertBytesToInt(bytes, i * 4)
      i += 1
    }
    ints
  }


  /**
   * Utility method to convert an array of bytes into a long.  Byte ordered is
   * assumed to be big-endian.
   * @param bytes The data to read from.
   * @param offset The position to start reading the 8-byte long from.
   * @return The 64-bit integer represented by the eight bytes.
   * @since 1.1
   */
  def convertBytesToLong(bytes: Array[Byte], offset: Int): Long = {
    var value = 0
    var i = offset
    while (i < offset + 8) {
      val b = bytes(i)
      value <<= 8
      value += b
      i += 1
    }
    value
  }


  /**
   * Converts a floating point value in the range 0 - 1 into a fixed
   * point bit string (where the most significant bit has a value of 0.5).
   * @param value The value to convert (must be between zero and one).
   * @return A bit string representing the value in fixed-point format.
   */
  def convertDoubleToFixedPointBits(value: Double): BitString = {
    if (value < 0.0d || value >= 1.0d) {
      throw new IllegalArgumentException("Value must be between 0 and 1.")
    }
    
    val bits = new StringBuilder(64)
    var bitValue = 0.5
    var d = value
    while (d > 0) {
      if (d >= bitValue) {
        bits.append('1')
        d -= bitValue
      } else {
        bits.append('0')
      }
      bitValue /= 2
    }
    BitString(bits.toString)
  }
}
