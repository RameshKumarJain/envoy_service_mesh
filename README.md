# envoy_service_mesh
Envoy complete dynamic configurations and service mesh. Integration with java-control-plane(management server).

In this example we have 6 docker containers :
1. Envoy_front_proxy
2. Service_1(envoy side car)
3. Service_2(envoy side car)
4. Service_3(envoy side car)
5. Service_4(envoy side car)
6. server(java control plane)

In above example envoy front proxy has two clusters, each cluster has two endpoints(two envoy side car services)

Envoy_front_proxy and all envoy side car services has dynamic configurations which are fetch from server(java control plane) via ADS.

On envoy side car calling ADS will get its endpoints service address along with the external service endpoints.

![Envoy Service Mesh](Envoy_Service_Mesh.png)


Start all containers of service envoy_service_mesh
----

Ensure that you have a recent version of the docker, docker-compose, and docker-machine installed.

1. Move to envoy folder

2. Run the run project with following command

        $ docker-compose up --build -d

3. To view all running containers

        $ docker-compose ps

4. To stop all containers

        $ docker-compose stop

5. To stop specific container

        $ docker-compose stop <container-name>

6. To remove all containers
  Note : all containers should be stoped before removing it

        $ docker-compose rm

7. To remove specific container
  Note : specific container should be stoped before removing it

        $ docker-compose rm <container-name>

Java control plane
------
Sample java control plane main class code :
https://github.com/RameshKumarJain/envoy_service_mesh/master/refrence_files/TestMain.java

Above file contains ADS configuration of envoy front proxy and all envoy side car services. 
