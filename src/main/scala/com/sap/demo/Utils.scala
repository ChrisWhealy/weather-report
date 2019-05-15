package com.sap.demo

import java.time.Instant

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scalatags.JsDom.all._

@JSExport
object Utils {
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // OpenWeather endpoint details
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  def openWeatherMapHost = "openweathermap.org"
  def openWeatherMapAPI  = "https://api." + openWeatherMapHost
  def openWeatherMapImg  = "https://" + openWeatherMapHost

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // APIXU endpoint details
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  def apixuHost = "apixu.com"
  def apixuAPI  = "https://api." + apixuHost

  def weatherEndpoint    = openWeatherMapAPI + "/data/2.5/weather"
  def searchEndpoint     = openWeatherMapAPI + "/data/2.5/find"
  def imageEndpoint      = openWeatherMapImg + "/img/w/"


  var owmQueryParams = scala.collection.mutable.Map[String,String](
    "q"      -> ""
    ,"type"   -> "accurate"
    ,"mode"   -> "json"
//    ,"apikey" -> "<Paste your API Key value here>"
    ,"apikey" -> "9ff16c79edd6ad12396c22ed8a7996ec"
  )

  def openStreetMapHost  = "https://www.openstreetmap.org"
  def mapBoxHost         = "https://api.tiles.mapbox.com"

  def mapBoxEndpoint = mapBoxHost + "/v4/{id}/{z}/{x}/{y}.png"
  var mbQueryParams  = scala.collection.mutable.Map[String,String](
    "access_token" -> "pk.eyJ1IjoiZmFuY2VsbHUiLCJhIjoiY2oxMHRzZm5zMDAyMDMycndyaTZyYnp6NSJ9.AJ3owakJtFAJaaRuYB7Ukw"
  )

  def char_0  = 48
  def char_9  = 57
  def small_a = 97
  def small_f = 102

  // Inclusive "between" value check
  def between(a: Int, b: Int) = (x: Int) => x >= a && x <= b

  def isHexChar = between(small_a, small_f)
  def isNumeric = between(char_0, char_9)

  // Check if a character string is a valid hex number
  def isHexStr(s: String): Boolean = s.toLowerCase.foldLeft(true) { (acc, c) => acc && (isHexChar(c) || isNumeric(c)) }

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // Calculate bounding box for a given city
  // This is necessary because OpenWeatherMap sometimes returns multiple
  // instances of the same city, but with slightly different coordinates(?!)
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

  def toRadians(deg: Double) = deg * Math.PI / 180
  def earthDiameter = 12742

  def distance(lat1: Double, long1: Double, lat2: Double, long2: Double) = {
    val sineHalfLatDelta = Math.sin(toRadians(lat2 - lat1) / 2)
    val sineHalfLonDelta = Math.sin(toRadians(long2 - long1) / 2)

    val a = sineHalfLatDelta * sineHalfLatDelta +
            Math.cos(toRadians(lat1)) *
            Math.cos(toRadians(lat2)) *
            sineHalfLonDelta * sineHalfLonDelta

    earthDiameter * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  }

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // Various text string constants
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  def months = Array(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
  )

  def ordinalTxt = Array("th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th")

  def compassPoints = Array(
    "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
    "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
  )

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // Various string formatting functions
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

  // All times returned from OpenWeather are simply UTC time stamps
  // This does not include any time zone information!
  def utcToDateStr(utc: Long): String = {
    if (utc == 0)
      "Not available"
    else {
      var d = Instant.ofEpochSecond(utc).toString

      var year = d.substring(0, 4)
      var month = d.substring(5, 7)
      var day = d.substring(8, 10)
      var tempus = d.substring(11, 19)

      s"$tempus on ${months(month.toInt)} ${day}${ordinalTxt(day.toInt % 10)}, $year"
    }
  }

  // A temperatures in degrees centigrade are returned in Kelvin
  def kelvinToDegStr(k: Double, min: Double, max: Double):String = {
    val variation = (max - min) / 2
    (k - 272.15).toInt + "˚C" + (if (variation > 0) s" ±${variation}˚C" else "")
  }

  // Format the lat/lon coordinates into a text string
  def formatCoords(lat: Double, lon: Double): String = {
    val latStr = s"${Math.abs(lat)}˚${if (lat >= 0) "N" else "S"}"
    val lonStr = s"${Math.abs(lon)}˚${if (lon >= 0) "E" else "W"}"

    s"$latStr, $lonStr"
  }

  // The weather conditions text string is all lowercase.
  // Convert it to sentence case
  def formatDescription(d: String): String = {
    val (head, tail) = d.splitAt(1)
    head.toUpperCase + tail
  }

  // Convert the wind direction heading in degrees to the nearest compass point
  def formatHeading(h: Double): String = {
    val upper = Math.floor((h + 12.25) / 22.5).toInt % 16
    val lower = Math.floor((h - 12.25) / 22.5).toInt % 16

    h + s"˚ (${compassPoints(Math.max(upper,lower))})"
  }

  // Convert weather icon code into an actual image
  def formatIcon(id: String)              = img(src := imageEndpoint + s"$id.png")

  def formatVisibility(v: Int): String    = v + "m"
  def formatVelocity(v: Double): String   = v + "m/s"
  def formatPercentage(p: Double): String = p + "%"
  def formatPressure(p: Double): String   = p + " mBar"

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // Builds and opens an XHR request to a given endpoint
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  def buildXhrRequest(userInput: String, targetEndpoint: String): dom.XMLHttpRequest = {
    owmQueryParams += ("q" -> userInput)

    val queryStr = (
      for (p <- owmQueryParams.keys)
        yield s"$p=${owmQueryParams.get(p).get}"
      ).mkString("?", "&", "")

    val xhr = new dom.XMLHttpRequest
    xhr.open("GET", targetEndpoint + queryStr)

    xhr
  }
}
