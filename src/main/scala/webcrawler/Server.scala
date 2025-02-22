package webcrawler

import cats.Parallel
import cats.effect.Async
import com.comcast.ip4s.*
import fs2.io.net.Network
import org.http4s.client.middleware.FollowRedirect
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.middleware.Logger

import scala.concurrent.duration.DurationInt

object Server:

  def run[F[_] : Async : Network : Parallel]: F[Nothing] = {
    for {
      client <- EmberClientBuilder.default[F]
        .withMaxResponseHeaderSize(10 * 1024)
        .build
      crawler = Crawler.impl[F](FollowRedirect(maxRedirects = 5)(client))
      httpApp = Routes.mainApiRoutes[F](crawler).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      _ <-
        EmberServerBuilder.default[F]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8880")
          .withIdleTimeout(10.minutes)
          .withHttpApp(finalHttpApp)
          .build
    } yield ()
  }.useForever
