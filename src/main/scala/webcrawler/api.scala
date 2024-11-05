package webcrawler

import org.http4s.Uri

/**
 * @author Dmitry Openkov
 */
object api {
  case class CrawlerError(code: String, message: String)

  type TitleRequest = Seq[Uri]
  type TitleResponse = Seq[(Uri, Either[CrawlerError, String])]

  

}
