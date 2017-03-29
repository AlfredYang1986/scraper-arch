
import play.api.libs.json.{JsValue, Json}
import java.io.FileInputStream

import scraper.scraper_node
import sketch.sketch

object scarper_main extends App {
	val sk = Json.parse(new FileInputStream("src/main/resources/sketch.json")).asOpt[List[JsValue]].map (x => x).getOrElse(Nil)
	println(s"Json is ${sk}")
	val lst = sketch(sk.head)
	val test = scraper_node(lst.head).process

	println(s"test result is $test")
}