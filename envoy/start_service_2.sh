#!/bin/sh
python3 /code/service.py &
envoy -c /etc/service-envoy.yaml  --service-cluster envoy_side_car2 --service-node side_car_node
