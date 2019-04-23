from flask import Flask
from flask import request
import os
import requests
import socket
import sys

app = Flask(__name__)

TRACE_HEADERS_TO_PROPAGATE = [
    'X-Ot-Span-Context',
    'X-Request-Id',

    # Zipkin headers
    'X-B3-TraceId',
    'X-B3-SpanId',
    'X-B3-ParentSpanId',
    'X-B3-Sampled',
    'X-B3-Flags',

    # Jaeger header (for native client)
    "uber-trace-id"
]

@app.route('/')
def healthcheck():
  return ('Success')

@app.route('/service/<service_number>')
def hello(service_number):
  return ('Hello cluster0 from behind Envoy (service {})! hostname: {} resolved'
          'hostname: {}\n'.format(os.environ['SERVICE_NAME'], socket.gethostname(),
                                  socket.gethostbyname(socket.gethostname())))

@app.route('/service/external-service/<service_number>')          
def hello_external(service_number):
    resp = requests.get('http://13.233.184.121:8002/external_service/1')
    return ('Hello cluster0 from behind Envoy (service {})! hostname: {} resolved'
        'hostname: {}\nResponse : {}\n'.format(os.environ['SERVICE_NAME'], socket.gethostname(),
                                  socket.gethostbyname(socket.gethostname()), resp.text))


@app.route('/external_service/<service_number>')
def external(service_number):
    return ('Hello cluster0 external call service :  from behind Envoy (service {})! hostname: {} resolved'
          'hostname: {}\n'.format(os.environ['SERVICE_NAME'], socket.gethostname(),
                                  socket.gethostbyname(socket.gethostname())))
@app.route('/trace/<service_number>')
def trace(service_number):
  headers = {}
  # call service 2 from service 1
  if int(os.environ['SERVICE_NAME']) == 1:
    for header in TRACE_HEADERS_TO_PROPAGATE:
      if header in request.headers:
        headers[header] = request.headers[header]
    ret = requests.get("http://localhost:9000/trace/2", headers=headers)
  return ('Hello cluster0  from behind Envoy (service {})! hostname: {} resolved'
          'hostname: {}\n'.format(os.environ['SERVICE_NAME'], socket.gethostname(),
                                  socket.gethostbyname(socket.gethostname())))


if __name__ == "__main__":
  app.run(host='127.0.0.1', port=8080, debug=True)