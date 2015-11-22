package silky.persistence.file

import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.fixture
import org.scalatest.time.{Millis, Seconds, Span}
import silky.persistence.Entry
import silky.persistence.FutureMatchers._

class FilePersistenceSpec extends fixture.WordSpec with fixture.TestDataFixture {
  implicit def patienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(50, Millis))

  private val Seq(message1, ticket1, ticket2, ticket3, ticket4) = Seq(
    Entry("messages", "M00000001", """{ "message": "Hello World!" }"""),
    Entry("deleted",  "T00000001", """{ "quantity": 1000 }"""),
    Entry("tickets",  "T00000002", """{ "quantity": 2000 }"""),
    Entry("tickets",  "T00000003", """{ "quantity": 3000 }"""),
    Entry("incoming", "T00000004", """{ "quantity": 4000 }""")
  )

  "lastRefAcross returns the last reference across multiple contexts" in { td ⇒
    new Fixture(td.name, ticket1, ticket2, ticket3, ticket4) {
      persistence.lastRefAcross(prefix = 'T', "deleted", "incoming", "tickets") willReturn "00000004"
    }
  }

  "lastRefAcross returns '00000000' when no matching entries are found in a given set of contexts" in { td ⇒
    new Fixture(td.name, ticket1, ticket2, ticket3, ticket4) {
      persistence.lastRefAcross(prefix = 'T', "one", "two", "three") willReturn "00000000"
    }
  }

  "find returns an entry existing in a given context" in { td ⇒
    new Fixture(td.name, message1) {
      persistence.find("messages", "M00000001") willReturn message1
    }
  }

  "find returns none for a non-existent entry in a given context" in { td ⇒
    new Fixture(td.name, message1) {
      persistence.find("messages", "M00000002") willReturn None
    }
  }

  "load returns entries whose reference matches a given predicate in a given context" in { td ⇒
    new Fixture(td.name, message1, ticket1, ticket2, ticket3, ticket4) {
      persistence.load("tickets", predicate = _.startsWith("T")) will contain only (ticket2, ticket3)
    }
  }

  "move simply moves the file containing the given entry from one directory (context) to another" in { td ⇒
    new Fixture(td.name, ticket4) {
      val target = "tickets"
      persistence.move(ticket4.ref, ticket4.context, target).futureValue
      persistence.find(target, ticket4.ref) willReturn ticket4.copy(context = target)
    }
  }

  abstract class Fixture(testName: String, entries: Entry*) {
    val baseDir = s"target/tests/${getClass.getPackage.getName}.$suiteName/$testName/data"
    val persistence = new FilePersistence(baseDir)(concurrent.ExecutionContext.global)
    entries map persistence.save map { _.futureValue }
  }
}
