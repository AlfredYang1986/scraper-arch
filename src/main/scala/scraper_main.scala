
import play.api.libs.json.{JsValue, Json}
import java.io.FileInputStream

import akka.actor.ActorSystem
import dispatch.scraper_dispatch
import dispatch.signal.dis_signal._

import scala.io.StdIn

object scarper_main extends App {
	def actions(a : List[String]) : (Int, Option[String]) = a match {
			case Nil => {

				(-1, None)
			}
			case head :: tail => {
				head match {
					case "scraper" => (1, filePath(tail))
					case "merge" => (0, None)
					case _ => (-1, None)
				}
			}
		}

<<<<<<< Updated upstream
	def filePath(a : List[String]) : Option[String] = a match {
			case Nil => None
			case head :: _ => Some(head)
		}
=======
		// val str = StdIn.readLine()
		// val op = str.toInt
		val op = 4
>>>>>>> Stashed changes

	override def main(args: Array[String]): Unit = {
		val path = "src/main/resources/"
		val sys = ActorSystem("scraper")
		val s = sys.actorOf(scraper_dispatch.props)
		val (act, f_opt) = actions(args.toList)
		if (act < 0) {
			println(
				"""
				  | you have to input option args
				  |     scraper: crawl the web
				  |     merge: merge existing data in data directory
				""".stripMargin)
			System.exit(-1)
		} else if (act == 0) {
			println("only merge result")
			s ! merge_result()
		} else {
			f_opt match {
				case None => println("should have file path")
				case Some(file) => {
					val f = path + file
					println(s"json file is $f")
					s ! start(f)
				}
			}
		}
	}
}