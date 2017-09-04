package com.sap.demo

import scala.scalajs.js

// Basic atmospheric conditions:
// o Air temperature plus optional max/min variation values
// o Humidity
// o Air pressure.  This is either a simple value in mBar, or it is a
//   pair of values: air pressure at sea level and air pressure at
//   ground level
class WeatherMain(weatherMain: js.Dynamic) {
//  println(s"WeatherMain: temp = ${weatherMain.temp}")
//  println(s"WeatherMain: pressure = ${weatherMain.pressure}")
//  println(s"WeatherMain: humidity = ${weatherMain.humidity}")
//  println(s"WeatherMain: temp_min = ${weatherMain.temp_min}")
//  println(s"WeatherMain: temp_max = ${weatherMain.temp_max}")
//  println(s"WeatherMain: sea_level = ${weatherMain.sea_level}")
//  println(s"WeatherMain: grnd_level = ${weatherMain.grnd_level}")

  val temp        = weatherMain.temp.asInstanceOf[Double]
  val airPressure = weatherMain.pressure.asInstanceOf[Double]
  val humidity    = weatherMain.humidity.asInstanceOf[Int]
  val temp_min    = weatherMain.temp_min.asInstanceOf[Double]
  val temp_max    = weatherMain.temp_max.asInstanceOf[Double]

  val sea_level = {
    if (weatherMain.sea_level.toString == "undefined")
      0.0
    else
      weatherMain.sea_level.asInstanceOf[Double]
  }

  val grnd_level = {
    if (weatherMain.grnd_level.toString == "undefined")
      0.0
    else
      weatherMain.grnd_level.asInstanceOf[Double]
  }
}
