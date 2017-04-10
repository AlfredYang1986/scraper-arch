package excel.core

import java.io.FileInputStream

import excel.data.dianping_select_row
import akka.actor.ActorRef
import play.api.libs.json.{JsValue, Json}
import play.api.libs.json.Json.parse
import java.util.Date

import exchange.dianping_shops

trait rowinteractparser extends interactparser {
	override def handleOneTarget(target : target_type) = a ! target
}

case class dianping_data_parse(xml_file_name : String, xml_file_name_ch : String, a : ActorRef) extends rowinteractparser {
	type target_type = dianping_select_row
	override def targetInstance = new dianping_select_row
	override def handleOneTarget(target : target_type) = {
		if (!(target.getShop == null || target.getShop.isEmpty)) {
			last_shop_name = Some(target.getShop)

			shops.find { x =>
				(x \ "name").asOpt[String].map (y => y == last_shop_name.get).getOrElse(false)
			} match {
				case None => Unit
				case Some(shop) => dianping_shops.pushShop(shop)
			}
		}


		if (!(target.getCourse == null || target.getShop.isEmpty)) {

		}

		println(target.getCourse)
	}

	var last_shop_name : Option[String] = None

	// get result json file
	val shops =  Json.parse(new FileInputStream("""output/dianping-origin.json""")).asOpt[List[JsValue]].map (x => x).getOrElse(Nil)
}