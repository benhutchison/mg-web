import java.io.Reader

package object mg {

  implicit def ReaderOps(value: Reader) = new ReaderOps(value)
}
class ReaderOps(val reader: Reader) extends AnyVal {

  /** Read the reader into a string, unless the size limit is exceeded,
    * in which case IllegalArgumentException.
    *
    * Closes the reader when finished.*/
  def readAll(limit: Int = 1000000): String = {
    try {
      val builder = new java.lang.StringBuilder()
      val cbuf = new Array[Char](1000)
      var numChars = 0
      var totalChars = 0
      while ({numChars = reader.read(cbuf); numChars} >= 0) {
        builder.append(cbuf, 0, numChars)
        totalChars += numChars
        if (totalChars > limit)
          throw new IllegalArgumentException(s"Limit of $limit chars exceeded while reading: $reader")
      }
      builder.toString()
    } finally {reader.close()}
  }
}

