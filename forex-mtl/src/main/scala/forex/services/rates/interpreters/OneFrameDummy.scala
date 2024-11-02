package forex.services.rates.interpreters

import forex.services.rates.Algebra
import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.{ Price, Rate, Timestamp }
import forex.services.rates.errors._
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import java.time.OffsetDateTime
import io.circe.parser.parse
import io.circe.Json


class OneFrameDummy[F[_]: Applicative] extends Algebra[F] {

  private val httpClient = HttpClient.newHttpClient()
  private val endpoint = "http://localhost:8080/rates"
  private val token = "10dc303535874aeccc86a8251e6992f5"

  def extractRate(json: Json, pair: Rate.Pair):  Either[Error, Rate] ={
    val input = for{
      price <- json.hcursor.get[BigDecimal]("price").toOption
      // if matched, then it will be BigDecimal, else None
      timestampString <- json.hcursor.get[String]("timestamp").toOption
      timestamp = Timestamp(OffsetDateTime.parse(timestampString))
    } yield Rate(pair, Price(price), timestamp)

    input.toRight(Error.OneFrameLookupFailed("Your parse failed"))
  }

  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    // Rate(pair, Price(BigDecimal(100)), Timestamp.now).asRight[Error].pure[F]
    // still need to do cache + find the right answer to return
    val query = s"$endpoint?pair=${pair.from}${pair.to}"
    val uri = URI.create(query)
    // try opening up connection and set token 
    val request = HttpRequest.newBuilder().uri(uri).header("token", token).GET().build()

    val httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

    // How to retrieve answer from connection response 
    // Store value + compare with cache
    val search = 
      if (httpResponse.statusCode() == 200){
        parse(httpResponse.body()) match {
          case Right(json) => extractRate(json, pair) 
          // If pattern matched, use extractRate to get Rate
          case Left(_) => Left(Error.OneFrameLookupFailed("JSON parse failed"))
        }
      }else {
        Error.OneFrameLookupFailed("Your query request failed").asLeft[Rate]
      }
    
    search.pure[F]
  }
    
  
  

  
    
}
