package mg


import domain._

import java.util.concurrent.Executors

import prickle._
import unfiltered.jetty.ContextAdder
import unfiltered.request._
import unfiltered.response._

import scala.concurrent.{ExecutionContext, Await, Future, Promise}
import scala.util.{Random, Failure, Success}

import scala.concurrent.duration._


object UnfilteredApp {

  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(30))

  def main(args: Array[String]) {
    val api = unfiltered.filter.Planify {
      case req@ControllerApi(method, json) => {
        val r = Await.result(invokeController(method, json), 240 seconds)
        ResponseString(r)
      }
      case req@Path(x) => {
        println(s"Unrecognized: method=${req.method} path=$x")
        NotFound ~> ResponseString(s"Unrecognized: method=${req.method} path=$x")
      }
    }
    unfiltered.jetty.Server.http(8080).
      context("/client"){ ctx: ContextAdder =>
      ctx.resources(new java.net.URL(
        """file:../mg-client"""
      )).allowAliases(true)
      }.filter(api).run()
  }

  object ControllerApi {
    object ControllerMethod {
      def unapply(s: String): Option[String] =
        if (s.startsWith("/mg/domain/Api/"))
          Some(s.stripPrefix("/mg/domain/Api/"))
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
    val router = MyServer.route[Api](Games)
    router.apply {
      autowire.Core.Request(List("mg", "domain", "Api", method),
        Unpickle[Map[String, String]].fromString(json).get)
    }
  }

}

object Games extends Api {

  import UnfilteredApp.ec

  import collection.mutable.Buffer

  val PairCount = 8

  var seq: Int = 1

  def nextSeq() = {
    val s = seq; seq = seq + 1; s
  }

  var gamesAndObservers = Map.empty[Int, (Game, Buffer[Promise[Game]])]

  def games = gamesAndObservers.values.map(_._1)

  var waitingPlayer: Option[(String, Promise[Game])] = None

  def join(player1: String): Future[Game] = {
    synchronized {
      waitingPlayer match {
        case Some((player2, promise)) => {
          println(s"player $player2 waiting, signalling OK, player $player1 joined. Game ready.")
          waitingPlayer = None
          val g = newGame(player1, player2)
          gamesAndObservers = gamesAndObservers.updated(g.id, (g, Buffer.empty[Promise[Game]]))
          promise.success(g)
          Future(g)
        }
        case None => {
          val p = Promise[Game]
          waitingPlayer = Some(player1, p)
          println(s"player $player1 queued up for next game")
          p.future
        }
      }
    }
  }

  def waitOnUpdate(game: Game): Future[Game] = {
    println(s"observer watching game ${game.id}")
    val p = Promise[Game]
    gamesAndObservers.get(game.id).foreach(pr =>
      pr._2.append(p)
    )
    p.future
  }

  def newGame(player1Name: String, player2Name: String): Game = {
    val cards = 1.to(PairCount).flatMap(n =>
      Seq(Card(Random.nextInt, n), Card(Random.nextInt, n)))

    val shuffled = Random.shuffle(cards)

    val player1 = Player(1, player1Name)
    val player2 = Player(2, player2Name)
    val game = new Game(
      nextSeq(),
      if (Random.nextBoolean()) player1.id else player2.id,
      Map(player1.id -> player1, player2.id -> player2),
      shuffled)
    game
  }

  def reveal(game: Game, playerId: Int, card: Card): Game = {
    notifyObservers(
      if (playerId == game.currentPlayerId) {
        if (game.revealed.size == 2)
          game.advanceTurn()
        else
          game.reveal(card)
      }
      else game
    )
  }

  def advanceTurn(game: Game): Game = {
    notifyObservers(game.advanceTurn())
  }

  private def notifyObservers(game: Game): Game = {
    gamesAndObservers.get(game.id).foreach(pr => {
      val observers = pr._2
      observers.foreach(_.success(game))
    })
    gamesAndObservers = gamesAndObservers.updated(game.id, (game, Buffer.empty[Promise[Game]]))
    game
  }
}

object MyServer extends autowire.Server[String, Unpickler, Pickler]{
  def write[Result: Pickler](r: Result) = Pickle.intoString(r)
  def read[Result: Unpickler](p: String) = Unpickle[Result].fromString(p).get
}
