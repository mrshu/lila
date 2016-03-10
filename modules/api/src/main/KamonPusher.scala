package lila.api

import akka.actor._
import java.lang.management.ManagementFactory
import scala.concurrent.duration._

import lila.socket.actorApi.NbMembers
import lila.hub.actorApi.round.NbRounds

private final class KamonPusher(
  countUsers: () => Int) extends Actor {

  import KamonPusher._

  override def preStart() {
    context.system.lilaBus.subscribe(self, 'nbMembers, 'nbRounds)
    scheduleTick
  }

  private val threadStats = ManagementFactory.getThreadMXBean
  private val app = lila.common.PlayApp

  private def scheduleTick =
    context.system.scheduler.scheduleOnce(1 second, self, Tick)

  def receive = {

    case NbMembers(nb) =>
      lila.mon.socket.member(nb)

    case NbRounds(nb) =>
      lila.mon.round.actor.member(nb)

    case Tick =>
      lila.mon.jvm.thread(threadStats.getThreadCount)
      lila.mon.jvm.daemon(threadStats.getDaemonThreadCount)
      lila.mon.jvm.uptime(app.uptime.toStandardSeconds.getSeconds.pp)
      lila.mon.user.online(countUsers())
      scheduleTick
  }
}

object KamonPusher {

  private case object Tick
}