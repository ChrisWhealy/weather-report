package com.sap.demo

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom

import java.time._

import com.felstar.scalajs.leaflet._

import scalatags.JsDom.all._

@JSExport
object WeatherReport {
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // OpenWeather endpoint details
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  def openWeatherMapHost = "http://api.openweathermap.org"
  def openStreetMapHost  = "https://www.openstreetmap.org"
  def timeZoneDbHost     = "http://api.timezonedb.com"
  def mapBoxHost         = "https://api.tiles.mapbox.com"

  def weatherEndpoint    = openWeatherMapHost + "/data/2.5/weather"
  def searchEndpoint     = openWeatherMapHost + "/data/2.5/find"
  def timeZoneDbEndpoint = timeZoneDbHost + "/v2/get-time-zone"
  def mapBoxEndpoint     = mapBoxHost + "/v4/{id}/{z}/{x}/{y}.png"

  var owmQueryParams = scala.collection.mutable.Map(
    "q"      -> "",
    "type"   -> "like",
    "mode"   -> "json",
    "apikey" -> "Enter your API Key here"
  )

  var tzdbQueryParams = scala.collection.mutable.Map(
    "key"    -> "O0D5JNRE19JS",
    "format" -> "json",
    "by"     -> "position"
  )

  var mbQueryParams = scala.collection.mutable.Map(
    "access_token" -> "pk.eyJ1IjoiZmFuY2VsbHUiLCJhIjoiY2oxMHRzZm5zMDAyMDMycndyaTZyYnp6NSJ9.AJ3owakJtFAJaaRuYB7Ukw"
  )

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

  // A temperatures are returned in degree Kelvin
  def kelvinToDegStr(k: Double, min: Double, max: Double):String = {
    val variation = (max - min) / 2
    (k - 272.15).toInt + "˚C" + (if (variation > 0) s" ±${variation}˚C" else "")
  }

  // The weather conditions text string is all lowercase.  Convert to
  // sentence case
  def formatCoords(lat: Double, lon: Double): String = {
    val latStr = s"${Math.abs(lat)}˚${if (lat >= 0) "N" else "S"}"
    val lonStr = s"${Math.abs(lon)}˚${if (lon >= 0) "E" else "W"}"

    s"$latStr, $lonStr"
  }

  // The weather conditions text string is all lowercase.  Convert to
  // sentence case
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

  // Visibility information is not always supplied
  def formatVisibility(v: Int): String = if (v == 0) "Not available" else v + "m"

  // Convert weather icon code into an actual image
  def formatIcon(id: String) = img(src:=s"http://openweathermap.org/img/w/$id.png")

  def formatVelocity(v: Double): String   = v + "m/s"
  def formatPercentage(p: Double): String = p + "%"
  def formatPressure(p: Double): String   = p + " mBar"

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // Convert UTC time from a given lat/lon into a time in the local timezone
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  def getTimeZoneFromLatLon(lat: Double, lon: Double, utc: Long): Unit = {
    tzdbQueryParams += ("lat" -> lat.toString, "lng" -> lon.toString, "time" -> utc.toString)

    val queryStr = (
      for (p <- tzdbQueryParams.keys)
        yield s"$p=${tzdbQueryParams.get(p).get}"
      ).mkString("?", "&", "")

    val xhr = new dom.XMLHttpRequest
    xhr.open("GET", timeZoneDbEndpoint + queryStr)

    xhr.onload = (e: dom.Event) => {
      val data = js.JSON.parse(xhr.responseText)

      println(s"TimeZoneDB JSON response = ${xhr.responseText}")
    }

    // Send XHR request to TimeZoneDB.com
    xhr.send()
  }

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // Create a slippy map of the current city and as a side-effect, directly
  // updates the DOM element received as a parameter
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  def buildSlippyMap(mapDiv: String, report: WeatherReportBuilder): Unit = {
    //println(s"Building map container $mapDiv")

    // Clear mapDiv contents in case the id has already been used
    dom.document.getElementById(mapDiv).innerHTML = ""

    // Centre the map on the city's coordinates with a default zoom level of 12
    // Then place the slippy map inside the just-created div called mapDiv
    val mapOpts = LMapOptions.zoom(12).center((report.coord.lat, report.coord.lon))
    val map     = L.map(mapDiv, mapOpts)

    val queryStr = (for (p <- mbQueryParams.keys)
      yield s"$p=${mbQueryParams.get(p).get}"
    ).mkString("?", "&", "")

    val tileLayer = L.tileLayer(
      mapBoxEndpoint + queryStr,
      TileLayerOptions.
        id("mapbox.streets").
        maxZoom(19).
        attribution("""Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors,
                      |<a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>,
                      |Imagery © <a href="http://mapbox.com">Mapbox</a>""".stripMargin)
    )

    tileLayer.addTo(map)

    L.marker(
      (report.coord.lat, report.coord.lon),
      MarkerOptions.title(s"${report.cityName}, ${report.weatherSys.country}"))
      .addTo(map)
  }

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // Build HTML weather report
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  def buildSearchList(locationList: js.Dynamic, weatherDiv: dom.Element): Unit = {
    var counter: Int = 0

    locationList.list.map {
      (data: js.Dynamic) => {
        val report = new WeatherReportBuilder(data)
        weatherDiv.appendChild(buildWeatherReport(report, counter))
        buildSlippyMap(s"mapDiv$counter", report)
        counter += 1
      }
    }
  }

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // Build HTML weather report
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  def buildWeatherReport(report: WeatherReportBuilder, counter: Int): dom.Element = {
    // Call TimeZoneDB
    // getTimeZoneFromLatLon(report.coord.lat, report.coord.lon, report.measuredAt)

    div(
      style := "width: 100%",
      table(
        style := "margin: 1em; width: 95%",
        tr(
          th(
            colspan := 3,
            style := "background-color: #EEEEEE; text-align: center; font-size: larger",
            s"${report.cityName}, ${report.weatherSys.country} (${formatCoords(report.coord.lat, report.coord.lon)})"
          )
        ),
        tr(
          td("Temperature"), td(kelvinToDegStr(report.main.temp, report.main.temp_min, report.main.temp_max)),
          td(
            rowspan := 999,
            div(
              id := s"mapDiv$counter",
              style := "float: right; width: 400px; height: 400px; margin: 0.5rem; margin-right: 0rem"
            )
          )
        ),

// tr(td("Sunrise"), td(utcToDateStr(report.weatherSys.sunrise))),
// tr(td("Sunset"),  td(utcToDateStr(report.weatherSys.sunset))),

        // If ground level and sea level pressures are not available
        // use the general atmospheric pressure
        if (report.main.grnd_level == 0)
          tr(td("Atmospheric Pressure"), td(formatPressure(report.main.airPressure)))
        else {
          Seq(
            tr(td("Atmospheric Pressure (Ground Level)"), td(formatPressure(report.main.grnd_level))),
            tr(td("Atmospheric Pressure (Sea Level)"), td(formatPressure(report.main.sea_level)))
          )
        },

        tr(td("Humidity"),          td(formatPercentage(report.main.humidity))),
        tr(td("Visibility"),        td(formatVisibility(report.visibility))),
        tr(td("Wind speed"),        td(formatVelocity(report.wind.speed))),
        tr(td("Wind direction"),    td(formatHeading(report.wind.heading))),
        tr(td("Cloud cover"),       td(formatPercentage(report.clouds))),
// tr(td("Readings taken at"), td(utcToDateStr(report.measuredAt))),

        for (weather <- report.weatherConditions)
          yield Seq(
            tr( td(style := "background: #EEEEEE", colspan := 2, s"General conditions: ${weather.main}" )),
            tr( td("Description"), td(formatIcon(weather.icon), formatDescription(weather.desc)) )
          )
      )
    ).render
  }

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // Main program
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  @JSExport
  def main(container: dom.html.Div): Unit = {
    container.innerHTML = ""

    val apiKeyInput   = input.render
    val cityNameInput = input.render
    val btn           = button.render
    val weatherDiv    = div.render

    cityNameInput.defaultValue = owmQueryParams.get("q").get
    apiKeyInput.defaultValue   = owmQueryParams.get("apikey").get
    btn.textContent            = "Go"

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Button onclick event handler
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    btn.onclick = (e: dom.Event) => {
      weatherDiv.innerHTML = ""

      owmQueryParams += ("q" -> cityNameInput.value)

      val queryStr = (
        for (p <- owmQueryParams.keys)
          yield s"$p=${owmQueryParams.get(p).get}"
        ).mkString("?", "&", "")

      val xhr = new dom.XMLHttpRequest
      xhr.open("GET", weatherEndpoint + queryStr)

      xhr.onload = (e: dom.Event) => {
        val data = js.JSON.parse(xhr.responseText)

//        println(s"JSON response = ${xhr.responseText}")

        // Can the city be found?
        if (data.cod == "404")
          // Nope, so show error message
          weatherDiv.appendChild(p(s"City ${cityNameInput.value} not found").render)
        else {
          // So first add the div containing both the weather information
          // and the empty div that will hold the slippy map.
          // This is needed because Leaflet writes the map information to an
          // existing DOM element
          val report = new WeatherReportBuilder(data)
          weatherDiv.appendChild(buildWeatherReport(report, 0))

          buildSlippyMap("mapDiv0", report)
        }
      }

      // Send XHR request to OpenWeather
      xhr.send()
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Input field onkeyup event handler
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    cityNameInput.onkeyup = (e: dom.Event) => {
      // The city name must be at least 4 characters long
      if (cityNameInput.value.length > 3) {
        weatherDiv.innerHTML = ""

        owmQueryParams += ("q" -> cityNameInput.value)

        val queryStr = (
          for (p <- owmQueryParams.keys)
            yield s"$p=${owmQueryParams.get(p).get}"
          ).mkString("?", "&", "")

        val xhr = new dom.XMLHttpRequest
        xhr.open("GET", searchEndpoint + queryStr)

        xhr.onload = (e: dom.Event) => {
          val data: js.Dynamic = js.JSON.parse(xhr.responseText)

//          println(s"JSON response = ${xhr.responseText}")

          // Can any cities be found?
          if (data.count == 0)
          // Nope, so show error message
            weatherDiv.appendChild(p(s"Cannot find any city names starting with ${cityNameInput.value}").render)
          else {
            // Build a list of weather reports
            buildSearchList(data, weatherDiv)
          }
        }

        // Send XHR request to OpenWeather
        xhr.send()
      }
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Write HTML to the screen
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    container.appendChild(
      div(
        h1("Weather"),
        table(
          tr(td("API Key for OpenWeather interface"), td(apiKeyInput)),
          tr(td("Enter a city name (min 4 characters)"), td(cityNameInput)),
          tr(td(), td(style := "text-align: right", btn))
        ),
        weatherDiv
      ).render
    )
  }
}



