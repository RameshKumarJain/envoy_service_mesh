#!/bin/sh
python3 /code/service_new.py &
envoy -c /etc/service-envoy.yaml --service-cluster envoy_side_car4  --service-node side_car_node 
