
import play.api.libs.json.{JsValue, Json}
import java.io.FileInputStream

import akka.actor.ActorSystem
import dispatch.scraper_dispatch
import dispatch.signal.dis_signal._

import scala.io.StdIn

object scarper_main extends App {
//	val sk = Json.parse(new FileInputStream("src/main/resources/sketch.json")).asOpt[List[JsValue]].map (x => x).getOrElse(Nil)
//	println(s"Json is ${sk}")
//	val lst = sketch(sk.head)
//	val test = scraper_node(lst).process
//
//	println(s"test result is $test")

	override def main(args: Array[String]): Unit = {
		print(
			"""scraper options:
		  |000. scraper for dianping (大众点评)
		  |001. scraper for other (以后添加其他)
		  |002. scraper test
		  |003. scraper for all
		  |004. merge existing result
		""".stripMargin)

		val str = StdIn.readLine()
		val op = str.toInt

		val path = "src/main/resources/"
		val lst = "dianping-sketch.json" :: "other.json" :: "test.json" :: Nil

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