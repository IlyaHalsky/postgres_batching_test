import scalikejdbc._

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.sql.Types

object BindByteArray extends InsertTest {
  def test(data: Seq[(String, String)], batchSize: Int): Unit =
    DB localTx { implicit session =>
      data.grouped(batchSize).toSeq.map { param =>
        val stmt = session.connection.prepareCall("{? = call insert_test(?)}")
        val stream = new ByteArrayOutputStream(param.size * 2 * 36)
        param.foreach { case (a, b) =>
          stream.write(a.getBytes(StandardCharsets.UTF_8))
          stream.write(b.getBytes(StandardCharsets.UTF_8))
        }
        stmt.registerOutParameter(1, Types.INTEGER)
        stmt.setBytes(2, stream.toByteArray)
        stmt.execute()
        val result = stmt.getInt(1)
        stmt.close()
        result
      }
    }
}
