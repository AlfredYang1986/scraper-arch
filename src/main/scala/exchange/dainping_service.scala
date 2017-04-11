package exchange

import java.util.Date

import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import sercurity.Sercurity

/**
  * Created by Alfred on 11/04/2017.
  */
object dainping_service {
	var kidnaps : List[JsValue] = Nil

	def pushServices(s : JsValue) = {
		val shop_name = (s \ "shop_name").asOpt[String].map(x => x).getOrElse("")

		val shop_user_id = dianping_shops.queryShopsUserId(shop_name)

		// TODO: cans_cat
		val tmp =
			toJson(Map("service_id" -> toJson(Sercurity.md5Hash(shop_name + Sercurity.getTimeSpanWithMillSeconds)),
				"owner_id" -> toJson(shop_user_id),
				"title" -> toJson((s \ "name").asOpt[String].map (x => x).getOrElse("")),
				"description" -> toJson((s \ "course_description").asOpt[String].map (x => x + "\n").getOrElse("")
										+ (s \ "course_teacher").asOpt[String].map (x => x).getOrElse("")),
				"price" -> toJson((s \ "price").asOpt[Double].map (x => x).getOrElse(0.0)),
				"status" -> toJson(1), "rate" -> toJson(0), "cans_cat" -> toJson(2),
				"cans" -> toJson(-1), "facility" -> toJson(30), "distinct" -> toJson("北京市"),
				"address" -> toJson((s \ "address").asOpt[String].map (x => x).getOrElse("")),
				"least_hours" -> toJson(1), "allow_leave" -> toJson(0), "service_cat" -> toJson(0),
				"lecture_length" -> toJson(1), "servant_no" -> toJson(2), "capacity" -> toJson(6),
				"other_words" -> toJson((s \ "course_schedule").asOpt[String].map (x => x + "\n").getOrElse("")
										+ (s \ "course_times").asOpt[String].map (x => x + "\n").getOrElse("")
										+ (s \ "course_length").asOpt[String].map (x => x + "\n").getOrElse("")),
				"reserve1" -> toJson(""), "date" -> toJson(new Date().getTime)))
		kidnaps = tmp :: kidnaps
	}

//	def queryShopsUserId(shop_name : String) : Option[String] = None
}
