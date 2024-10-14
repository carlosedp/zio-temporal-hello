package e2etest

object E2ETestUtils:
  // This method checks if there is a port open on the given host
  def isPortOpen(host: String, port: Int): Boolean =
    try
      val socket = new java.net.Socket(host, port)
      socket.close()
      true
    catch
      case _: java.io.IOException => false
