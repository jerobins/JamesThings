/**
 *  Copyright 2015 SmartThings
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
 *  On/Off Button 
 *
 */
metadata {
	definition (name: "RemoteControl", namespace: "jerobins", author: "jerobins") {
		capability "Actuator"
		capability "Switch"
		capability "Sensor"
	}

	// simulator metadata
	simulator {
	}

	// UI tile definitions
	tiles {
		standardTile("button", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "off", label: 'Off', action: "switch.on", icon: "st.Electronics.electronics15", backgroundColor: "#ffffff", nextState: "on"
			state "on", label: 'On', action: "switch.off", icon: "st.Electronics.electronics15", backgroundColor: "#00A0DC", nextState: "off"
		}
		main "button"
		details "button"
	}
    
    preferences {
		input "ipaddr", "text", title: "IP Address", description: "Device address", required: true
		input "port", "text", title: "Port", description: "Device port", required: true
	}
}

def parse(String description) {
    log.debug "parse description: $description"

    def attrName = null
    def attrValue = null

    if (description?.startsWith("on/off:")) {
        log.debug "switch command"
        attrName = "switch"
        attrValue = description?.endsWith("1") ? "on" : "off"
    }

    def result = createEvent(name: attrName, value: attrValue)

    log.debug "Parse returned ${result?.descriptionText}"
    return result
}

def sendlocalrequest(Map params) {
    def host = getHostAddress()
    log.debug "Trying to send ${host} command ${params?.path}"

  	sendHubCommand(new physicalgraph.device.HubAction("""GET $params.path HTTP/1.1\r\nHOST: $host\r\n\r\n""",
    													physicalgraph.device.Protocol.LAN, "${host}"))
}

def on() {
	sendEvent(name: "switch", value: "on")
	def params = [
    	     path: "/go"
	]
   	sendlocalrequest(params)
}

def off() {
	sendEvent(name: "switch", value: "off")
	def params = [
    	     path: "/stop"
	]
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