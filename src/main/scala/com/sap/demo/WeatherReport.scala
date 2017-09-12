package com.sap.demo

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom

import com.felstar.scalajs.leaflet._

import scalatags.JsDom.all._

@JSExport
object WeatherReport {
  import Utils._

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // Create a slippy map of the current city and as a side-effect, directly
  // update the DOM element received as parameter mapDiv
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  def buildSlippyMap(mapDiv: String, report: WeatherReportBuilder): Unit = {
    //println(s"Building map container $mapDiv")

    // Clear mapDiv contents in case the id has already been used
    dom.document.getElementById(mapDiv).innerHTML = ""

    // Centre the map on the city's coordinates with a default zoom level of 12
    // Then place the slippy map inside the just-created div called mapDiv
    val mapOpts = LMapOptions.zoom(12).center((report.coord.lat, report.coord.lon))
    val map = L.map(mapDiv, mapOpts)

    val queryStr = (
      for (p <- mbQueryParams.keys)
        yield s"$p=${mbQueryParams.get(p).get}"
      ).mkString("?", "&", "")

    val tileLayer = L.tileLayer(
      mapBoxEndpoint + queryStr,
      TileLayerOptions.
        id("mapbox.streets").
        maxZoom(19).
        attribution(
          """Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors,
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
              style := "float: right; width: 500px; height: 500px; margin: 0.5rem; margin-right: 0rem"
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
            tr(td("Atmospheric Pressure (Sea Level)"),    td(formatPressure(report.main.sea_level)))
          )
        },

        tr(td("Humidity"), td(formatPercentage(report.main.humidity))),

        if (report.visibility > 0)
          tr(td("Visibility"), td(formatVisibility(report.visibility)))
        else {},

        tr(td("Wind speed"),     td(formatVelocity(report.wind.speed))),
        tr(td("Wind direction"), td(formatHeading(report.wind.heading))),
        tr(td("Cloud cover"),    td(formatPercentage(report.clouds))),
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
  // Generic event handler for UI control events
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  val eventHandler = (userInput: dom.html.Input, responseDiv: dom.Element, targetEndpoint: String, responseHandler: Function3[dom.XMLHttpRequest, dom.Element, dom.html.Input, Function1[ dom.Event, _]]) =>
    (e: dom.Event) => {
      // The city name must be at least 4 characters long
      if (userInput.value.length > 3) {
        // Start from an empty DIV
        responseDiv.innerHTML = ""
        responseDiv.render

        val xhr = buildXhrRequest(userInput.value, targetEndpoint)

        // Add response event handler and send XHR request
        xhr.onload = responseHandler(xhr, responseDiv, userInput)
        xhr.send()
      }
    }

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // Handler for response to a general search for cities starting with the
  // user input string
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  val keystrokeResponseHandler = (xhrResponse: dom.XMLHttpRequest, responseDiv: dom.Element, userInput: dom.html.Input) =>
    (e: dom.Event) => {
      val data: js.Dynamic = js.JSON.parse(xhrResponse.responseText)

      // Can any cities be found?
      if (data.count == 0)
      // Nope, so show error message
        responseDiv.appendChild(p(s"Cannot find any city names starting with ${userInput.value}").render)
      else {
        // Build a list of weather reports
        buildSearchList(data, responseDiv)
      }
    }


  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // Handler for response to searching for a specific city
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  val buttonPushResponseHandler = (xhr: dom.XMLHttpRequest, responseDiv: dom.Element, userInput: dom.html.Input) =>
    (e: dom.Event) => {
      val data = js.JSON.parse(xhr.responseText)

      // Can the city be found?
      if (data.cod == "404")
      // Nope, so show error message
        responseDiv.appendChild(p(s"City ${userInput.value} not found").render)
      else {
        // Yup, so add the div containing the weather information and then add
        // an empty div that will hold the slippy map.
        // This is needed because Leaflet needs to write its map information
        // into an existing DOM element
        val report = new WeatherReportBuilder(data)
        responseDiv.appendChild(buildWeatherReport(report, 0))

        buildSlippyMap("mapDiv0", report)
      }
    }

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // Main program
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  @JSExport
  def main(container: dom.html.Div): Unit = {
    container.innerHTML = ""

    val cityNameInput          = input.render
    cityNameInput.placeholder  = "Enter a city name"

    val btn         = button.render
    btn.textContent = "Go"

    val weatherDiv = div.render


    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Check for missing API Key.
    // This check assumes that all OpenWeatherMap API Keys are just hex strings
    // As long as an API Key is present, assign button onclick and input field
    // onkeyup event handlers to the UI controls
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    def apiKeyPresent = isHexStr(owmQueryParams.get("apikey").get)

    if (apiKeyPresent) {
      btn.onclick           = eventHandler(cityNameInput, weatherDiv, weatherEndpoint, buttonPushResponseHandler)
      cityNameInput.onkeyup = eventHandler(cityNameInput, weatherDiv, searchEndpoint,  keystrokeResponseHandler)
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Write HTML to the screen
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    container.appendChild(
      div(
        h1("Weather Report"),
        table(
          if (!apiKeyPresent)
            tr(td(colspan := "3", style := "color:red;", "Please edit the source of Utils.scala and add your own OpenWeatherMap API key", br, "This app cannot function without this value."))
          else {},
          tr(td("Enter a city name (min 4 characters)"), td(cityNameInput)),
          tr(td(), td(style := "text-align: right", btn))
        ),
        weatherDiv
      ).render
    )
  }
}



