package com.sap.demo

import scala.scalajs.js
import java.lang.{Long => JLong}

// Country, sunrise and sunset times.
class WeatherSys(sys: js.Dynamic) {
//  println(s"WeatherSys: country = ${sys.country}")
  val country = sys.country.asInstanceOf[String]

//  println(s"WeatherSys: sunrise = ${sys.sunrise}")
  val sunrise = {
    if (sys.sunrise.toString == "undefined")
      0
    else
      JLong.parseLong(sys.sunrise.toString)
  }

//  println(s"WeatherSys: sunset = ${sys.sunset}")
  val sunset = {
    if (sys.sunset.toString == "undefined")
      0
    else
      JLong.parseLong(sys.sunset.toString)
  }
}