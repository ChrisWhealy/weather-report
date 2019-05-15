package com.sap.demo

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import org.scalajs.dom
import com.felstar.scalajs.leaflet._

import scalatags.JsDom.all._

@JSExport
object WeatherReport {
  import Utils._
  import Trace._

  def traceFlow = Trace.flow("WeatherReport")(_: String)(_: Option[Boolean])(_: Any)

  def enter: Option[Boolean] = Option(true)
  def exit:  Option[Boolean] = Option(false)

  var cityList: List[CityWeather] = List()

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // Create a slippy map of the current city and as a side-effect, directly
  // update the DOM element received as parameter mapDiv
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  def buildSlippyMap(mapDiv: String, city: CityWeather): Unit = {
    //println(s"Building map container $mapDiv")

    // Clear mapDiv contents in case the id has already been used
//    val thisMapDiv = dom.document.getElementById(mapDiv)
//    val parent = thisMapDiv.parentNode
//    parent.removeChild(thisMapDiv)

    // Centre the map on the city's coordinates with a default zoom level of 12
    // Then place the slippy map inside the just-created div called mapDiv
    val mapOpts = LMapOptions.zoom(12).center((city.coord.lat, city.coord.lon))
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
      (city.coord.lat, city.coord.lon),
      MarkerOptions.title(s"${city.cityName}, ${city.weatherSys.country}"))
      .addTo(map)
  }

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // Build HTML weather report
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  def buildSearchList(locationList: js.Dynamic, weatherDiv: dom.Element): Unit = {
    var counter: Int = 0
    var distanceThreshold = 10.0

    locationList.list.map {
      (data: js.Dynamic) => {
        val city = new CityWeather(data)

        // Is this city within 10 km of any other cities we've seen?
        if (cityList.foldLeft(true)((acc, c) =>
          acc && (distance(c.coord.lat, c.coord.lon, city.coord.lat, city.coord.lon) > distanceThreshold))) {
          // Nope, so create a weather report
          weatherDiv.appendChild(buildWeatherReport(city, counter))

          buildSlippyMap(s"mapDiv$counter", city)
          counter += 1

          cityList = cityList :+ city
        }
        else {
          // Yup, so treat it as a duplicate because its likely to be the same city listed twice.
          println(s"Rejecting duplicate city ${city.cityName} located at (${city.coord.lat}, ${city.coord.lon})")
        }
      }
    }
  }

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // Build HTML weather report
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  def buildWeatherReport(city: CityWeather, counter: Int): dom.Element = {
    // Call TimeZoneDB
    // getTimeZoneFromLatLon(report.coord.lat, report.coord.lon, report.measuredAt)

    div(
      id := "weatherReport",
      table(
        id := "weatherReportTable",
        tr(
          td(
            colspan := 2,
            id := "weatherReportHeading",
            s"${city.cityName}, ${city.weatherSys.country} (${formatCoords(city.coord.lat, city.coord.lon)})"
          ),
          td(
            rowspan := 99,
            div(
              id := s"mapDiv$counter",
              style := "float: right; width: 500px; height: 500px; margin: 0.5em; margin-bottom: 1em; margin-left: 0em;"
            )
          )
        ),
        tr(
          td(id := "label", "Temperature"),
          td(kelvinToDegStr(city.main.temp, city.main.temp_min, city.main.temp_max))
        ),

        // tr(td("Sunrise"), td(utcToDateStr(report.weatherSys.sunrise))),
        // tr(td("Sunset"),  td(utcToDateStr(report.weatherSys.sunset))),

        // If ground level and sea level pressures are not available
        // use the general atmospheric pressure
        if (city.main.grnd_level == 0)
          tr(td(id := "label", "Atmospheric Pressure"), td(formatPressure(city.main.airPressure)))
        else {
          Seq(
            tr(td(id := "label", "Atmospheric Pressure (Ground Level)"), td(formatPressure(city.main.grnd_level))),
            tr(td(id := "label", "Atmospheric Pressure (Sea Level)"),    td(formatPressure(city.main.sea_level)))
          )
        },

        tr(td(id := "label", "Humidity"), td(formatPercentage(city.main.humidity))),

        if (city.visibility > 0)
          tr(td(id := "label", "Visibility"), td(formatVisibility(city.visibility)))
        else {},

        tr(td(id := "label", "Wind speed"),     td(formatVelocity(city.wind.speed))),
        tr(td(id := "label", "Wind direction"), td(formatHeading(city.wind.heading))),
        tr(td(id := "label", "Cloud cover"),    td(formatPercentage(city.clouds))),
        // tr(td(id := "label", "Readings taken at"), td(utcToDateStr(report.measuredAt))),

        for (weather <- city.weatherConditions)
          yield Seq(
//            tr( td(style := "background: #EEEEEE", colspan := 2, s"General conditions: ${weather.main}" )),
            tr(td(id := "label", formatDescription(weather.desc)), td(formatIcon(weather.icon)))
          )
      )
    ).render
  }

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // Generic event handler for UI control events
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  val eventHandler = (userInput: dom.html.Input,
                      responseDiv: dom.Element,
                      targetEndpoint: String,
                      responseHandler: Function3[dom.XMLHttpRequest,
                        dom.Element,
                        dom.html.Input,
                        Function1[ dom.Event, _]]
                     ) =>
    (e: dom.Event) => {
      // The city name must be at least 3 characters long
      if (userInput.value.length >= 3) {
        // Start from an empty DIV
        responseDiv.innerHTML = ""
        responseDiv.render

        // Blank out the coordinate list
        cityList = List()

        val xhr = buildXhrRequest(userInput.value, targetEndpoint)

        // Add response event handler and send XHR request
        xhr.onload = responseHandler(xhr, responseDiv, userInput)
        xhr.send()
      }
    }

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // Handler for response to a general search for cities starting with the
  // user input string
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  val keystrokeResponseHandler = (xhrResponse: dom.XMLHttpRequest, responseDiv: dom.Element, userInput: dom.html.Input) =>
    (e: dom.Event) => {
      val data: js.Dynamic = js.JSON.parse(xhrResponse.responseText)

      // Can any cities be found?
      if (data.count == 0)
      // Nope, so show error message
        responseDiv.appendChild(p(s"Cannot find any city names starting with ${userInput.value}").render)
      else
        // Build a list of weather reports
        buildSearchList(data, responseDiv)
    }


  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // Handler for response to searching for a specific city
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
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
        val report = new CityWeather(data)
        responseDiv.appendChild(buildWeatherReport(report, 0))

        buildSlippyMap("mapDiv0", report)
      }
    }

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // Main program
  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  @JSExport
  def main(container: dom.html.Div): Unit = {
    traceFlow("main", enter, null)

    // All sections of HTML are placed inside this container
    container.innerHTML = ""

    val noApiKeyMsg   = "Please edit the source of Utils.scala and add your own OpenWeatherMap API key. This app cannot function without this value."
    val hdr           = header.render
    val weatherDiv    = div.render
    val cityNameInput = input.render
    val btn           = button.render

    cityNameInput.placeholder = "Your city name"
    btn.textContent           = "Search"

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Check for missing API Key.
    // This check assumes that all OpenWeatherMap API Keys are just hex strings. As long as an API Key value is present
    // that conforms to this assumption, then assign the button onclick and input field onkeyup event handlers to the
    // UI controls
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    def apiKeyPresent = isHexStr(owmQueryParams.get("apikey").get)

    if (apiKeyPresent) {
      btn.onclick           = eventHandler(cityNameInput, weatherDiv, weatherEndpoint, buttonPushResponseHandler)
      btn.id                = "searchButton"

      cityNameInput.onkeyup = eventHandler(cityNameInput, weatherDiv, searchEndpoint,  keystrokeResponseHandler)
      cityNameInput.id      = "inputField"
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Write HTML to the screen
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    hdr.appendChild(h1("Weather Forecast").render)

    if (!apiKeyPresent) {
      hdr.appendChild(p(id := "noApiKey", noApiKeyMsg).render)
    }

    // Add header and form fields to the container
    container.appendChild(hdr)

    container.appendChild(
      div(
        id := "cityInput",
        cityNameInput,
        btn
      ).render
    )

    // Add the weatherDiv to the container
    traceFlow("main", exit, container.appendChild(weatherDiv).render)
  }
}



