package merge

import java.io.{File, FileInputStream, FileWriter}

import play.api.libs.json.{JsValue, Json}
import play.api.libs.json.Json.toJson

/**
  * Created by Alfred on 31/03/2017.
  */
object dianping_merge {
	def apply(): Unit = {
		try {
			val file = "src/main/resources/data/大众点评/"
			val tf = new File(file)
			val result = tf.listFiles.filter(x => x.isFile && !x.isHidden).map(x => x.getPath).toList.map { f =>
				Json.parse(new FileInputStream(f)).asOpt[JsValue].map (x => (x \ "shop").asOpt[List[JsValue]].get).getOrElse(Nil)
			}.flatten

			{
				val writer = new FileWriter(new File("output/dianping-origin.json"))
				writer.write(toJson(result.sortBy(x => (x \ "name").asOpt[String].map (y => y).getOrElse(""))).toString)
				writer.flush
				writer.close
			}

			{
				val writer = new FileWriter(new File("output/dianping-sales.json"))
				val sales = result.map (x => (x \ "sales").asOpt[List[JsValue]].map (x => x).getOrElse(Nil)).flatten
				writer.write(toJson(sales.sortBy(x => (x \ "shop_name").asOpt[String].map (y => y).getOrElse(""))).toString)
				writer.flush
				writer.close
			}

			{
				val writer = new FileWriter(new File("output/dianping-class.json"))
				val sales = result.map (x => (x \ "class").asOpt[List[JsValue]].map (x => x).getOrElse(Nil)).flatten
				writer.write(toJson(sales.sortBy(x => (x \ "shop_name").asOpt[String].map (y => y).getOrElse(""))).toString)
				writer.flush
				writer.close
			}

		} catch {
			case ex : Exception => {
				println("dianping merge error")
				ex.printStackTrace
			}
		}
	}
}
