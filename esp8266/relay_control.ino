/*
    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
    in compliance with the License. You may obtain a copy of the License at:

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
    on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
    for the specific language governing permissions and limitations under the License.
*/

#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <ESP8266WebServer.h>
#include <ESP8266mDNS.h>

// Configurable parts
#define WLAN_SSID       "ssid-goes-here"
#define WLAN_PASS       "wpa-key-goes-here"
#define SERVER_PORT     80
#define RELAYPIN        4
// End configurable parts

ESP8266WebServer server(SERVER_PORT);
MDNSResponder mdns;

int state = LOW;
int prevstate = state;

const char webPage[] = "<h1>Relay Control</h1><p>Relay <a href=\"on\"><button>On</button></a>&nbsp;<a href=\"off\"><button>Off</button></a></p>";
String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n";

void send_cur_state(void) {
  String curState = state ? "on" : "off";
  String xmlResult = xmlHeader + "<state>" + curState + "</state></device>\r\n";
  server.send(200, "text/xml", xmlResult);
  Serial.println("XML current state sent");
}

void handle_request(void) {
  int cnt = server.args();
  String message = "Number of args received: ";

  // no params, send default page for interactive usage
  if (cnt == 0) {
    server.send(200, "text/html", webPage);
    return;  
  } else {
    message += String(cnt) + "\n";

    for (int i = 0; i < cnt; i++) {
      message += "Arg " + (String)i + " –> ";  //Include the current iteration value
      message += server.argName(i) + " : ";     //Get the name of the parameter
      message += server.arg(i) + "\n";         //Get the value of the parameter
    }
    Serial.print(message); // already has newline
  }

  // otherwise, see what params we got
  if (server.arg("on") != "") {
    state = HIGH;
  }

  if (server.arg("off") != "") {
    state = LOW;
  }

  if (server.arg("refresh") != "") {
    // noop - we always send current state
  }
  
  // always respond with current state for any other request
  send_cur_state();
}

void setup(void) {
  String myHostname = "";
  String myIP = "";

  Serial.begin(115200);
  delay(10);
  Serial.println();
  Serial.println();
  Serial.println("Relay Control");
  Serial.println();

  pinMode(RELAYPIN, OUTPUT);

  // Connect to WiFi access point.
  Serial.print("Connecting to ");
  Serial.println(WLAN_SSID);
  
  WiFi.mode(WIFI_STA); // set WiFi station only, disable AP mode
  WiFi.begin(WLAN_SSID, WLAN_PASS);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println();
  Serial.println("WiFi connected");

  // make sure we reconnect if the AP reboots
  WiFi.setAutoReconnect(true);
  
  myIP = WiFi.localIP().toString();

  Serial.print("IP address: ");
  Serial.println(myIP);
  
  xmlHeader += "<device><id>" + myIP + "</id>";

  myIP.replace(".", "_");
  myHostname += "esp8266_" + myIP;
  WiFi.hostname(myHostname);
  
  Serial.print("Setting hostname: ");
  Serial.println(myHostname);
  
  if (mdns.begin(myHostname.c_str(), WiFi.localIP())) {
    Serial.println("MDNS responder started");
  }
  
  server.on("/", handle_request);
 
  server.on("/on", []() {
    server.send(200, "text/html", webPage);
    Serial.println("http on received");
    state = HIGH; // sets current state
  });

  server.on("/off", []() {
    server.send(200, "text/html", webPage);
    Serial.println("http off received");
    state = LOW;  // sets current state
  });

  server.begin();
  Serial.println("HTTP server started");

  Serial.println("Setup done");
  Serial.println();
}

void loop(void) {
  // check state and only change pin status if there is a need to
  if (state != prevstate) {
    Serial.println("Updating PIN state...");
    digitalWrite(RELAYPIN, state);
  }
  prevstate = state;

  server.handleClient();
}
