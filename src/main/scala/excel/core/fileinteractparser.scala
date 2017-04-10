package excel.core

import akka.actor.ActorRef

trait fileinteractparser extends interactparser {
	override def handleOneTarget(target : target_type) = Unit
}

