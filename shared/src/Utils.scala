import zio.*

/**
 * Custom extension methods for ZIO Effects
 */
extension [R, E, A](z: ZIO[R, E, A]) {

  /**
   * Measure the execution time for the wrapped effect. Returns a tuple
   * containing the effect result, time in milisseconds and description.
   * {{{
   *   for
   *     res <- my\Effect().measureTime("description")
   *     (e, t, d) = res
   *   ...
   *   yield ()
   * }}}
   *
   * @param description
   *   the description to be returned
   * @return
   *   a tuple containing the effect result, the execution time in milisseconds
   *   and the description.
   */
  def measureTime(description: String = ""): ZIO[R, E, (A, Double, String)] =
    for
      r       <- z.timed
      (t, res) = r
    yield (res, t.toNanos / 1000000.0, description)

  /**
   * Measure the execution time for the wrapped effect printing the output to
   * console.
   *
   * @param description
   *   the description to be used in console print
   * @return
   *   the effect result
   */
  def measureTimeConsole(description: String = z.toString): ZIO[R, Any, A] =
    for
      res      <- z.measureTime(description)
      (r, t, d) = res // Let's not depend on `-source:future`
      logMsg    = s"⏲ Execution of \"$d\" took $t milis."
      _        <- ZIO.logDebug(logMsg)
    yield r
}
