package silky.persistence.postgresql

import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.{BeforeAndAfterAll, WordSpec}
import silky.persistence.Entry
import silky.persistence.postgresql.PostgresDriver.api._
import silky.persistence.FutureMatchers._

class PostgresPersistenceSpec extends WordSpec with BeforeAndAfterAll {
  import org.scalatest.time.{Millis, Seconds, Span}

  implicit def patienceConfig = PatienceConfig(timeout = scaled(Span(10, Seconds)), interval = scaled(Span(100, Millis)))
  implicit val execution = concurrent.ExecutionContext.global

  private val db = Database.forURL(
    url    = "jdbc:postgresql://localhost:5432/silky_db",
    driver = "org.postgresql.Driver",
    user   = "silky_app")

  private val persistence = new PostgresPersistence(db)

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
    persistence.lastRefAcross(prefix = 'T', "deleted", "incoming", "tickets") willReturn "00000015"
  }

  "lastRefAcross returns '00000000' when no matching entries are found in a given set of contexts" in {
    persistence.lastRefAcross(prefix = 'T', "one", "two", "three") willReturn "00000000"
  }

  "find returns an entry existing in a given context" in {
    persistence.find("messages", "M00000001").map(_.get.ref) willReturn message1.ref
  }

  "find returns none for a non-existent entry in a given context" in {
    persistence.find("messages", "M00000002") willReturn None
  }

  "load returns entries whose reference matches a given predicate in a given context" in {
    val tickets = persistence.load("tickets", predicate = _.matches("T0000000[1-4]"))
    tickets.map(v ⇒ v.map(_.ref)) will contain only (ticket2.ref, ticket3.ref)
  }

  "move simply moves the file containing the given entry from one directory (context) to another" in {
    val moved = persistence.move(ticket4.ref, "incoming", "tickets")
    moved.map(v ⇒ (v.context, v.ref)) willReturn ("tickets", ticket4.ref)
  }

  "move fails for a non-existent entry" in {
    import org.scalatest.MustMatchers._
    import org.scalatest.exceptions.TestFailedException

    val exception = the [TestFailedException] thrownBy persistence.move("foo", "thin-air", "tickets").futureValue
    exception.getCause must have message s"requirement failed: Entry 'foo' not found in 'thin-air'"
  }

  "save writes updated contents to the database" in {
    val updated = message1.copy(contents = """{"message": "Hello New World!"}""")
    persistence.save(updated).futureValue
    persistence.find(message1.context, message1.ref) willReturn updated
  }

  "save cannot update an entry with a different context but the same ref" in {
    import org.scalatest.MustMatchers._
    import org.scalatest.exceptions.TestFailedException

    a [TestFailedException] mustBe thrownBy (persistence.save(message1.copy(context = "foo")).futureValue)
  }
}
