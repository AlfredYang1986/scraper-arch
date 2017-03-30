package dispatch.job

import play.api.libs.json.JsValue
import scraper.scraper_node

/**
  * Created by Alfred on 30/03/2017.
  */
case class scraper_job(val l : List[scraper_node])
