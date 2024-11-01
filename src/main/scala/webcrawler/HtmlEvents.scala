package webcrawler

import cats.data.EitherT
import cats.effect.Async
import cats.effect.std.{Dispatcher, Queue}
import cats.syntax.all.*
import org.http4s.{DecodeFailure, EntityDecoder, MalformedMessageBodyFailure, MediaType}
import fs2.io.toInputStreamResource
import fs2.Stream
import org.xml.sax.{Attributes, InputSource}
import org.xml.sax.helpers.DefaultHandler
import webcrawler.HtmlEvents.{EndTag, StartTag, Text}

import javax.xml.parsers.SAXParser
import scala.xml.{Elem, SAXParseException, XML}

/**
 * @author Dmitry Openkov
 */
object HtmlEvents {
  sealed trait HtmlEvent

  case class StartTag(name: String) extends HtmlEvent
  case class EndTag(name: String) extends HtmlEvent
  case class Text(s: String) extends HtmlEvent

  private val saxFactory = new org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl()
  
  def elements[F[_]](inputSource: InputSource)(implicit F: Async[F]): Stream[F, HtmlEvent] = {
    for {
      dispatcher <- Stream.resource(Dispatcher.sequential[F])
      queue <- Stream.eval(Queue.unbounded[F, Option[HtmlEvent]])
      _ <- Stream.eval {
        F.delay {
          def enqueue(v: Option[HtmlEvent]): Unit = dispatcher.unsafeRunAndForget(queue.offer(v))
          
          val parser = saxFactory.newSAXParser()
          parser.parse(inputSource, new DefaultHandler() {
            override def startElement(
              uri: String,
              localName: String,
              qName: String,
              attributes: Attributes
            ): Unit = enqueue(Some(StartTag(localName)))

            override def endElement(
              uri: String,
              localName: String,
              qName: String
            ): Unit = enqueue(Some(EndTag(localName)))

            override def characters(ch: Array[Char], start: Int, length: Int): Unit =
              enqueue(Some(Text(new String(ch, start, length))))
          })
          // Upon returning from withRows, signal that our stream has ended.
          enqueue(None)
        }
      }
      // Due to `fromQueueNoneTerminated`, the stream will terminate when it encounters a `None` value
      row <- Stream.fromQueueNoneTerminated(queue)
    } yield row
  }

}
