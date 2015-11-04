package silky.persistence

trait Persistence {
  def lastRefAcross(prefix: Char, contexts: String*): String
  def save(entry: Entry): Entry
  def find(context: String, ref: String): Option[Entry]
  def load(context: String, predicate: String ⇒ Boolean): Seq[Entry]
  def move(ref: String, source: String, target: String): Unit
}

case class Entry(context: String, ref: String, contents: String)
