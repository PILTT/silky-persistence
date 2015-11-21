package silky.persistence.postgresql

import com.github.tminglei.slickpg.{ExPostgresDriver, PgJsonSupport}

trait PostgresDriver extends ExPostgresDriver with PgJsonSupport {
  override val api = new API with JsonImplicits {}

  def pgjson = "jsonb"
}

object PostgresDriver extends PostgresDriver
