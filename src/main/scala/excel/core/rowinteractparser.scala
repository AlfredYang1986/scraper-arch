package excel.core

import java.io.FileInputStream

import excel.data.dianping_select_row
import akka.actor.ActorRef
import play.api.libs.json.{JsValue, Json}
import play.api.libs.json.Json.parse
import java.util.Date

import exchange.{dainping_service, dianping_shops}

trait rowinteractparser extends interactparser {
	override def handleOneTarget(target : target_type) = a ! target
}

case class dianping_data_parse(xml_file_name : String, xml_file_name_ch : String, a : ActorRef) extends rowinteractparser {
	type target_type = dianping_select_row
	override def targetInstance = new dianping_select_row
	override def handleOneTarget(target : target_type) = {
		handleShop(target)
		handleCourse(target)
	}

	var last_shop_name : Option[String] = None

	// get result json file
	val shops =  Json.parse(new FileInputStream("""output/dianping-origin.json""")).asOpt[List[JsValue]].map (x => x).getOrElse(Nil)
	val courses =  Json.parse(new FileInputStream("""output/dianping-class.json""")).asOpt[List[JsValue]].map (x => x).getOrElse(Nil)
	val sales =  Json.parse(new FileInputStream("""output/dianping-sales.json""")).asOpt[List[JsValue]].map (x => x).getOrElse(Nil)

	def handleShop(target : target_type) = {
		if (!(target.getShop == null || target.getShop.isEmpty)) {
			last_shop_name = Some(target.getShop)

			shops.find { x =>
				(x \ "name").asOpt[String].map (y => y == last_shop_name.get).getOrElse(false)
			} match {
				case None => Unit
				case Some(shop) => dianping_shops.pushShop(shop, dianping_shops.downloadShopImg(shop))
			}
		}
	}

//	var handle_course = 0
	def handleCourse(target : target_type) = {
		if (!(target.getCourse == null || target.getCourse.isEmpty)) {
//			handle_course = handle_course + 1
//			println(s"$handle_course is ${target.getCourse()}")
			courses.find { x =>
				(x \ "name").asOpt[String].map (y => y == target.getCourse).getOrElse(false)
			} match {
				case None => handleSales(target)
				case Some(c) => dainping_service.pushServices(c, dainping_service.downloadServiceImages(c))
			}
		}
	}

	def handleSales(target : target_type) = {
		if (!(target.getCourse == null || target.getCourse.isEmpty)) {
			courses.find { x =>
				(x \ "name").asOpt[String].map (y => y == target.getCourse).getOrElse(false)
			} match {
				case None => Unit
				case Some(c) => println(s"sales is $c")
			}
		}
	}
}