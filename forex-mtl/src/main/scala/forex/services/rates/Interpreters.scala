package forex.services.rates

import cats.Applicative
import interpreters._

// If want to provide scalability, we can use cache to support no longer than 5 minutes request
  // In that, we can avoid keep calling OneFrameAPI
  // Rates is predefined and use http get response to consume OneFrameAPI
object Interpreters {
  
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()

}
