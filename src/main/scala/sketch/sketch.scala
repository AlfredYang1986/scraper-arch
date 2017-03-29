package sketch

/**
  * Created by Alfred on 28/03/2017.
  */
import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson

object sketch {
	def apply(node : JsValue) : List[sketch] = (new entrance_sketch).dataFromNode(node) :: Nil
	def apply(node : JsValue, parent : sketch) : sketch = {
		val t =	(new sub_sketch).dataFromNode(node)
		t.parent = Some(parent)
		t
	}
}

trait sketch {
	val current : Option[String] = Some("list")
	var host : Option[String] = None
	var platform : Option[String] = None
	var category : Option[String] = None
	var entrance : Option[JsValue] = None
	var url : Option[String] = None

	var parent : Option[sketch] = None
	var subs : Map[String, sketch] = Map.empty

	var attrs : Map[String, JsValue] = Map.empty
	var lst_attrs : Map[String, JsValue] = Map.empty

	def isSub : Boolean
	def getPlatform : String = if (platform.isEmpty) parent.map(x => x.getPlatform).getOrElse(throw new Exception("should have platform"))
								else platform.get
	def getCategory : String = if (category.isEmpty) parent.map(x => x.getCategory).getOrElse(throw new Exception("should have category"))
								else category.get
	def getHost : String = if (host.isEmpty) parent.map(x => x.getHost).getOrElse(throw new Exception("should have host"))
							else host.get

	def dataFromNode(node : JsValue) : sketch = {
		host = (node \ "host").asOpt[String]
		platform = (node \ "platform").asOpt[String]
		category = (node \ "category").asOpt[String]

		entrance = (node \ "entrance").asOpt[JsValue]
		url = (node \ "url").asOpt[String]

		val container = (node \ "container").asOpt[JsValue].map (x => x).getOrElse(null)

		val d = (container \ "defines").asOpt[List[String]].map(x => x).getOrElse(throw new Exception("should have defines"))
		d foreach (x => attrs += x -> toJson((container \ x).asOpt[JsValue]))

		val lst = (container \ "lst_entrance").asOpt[List[String]].map(x => x).getOrElse(Nil)
		lst foreach (x => lst_attrs += x -> toJson((container \ x).asOpt[JsValue]))

		val sb = (container \ "sub_entrance").asOpt[List[String]].map(x => x).getOrElse(Nil)
		subs = sb.map (x => x -> sketch((container \ x).asOpt[JsValue].map(x => x).getOrElse(throw new Exception("sub entrance should be JsValue")), this)).toMap

		this
	}
}

class entrance_sketch extends sketch {
	def isSub : Boolean = false
}

class sub_sketch extends sketch {
	def isSub : Boolean = true
}

