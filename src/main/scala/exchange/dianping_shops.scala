package exchange

import java.util.{Date, UUID}

import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import sercurity.Sercurity
import util.dao._data_connection
import util.dao.from
import util.errorcode.ErrorCode
import com.mongodb.casbah.Imports._
import http.{Download, HTTP}
import java.io.File
import java.io.FileWriter

/**
  * Created by Alfred on 10/04/2017.
  */
object dianping_shops {
	var shops: List[JsValue] = Nil

	def downloadShopImg(shop : JsValue) : String = {
		val images = (shop \ "images").asOpt[List[JsValue]].map (x => x).getOrElse(Nil)

		val img = images match {
			case Nil => ""
			case head :: _ => {
				val str = UUID.randomUUID().toString
				Download((head \ "image").asOpt[String].get)("""upload/""" + str)
				str
			}
		}
		img
	}

	def pushShop(shop: JsValue, img: String) = {
		val shop_name = (shop \ "name").asOpt[String].map(x => x).getOrElse("")

		val tmp =
			toJson(Map("user_id" -> toJson(Sercurity.md5Hash(shop_name + Sercurity.getSercuritySeed)), "screen_name" -> toJson(shop_name),
				"screen_photo" -> toJson(img), "isLogin" -> toJson(1), "gender" -> toJson(1), "school" -> toJson(""),
				"company" -> toJson((shop \ "name").asOpt[String].map(x => x).getOrElse("")),
				"occupation" -> toJson(""), "personal_description" -> toJson(""), "is_service_provider" -> toJson(1),
				"address" -> toJson((shop \ "address").asOpt[String].map(x => x).getOrElse("")),
				"date" -> toJson(new Date().getTime), "dob" -> toJson(0), "about" -> toJson(""),
				"contact_no" -> toJson((shop \ "phone").asOpt[String].map(x => x).getOrElse(""))))
		shops = tmp :: shops
	}

	def datamodify(shop_name : String) : String = {
		shop_name match {
			case "金宝贝(西直门凯德MALL店)" =>  "金宝贝早教中心(五棵松店)"
			case "金宝贝(望京中心)" => "金宝贝早教中心(五棵松店)"
			case "NYC纽约国际儿童俱乐部(五棵松店)" => "纽约国际儿童俱乐部(凯德MALL太阳宫店)"
			case "星动派艺术培训中心(金源校区)" => "新奥品学·星动派艺能学院(新奥校区)"
			case "新武门国际双语少儿武术(北京英国学校)" => "新武门国际武道教育(北京英国学校)"
			case _ => shop_name
		}
	}

	def queryShopsUserId(shop_name: String): String = {
		shops.find(x => (x \ "screen_name").asOpt[String].get.trim == datamodify(shop_name).trim).map(x => (x \ "user_id").asOpt[String].get).getOrElse("")
	}

	def queryShopsAddressWithName(shop_name: String): String =
		shops.find(x => (x \ "screen_name").asOpt[String].get.trim == datamodify(shop_name).trim).map(x => (x \ "address").asOpt[String].get).getOrElse("")

	def store2DB = shops foreach { data =>

		val user_id = (data \ "user_id").asOpt[String].get
		val auth_token = user_id
		val screen_name = (data \ "screen_name").asOpt[String].get
		val screen_photo = (data \ "screen_photo").asOpt[String].get

		(from db() in "user_profile" where ("user_id" -> user_id) select (x => x)).toList match {
			case Nil => {
				val builder = MongoDBObject.newBuilder

				builder += "user_id" -> user_id // c_r_user_id
				builder += "screen_name" -> screen_name
				builder += "screen_photo" -> screen_photo
				builder += "isLogin" -> (data \ "isLogin").asOpt[Int].map(x => x).getOrElse(1)
				builder += "gender" -> (data \ "gender").asOpt[Int].map(x => x).getOrElse(0)

				builder += "school" -> (data \ "school").asOpt[String].map(x => x).getOrElse("")
				builder += "company" -> (data \ "company").asOpt[String].map(x => x).getOrElse("")
				builder += "occupation" -> (data \ "occupation").asOpt[String].map(x => x).getOrElse("")
				builder += "personal_description" -> (data \ "personal_description").asOpt[String].map(x => x).getOrElse("")

				builder += "is_service_provider" -> (data \ "is_service_provider").asOpt[Int].map(x => x).getOrElse(0)

				val coordinate = MongoDBObject.newBuilder
				coordinate += "longtitude" -> (data \ "longtitude").asOpt[Float].map(x => x).getOrElse(0.toFloat.asInstanceOf[Number])
				coordinate += "latitude" -> (data \ "latitude").asOpt[Float].map(x => x).getOrElse(0.toFloat.asInstanceOf[Number])
				builder += "coordinate" -> coordinate.result

				builder += "address" -> (data \ "address").asOpt[String].map(x => x).getOrElse("")
				builder += "date" -> new Date().getTime
				builder += "dob" -> (data \ "dob").asOpt[Long].map(x => x).getOrElse(0.toFloat.asInstanceOf[Number])
				builder += "about" -> (data \ "about").asOpt[String].map(x => x).getOrElse("")

				builder += "contact_no" -> (data \ "phoneNo").asOpt[String].map(x => x).getOrElse("")

				(data \ "kids").asOpt[List[JsValue]].map { lst =>
					val kids = MongoDBList.newBuilder
					lst foreach { tmp =>
						val kid = MongoDBObject.newBuilder
						kid += "gender" -> (tmp \ "gender").asOpt[Int].map(x => x).getOrElse(0)
						kid += "dob" -> (tmp \ "dob").asOpt[Long].map(x => x).getOrElse(0.toFloat.asInstanceOf[Number])

						kids += kid.result
					}
					builder += "kids" -> kids.result
				}.getOrElse(builder += "kids" -> MongoDBList.newBuilder.result)

				val result = builder.result
				_data_connection.getCollection("user_profile") += result
			}
			case user :: Nil => {
				List("signature", "role_tag", "screen_name", "screen_photo", "about", "address", "school", "company", "occupation", "personal_description", "contact_no") foreach { x =>
					(data \ x).asOpt[String].map { value =>
						user += x -> value
					}.getOrElse(Unit)
				}

				List("followings_count", "followers_count", "posts_count", "friends_count", "cycle_count", "isLogin", "gender", "is_service_provider") foreach { x =>
					(data \ x).asOpt[Int].map { value =>
						user += x -> new Integer(value)
					}.getOrElse(Unit)
				}

				List("dob") foreach { x =>
					(data \ x).asOpt[Long].map { value =>
						user += x -> value.asInstanceOf[Number]
					}.getOrElse(Unit)
				}

				List("longtitude", "latitude") foreach { x =>
					(data \ x).asOpt[Float].map { value =>
						val co = user.getAs[MongoDBObject]("coordinate").get
						co += x -> x.asInstanceOf[Number]
					}.getOrElse(Unit)
				}

				(data \ "kids").asOpt[List[JsValue]].map { lst =>
					val kids = MongoDBList.newBuilder
					lst foreach { tmp =>
						val kid = MongoDBObject.newBuilder
						kid += "gender" -> (tmp \ "gender").asOpt[Int].map(x => x).getOrElse(0)
						kid += "dob" -> (tmp \ "dob").asOpt[Long].map(x => x).getOrElse(0.toFloat.asInstanceOf[Number])

						kids += kid.result
					}
					user += "kids" -> kids.result
				}.getOrElse(Unit)

				_data_connection.getCollection("user_profile").update(DBObject("user_id" -> user_id), user)
			}
			case _ => ???
		}
	}
}
