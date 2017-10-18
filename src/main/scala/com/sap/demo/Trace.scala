package com.sap.demo

import scalatags.JsDom.all.s

object Trace {
  def flow(lib: String)(fn: String)(isStart: Option[Boolean] = null)(retVal: Any): Any = {
    val ptrStr: String = isStart match {
      case Some(b) => if (b) "-->" else "<--"
      case None    => "<-->"
    }

    println(s"${java.time.LocalTime.now()} $ptrStr $lib.$fn")

    return retVal
  }
}
