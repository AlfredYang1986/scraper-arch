
import play.api.libs.json.{JsValue, Json}
import java.io.FileInputStream

import akka.actor.ActorSystem
import dispatch.scraper_dispatch
import dispatch.signal.dis_signal._

import scala.io.StdIn

object scarper_main extends App {
	override def main(args: Array[String]): Unit = {
		print(
			"""scraper options:
		  |000. scraper for dianping (大众点评)
		  |001. scraper for weiyi (为艺)
		  |002. scraper for victory (决胜网)
		  |003. scraper for other (以后添加其他)
		  |004. scraper test
		  |005. scraper for all
		  |006. merge existing result
		""".stripMargin)

		val str = StdIn.readLine()
		val op = str.toInt

		val path = "src/main/resources/"
		val lst = "dianping-sketch.json" :: "weiyi-sketch.json" :: "webvictory.json" :: "other.json" :: "test.json" :: Nil

		val sys = ActorSystem("scraper")
		val s = sys.actorOf(scraper_dispatch.props)

		if (op == lst.length) println("all is not implement")
		else if (op > lst.length) {
			println("only merge result")
			s ! merge_result()
		}
		else {
			val f = path + lst(op)
			println(s"json file is $f")
			s ! start(f)
		}
	}
}