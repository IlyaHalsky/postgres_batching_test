import scalikejdbc._

object BindDataTest {
  def apply(batchSize: Int): Long = {
    DB localTx { implicit session =>
      sql"truncate table test".update.apply()
    }
    val params = GetData()
    Time {
      DB localTx { implicit session =>
        params.grouped(batchSize).toSeq.map { param =>
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
  }

}
