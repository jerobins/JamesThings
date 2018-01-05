/**
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
 */
metadata {
	definition (name: "REST-API-Control", namespace: "jerobins", author: "jerobins") {
		capability "Actuator"
		capability "Switch"
		capability "Sensor"
		capability "Health Check"
		command "refresh"
	}

	// simulator metadata
	simulator {
	}

	// UI tile definitions
	tiles {
		standardTile("button", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "off", label: 'Off', action: "switch.on", icon: "st.shields.shields.arduino", backgroundColor: "#ffffff", nextState: "turningOn"
			state "turningOn", label: 'turningOn', action: "switch.off", icon: "st.shields.shields.arduino", backgroundColor: "#00A0DC", nextState: "on"
			state "on", label: 'On', action: "switch.off", icon: "st.shields.shields.arduino", backgroundColor: "#00A0DC", nextState: "turningOff"
			state "turningOff", label: 'turningOff', action: "switch.on", icon: "st.shields.shields.arduino", backgroundColor: "#ffffff", nextState: "off"
		}
		standardTile ("refresh", "device.refresh", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		main (["button"])
		details(["button", "refresh"])
	}
    
    preferences {
		input "ipaddr", "text", title: "IP Address", description: "Device address", required: true
		input "port", "text", title: "Port", description: "Device port", required: true
	}
}

def initialize() {
	sendEvent(name: "DeviceWatch-Enroll", value: "{\"protocol\": \"LAN\", \"scheme\":\"untracked\", \"hubHardwareId\": \"${device.hub.hardwareID}\"}", displayed: false)

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
		log.debug "stringToMap - ${map}"
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

void refresh() {
	log.debug "Executing 'refresh'"
	def params = [ path: "/?refresh=1" ]
   	sendlocalrequest(params)
   	// send to device, wait for response to send ST event
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
