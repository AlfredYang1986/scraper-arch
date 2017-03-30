package dispatch.signal

/**
  * Created by Alfred on 30/03/2017.
  */
object dis_signal {
	case class schedule()

	case class node_done()

	case class start(f : String)
	case class shutting_down()

	case class merge_result()
}
