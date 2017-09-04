package com.sap.demo

import scala.scalajs.js

// Latitude and logitude of a city
class Coord(coord: js.Dynamic) {
//  println(s"Coord: lon = ${coord.lon}")
//  println(s"Coord: lat = ${coord.lat}")

  val lon = coord.lon.asInstanceOf[Double]
  val lat = coord.lat.asInstanceOf[Double]
}
