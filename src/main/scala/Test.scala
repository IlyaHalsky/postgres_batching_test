import com.zaxxer.hikari.HikariDataSource
import scalikejdbc._

import java.util.UUID

object RandomUUID {
  def apply(): String = UUID.randomUUID().toString
}

object Time {
  def apply[R](r: => R): Long = {
    val start = System.currentTimeMillis()
    val result = r
    val end = System.currentTimeMillis()
    end - start
  }
}

object Test extends App {
  val dataSource: HikariDataSource = {
    val ds = new HikariDataSource()
    ds.setJdbcUrl("jdbc:postgresql://host:port/db?reWriteBatchedInserts=true")
    ds.setUsername("user")
    ds.setPassword("password")
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

  def batchedTest = {
    (for {
      batchSize <- List(128, 1024, 16384, 131072)
    } yield {
      val average = (for {
        i <- 1 to 10
      } yield BatchedTest(batchSize)).sum / 10
      average -> batchSize
    }).foreach { case (av, bs) => println(s"Batch size $bs average time is $av ms") }
  }

  def arrayTest = {
    (for {
      batchSize <- List(100, 1000, 10000, 100000)
    } yield {
      val average = (for {
        i <- 1 to 10
      } yield BindDataTest(batchSize)).sum / 10
      average -> batchSize
    }).foreach { case (av, bs) => println(s"Batch size $bs average time is $av ms") }
  }

  arrayTest
}
