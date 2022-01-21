import scalikejdbc._

import scala.collection.compat.Factory
import scala.jdk.IntAccumulator

object BatchedTest {
  implicit val factory: Factory[Int, IntAccumulator] = IntAccumulator
  def apply(batchSize: Int): Long = {
    DB localTx { implicit session =>
      sql"truncate table test".update.apply()
    }
    val params = GetData().map(_.productIterator.toSeq)
    Time {
      DB localTx { implicit session =>
        params.grouped(batchSize).toSeq.map { param => sql"insert into test values (?, ?)".batch(param: _*).apply() }
      }
    }
  }
}
