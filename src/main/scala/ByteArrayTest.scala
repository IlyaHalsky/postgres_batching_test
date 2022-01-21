import scalikejdbc._

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.sql.Types

/*
create or replace function insert_test(data bytea) returns int as $$
    offset = 0
    length = len(data)
    result = []
    while offset < length:
        a = data[offset: offset + 36].decode("utf-8")
        offset = offset + 36
        b = data[offset: offset + 36].decode("utf-8")
        offset = offset + 36
        result.append((a,b))
    if "insert_test" in SD:
        plan = SD["insert_test"]
    else:
        plan = plpy.prepare("""
        insert into test
          (select inserts.a, inserts.b from unnest($1) inserts)
        """,
        ["test[]"])
        SD["insert_test"] = plan
    changed = plan.execute([result])
    return changed.nrows()
$$ language plpython3u;
 */

object ByteArrayTest {
  def apply(batchSize: Int): Long = {
    DB localTx { implicit session =>
      sql"truncate table test".update.apply()
    }
    val params = GetData()
    Time {
      DB localTx { implicit session =>
        params.grouped(batchSize).toSeq.map { param =>
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
  }
}
