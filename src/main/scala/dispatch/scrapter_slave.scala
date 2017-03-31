package dispatch

import akka.actor.{Actor, ActorLogging, Props}
import play.api.libs.json.JsValue
import scraper.scraper_node
import sketch.sketch
import java.io.{File, FileWriter}
import java.util.UUID

import dispatch.signal.dis_signal.node_done

/**
  * Created by Alfred on 30/03/2017.
  */
object scrapter_slave {
	def props = Props[scrapter_slave]
}

class scrapter_slave extends Actor with ActorLogging{

	val path = "src/main/resources/data/"

	override def receive: Receive = {
		case sn : scraper_node => {
			val f = UUID.randomUUID.toString
			val writer = new FileWriter(new File(path + sn.s.platform.get + "/" + f))
			val result = sn.process
			if (result.isEmpty) println("job error")
			else writer.write(result.get.toString)
			writer.flush
			writer.close

			sender() ! node_done()
		}

		case _ => println("receive a message")
	}
}
