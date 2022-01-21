
object TestMain extends App with TestRunnerUtils {
  def reWriteBatchedInserts = true

  val batchedInserts: InsertTest = BatchedInserts
  val bindTwoArraysInserts: InsertTest = BindTwoArrays
  val bindByteArrayInserts: InsertTest = BindByteArray

  val binaryBatches = BinaryBatchSizes
  val decimalBatches = DecimalBatchSizes
  // Call tests here
  runTest(batchedInserts, binaryBatches, keepRows = 100000)
  runTest(bindTwoArraysInserts, decimalBatches, keepRows = 100000)
  runTest(bindByteArrayInserts, decimalBatches, keepRows = 100000)
}
