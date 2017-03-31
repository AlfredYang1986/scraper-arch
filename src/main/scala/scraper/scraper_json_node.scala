package scraper

import http.HTTP
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import play.api.libs.json.Json.parse
import sketch.sketch

/**
  * Created by Alfred on 31/03/2017.
  */
case class scraper_json_node(sv : sketch,
	                         uv : Option[String] = None,
                             val cur : Option[JsValue] = None) extends scraper_node(sv, uv) {
	override def process : Option[JsValue] = {
		if (u.isEmpty)
			u = s.url

		try {
			val dd = if (cur.isEmpty) {
				Some(parse(HTTP(u.get).get(Map.empty)))
			} else cur

			val result_opt = dd.map { d =>
				val tmp = documentParse(d).get
				val sbs = s.subs.map (k => documentParseSubs(k._1, d))
					.filterNot(_ == None).map(x => x.get._1 -> x.get._2).toMap
				val lst = s.lst_attrs.map (l => dobumentParseLstElem(l._1, d))
					.filterNot(_ == None).map (x => x.get._1 -> x.get._2).toMap

				println(s"scraper doing with json ...")
//				println(s"scraper doing with json $d ...")
				Some(toJson(tmp ++: sbs ++: lst))

			}.getOrElse(None)

			result_opt

		} catch {
			case ex : Exception => {
				println(s"error url $u")
				// println(s"wrong at ${s.getPlatform} ${s.getCategory}")
				println(ex.getStackTraceString)
				None
			}
		}
	}

	def documentParse(d : JsValue) : Option[Map[String, JsValue]] = {
		var result : Map[String, JsValue] = Map.empty
		s.attrs.keys foreach { x =>
			s.attrs.get(x).get.asOpt[String] match {
				case None => {
					val sjs = s.attrs.get(x).get.asOpt[List[String]].get
					result = result + (x -> toJson(Js2String(d, sjs)))
				}
				case Some(y) => result = result + (x -> toJson(Js2String(d, y :: Nil)))
			}
		}
		Some(result)
	}

	def dobumentParseLstElem(lst_name : String, d : JsValue) : Option[(String, JsValue)] = {
		val x = s.lst_attrs.get(lst_name).get

			x.asOpt[String] match {
				case None => {
					val sjs = x.asOpt[List[String]].get
					Some(lst_name -> toJson(Js2String(d, sjs)))
				}
				case Some(y) => Some(lst_name -> toJson(Js2String(d, y :: Nil)))
			}
	}

	def documentParseSubs(subs: String, d : JsValue) : Option[(String, JsValue)] = {
		val sk = s.subs.get(subs).get
		val sjs = sk.entrance.get.asOpt[String].get

		if (!sjs.isEmpty) {
			println(sjs)
			val s = (d \ sjs).asOpt[List[JsValue]].map (x => x).getOrElse(Nil)
			println(s.length)
			if (!s.isEmpty)
				Some(subs -> toJson(s.map (js => scraper_json_node(sk, None, Some(js)).process.get)))
			else None
		} else None
	}

	def Js2String(d : JsValue, l : List[String]) : String = {
		l match {
			case Nil => ""
			case head :: tail => stringElem(d, head) + Js2String(d, tail)
		}
	}

	def stringElem(d : JsValue, elem : String) : String = {
		def str2Elem(d : JsValue, ls : List[String]) : JsValue = {
			ls match {
				case Nil => d
				case head :: tail => str2Elem((d \ head).asOpt[JsValue].map (x => x).getOrElse(toJson("")), tail)
			}
		}

		val l = elem.split(">").map (_.trim).toList
		str2Elem(d, l).asOpt[String].map (x => x).getOrElse("")
	}
}
