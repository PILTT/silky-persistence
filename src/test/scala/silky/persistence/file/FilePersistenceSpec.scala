package silky.persistence.file

import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.fixture
import silky.persistence.Entry
import silky.persistence.FutureMatchers._

class FilePersistenceSpec extends fixture.WordSpec with fixture.TestDataFixture {

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
      persistence.move(ticket4.ref, ticket4.context, "tickets") willReturn ticket4.copy(context = "tickets")
      persistence.find(ticket4.context, ticket4.ref) willReturn None
    }
  }

  "move fails for a non-existent entry" in { td ⇒
    import org.scalatest.MustMatchers._
    import org.scalatest.exceptions.TestFailedException

    new Fixture(td.name, ticket4) {
      val expectedMessage = s"requirement failed: Entry 'foo' not found in 'thin-air': $baseDir/thin-air/foo.json does not exist"
      val exception = the [TestFailedException] thrownBy persistence.move("foo", "thin-air", "tickets").futureValue
      exception.getCause must have message expectedMessage
    }
  }

  abstract class Fixture(testName: String, entries: Entry*) {
    val baseDir = s"target/tests/${getClass.getPackage.getName}.$suiteName/$testName/data"
    val persistence = new FilePersistence(baseDir)(concurrent.ExecutionContext.global)
    entries map persistence.save map { _.futureValue }
  }
}
