package domain

case class TableSearch(
  global_search: Option[String] = None,
  sort_by:       Option[String] = None,
  order_by:      Option[String] = Some("asc"),
  offset:        Option[Int]    = Some(0),
  limit:         Option[Int]    = Some(30)
) {
  require(
    (order_by.map(s => s.equalsIgnoreCase("asc") || s.equalsIgnoreCase("desc")).getOrElse(true)),
    "order_by should be None or asc or desc"
  )
  require(sort_by.map(_.matches("[a-zA-Z0-9\\-\\_]*")).getOrElse(true), "sort_by should be a normal name")
}

case class SearchWithTotalSize[T](
  total_size: Int     = 0,
  data:       List[T] = List()
)
