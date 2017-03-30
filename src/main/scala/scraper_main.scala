
import play.api.libs.json.{JsValue, Json}
import java.io.FileInputStream

import akka.actor.ActorSystem
import dispatch.scraper_dispatch
import dispatch.signal.dis_signal._

object scarper_main extends App {
//	val sk = Json.parse(new FileInputStream("src/main/resources/sketch.json")).asOpt[List[JsValue]].map (x => x).getOrElse(Nil)
//	println(s"Json is ${sk}")
//	val lst = sketch(sk.head)
//	val test = scraper_node(lst).process
//
//	println(s"test result is $test")

	val f = "src/main/resources/sketch.json"
	val sys = ActorSystem("scraper")
	val s = sys.actorOf(scraper_dispatch.props)
	s ! start(f)
}