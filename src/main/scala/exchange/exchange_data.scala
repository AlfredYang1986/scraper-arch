package exchange

import java.io.FileInputStream

import play.api.libs.json.{JsValue, Json}

/**
  * Created by BM on 13/04/2017.
  */
object exchange_data {
	lazy val shops =  Json.parse(new FileInputStream("""output/dianping-origin.json""")).asOpt[List[JsValue]].map (x => x).getOrElse(Nil)
	lazy val courses =  Json.parse(new FileInputStream("""output/dianping-class.json""")).asOpt[List[JsValue]].map (x => x).getOrElse(Nil)
	lazy val sales =  Json.parse(new FileInputStream("""output/dianping-sales.json""")).asOpt[List[JsValue]].map (x => x).getOrElse(Nil)
}
