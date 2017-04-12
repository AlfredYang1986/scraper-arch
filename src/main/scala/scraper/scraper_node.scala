package scraper

import java.io.FileWriter

import org.jsoup.nodes._
import org.jsoup.Jsoup
import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import sketch.sketch

/**
  * Created by Alfred on 28/03/2017.
  */
object scraper_node {
	def apply(sv: sketch, uv: Option[String] = None): scraper_node = new scraper_node(sv, uv)
}

class scraper_node(val s : sketch,
                   var u : Option[String] = None) {

	def process : Option[JsValue] = {
		if (u.isEmpty)
			u = s.url

		try {
			val result_opt = u.map { x =>
				val d = Jsoup.connect(x).timeout(0).get
				val tmp = documentParse(d).get
				val sbs = s.subs.map (k => documentParseSubs(k._1, d))
							.filterNot(_ == None).map(x => x.get._1 -> x.get._2).toMap

				val lst = s.lst_attrs.map (l => documentParseLstElem(l._1, d))
							.filterNot(_ == None).map (x => x.get._1 -> x.get._2).toMap

				println(s"scraper doing with url $x ...")
				Some(toJson(tmp ++: sbs ++: lst ++: Map("url" -> toJson(x))))

			}.getOrElse(None)

			result_opt

		} catch {
			case ex : Exception => {
				println(s"error url $u")
//				println(s"wrong at ${s.getPlatform} ${s.getCategory}")
				println(ex.getStackTraceString)
				None
			}
		}
	}

	def documentParse(d : Document) : Option[Map[String, JsValue]] = {
		var result : Map[String, JsValue] = Map.empty
		s.attrs.keys foreach { x =>
			s.attrs.get(x).get.asOpt[String] match {
				case None => {
					val sjs = s.attrs.get(x).get.asOpt[JsValue].get
					val elem = (sjs \ "elem").asOpt[String].map (x => x).getOrElse("")
					val attr = (sjs \ "attr").asOpt[String].map (x => x).getOrElse("")

					result = addTextFromAttr(x, elem, attr, d, result) //result + (x -> toJson(d.select(elem).attr(attr)))
				}
				case Some(y) => result = addTextFromElement(x, y, d, result)
			}
		}
		Some(result)
	}

	def documentParseLstElem(lst_name : String, d : Document) : Option[(String, JsValue)] = {
		val sk = s.lst_attrs.get(lst_name).get

		val lst_entry = (sk \ "entrance").asOpt[String].map (x => x).getOrElse(throw new Exception("should be a string"))
		val def_lst = (sk \ "defines").asOpt[List[String]].map (x => x).getOrElse(throw new Exception("should have defines"))

		def elem2String(y : String, r : String) : JsValue =
			if (y == "image" && r.startsWith("//"))
				toJson("http:" + r)
			else toJson(r)

		Some(lst_name ->
			toJson(d.select(lst_entry).toArray.toList.asInstanceOf[List[Element]].map { x =>
				def_lst.map { y =>
					(sk \ y).asOpt[String] match {
						case Some(k) => y -> toJson(x.select(k).text)
						case None => {
							val sjs = (sk \ y).asOpt[JsValue].get
							val elem = (sjs \ "elem").asOpt[String].map (x => x).getOrElse("")
							val attr = (sjs \ "attr").asOpt[String].map (x => x).getOrElse("")
//							println(s"image count is ${x.select(elem)}")
							val r = x.select(elem)
							if (r.size() > 1) {
								val r_lst = x.select(elem).toArray.toList.asInstanceOf[List[Element]]
								y -> toJson(r_lst.map (z => elem2String(y, z.attr(attr))))
							} else y -> elem2String(y, r.attr(attr))
						}
					}
				}.toMap
			}))
	}

	def documentParseSubs(subs: String, d : Document) : Option[(String, JsValue)] = {
		val sk = s.subs.get(subs).get
		val sjs = sk.entrance.get

		val elem = (sjs \ "elem").asOpt[String].map (x => x).getOrElse("")
		val attr = (sjs \ "attr").asOpt[String].map (x => x).getOrElse("")

		if (!elem.isEmpty && !attr.isEmpty) {
			val shops_url = d.select(elem).toArray.toList.asInstanceOf[List[Element]].map (x => x.attr(attr)).map { x =>
				if (x.startsWith("http")) x
				else s.getHost + x
			}

			Some(subs -> toJson(shops_url.map (url => scraper_node(sk, Some(url)).process.get)))
		} else None
	}

	def addTextFromElement(x : String, e : String, d : Document, r : Map[String, JsValue]) : Map[String, JsValue] = {
		val s = d.select(e).text
		if (s.isEmpty) r
		else r + (x -> toJson(s))
	}

	def addTextFromAttr(x : String, e : String, a : String, d : Document, r : Map[String, JsValue]) : Map[String, JsValue] = {
		try {
			val s = d.select(e).attr(a)
			if (s.isEmpty) r
			else r + (x -> toJson(s))

		} catch {
			case ex : Exception => r
		}
	}
}
