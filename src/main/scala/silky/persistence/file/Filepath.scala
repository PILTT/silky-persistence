package silky.persistence.file

import java.nio.charset.StandardCharsets._
import java.nio.file.Files._
import java.nio.file.StandardCopyOption._
import java.nio.file.StandardOpenOption._
import java.nio.file.{Files, Path}

object Filepath {
  def save(content: String, path: Path): Path = write(path, content.getBytes(UTF_8), CREATE, WRITE, TRUNCATE_EXISTING)
  def move(source: Path, target: Path):  Path = Files.move(source, target, ATOMIC_MOVE)
}
