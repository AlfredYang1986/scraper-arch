package exchange

import java.util.Date

import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import sercurity.Sercurity

/**
  * Created by Alfred on 10/04/2017.
  */
object dianping_shops {
	var shops : List[JsValue] = Nil

	def pushShop(shop : JsValue) = {
		val shop_name = (shop \ "name").asOpt[String].map(x => x).getOrElse("")
		val tmp =
		toJson(Map("user_id" -> toJson(Sercurity.md5Hash(shop_name + Sercurity.getTimeSpanWithMillSeconds)), "screen_name" -> toJson(shop_name),
			"screen_photo" -> toJson(""), "isLogin" -> toJson(1), "gender" -> toJson(1), "school" -> toJson(""),
			"company" -> toJson((shop \ "name").asOpt[String].map (x => x).getOrElse("")),
			"occupation" -> toJson(""), "personal_description" -> toJson(""), "is_service_provider" -> toJson(1),
			"address" -> toJson((shop \ "address").asOpt[String].map (x => x).getOrElse("")),
			"date" -> toJson(new Date().getTime), "dob" -> toJson(0), "about" -> toJson(""),
			"contact_no" -> toJson((shop \ "phone").asOpt[String].map (x => x).getOrElse(""))))
		shops = tmp :: shops
	}

	def queryShopsUserId(shop_name : String) : Option[String] = None
}
