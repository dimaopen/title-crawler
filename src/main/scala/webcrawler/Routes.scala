package webcrawler

import cats.effect.Concurrent
import cats.syntax.all.*
import io.circe.Decoder
import io.circe.generic.auto.*
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import webcrawler.api.{TitleRequest, TitleResponse}

object Routes:
  given [F[_]: Concurrent]: EntityDecoder[F, TitleRequest] = jsonOf[F, TitleRequest]
  given [F[_]: Concurrent]: EntityEncoder[F, TitleResponse] = jsonEncoderOf[F, TitleResponse]

  def mainApiRoutes[F[_]: Concurrent](crawler: Crawler[F]): HttpRoutes[F] =
    val dsl = new Http4sDsl[F]{}
    import dsl.*
    HttpRoutes.of[F] {
      case req @ POST -> Root / "api" /"v1" / "tasks" =>
        for {
          request <- req.as[TitleRequest]
          result <- crawler.gatherTitles(request)
          resp <- Ok(result)
        } yield resp
    }
