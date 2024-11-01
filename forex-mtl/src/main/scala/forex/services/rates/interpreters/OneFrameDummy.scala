package forex.services.rates.interpreters

import forex.services.rates.Algebra
import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.{ Price, Rate, Timestamp }
import forex.services.rates.errors._
import java.net.HttpURLConnection
import java.net.URL


class OneFrameDummy[F[_]: Applicative] extends Algebra[F] {

  private val endpoint = "http://localhost:8080/rates"
  private val token = "10dc303535874aeccc86a8251e6992f5"
  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    // Rate(pair, Price(BigDecimal(100)), Timestamp.now).asRight[Error].pure[F]
    // setup the query endpoint
    val query = s"$endpoint?pair=${pair.from}${pair.to}"

    // try opening up connection and set token 
    // still need to do cache + find the right answer to return
    // some easy test cases
    val connection = new URL(query).openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestProperty("token", token)

    val search = 
      if (connection.getResponseCode == 200) Rate(pair, Price(BigDecimal(100)), Timestamp.now).asRight[Error]
      else Error.OneFrameLookupFailed("Your query request failed").asLeft[Rate]
    
    search.pure[F]
  }
    
  
  

  
    
}
