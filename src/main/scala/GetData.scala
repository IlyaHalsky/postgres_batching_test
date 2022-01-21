object GetData {
  def apply(): Seq[(String, String)] = for {i <- 1 to 100000} yield (RandomUUID(), RandomUUID())
}
