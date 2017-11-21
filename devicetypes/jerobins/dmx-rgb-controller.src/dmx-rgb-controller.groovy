/**
 * DMX RGB Controller
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 * DMX HTTP Interface w/ Minimal Settings
 *
 * icons: https://cdn.rawgit.com/JZ-SmartThings/SmartThings/master/Icons/ST-Icons.html
 */
  
metadata {
	definition (name: "DMX RGB Controller", namespace: "jerobins", author: "jerobins") {
		capability "Switch Level"
		capability "Actuator"
		capability "Color Control"
		capability "Switch"
		capability "Refresh"
		capability "Sensor"
		capability "Health Check"
		capability "Light"

		command "reset"
        command "refresh"
        command "scenePolice"
        command "sceneNight"
        command "sceneParty"
        command "sceneMovie"
        command "sceneSound"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2) {
		multiAttributeTile (name:"rich-control", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", 
                	icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#00A0DC", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", 
                	icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", 
                	icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#00A0DC", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", 
                	icon:"st.Seasonal Winter.seasonal-winter-011", backgroundColor:"#ffffff", nextState:"turningOn"
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel", range:"(0..100)"
            }
			tileAttribute ("device.color", key: "COLOR_CONTROL") {
				attributeState "color", action:"setColor"
			}
		}

		standardTile ("reset", "device.reset", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:"White", action:"reset", icon:"st.illuminance.illuminance.bright"
		}

		standardTile ("refresh", "device.refresh", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		standardTile ("night", "device.scene", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:"Night", action:"sceneNight", icon:"st.Weather.weather4"
		}
        standardTile ("movie", "device.scene", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:"Movie", action:"sceneMovie", icon:"st.Entertainment.entertainment7"
		}
        standardTile ("party", "device.scene", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:"Party", action:"sceneParty", icon:"st.Entertainment.entertainment3"
		}
        standardTile ("police", "device.scene", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:"Police", action:"scenePolice", icon:"st.security.alarm.alarm"
		}
        standardTile ("sound", "device.scene", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:"Sound", action:"sceneSound", icon:"st.Electronics.electronics16"
		}
        
		main(["rich-control"])
		details(["rich-control", "reset", "night", "movie", "party", "sound", "police", "refresh"])
	}
    
    preferences {
		input "ipaddr", "text", title: "IP Address", description: "Device address", required: true
		input "port", "text", title: "Port", description: "Device port", required: true
	}
}

def initialize() {
	sendEvent(name: "DeviceWatch-Enroll", 
    			value: "{\"protocol\": \"LAN\", \"scheme\":\"untracked\", \"hubHardwareId\": \"${device.hub.hardwareID}\"}", 
                displayed: false)

    log.debug "Configured health checkInterval - 12 hours - initialize()"
	sendEvent(name: "checkInterval", value: 12 * 60 * 60, displayed: false, data: [protocol: "LAN", hubHardwareId: device.hub.hardwareID])

}

void installed() {
	log.debug "installed()"
	initialize()
}

def updated() {
	log.debug "updated()"
	initialize()
}

def ping() {
	refresh()
}

// parse incoming messages from DEVICE to ST Events
def parse(description) {
	log.debug "parse() - $description"
	def results = []

	def map = description
	if (description instanceof String)  {
		log.debug "DMX RGB stringToMap - ${map}"
		map = stringToMap(description)
	}

	if (map?.name && map?.value) {
		results << createEvent(name: "${map?.name}", value: "${map?.value}")
	}
	results
}

void sendlocalrequest(Map params) {
    def host = getHostAddress()
    log.debug "Trying to send ${host} command ${params?.path}"

  	sendHubCommand(new physicalgraph.device.HubAction("""GET $params.path HTTP/1.1\r\nHOST: $host\r\n\r\n""",
    							physicalgraph.device.Protocol.LAN, "${host}",
                                                        [callback: hubActionHandler]))
}

void hubActionHandler(physicalgraph.device.HubResponse hubResponse) {
    def device = hubResponse.xml
    def deviceId = device?.id?.text()
    def result = device?.result?.text()
    def state = device?.state?.text()
    def level = device?.level?.text()
    def color = device?.color?.text()

    log.debug "Executing hubActionHandler - ${deviceId}"
    log.debug "hubActionHandler - deviceId : ${deviceId}"
    log.debug "hubActionHandler - result : ${result}"
    log.debug "hubActionHandler - switch : ${state}"
    log.debug "hubActionHandler - level : ${level}"
    log.debug "hubActionHandler - color : ${color}"

    if (deviceId != null) {
    	if (result == "ok") {
            // Device wakes up every 1 hour, this interval allows us to miss one wakeup notification before marking offline
	        log.debug "Configured health checkInterval - 12 hours - hubActionHandler()"
	        sendEvent(name: "checkInterval", value: 12 * 60 * 60, displayed: false, data: [protocol: "LAN", hubHardwareId: device.hub.hardwareID])

        	if (state != null) {
               	sendEvent(name: "switch", value: state)
            }
            if (level != null) {
                sendEvent(name: "level", value: level)
            }
        }
    }
}

// handle commands
void on() {    
    def params = [ path: "/?on=1" ]
   	sendlocalrequest(params)
	// send to device, wait for response to send ST event
}

void off() {
    def params = [ path: "/?off=1" ]
   	sendlocalrequest(params)
	// send to device, wait for response to send ST event
}

void setLevel(dimmer) {
    log.debug "Executing 'setLevel'"
    def params = [ path: "/?brightness=${dimmer}" ]
   	sendlocalrequest(params)
	// send to device, wait for response to send ST event
}

void setColor(value) {
    log.debug "Executing 'setColor' - ${value}"
	def params = false
    if (value.red != null) {
  	    params = [
     	    path: "/?red=${value.red}&blue=${value.blue}&green=${value.green}"
	    ]
    } else {
        params = [
     	    path: "/?hue=${value.hue}&sat=${value.saturation}"
	    ]
    }
   	sendlocalrequest(params)
   	// send to device, wait for response to send ST event
}

void refresh() {
    log.debug "Executing 'refresh'"
    def params = [ path: "/?refresh=1" ]
   	sendlocalrequest(params)
   	// send to device, wait for response to send ST event
}

// set to white, full bright
void reset() { setScene("reset") }

void scenePolice() { setScene("police") }
void sceneParty() { setScene("party") }
void sceneNight() { setScene("night") }
void sceneMovie() { setScene("movie") }
void sceneSound() { setScene("sound") }

private setScene(value) {
    log.debug "Executing 'setScene' - ${value}"
    def params = [ path: "/?scene=${value}" ]
   	sendlocalrequest(params)
}

private getIP() {
    return settings.ipaddr
}

private getPort() {
    return settings.port
}

private getHostAddress() {
	def ip = getIP()
	def port = getPort()
	return ip + ":" + port
}
