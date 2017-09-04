package com.sap.demo

import scala.scalajs.js

// A set of weather conditions.  Each city will have one or more
// of these objects
class WeatherCond(weatherItem: js.Dynamic) {
//  println(s"WeatherCond: id = ${weatherItem.id}")
//  println(s"WeatherCond: main = ${weatherItem.main}")
//  println(s"WeatherCond: description = ${weatherItem.description}")
//  println(s"WeatherCond: icon = ${weatherItem.icon}")

  val weatherId = weatherItem.id.asInstanceOf[Int]
  val main      = weatherItem.main.asInstanceOf[String]
  val desc      = weatherItem.description.asInstanceOf[String]
  val icon      = weatherItem.icon.asInstanceOf[String]
}
