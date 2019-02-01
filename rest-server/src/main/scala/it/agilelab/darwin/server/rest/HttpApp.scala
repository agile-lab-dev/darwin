package it.agilelab.darwin.server.rest

import java.util.concurrent.Executor

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RouteConcatenation
import akka.stream.ActorMaterializer
import it.agilelab.darwin.common.Logging

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}

class HttpApp(services: Service*)(implicit system: ActorSystem, materializer: ActorMaterializer) extends Logging {
  def run(): Unit = {
    val interface = "0.0.0.0"
    val port = 8080


    val route = RouteConcatenation.concat(services.map(_.route): _*)

    log.info("Starting http server on {}:{}", interface, port)
    val eventuallyBinding = Http().bindAndHandle(route, interface, port)
    val binding = Await.result(eventuallyBinding, Duration.Inf)
    log.info("Started http server on {}:{}", interface, port)

    val shutdownThread = new Thread(new Runnable {
      override def run(): Unit = {
        implicit val ec: ExecutionContext = newSameThreadExecutor
        log.info("Received shutdown hook")

        val termination = for {
          _ <- binding.unbind()
          terminated <- system.terminate()
        } yield terminated

        Await.ready(termination, Duration.Inf)
        log.info("Shutdown")
      }
    })

    shutdownThread.setName("shutdown")

    Runtime.getRuntime.addShutdownHook(shutdownThread)

    log.info("registered shutdown hook")
  }


  private def newSameThreadExecutor: ExecutionContextExecutor = ExecutionContext.fromExecutor(new Executor {
    override def execute(command: Runnable): Unit = command.run()
  })
}

object HttpApp {
  def apply(services: Service*)(implicit system: ActorSystem, materializer: ActorMaterializer): HttpApp =
    new HttpApp(services: _*)
}
