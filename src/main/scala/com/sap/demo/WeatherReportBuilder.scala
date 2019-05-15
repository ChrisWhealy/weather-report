package com.sap.demo

import scala.scalajs.js
import java.lang.{Long => JLong}

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// Class initialiser extracts weather data from JSON response
// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
class CityWeather(data: js.Dynamic) {
  // City coordinates
  val coord = new Coord(data.coord)

  // Multiple weather conditions might be supplied for one location
  val weatherConditions = data.weather.map { (weatherItem: js.Dynamic) =>
    new WeatherCond(weatherItem)
  }.asInstanceOf[js.Array[WeatherCond]]
    .toSeq

  // Extract basic atmospheric conditions:
  val main = new WeatherMain(data.main)

  // Wind speed and optional direction (given as a heading in degrees)
  val wind = new Wind(data.wind)

  // Optional visibility information
  val visibility = {
//    println(s"WeatherReportBuilder: visibility = ${data.visibility}")

    if (data.visibility.toString == "undefined")
      0
    else
      data.visibility.asInstanceOf[Int]
  }

  // Percentage cloud cover
  val clouds = {
//    println(s"WeatherReportBuilder: clouds = ${data.clouds.all}")

    data.clouds.all.asInstanceOf[Int]
  }

  // Optional rain and snow fall information
  val rain = {
    if (data.rain == null || data.rain.toString == "undefined") {
//      println(s"WeatherReportBuilder: rain = null or undefined")
      0.0
    }
    else {
//      println(s"WeatherReportBuilder: rain = ${data.rain.`3h`}")
      data.rain.`3h`.asInstanceOf[Double]
    }
  }

  val snow = {
    if (data.snow == null || data.snow.toString == "undefined") {
//      println(s"WeatherReportBuilder: snow = null or undefined")
      0.0
    }
    else {
//      println(s"WeatherReportBuilder: snow = ${data.snow.`3h`}")
      data.snow.`3h`.asInstanceOf[Double]
    }
  }

  // UTC timestamp showing when measurements were taken
//  println(s"WeatherReportBuilder: measuredAt = ${data.dt}")
  val measuredAt = JLong.parseLong(data.dt.toString)

  // Country, sunrise and sunset times.
  val weatherSys = new WeatherSys(data.sys)

  // City Id and Name.  HTTP response code
//  println(s"WeatherReportBuilder: cityId = ${data.id}")
  val cityId     = data.id.asInstanceOf[Int]

//  println(s"WeatherReportBuilder: cityName = ${data.name}")
  val cityName   = data.name.asInstanceOf[String]

//  println(s"WeatherReportBuilder: returnCode = ${data.cod}")
  val returnCode = {
    if (data.cod.toString == "undefined")
      -1
    else
      data.cod.asInstanceOf[Int]
  }
}

