package utils

import java.math.BigInteger

/**
 * Created by Alper on 07.04.2015.
 */
object HashFactory {

  final val HASH_LONG: Int = 32

  def hash(text: String): String = {
    var digest: java.security.MessageDigest = java.security.MessageDigest.getInstance("MD5")
    digest.update(text.getBytes(), 0, text.length)
    var md5 = new BigInteger(1, digest.digest()).toString(16)
    if (md5.length != HASH_LONG) {
      md5 = padLeft(md5, "0", HASH_LONG)
    }
    md5
  }

  def padLeft(str: String, padding: String, size: Int): String = {
    var shortBy: Int = size - str.length
    var buff: StringBuilder = new StringBuilder()
    for (i <- 0 to shortBy) {
      buff.append(padding)
    }
    buff.append(str)
    buff.toString()
  }

}