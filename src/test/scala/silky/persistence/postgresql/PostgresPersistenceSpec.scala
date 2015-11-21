package silky.persistence.postgresql

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.exceptions.TestFailedException
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, MustMatchers, OptionValues, WordSpec}
import silky.persistence.Entry
import silky.persistence.postgresql.PostgresDriver.api._

class PostgresPersistenceSpec extends WordSpec with MustMatchers with BeforeAndAfterAll with ScalaFutures with OptionValues {
  override implicit def patienceConfig = PatienceConfig(timeout = Span(2, Seconds), interval = Span(50, Millis))

  private val db = Database.forURL(
    url    = "jdbc:postgresql://localhost:5432/silky_db",
    driver = "org.postgresql.Driver",
    user   = "silky_app")

  private val persistence = new PostgresPersistence(db)(concurrent.ExecutionContext.global)

  private val message1 :: ticket1 :: ticket2 :: ticket3 :: ticket4 :: xs = Seq(
    Entry("messages", "M00000001", """{ "metadata": { "status": "Pending" }, "message": "Hello World!" }"""),
    Entry("tickets",  "T00000001", """{ "metadata": { "status": "Deleted" }, "quantity":  100 }"""),
    Entry("tickets",  "T00000002", """{ "metadata": { "status": "Active"  }, "quantity":  200 }"""),
    Entry("tickets",  "T00000003", """{ "metadata": { "status": "Active"  }, "quantity":  300 }"""),
    Entry("incoming", "T00000004", """{ "metadata": { "status": "Pending" }, "quantity":  400 }"""),
    Entry("tickets",  "T00000005", """{ "metadata": { "status": "Deleted" }, "quantity":  500 }"""),
    Entry("tickets",  "T00000006", """{ "metadata": { "status": "Active"  }, "quantity":  600 }"""),
    Entry("incoming", "T00000007", """{ "metadata": { "status": "Pending" }, "quantity":  700 }"""),
    Entry("tickets",  "T00000008", """{ "metadata": { "status": "Active"  }, "quantity":  800 }"""),
    Entry("tickets",  "T00000009", """{ "metadata": { "status": "Deleted" }, "quantity":  900 }"""),
    Entry("tickets",  "T00000010", """{ "metadata": { "status": "Active"  }, "quantity": 1000 }"""),
    Entry("tickets",  "T00000011", """{ "metadata": { "status": "Active"  }, "quantity": 1100 }"""),
    Entry("tickets",  "T00000012", """{ "metadata": { "status": "Active"  }, "quantity": 1200 }"""),
    Entry("incoming", "T00000013", """{ "metadata": { "status": "Pending" }, "quantity": 1300 }"""),
    Entry("incoming", "T00000014", """{ "metadata": { "status": "Pending" }, "quantity": 1400 }"""),
    Entry("incoming", "T00000015", """{ "metadata": { "status": "Pending" }, "quantity": 1500 }""")
  )

  override protected def beforeAll() = {
    persistence.createSchema().futureValue
    message1 :: ticket1 :: ticket2 :: ticket3 :: ticket4 :: xs map persistence.save map { _.futureValue }
    persistence.optimise()
  }

  override protected def afterAll() = { persistence.dropSchema().futureValue; db.close() }

  "lastRefAcross returns the last reference across multiple contexts" in {
    persistence.lastRefAcross(prefix = 'T', "deleted", "incoming", "tickets").futureValue mustBe "00000015"
  }

  "lastRefAcross returns '00000000' when no matching entries are found in a given set of contexts" in {
    persistence.lastRefAcross(prefix = 'T', "one", "two", "three").futureValue mustBe "00000000"
  }

  "find returns an entry existing in a given context" in {
    persistence.find("messages", "M00000001").futureValue.value.ref mustBe message1.ref
  }

  "find returns none for a non-existent entry in a given context" in {
    persistence.find("messages", "M00000002").futureValue mustBe None
  }

  "load returns entries whose reference matches a given predicate in a given context" in {
    val tickets = persistence.load("tickets", predicate = _.matches("T0000000[1-4]")).futureValue
    tickets.map(_.ref) must contain only (ticket2.ref, ticket3.ref)
  }

  "move simply moves the file containing the given entry from one directory (context) to another" in {
    val moved = persistence.move(ticket4.ref, "incoming", "tickets").futureValue
    (moved.context, moved.ref) mustBe ("tickets", ticket4.ref)
  }

  "save writes updated contents to the database" in {
    val updated = message1.copy(contents = """{"message": "Hello New World!"}""")
    persistence.save(updated).futureValue
    persistence.find(message1.context, message1.ref).futureValue.value mustBe updated
  }

  "save cannot update an entry with a different context but the same ref" in {
    a [TestFailedException] mustBe thrownBy (persistence.save(message1.copy(context = "foo")).futureValue)
  }
}
