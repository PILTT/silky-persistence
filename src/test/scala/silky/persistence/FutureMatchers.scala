package silky.persistence

import org.scalatest.words.MatcherWords

object FutureMatchers extends MatcherWords {
  import org.scalatest.concurrent.ScalaFutures._
  import scala.concurrent.Future

  implicit def toActualFuture[T](future: Future[T]): ActualFuture[T] = new ActualFuture[T](future)

  private[persistence] class ActualFuture[T](concept: FutureConcept[T]) {
    import org.scalatest.Assertion
    import org.scalatest.MustMatchers._
    import org.scalatest.OptionValues._
    import org.scalatest.words.{ContainWord, ResultOfContainWord}

    def will(containWord : ContainWord) : ResultOfContainWord[T] = concept.futureValue must containWord

    def willReturn(expected: Any): Assertion = actual mustBe expected

    private def actual: T = concept.futureValue match {
      case r: Some[T] ⇒ r.value
      case r ⇒ r
    }
  }
}
