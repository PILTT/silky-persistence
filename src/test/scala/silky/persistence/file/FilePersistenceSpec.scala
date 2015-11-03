package silky.persistence.file

import org.scalatest.{MustMatchers, fixture}
import silky.persistence.Entry

class FilePersistenceSpec extends fixture.WordSpec with MustMatchers with fixture.TestDataFixture {

  private val Seq(message1, ticket1, ticket2, ticket3, ticket4) = Seq(
    Entry("messages", "M00000001", "{ \"message\": \"Hello World!\" }"),
    Entry("deleted",  "T00000001", "{ \"quantity\": 1000 }"),
    Entry("tickets",  "T00000002", "{ \"quantity\": 2000 }"),
    Entry("tickets",  "T00000003", "{ \"quantity\": 3000 }"),
    Entry("incoming", "T00000004", "{ \"quantity\": 4000 }")
  )

  "lastRefAcross returns the last reference across multiple contexts" in { td ⇒
    new Fixture(td.name, ticket1, ticket2, ticket3, ticket4) {
      persistence.lastRefAcross(prefix = 'T',"deleted", "incoming", "tickets") mustBe "00000004"
    }
  }

  "find returns an entry existing in a given context" in { td ⇒
    new Fixture(td.name, message1) {
      persistence.find("messages", "M00000001") mustBe Some(message1)
    }
  }

  "find returns none for a non-existent entry in a given context" in { td ⇒
    new Fixture(td.name, message1) {
      persistence.find("messages", "M00000002") mustBe None
    }
  }

  "load returns entries whose reference matches a given predicate in a given context" in { td ⇒
    new Fixture(td.name, message1, ticket1, ticket2, ticket3, ticket4) {
      persistence.load("tickets", predicate = _.startsWith("T")) must contain only (ticket2, ticket3)
    }
  }

  abstract class Fixture(testName: String, entries: Entry*) {
    val persistence = new FilePersistence(s"target/tests/${getClass.getPackage.getName}.$suiteName/$testName/data")
    persistence.initialise(entries.map(_.context).distinct: _*)
    entries foreach persistence.save
  }
}
