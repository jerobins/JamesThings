#!/usr/bin/python

from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
import subprocess, urlparse

PORT_NUMBER = 8080

class myHandler(BaseHTTPRequestHandler):
   state = "off" # boots in off state
   
   # turn off logging
   def log_request(code='-', size='-'):
      return

   #Handler for the GET requests
   def do_GET(self):
      # send valid HTTP response, payload is XML result as shown below
      # <device>
      #  <id>ipadder</id>
      #  <state>on/off</state>
      # </device>

      self.send_response(200)
      self.send_header('Content-type','text/xml')
      self.end_headers()

      msg = '<?xml version="1.0" encoding="UTF-8"?>' + "\r\n"

      # include ip of our device in the response - for tracing
      id = self.headers.get('Host')
      msg += '<device><id>{:s}</id>'.format(id)

      # let's see what we got
      params = urlparse.urlparse(self.path)
      query = dict(urlparse.parse_qsl(params.query))

      if ('on' in query):
         self.state = "on"
         subprocess.call('./go.sh')
      elif ('off' in query):
         self.state = "off"
         subprocess.call('./stop.sh')

      # even if the first two if's don't match anything, 
      # we always return the current state - this will fulfil
      # a /?refresh=1 request

      msg += '<state>{:s}</state>'.format(self.state)
      msg += '</device>' + "\r\n"

      # Send the html message
      self.wfile.write(msg)
      return

try:
   server = HTTPServer(('', PORT_NUMBER), myHandler)
   #Wait forever for http requests
   server.serve_forever()

except KeyboardInterrupt:
   server.socket.close()

