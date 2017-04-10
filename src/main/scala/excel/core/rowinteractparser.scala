package excel.core

import akka.actor.ActorRef

trait rowinteractparser extends interactparser {
	override def handleOneTarget(target : target_type) = a ! target
}
