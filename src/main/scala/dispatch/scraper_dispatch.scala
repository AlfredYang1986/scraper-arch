package dispatch

import java.io.{File, FileInputStream, FileWriter}

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.routing.RoundRobinPool
import play.api.libs.json.Json.toJson

import scala.concurrent.duration._
import scala.concurrent.stm._
import scala.concurrent.ExecutionContext.Implicits.global

import signal.dis_signal.{shutting_down, _}
import job.scraper_job
import play.api.libs.json.{JsValue, Json}
import sketch.sketch
import scraper.scraper_node

/**
  * Created by  on 30/03/2017.
  */
object scraper_dispatch {
	def props = Props[scraper_dispatch]
}

class scraper_dispatch extends Actor
						  with ActorLogging
						  with create_scraper_schedule
						  with create_slave_router {

	val core_number = 4
	val router = createSlaveRouter

	override def receive: Receive = {
		case start(f) => {
			val lst = Json.parse(new FileInputStream(f)).asOpt[List[JsValue]].map (x => x).getOrElse(Nil)
			val r = lst map (x => (sketch(x), x))
			r map { x =>
				val url = (x._2 \ "url").asOpt[String].map (x => x).getOrElse(throw new Exception("must have url"))
				val page = (x._2 \ "page").asOpt[String].map (x => x).getOrElse(throw new Exception("must have page"))
				val upper = (x._2 \ "upper").asOpt[String].map (x => x.toInt).getOrElse(0)

				if (page.isEmpty)
					scraper_job(scraper_node(x._1, Some(url)) :: Nil)
				else
					scraper_job((1 to upper).map (y => scraper_node(x._1, Some(url + page + y))).toList)

			} match {
				case head :: tail => {
					atomic { implicit tnx =>
						current() = Some(head)
						waiting_lst() = tail
					}
				}
			}
			println(r)
			println(current.single.get)
			println(waiting_lst.single.get)

			(1 to core_number) foreach { _ =>
				signStep(router)
			}
		}
		case node_done() => {
			println("node done")
			if (!isOver) {
				println("not over")
				signStep(sender())
			} else {
				context.system.scheduler.scheduleOnce(1 minute, self, merge_result())
				println("job down")
			}
		}
		case merge_result() => {
			println("merge result")
			val file = "src/main/resources/data/"
			val tf = new File(file)
			val result = tf.listFiles.filter(x => x.isFile && !x.isHidden).map(x => x.getPath).toList.map { f =>
				Json.parse(new FileInputStream(f)).asOpt[JsValue].map (x => (x \ "shop").asOpt[List[JsValue]].get).getOrElse(Nil)
			}.flatten

			{
				val writer = new FileWriter(new File("output/origin.json"))
				writer.write(toJson(result.sortBy(x => (x \ "name").asOpt[String].map (y => y).getOrElse(""))).toString)
				writer.flush
				writer.close
			}

			{
				val writer = new FileWriter(new File("output/sales.json"))
				val sales = result.map (x => (x \ "sales").asOpt[List[JsValue]].map (x => x).getOrElse(Nil)).flatten
				writer.write(toJson(sales.sortBy(x => (x \ "shop_name").asOpt[String].map (y => y).getOrElse(""))).toString)
				writer.flush
				writer.close
			}

			{
				val writer = new FileWriter(new File("output/class.json"))
				val sales = result.map (x => (x \ "class").asOpt[List[JsValue]].map (x => x).getOrElse(Nil)).flatten
				writer.write(toJson(sales.sortBy(x => (x \ "shop_name").asOpt[String].map (y => y).getOrElse(""))).toString)
				writer.flush
				writer.close
			}

			self ! shutting_down()
		}

		case shutting_down() => {
			context.stop(self)
			context.system.terminate()
			println("shut down")
		}
		case _ => println("receive a message")
	}
}

trait create_slave_router { this : Actor =>
	def createSlaveRouter =
		context.actorOf(RoundRobinPool(4).props(scrapter_slave.props), name = "slave-router")
}

trait create_scraper_schedule { this : Actor =>
//	val schedule = context.system.scheduler.schedule(0 second, 1 second, self, schedule())
	val current : Ref[Option[scraper_job]] = Ref(None)
	val waiting_lst = Ref(List[scraper_job]())

	def isIdling : Boolean = current.single.get match {
			case None => true
			case Some(j) => false
		}

	def signThread(a : ActorRef) : Boolean = {
		if (current.single.get.isEmpty) false
		else {
			current.single.get.get.l match {
				case Nil => false
				case head :: Nil => {
					atomic { implicit tnx =>
						current() = None
					}
					a ! head
					println("sign thread success")
					true
				}
				case head :: tail => {
					atomic { implicit tnx =>
						current() = Some(scraper_job(tail))
					}
					a ! head
					println("sign thread success")
					true
				}
			}
		}
	}

	def signJob : Boolean = {
		if (!current.single.get.isEmpty) true
		else {
			waiting_lst.single.get match {
				case Nil => false
				case head :: tail => {
					atomic { implicit tnx =>
						current() = Some(head)
						waiting_lst() = tail
						true
					}
				}
			}
		}
	}

	def signStep(a : ActorRef) = {
		if (!signThread(a)) {
			signJob
			signThread(a)
		}
	}

	def isOver : Boolean = current.single.get.isEmpty && waiting_lst.single.get.isEmpty
}
