import scalikejdbc._

object BindTwoArrays extends InsertTest {
  def test(data: Seq[(String, String)], batchSize: Int): Unit =
    DB localTx { implicit session =>
      data.grouped(batchSize).toSeq.map { param =>
        val (a, b) = param.unzip
        val stmt = session.connection.prepareStatement("insert into test (a, b) (select a, b from (select unnest(?) as a, unnest(?) as b) input);")
        val aArray = session.connection.createArrayOf("varchar", a.toArray)
        stmt.setArray(1, aArray)
        val bArray = session.connection.createArrayOf("varchar", b.toArray)
        stmt.setArray(2, bArray)
        stmt.execute()
        aArray.free()
        bArray.free()
      }
    }
}
