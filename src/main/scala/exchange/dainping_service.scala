package exchange

import java.util.{Date, UUID}

import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import sercurity.Sercurity
import com.mongodb.casbah.Imports._
import http.Download
import util.dao.{_data_connection, from}

/**
  * Created by Alfred on 11/04/2017.
  */
object dainping_service {
	var kidnaps : List[JsValue] = Nil

	def downloadServiceImages(s : JsValue) : List[String] = {
		// TODO :
		//      3. cans and service_cat
		val container = (s \ "images").asOpt[List[JsValue]].map (x => x).getOrElse(Nil)
		val images = container.map { x =>
			(x \ "image").asOpt[List[String]].map(x => x).getOrElse(Nil)
		}.flatten

		val imgs = images map { iter =>
			val str = UUID.randomUUID().toString
			Download(iter)("""upload/""" + str)
			str
		}
		//		println(imgs)
		imgs
	}

	def pushServices(s : JsValue, imgs: List[String]) = {
		val shop_name = (s \ "shop_name").asOpt[String].map(x => x).getOrElse("")
		val name = (s \ "name").asOpt[String].map(x => x).getOrElse("")

		val shop_user_id = dianping_shops.queryShopsUserId(shop_name)
		val address = dianping_shops.queryShopsAddressWithName(shop_name)

		if (shop_user_id.isEmpty) {
			println(s"should have $shop_name on name: $name")
		}

		val tmp =
			toJson(Map("service_id" -> toJson(Sercurity.md5Hash(name + Sercurity.getSercuritySeed)),
				"owner_id" -> toJson(shop_user_id),
				"title" -> toJson((s \ "name").asOpt[String].map (x => x).getOrElse("")),
				"images" -> toJson(imgs),
				"description" -> toJson((s \ "course_description").asOpt[String].map (x => x + "\n").getOrElse("")
										+ (s \ "course_teacher").asOpt[String].map (x => x).getOrElse("")),
				"price" -> toJson((s \ "price").asOpt[Double].map (x => x).getOrElse(0.0)),
				"status" -> toJson(2), "rate" -> toJson(0), "cans_cat" -> toJson(2),
				"cans" -> toJson(-1), "facility" -> toJson(30), "distinct" -> toJson("北京市"),
				"address" -> toJson(address), "least_hours" -> toJson(1), "allow_leave" -> toJson(0), "service_cat" -> toJson(0),
				"lecture_length" -> toJson(1), "servant_no" -> toJson(2), "capacity" -> toJson(6),
				"other_words" -> toJson((s \ "course_schedule").asOpt[String].map (x => x + "\n").getOrElse("")
										+ (s \ "course_times").asOpt[String].map (x => x + "\n").getOrElse("")
										+ (s \ "course_length").asOpt[String].map (x => x + "\n").getOrElse("")),
				"reserve1" -> toJson(""), "date" -> toJson(new Date().getTime)))
		kidnaps = tmp :: kidnaps
	}

	def push2DB(data : JsValue) = {
		val owner_id = (data \ "owner_id").asOpt[String].map (x => x).getOrElse(throw new Exception("unknown user"))
		val service_id = (data \ "service_id").asOpt[String].map (x => x).getOrElse("")

		if (!owner_id.isEmpty) {

			val service_builder = MongoDBObject.newBuilder
			service_builder += "service_id" -> service_id
			service_builder += "owner_id" -> owner_id

			val location = MongoDBObject.newBuilder
			(data \ "location").asOpt[JsValue].map { loc =>
				location += "latitude" -> (loc \ "latitude").asOpt[Float].map(tmp => tmp).getOrElse(0.floatValue)
				location += "longtitude" -> (loc \ "longtitude").asOpt[Float].map(tmp => tmp).getOrElse(0.floatValue)
			}.getOrElse {
				location += "latitude" -> 0.floatValue
				location += "longtitude" -> 0.floatValue
			}
			service_builder += "location" -> location.result

			service_builder += "comments" -> MongoDBList.newBuilder.result
			(data \ "title").asOpt[String].map(tmp => service_builder += "title" -> tmp).getOrElse(throw new Exception("wrong input"))
			(data \ "description").asOpt[String].map(tmp => service_builder += "description" -> tmp).getOrElse(service_builder += "description" -> "")
			(data \ "capacity").asOpt[Int].map(tmp => service_builder += "capacity" -> tmp).getOrElse(service_builder += "capacity" -> 0.intValue)
			(data \ "price").asOpt[Float].map(tmp => service_builder += "price" -> tmp).getOrElse(service_builder += "price" -> 0.floatValue)

			service_builder += "status" -> 2
			service_builder += "rate" -> 0.floatValue

			(data \ "cans_cat").asOpt[Long].map(x => service_builder += "cans_cat" -> x.asInstanceOf[Number]).getOrElse(service_builder += "cans_cat" -> -1.longValue)
			(data \ "cans").asOpt[Long].map(cans => service_builder += "cans" -> cans.asInstanceOf[Number]).getOrElse(service_builder += "cans" -> -1.longValue)
			(data \ "facility").asOpt[Long].map(cans => service_builder += "facility" -> cans.asInstanceOf[Number]).getOrElse(service_builder += "facility" -> 0.intValue)

			(data \ "images").asOpt[List[String]].map { lst =>
				service_builder += "images" -> lst
			}.getOrElse(service_builder += "images" -> MongoDBList.newBuilder.result)

			service_builder += "distinct" -> (data \ "distinct").asOpt[String].map(x => x).getOrElse("")
			service_builder += "address" -> (data \ "address").asOpt[String].map(x => x).getOrElse("")
			service_builder += "adjust_address" -> (data \ "adjust_address").asOpt[String].map(x => x).getOrElse("")

			val age_boundary = MongoDBObject.newBuilder
			(data \ "age_boundary").asOpt[JsValue].map { boundary =>
				age_boundary += "lsl" -> (boundary \ "lsl").asOpt[Int].map(x => x).getOrElse(3)
				age_boundary += "usl" -> (boundary \ "usl").asOpt[Int].map(x => x).getOrElse(11)
			}.getOrElse {
				age_boundary += "lsl" -> 3.intValue
				age_boundary += "usl" -> 11.intValue
			}
			service_builder += "age_boundary" -> age_boundary.result

			service_builder += "least_hours" -> (data \ "least_hours").asOpt[Int].map(x => x).getOrElse(0)
			service_builder += "allow_leave" -> (data \ "allow_leave").asOpt[Int].map(x => x).getOrElse(0)
			service_builder += "service_cat" -> (data \ "service_cat").asOpt[Int].map(x => x).getOrElse(0)

			/** ************************************************************/
			service_builder += "least_times" -> (data \ "least_times").asOpt[Int].map(x => x).getOrElse(0)
			service_builder += "lecture_length" -> (data \ "lecture_length").asOpt[Float].map(x => x).getOrElse(0)
			service_builder += "other_words" -> (data \ "other_words").asOpt[String].map(x => x).getOrElse("")
			/** ************************************************************/

			service_builder += "date" -> new Date().getTime
			service_builder += "servant_no" -> (data \ "servant_no").asOpt[Int].map(x => x).getOrElse(1)
			service_builder += "reserve1" -> (data \ "reserve1").asOpt[String].map(x => x).getOrElse("")

			_data_connection.getCollection("kidnap") += service_builder.result
		}
	}

	def update2DB(data : JsValue, origin : MongoDBObject) = {
		val service_id = (data \ "service_id").asOpt[String].map (x => x).getOrElse(throw new Exception("service not existing"))

		(data \ "title").asOpt[String].map (x => origin += "title" -> x).getOrElse(Unit)
		(data \ "description").asOpt[String].map (x => origin += "description" -> x).getOrElse(Unit)
		(data \ "capacity").asOpt[Int].map (x => origin += "capacity" -> x.asInstanceOf[Number]).getOrElse(Unit)
		(data \ "price").asOpt[Float].map (x => origin += "price" -> x.asInstanceOf[Number]).getOrElse(Unit)

		(data \ "location").asOpt[JsValue].map { loc =>
			val location = MongoDBObject.newBuilder
			location += "latitude" -> (loc \ "latitude").asOpt[Float].map (tmp => tmp).getOrElse(0.floatValue)
			location += "longtitude" -> (loc \ "longtitude").asOpt[Float].map (tmp => tmp).getOrElse(0.floatValue)
			origin += "location" -> location.result
		}.getOrElse(Unit)

		(data \ "cans_cat").asOpt[Long].map (x => origin += "cans_cat" -> x.asInstanceOf[Number]).getOrElse(Unit)
		(data \ "cans").asOpt[Long].map (cans => origin += "cans" -> cans.asInstanceOf[Number]).getOrElse(Unit)
		(data \ "facility").asOpt[Long].map (cans => origin += "facility" -> cans.asInstanceOf[Number]).getOrElse(Unit)

		(data \ "images").asOpt[List[String]].map { lst =>
			origin += "images" -> lst
		}.getOrElse(Unit)

		(data \ "distinct").asOpt[String].map (x => origin += "distinct" -> x).getOrElse(Unit)
		(data \ "address").asOpt[String].map (x => origin += "address" -> x).getOrElse(Unit)
		(data \ "adjust_address").asOpt[String].map (x => origin += "adjust_address" -> x).getOrElse(Unit)
		(data \ "status").asOpt[Int].map (x => origin += "status" -> x.asInstanceOf[Number]).getOrElse(Unit)

		(data \ "age_boundary").asOpt[JsValue].map { boundary =>
			val age_boundary = MongoDBObject.newBuilder
			age_boundary += "lsl" -> (boundary \ "lsl").asOpt[Int].map (x => x).getOrElse(3)
			age_boundary += "usl" -> (boundary \ "usl").asOpt[Int].map (x => x).getOrElse(11)
			origin += "age_boundary" -> age_boundary.result
		}.getOrElse(Unit)

		(data \ "least_hours").asOpt[Int].map (x => origin += "least_hours" -> x.asInstanceOf[Number]).getOrElse(Unit)
		(data \ "allow_leave").asOpt[Int].map (x => origin += "allow_leave" -> x.asInstanceOf[Number]).getOrElse(Unit)
		(data \ "service_cat").asOpt[Int].map (x => origin += "service_cat" -> x.asInstanceOf[Number]).getOrElse(Unit)

		/**
		  * somethin that need to be modified at last
		  */
		/**************************************************************/
		(data \ "least_times").asOpt[Int].map (x => origin += "least_times" -> x.asInstanceOf[Number]).getOrElse(Unit)
		(data \ "lecture_length").asOpt[Float].map (x => origin += "lecture_length" -> x.asInstanceOf[Number]).getOrElse(Unit)
		(data \ "other_words").asOpt[String].map (x => origin += "other_words" -> x).getOrElse(Unit)

		(data \ "reserve1").asOpt[String].map (x => origin += "reserve1" -> x).getOrElse(Unit)
		/**************************************************************/

		_data_connection.getCollection("kidnap").update(DBObject("service_id" -> service_id), origin)
	}

	def store2DB = kidnaps foreach { data =>
		val service_id = (data \ "service_id").asOpt[String].get

		(from db() in "kidnap" where ("service_id" -> service_id) select (x => x)).toList match {
			case Nil => push2DB(data)
			case head :: _ => update2DB(data, head)
		}
	}

	def onlineAllService = {
		val mongoColl = _data_connection._conn("baby")("kidnap")
		val ct = mongoColl.find(DBObject())

		while (ct.hasNext) {
			val head = ct.next().asDBObject
			val service_id = head.getAs[String]("service_id").get
			update2DB(toJson(Map("service_id" -> toJson(service_id), "status" -> toJson(2))), head)
		}
	}
}
