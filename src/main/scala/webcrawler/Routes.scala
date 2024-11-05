package webcrawler

import cats.effect.Concurrent
import cats.syntax.all.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, Json}
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, Uri}
import webcrawler.api.{CrawlerError, TitleRequest, TitleResponse}

object Routes:
  given [F[_] : Concurrent]: EntityDecoder[F, TitleRequest] = jsonOf[F, TitleRequest]

  given Encoder[CrawlerError] = Encoder.forProduct2("error_code", "error_message") { crawlerError =>
    (crawlerError.code, crawlerError.message)
  }

  given Encoder[(Uri, Either[CrawlerError, String])] = (uriResult: (Uri, Either[CrawlerError, String])) =>
    val (uri, result) = uriResult
    val jsonResult = result match
      case Left(error) => Json.obj("error" -> error.asJson)
      case Right(title) => Json.obj("title" -> title.asJson)
    Json.obj(
      "uri" -> uri.asJson,
    ).deepMerge(jsonResult)

  given [F[_] : Concurrent]: EntityEncoder[F, TitleResponse] = jsonEncoderOf[F, TitleResponse]

  def mainApiRoutes[F[_] : Concurrent](crawler: Crawler[F]): HttpRoutes[F] =
    val dsl = new Http4sDsl[F] {}
    import dsl.*
    HttpRoutes.of[F] {
      case req@POST -> Root / "api" / "v1" / "tasks" =>
        for {
          request <- req.as[TitleRequest]
          result <- crawler.gatherTitles(request)
          resp <- Ok(result)
        } yield resp
    }
