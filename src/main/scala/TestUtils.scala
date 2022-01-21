import DBConfig.{password, url, user}
import TestMain.setupDB
import com.zaxxer.hikari.HikariDataSource
import scalikejdbc._

import java.util.UUID

object Time {
  def apply[R](r: => R): Long = {
    val start = System.currentTimeMillis()
    val result = r
    val end = System.currentTimeMillis()
    end - start
  }
}

object RandomUUID {
  def apply(): String = UUID.randomUUID().toString
}

object GetData {
  def apply(): Seq[(String, String)] = for {i <- 1 to 100000} yield (RandomUUID(), RandomUUID())
  def ofSize(size: Int): Seq[(String, String)] = for {i <- 1 to size} yield (RandomUUID(), RandomUUID())
}

trait InsertTest {
  private def setup(keep: Int): Seq[(String, String)] = {
    if (keep == 0)
      DB localTx { implicit session =>
        sql"truncate table test".update.apply()
      }
    else
      DB localTx { implicit session =>
        sql"delete from test where (a, b) in (select a, b from (select a, b, row_number() over () as rn from test) enum where enum.rn > ${keep})".update.apply()
      }
    val data = GetData()
    System.gc()
    data
  }

  def runTest(batchSize: Int, keep: Int): Long = {
    val data = setup(keep)
    Time(test(data, batchSize))
  }

  def test(data: Seq[(String, String)], batchSize: Int): Unit
}

trait BatchSizes {
  def batches: List[Int]
}

case object BinaryBatchSizes extends BatchSizes {
  def batches: List[Int] = List(128, 1024, 16384, 131072)
}

case object DecimalBatchSizes extends BatchSizes {
  def batches: List[Int] = List(100, 1000, 10000, 100000)
}

trait TestRunnerUtils {
  def reWriteBatchedInserts: Boolean
  def setupDB(): Unit = {
    val dataSource: HikariDataSource = {
      val ds = new HikariDataSource()
      ds.setJdbcUrl(s"jdbc:postgresql://$url?reWriteBatchedInserts=$reWriteBatchedInserts")
      ds.setUsername(user)
      ds.setPassword(password)
      ds.setMaxLifetime(900000)
      ds.setMaximumPoolSize(5)
      ds
    }
    val pool = new DataSourceConnectionPool(dataSource)
    ConnectionPool.singleton(pool)
    GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(
      enabled = true,
      singleLineMode = true,
      logLevel = "DEBUG"
    )
  }
  setupDB()

  def prepareTest(rows: Int): Unit = {
    DB localTx {implicit session =>
      sql"create table if not exists test (a varchar(180) not null, b varchar(180) not null, constraint test_pk primary key (a, b))".update.apply()
      SQL(s"""create or replace function insert_test(data bytea) returns int as $$$$
          |    offset = 0
          |    length = len(data)
          |    result = []
          |    while offset < length:
          |        a = data[offset: offset + 36].decode("utf-8")
          |        offset = offset + 36
          |        b = data[offset: offset + 36].decode("utf-8")
          |        offset = offset + 36
          |        result.append((a,b))
          |    if "insert_test" in SD:
          |        plan = SD["insert_test"]
          |    else:
          |        plan = plpy.prepare(\"\"\"
          |        insert into test
          |          (select inserts.a, inserts.b from unnest($$1) inserts)
          |        \"\"\",
          |        ["test[]"])
          |        SD["insert_test"] = plan
          |    changed = plan.execute([result])
          |    return changed.nrows()
          |$$$$ language plpython3u""".stripMargin).update.apply()
    }
    BindTwoArrays.test(GetData.ofSize(rows), rows)
  }

  def runTest(test: InsertTest, batchSizes: BatchSizes, times: Int = 10, keepRows: Int = 0): Unit = {
    println(s"Running ${test.getClass.getSimpleName}")
    prepareTest(keepRows)
    for {
      batchSize <- batchSizes.batches
    } yield {
      val average = (for {
        i <- 1 to times
      } yield test.runTest(batchSize, keepRows)).sum / times
      println(s"Batch size $batchSize average time is $average ms in $times runs")
    }
  }
}