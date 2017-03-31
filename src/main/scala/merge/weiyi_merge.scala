package merge

import java.io.{File, FileInputStream, FileWriter}

import play.api.libs.json.{JsValue, Json}
import play.api.libs.json.Json.toJson

/**
  * Created by Alfred on 31/03/2017.
  */
object weiyi_merge {
	def apply() : Unit = {
		val file = "src/main/resources/data/为艺/"
		val tf = new File(file)
		val result = tf.listFiles.filter(x => x.isFile && !x.isHidden).map(x => x.getPath).toList.map { f =>
			Json.parse(new FileInputStream(f)).asOpt[JsValue].map (x => (x \ "teacher").asOpt[List[JsValue]].get).getOrElse(Nil)
		}.flatten

		{
			val writer = new FileWriter(new File("output/weiyi-teacher.json"))
			writer.write(toJson(result.sortBy(x => (x \ "bio").asOpt[String].map (y => y).getOrElse(""))).toString)
			writer.flush
			writer.close
		}
	}
}