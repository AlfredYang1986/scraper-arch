package http

import java.io.{BufferedInputStream, ByteArrayOutputStream, InputStream}
import java.net.URL
import java.io.FileOutputStream

/**
  * Created by Alfred on 12/04/2017.
  */

object Download {
	def apply(u : String) : httpDownloadOpt = new httpDownloadOpt(u)
}

case class httpDownloadOpt(val u : String) {

	def apply(file : String) = {
		val url = new URL(u)
		val in = new BufferedInputStream(url.openStream())
		val out = new ByteArrayOutputStream()
		val buf = new Array[Byte](1024)

		var n = 0
		do {
			n = in.read(buf)
			if (n > 0)
				out.write(buf, 0, n)

		} while (n != -1)
		out.close()
		in.close()

		val response = out.toByteArray()

		val fos = new FileOutputStream(file)
		fos.write(response)
		fos.flush()
		fos.close()
	}
}
