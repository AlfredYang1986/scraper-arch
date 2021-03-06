
import java.io.FileInputStream

import dispatch.scraper_dispatch
import dispatch.signal.dis_signal._
import excel.core.dianping_data_parse
import akka.actor.ActorSystem
import exchange.{dainping_service, dianping_shops}

import scala.io.StdIn

object scarper_main extends App {
	def actions(a : List[String]) : (Int, Option[String]) = a match {
			case Nil => (-1, None)
			case head :: tail => head match {
					case "scraper" => (1, filePath(tail))
					case "merge" => (0, None)
					case "exchange" => (2, filePath(tail))
					case "online" => (3, None)
					case "modify" => (4, None)
					case "schedule" => (5, None)
					case _ => (-1, None)
				}
		}

	def filePath(a : List[String]) : Option[String] = a match {
			case Nil => None
			case head :: _ => Some(head)
		}

	def printUsage = {
		println(
			"""
			  | you have to input option args
			  |     scraper: crawl the web
			  |     merge: merge existing data in data directory
			""".stripMargin)
		System.exit(-1)
	}

	override def main(args: Array[String]): Unit = {
		val path = "src/main/resources/"
		val sys = ActorSystem("scraper")
		val s = sys.actorOf(scraper_dispatch.props)
		val (act, f_opt) = actions(args.toList)
		if (act < 0) {
			printUsage
		} else if (act == 0) {
			println("only merge result")
			s ! merge_result()
		} else if (act == 1) {
			f_opt match {
				case None => println("should have file path")
				case Some(file) => {
					val f = path + file
					println(s"json file is $f")
					s ! start(f)
				}
			}
		} else if (act == 2) {
			println("exchange to database")

			f_opt match {
				case None => println("should have file path")
				case Some(file) => {
					val f = path + file
					println(s"excel file is $f")
					dianping_data_parse("""config/FieldNamesDataStruct.xml""", """config/xmlDataStruct.xml""", null).startParse(f, 1)
					println(s"dianping shops count: ${dianping_shops.shops.length}")
					println(s"dianping services count: ${dainping_service.kidnaps.length}")

					dianping_shops.store2DB
					dainping_service.store2DB
					println(s"dianping save to DB End")
				}
			}
		} else if (act == 3) {
			dainping_service.onlineAllService
			println(s"dianping online service end")
		} else if (act == 4) {
			dianping_shops.adjustDescriptionData
//			dainping_service.adjustAddressData
//			dainping_service.adjustData
//			dainping_service.adjustPriceData
			println(s"dianping adjust user & service end")
		} else if (act == 5) {
			dainping_service.adjustTimeManagement
		} else {
			printUsage
		}
	}
}