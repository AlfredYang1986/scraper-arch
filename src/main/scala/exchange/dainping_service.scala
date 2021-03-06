package exchange

import java.util.{Date, UUID}

import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import sercurity.Sercurity
import com.mongodb.casbah.Imports._
import http.Download
import util.dao.{_data_connection, from}
import java.util.Calendar

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
				"price" -> toJson((s \ "price").asOpt[Int].map (x => x.toDouble).getOrElse(0.0)),
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

	def adjustAddressData = {
		val mongoColl = _data_connection._conn("baby")("kidnap")
		val ct = mongoColl.find(DBObject())

		def filterAddress(a : String) : String = if (a.contains("地址")) {
													a.substring("地址:".length).trim
									 			 } else a

		while (ct.hasNext) {
			val service = ct.next().asDBObject
			val service_id = service.getAs[String]("service_id").get
			val address = service.getAs[String]("address").get
			update2DB(toJson(Map("service_id" -> toJson(service_id),
				"address" -> toJson(filterAddress(address)))), service)
		}
	}

	def adjustPriceData = {
		val mongoColl = _data_connection._conn("baby")("kidnap")
		val ct = mongoColl.find(DBObject())

		while (ct.hasNext) {
			val service = ct.next().asDBObject
			val service_id = service.getAs[String]("service_id").get
			val service_name = service.getAs[String]("title").get
			val price = service.getAs[Number]("price").get.doubleValue()

			if (price == 0.0) {
				exchange_data.courses.find(x => (x \ "name").asOpt[String].map (x => x).getOrElse("") == service_name) match {
					case None => Unit
					case Some(x) => {
						val p = (x \ "price").asOpt[String].map (x => x.toDouble).getOrElse(0.0)
						update2DB(toJson(Map("service_id" -> toJson(service_id), "price" -> toJson(p))), service)
					}
				}
			}

		}
	}

	def adjustData = {
		val mongoColl = _data_connection._conn("baby")("kidnap")
		val ct = mongoColl.find(DBObject())

		while (ct.hasNext) {
			val service = ct.next().asDBObject
			val service_id = service.getAs[String]("service_id").get
			val owner_id = service.getAs[String]("owner_id").get

			val owner = (from db() in "user_profile" where ("user_id" -> owner_id) select (x => x)).toList.head
			val company = owner.getAs[String]("screen_name").get
			val personal_description = owner.getAs[String]("personal_description").map (x => x).getOrElse()

			val service_title = service.getAs[String]("title").get
			val service_description = service.getAs[String]("description").get

			val cat = keywordsMapping(company + personal_description + service_title + service_description)

			update2DB(toJson(Map("service_id" -> toJson(service_id),
								 "service_cat" -> toJson(cat._1),
								 "cans_cat" -> toJson(cat._2),
								 "cans" -> toJson(cat._3))), service)
		}
	}

	/**
	  * 	service_cat: 一级 （课程=0，看顾=1）
	  *     cans_cat： 二级 当 service_cat == 1 => 日间=0 课后=1   #define kAY_service_options_title_lookafter        @[@"日间看顾", @"课后看顾"]
	  *				 service_cat == 0 =>
	  *				    #define kAY_service_options_title_course        @[@"艺术", @"运动", @"科学", @"语言", @"阅读", @"手工"]
	  *
	  *		cans
	  *					#define kAY_service_course_title_0              @[@"钢琴", @"舞蹈", @"书法", @"中国画", @"绘画", @"尤克丽丽", @"戏剧"]
	  *					#define kAY_service_course_title_1              @[@"瑜伽健身", @"篮球", @"马术", @"围棋", @"击剑", @"桌游"]
	  *					#define kAY_service_course_title_2              @[@"3D打印", @"机器人", @"心理学", @"行为习惯"]
	  *					#define kAY_service_course_title_3              @[@"英语"]
	  *					#define kAY_service_course_title_4              @[@"绘本"]
	  *					#define kAY_service_course_title_5              @[@"烘焙", @"陶艺"]
	  */
	def keywordsMapping(k : String) : (Int, Int, Int) = {

		def contains(lst : List[String]) : Boolean = (lst.map (x => k.contains(x)).count(_ => true)) > 0

		val pinano_lst = "钢琴" :: Nil
		val dance_lst = "舞" :: "芭蕾" :: Nil
		val draw_lst = "绘画" :: "水彩" :: "国画" :: "插画" :: "版画" :: "写生" :: "美术" :: Nil
		val youga_lst = "瑜伽" :: Nil
		val basketball_lst = "篮球" :: Nil
		val hourse_lst = "马术" :: "骑马" :: Nil
		val weiqi_lst = "围棋" :: Nil
		val fighter_lst = "击剑" :: Nil
		val reboat_lst = "机器人" :: "robot" :: "stem" :: "STEM" :: "steam" :: "STEAM" :: Nil
		val threedemention_lst = "3D打印" :: Nil

		val sport_lst = "橄榄球" :: "足球" :: "乒乓球" :: "羽毛球" :: "运动" :: "跑酷" :: Nil
		val art_lst = "音乐" :: "艺术" :: Nil
		val scient_lst = "科学" :: "自然" :: Nil

		if (contains(dance_lst)) (0, 0, 1)
		else if (contains(draw_lst)) (0, 0, 4)
		else if (contains(pinano_lst)) (0, 0, 0)
		else if (contains(youga_lst)) (0, 1, 0)
		else if (contains(basketball_lst)) (0, 1, 1)
		else if (contains(hourse_lst)) (0, 1, 2)
		else if (contains(threedemention_lst)) (0, 2, 0)
		else if (contains(reboat_lst)) (0, 2, 1)
		else if (contains(sport_lst)) (0, 1, 999)
		else if (contains(art_lst)) (0, 0, 999)
		else if (contains(scient_lst)) (0, 2, 999)
		else (0, 999, 999)
	}

	def adjustTimeManagement = {
		val mongoColl = _data_connection._conn("baby")("kidnap")
		val ct = mongoColl.find(DBObject())

		while (ct.hasNext) {
			val service = ct.next().asDBObject
			val service_id = service.getAs[String]("service_id").get
			val owner_id = service.getAs[String]("owner_id").get

			val owner = (from db() in "user_profile" where ("user_id" -> owner_id) select (x => x)).toList.head
			val company = owner.getAs[String]("screen_name").get

			val schedule = exchange_data.shops.find(x => (x \ "name").asOpt[String].map (x => x).getOrElse("") == company) match {
				case None => ""
				case Some(s) => (s \ "schedule").asOpt[String].map (x => x).getOrElse("")
			}

			if (!schedule.isEmpty) {
				val schedule_item_lst = splitSchedule(schedule)
				val date = service.getAs[Number]("date").get.longValue
				println(schedule_item_lst.length)

				schedule_item_lst.foreach(x => pushSchedule2DB(handleScheduleItem(x)(date), service_id))
			}
		}
	}

	def splitSchedule(schedule : String) : List[String] = schedule.split(";").map (x => x.split("、")).flatten.toList

	def pushSchedule2DB(tms : List[JsValue], service_id : String) = {
		updateServiceTM(toJson(Map("tms" -> toJson(tms), "service_id" -> toJson(service_id))))
	}

	def handleScheduleItem(item : String)(date : Long) : List[JsValue] = {
		val iter = item.replace("：", ":").replace("至", "~").replace("-", "~").trim
		val index = if (iter.head.isDigit) iter.lastIndexWhere(x => x.isDigit) + 1
					else iter.indexWhere(x => x.isDigit)
		var (days, hours) =  (iter.splitAt(index))

		println(s"days : $days hours : $hours")

		if (days.isEmpty) Nil
		else {
			if (days.trim.head.isDigit) {
				val tmp = days.trim
				days = hours.trim
				hours = tmp
			}

			val (starthours, endhours) = praseHours(hours)
			println(s"hours start: $starthours, end: $endhours")

			praseDays(days, date) map { x =>
				toJson(Map("pattern" -> toJson(1), "startdate" -> toJson(x._1), "enddate" -> toJson(x._2),
					"starthours" -> toJson(starthours), "endhours" -> toJson(endhours)))
			}
		}
	}

	def praseHours(hours : String) : (Long, Long) = {
		def string2time(s : String) : Long = s.replace(":", "").toLong
		val lst = hours.split("~")
		(string2time(lst.head), string2time(lst.tail.head))
	}

	def praseDays(days : String, date : Long) : List[(Long, Long)] = {
		val map = Map("一" -> 1, "二" -> 2, "三" -> 3, "四" -> 4, "五" -> 5, "六" -> 6, "日" -> 0)
		val keys = map.keys.toList.sortBy(map(_))

		val public_cal = Calendar.getInstance
		val public_d = new Date(date)

		val public_week_day =
		{
			public_cal.setTime(public_d)
			public_cal.get(Calendar.DAY_OF_WEEK) - 1
		}

		var contians : List[String] = keys.filter(p => days.contains(p)).sorted

		if (days.isEmpty) {
			contians = keys

		} else if (days.contains("~")) {
			val st = contians.head
			val ed = contians.last

			var select = false
			contians = keys.map { x =>
				if (select == false && x == st) select = true
				else if (select == true && x == ed) select = false

				if (select) x
				else if (x == st || x == ed) x
				else ""
			}.filterNot(x => x == "").sorted

			println(s"contains are : $contians")
		} else Unit

		val cal = Calendar.getInstance()
		contians.map { x =>
			cal.setTime(new Date(date))
			cal.add(Calendar.DATE, public_week_day - map(x))
			(cal.getTime.getTime, -1.toLong)
		}
	}

	def Js2DBObject(data : JsValue, service_id : String) : MongoDBObject = {
		val builder = MongoDBObject.newBuilder

		builder += "service_id" -> service_id

		val tm_builder = MongoDBList.newBuilder
		(data \ "tms").asOpt[List[JsValue]].map { arr => arr foreach { one =>
			val tmp = MongoDBObject.newBuilder
			tmp += "pattern" -> (one \ "pattern").asOpt[Int].map (x => x).getOrElse(throw new Exception("wrong input"))
			tmp += "startdate" -> (one \ "startdate").asOpt[Long].map (x => x).getOrElse(throw new Exception("wrong input"))
			tmp += "enddate" -> (one \ "enddate").asOpt[Long].map (x => x).getOrElse(-1)
			tmp += "starthours" -> (one \ "starthours").asOpt[Long].map (x => x).getOrElse(throw new Exception("wrong input"))
			tmp += "endhours" -> (one \ "endhours").asOpt[Long].map (x => x).getOrElse(throw new Exception("wrong input"))

			tm_builder += tmp.result

		}}.getOrElse("wrong input")
		builder += "tms" -> tm_builder.result

		builder.result
	}

	def pushServiceTM(data : JsValue) = {
		try {
			val service_id = (data \ "service_id").asOpt[String].get

			val obj = Js2DBObject(data, service_id)
			_data_connection.getCollection("service_time") += obj

		} catch {
			case ex : Exception => ex.printStackTrace()
		}
	}

	def updateServiceTM(data : JsValue) = {
		try {
			val service_id = (data \ "service_id").asOpt[String].get

			(from db() in "service_time" where ("service_id" -> service_id) select (x => x)).toList match {
				case head :: Nil => {
					val obj = Js2DBObject(data, service_id)
					_data_connection.getCollection("service_time").update(head, obj)
				}
				case _ => pushServiceTM(data)
			}

		} catch {
			case ex : Exception => ex.printStackTrace()
		}
	}
}
