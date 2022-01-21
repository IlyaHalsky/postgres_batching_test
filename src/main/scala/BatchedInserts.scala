import scalikejdbc._

import scala.collection.compat.Factory
import scala.jdk.IntAccumulator

object BatchedInserts extends InsertTest {
  implicit val factory: Factory[Int, IntAccumulator] = IntAccumulator

  def test(data: Seq[(String, String)], batchSize: Int): Unit = {
    val params = data.map(_.productIterator.toSeq)
    DB localTx { implicit session =>
      params.grouped(batchSize).toSeq.map { param => sql"insert into test values (?, ?)".batch(param: _*).apply() }
    }
  }
}
