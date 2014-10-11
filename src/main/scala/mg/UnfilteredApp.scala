package mg

import prickle._
import unfiltered.jetty.ContextBuilder
import unfiltered.request._
import unfiltered.response._

import scala.concurrent.Await
import scala.concurrent.duration._



object UnfilteredApp {

  def main(args: Array[String]) {
    val echo = unfiltered.filter.Planify {
      case ControllerApi(method, json) => {
        val r = invokeController(method, json)
        ResponseString(Await.result(r, 1000 millis))
      }
      case Path(x) => {
        println(s"Unrecognized path: $x")
        ResponseString(s"Unrecognized path: $x")
      }
    }
    unfiltered.jetty.Http.local(8080).
      context("/client"){ ctx: ContextBuilder =>
      ctx.resources(new java.net.URL(
        """file:///Users/ben_hutchison/swork/mg-client"""
      ))
      }.filter(echo).run()
  }

  object ControllerApi {
    object ControllerMethod {
      def unapply(s: String): Option[String] =
        if (s.startsWith("/api/"))
          Some(s.stripPrefix("/api/"))
        else
          None
    }

    def unapply[T](req: HttpRequest[T]): Option[(String, String)] = req match {
      case POST(Path(ControllerMethod(m))) =>
        Some((m, req.reader.readAll()))
      case _ => None
    }
  }

  def invokeController(method: String, json: String) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val router = MyServer.route[Api](Games)
    router.apply {
      autowire.Core.Request(Seq("mg", "Api", method),
        Unpickle[Map[String, String]].fromString(json).get)
    }
  }

}

object MyServer extends autowire.Server[String, Unpickler, Pickler]{
  def write[Result: Pickler](r: Result) = Pickle.intoString(r)
  def read[Result: Unpickler](p: String) = Unpickle[Result].fromString(p).get
}
