package forex.services.rates.interpreters

import forex.services.rates.Algebra
import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.{ Price, Rate, Timestamp, Currency}
import forex.services.rates.errors._
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import java.time.OffsetDateTime
import io.circe.parser.parse
import io.circe.Json
import scala.collection.mutable


class OneFrameDummy[F[_]: Applicative] extends Algebra[F] {

  private val httpClient = HttpClient.newHttpClient()
  private val endpoint = "http://localhost:8080/rates"
  private val token = "10dc303535874aeccc86a8251e6992f5"
  
  // Initialize cache using mutable.Map -> key being pair, value being Rate
  private val cache = mutable.Map.empty[Rate.Pair, Rate]
  val allCurrencies = List(Currency.AUD, Currency.CAD, Currency.CHF, Currency.EUR, Currency.GBP, Currency.NZD, Currency.JPY, Currency.SGD, Currency.USD)

  private val allPairs = for{
    c1 <- allCurrencies
    c2 <- allCurrencies if c1 != c2
  } yield Rate.Pair(c1, c2)
  
  def getCache(pair: Rate.Pair): Option[Rate]= {
    // choosing flatMap here allows else case to simply be None instead of Some[None]
    cache.get(pair).flatMap { 
    rate =>
      val checkTime = Timestamp(OffsetDateTime.now.minusMinutes(5))
      if(rate.timestamp.value.isAfter(checkTime.value)){
        Some(rate)
      }
      else {
        cache.remove(pair)
        None
      }
    }
  }
  
  def cacheRate(pair: Rate.Pair, rate: Rate): Rate = {
    cache.put(pair, rate)
    rate // Return rate
  }

  def extractRate(json: Json, pair: Rate.Pair):  Either[Error, Rate] ={
    val input = for{
      firstRateObject <- json.asArray.flatMap(array => array.headOption)
      // get the first rate object of json as Array 
      price <- firstRateObject.hcursor.get[BigDecimal]("price").toOption
      // if matched, then it will be BigDecimal, else None
      timestampString <- firstRateObject.hcursor.get[String]("time_stamp").toOption
      timestamp = Timestamp(OffsetDateTime.parse(timestampString))
      
    } yield Rate(pair, Price(price), timestamp)
    input.toRight(Error.OneFrameLookupFailed("Your parse failed"))
  }

  def newExtractRate(json: Json):  Either[Error, List[Rate]] ={
    json.asArray.map{ ratesArray => 
      ratesArray.flatMap{ singleRate => 
        for{
          from <- singleRate.hcursor.get[String]("from").toOption
          to <- singleRate.hcursor.get[String]("to").toOption
          price <- singleRate.hcursor.get[BigDecimal]("price").toOption
          // if matched, then it will be BigDecimal, else None
          timestampString <- singleRate.hcursor.get[String]("time_stamp").toOption
          timestamp = Timestamp(OffsetDateTime.parse(timestampString))
        } yield{
          val currentPair = Rate.Pair(Currency.fromString(from), Currency.fromString(to))
          val currentRate = Rate(currentPair, Price(price), timestamp)
          cacheRate(currentPair, currentRate)
        }
      }.toList // Right: List[Rate]
    }.toRight(Error.OneFrameLookupFailed("At least one of parsing failed"))
  }

  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    getCache(pair) match{
      case Some(rate) => (Right(rate): Either[Error, Rate]).pure[F]
      // match has two cases from getCache, if found, then rate can be returned
      case None => 
        
        val query = s"$endpoint?pair=${pair.from}${pair.to}"
        val uri = URI.create(query)
        // try opening up connection and set token 
        val request = HttpRequest.newBuilder().uri(uri).header("token", token).GET().build()
        val httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        
        val search = 
          if (httpResponse.statusCode() == 200){
            parse(httpResponse.body()) match {
              case Right(json) => extractRate(json, pair).map(rate => cacheRate(pair, rate))
              // If pattern matched, use extractRate to get Rate
              case Left(_) => Left(Error.OneFrameLookupFailed("Fail to parse your JSON"))
            }
          }else {
            Error.OneFrameLookupFailed("Your query request failed").asLeft[Rate]
          }
        println(search)
        search.pure[F]//get the results back to function type F      
    }
  } 
}
