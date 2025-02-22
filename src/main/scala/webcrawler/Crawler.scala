package webcrawler

import cats.Parallel
import cats.effect.{Async, Concurrent, Sync}
import cats.syntax.all.*
import fs2.*
import fs2.io.toInputStream
import org.http4s.Method.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.ember.core.EmberException
import org.http4s.{Response, Uri}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.xml.sax.InputSource
import webcrawler.HtmlEvents.*
import webcrawler.api.CrawlerError

import java.io.IOException
import scala.xml.SAXParseException

/**
 * @author Dmitry Openkov
 */
trait Crawler[F[_]]:
  def gatherTitles(uris: Seq[Uri]): F[Seq[(Uri, Either[CrawlerError, String])]]


object Crawler {
  implicit def logger[F[_] : Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  def impl[F[_] : Async : Parallel](client: Client[F]): Crawler[F] =
    new Crawler[F]:
      private val dsl = new Http4sClientDsl[F] {}

      import dsl.*

      override def gatherTitles(uris: Seq[Uri]): F[Seq[(Uri, Either[CrawlerError, String])]] =
        uris.parTraverse { uri =>
          for {
            _ <- Logger[F].info(s"Getting $uri")
            title <- client.stream(GET(uri))
              .flatMap(responseToInputSource)
              .flatMap(htmlEvents)
              .through(findTitle)
              .recover {
                case e: EmberException => CrawlerError("HTTP_CLIENT_ERROR", e.getMessage)
                case e: SAXParseException => CrawlerError("BAD_HTML", e.getMessage)
                case e: IOException => CrawlerError("CONNECTION_ERROR", e.getMessage)
              }
              .compile.toList
            titleResult = title match
              case (title: String) :: tail => Right(title)
              case (error: CrawlerError) :: tail => Left(error)
              case Nil => Left(CrawlerError("NO_TITLE", "No title found"))
          } yield uri -> titleResult
        }


  def responseToInputSource[F[_] : Async](response: Response[F]): Stream[F, InputSource] =
    response.body.through(toInputStream).map { inputStream =>
      val is = new InputSource()
      response.charset.foreach(charset => is.setEncoding(charset.nioCharset.name))
      is.setByteStream(inputStream)
      is
    }

  def findTitle[F[_]]: Pipe[F, HtmlEvent, String] =
    val titlePath = "title" :: "head" :: "html" :: Nil

    def go(s: Stream[F, HtmlEvent], state: (IndexedSeq[String], List[String])): Pull[F, String, Unit] = {
      val (title, path) = state
      s.pull.uncons1.flatMap {
        case Some((event, tail)) =>
          event match
            case StartTag(name) =>
              go(tail, (title, name.toLowerCase :: path))
            case EndTag(name) if name == "title" && path == titlePath =>
              Pull.output1(title.mkString("")) >> Pull.done
            case EndTag(name) =>
              go(tail, (title, path.tail))
            case Text(s) if path == titlePath =>
              go(tail, (title :+ s, path))
            case _ =>
              go(tail, state)
        case None => Pull.done
      }
    }

    in => go(in, (IndexedSeq.empty, List.empty)).stream
}
