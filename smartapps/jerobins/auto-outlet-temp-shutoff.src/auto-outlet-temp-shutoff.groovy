/**
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
definition(
    name: "Auto Outlet Temp Shutoff",
    namespace: "jerobins",
    author: "jerobins",
    description: "Auto-shutoff outlet(s) based on rising value of temperature sensor.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
	section("Choose a temperature sensor... "){
		input "sensor", "capability.temperatureMeasurement", title: "Sensor"
	}
	section("Select the outlet(s)... "){
		input "outlets", "capability.switch", title: "Outlets", multiple: true
	}
	section("Set the desired temperature..."){
		input "setpoint", "decimal", title: "Set Temp"
	}
}

def installed()
{
	subscribe(sensor, "temperature", temperatureHandler)
}

def updated()
{
	unsubscribe()
	subscribe(sensor, "temperature", temperatureHandler)
}

def temperatureHandler(evt)
{
  def currentTemp = evt.doubleValue
  def desiredTemp = setpoint
  def threshold = 1.0
  log.debug "EVALUATE($currentTemp, $desiredTemp)"

  if (currentTemp - desiredTemp >= threshold) {
    outlets.off()
  }
  
}