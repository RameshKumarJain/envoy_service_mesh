package io.envoyproxy.controlplane.server;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.*;
import com.google.protobuf.util.JsonFormat;
import io.envoyproxy.controlplane.cache.SimpleCache;
import io.envoyproxy.controlplane.cache.Snapshot;
import io.envoyproxy.envoy.api.v2.Cluster;
import io.envoyproxy.envoy.api.v2.Cluster.DiscoveryType;
import io.envoyproxy.envoy.api.v2.ClusterLoadAssignment;
import io.envoyproxy.envoy.api.v2.Listener;
import io.envoyproxy.envoy.api.v2.RouteConfiguration;
import io.envoyproxy.envoy.api.v2.core.*;
import io.envoyproxy.envoy.api.v2.endpoint.Endpoint;
import io.envoyproxy.envoy.api.v2.endpoint.LbEndpoint;
import io.envoyproxy.envoy.api.v2.endpoint.LocalityLbEndpoints;
import io.envoyproxy.envoy.api.v2.listener.Filter;
import io.envoyproxy.envoy.api.v2.listener.FilterChain;
import io.envoyproxy.envoy.api.v2.route.*;
import io.envoyproxy.envoy.config.filter.network.http_connection_manager.v2.HttpConnectionManager;
import io.envoyproxy.envoy.config.filter.network.http_connection_manager.v2.HttpFilter;
import io.envoyproxy.envoy.config.filter.network.http_connection_manager.v2.Rds;
import io.envoyproxy.envoy.type.FractionalPercent;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;

import java.io.IOException;

public class TestMain {

  private static final String ENVOY_FRONT_PROXY_GROUP = "envoy_front_proxy";
  private static final String ENVOY_SIDE_CAR_GROUP_1 = "envoy_side_car1";
  private static final String ENVOY_SIDE_CAR_GROUP_2 = "envoy_side_car2";
  private static final String ENVOY_SIDE_CAR_GROUP_3 = "envoy_side_car3";
  private static final String ENVOY_SIDE_CAR_GROUP_4 = "envoy_side_car4";

  /**
   * Example minimal xDS implementation using the java-control-plane lib.
   *
   * @param arg command-line args
   */
  public static void main(String[] arg) throws IOException, InterruptedException {
    SimpleCache<String> cache = new SimpleCache<>(node -> {
      switch (node.getCluster()) {
        case ENVOY_FRONT_PROXY_GROUP:
          return ENVOY_FRONT_PROXY_GROUP;
        case ENVOY_SIDE_CAR_GROUP_1:
          return ENVOY_SIDE_CAR_GROUP_1;
        case ENVOY_SIDE_CAR_GROUP_2:
          return ENVOY_SIDE_CAR_GROUP_2;
        case ENVOY_SIDE_CAR_GROUP_3:
          return ENVOY_SIDE_CAR_GROUP_3;
        case ENVOY_SIDE_CAR_GROUP_4:
          return ENVOY_SIDE_CAR_GROUP_4;
        default:
          return ENVOY_FRONT_PROXY_GROUP;
      }
    });

    cache.setSnapshot(
        ENVOY_SIDE_CAR_GROUP_1,
        Snapshot.create(
            ImmutableList.of(
                getClusterBuild("cluster0"),
                getClusterBuild("another_service_cluster")),
            ImmutableList.of(
                getClusterLoadAssignment("cluster0", "127.0.0.1", 8080, 0),
                getClusterLoadAssignment("another_service_cluster", "13.233.184.121", 8003, 0)
            ),
            ImmutableList.of(
                getListenerBuild("listener_0", "0.0.0.0", 80)
            ),
            ImmutableList.of(
                getRouteConfigurationBuild("local_route", "cluster0", "*", "/")
            ),
            ImmutableList.of(),
            "1"));
    cache.setSnapshot(
        ENVOY_SIDE_CAR_GROUP_2,
        Snapshot.create(
            ImmutableList.of(
                getClusterBuild("cluster0"),
                getClusterBuild("another_service_cluster")),
            ImmutableList.of(
                getClusterLoadAssignment("cluster0", "127.0.0.1", 8080, 0),
                getClusterLoadAssignment("another_service_cluster", "13.233.184.121", 8002, 0)
            ),
            ImmutableList.of(
                getListenerBuild("listener_0", "0.0.0.0", 80)
            ),
            ImmutableList.of(
                getRouteConfigurationBuild("local_route", "cluster0", "*", "/")
            ),
            ImmutableList.of(),
            "1"));
    cache.setSnapshot(
        ENVOY_SIDE_CAR_GROUP_3,
        Snapshot.create(
            ImmutableList.of(
                getClusterBuild("cluster0"),
                getClusterBuild("another_service_cluster")),
            ImmutableList.of(
                getClusterLoadAssignment("cluster0", "127.0.0.1", 8080, 0),
                getClusterLoadAssignment("another_service_cluster", "13.233.184.121", 7003, 0)
            ),
            ImmutableList.of(
                getListenerBuild("listener_0", "0.0.0.0", 80)
            ),
            ImmutableList.of(
                getRouteConfigurationBuild("local_route", "cluster0", "*", "/")
            ),
            ImmutableList.of(),
            "1"));
    cache.setSnapshot(
        ENVOY_SIDE_CAR_GROUP_4,
        Snapshot.create(
            ImmutableList.of(
                getClusterBuild("cluster0"),
                getClusterBuild("another_service_cluster")),
            ImmutableList.of(
                getClusterLoadAssignment("cluster0", "127.0.0.1", 8080, 0),
                getClusterLoadAssignment("another_service_cluster", "13.233.184.121", 7002, 0)
            ),
            ImmutableList.of(
                getListenerBuild("listener_0", "0.0.0.0", 80)
            ),
            ImmutableList.of(
                getRouteConfigurationBuild("local_route", "cluster0", "*", "/")
            ),
            ImmutableList.of(),
            "1"));

    cache.setSnapshot(
        ENVOY_FRONT_PROXY_GROUP,
        Snapshot.create(
            ImmutableList.of(
                getClusterBuild("cluster0"),
                getClusterBuild("cluster1")),
            ImmutableList.of(
                getClusterLoadAssignmentForFrontProxy("cluster0", "13.233.184.121", 8002, 8003),
                getClusterLoadAssignmentForFrontProxy("cluster1", "13.233.184.121", 7002, 7003)
            ),
            ImmutableList.of(
                getListenerBuild("listener_0", "13.233.184.121", 80)
            ),
            ImmutableList.of(
                getRouteConfigurationBuild("local_route", "cluster0", "cluster1", "*", "/")
            ),

            ImmutableList.of(),
            "1"));

    DiscoveryServer discoveryServer = new DiscoveryServer(cache);

    ServerBuilder builder = NettyServerBuilder.forPort(5678)
        .addService(discoveryServer.getAggregatedDiscoveryServiceImpl());
//        .addService(discoveryServer.getClusterDiscoveryServiceImpl())
//        .addService(discoveryServer.getEndpointDiscoveryServiceImpl())
//        .addService(discoveryServer.getListenerDiscoveryServiceImpl())
//        .addService(discoveryServer.getRouteDiscoveryServiceImpl());

    Server server = builder.build();

    server.start();

    System.out.println("Server has started on port " + server.getPort());

    Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

    server.awaitTermination();
  }

  private static RouteConfiguration getRouteConfigurationBuild(String routeName, String clusterName0, String clusterName1, String domain, String route_prefix) {
    return RouteConfiguration.newBuilder()
        .setName(routeName)
        .addVirtualHosts(VirtualHost.newBuilder()
            .setName(clusterName0)
            .addDomains(domain)
            .addRoutes(Route.newBuilder()
                .setMatch(RouteMatch.newBuilder()
                    .setPrefix(route_prefix)
                    .build())
                .setRoute(RouteAction.newBuilder()
                    .setWeightedClusters(WeightedCluster.newBuilder()
                        .addClusters(WeightedCluster.ClusterWeight.newBuilder()
                            .setName(clusterName0)
                            .setWeight(UInt32Value.newBuilder().setValue(75).build())
                            .build())
                        .addClusters(WeightedCluster.ClusterWeight.newBuilder()
                            .setName(clusterName1)
                            .setWeight(UInt32Value.newBuilder().setValue(25).build())
                            .build())
                        .build())
                    .setCluster(clusterName0)
                    .build())
                .build())
            .addRoutes(Route.newBuilder()
                .setMatch(RouteMatch.newBuilder()
                    .setPrefix(route_prefix)
                    .build())
                .setRoute(RouteAction.newBuilder()
                    .setCluster(clusterName1)
                    .build())
                .build())
            .build())
        .build();
  }

  private static RouteConfiguration getRouteConfigurationBuild(String routeName, String clusterName0, String domain, String route_prefix) {
    return RouteConfiguration.newBuilder()
        .setName(routeName)
        .addVirtualHosts(VirtualHost.newBuilder()
            .setName(clusterName0)
            .addDomains(domain)
            .addRoutes(Route.newBuilder()
                .setMatch(RouteMatch.newBuilder()
                    .setPrefix(route_prefix)
                    .build())
                .setRoute(RouteAction.newBuilder()
                    .setCluster(clusterName0)
                    .build())
                .build())
            .addRoutes(Route.newBuilder()
                .setMatch(RouteMatch.newBuilder()
                    .setPrefix("/external_service")
                    .build())
                .setRoute(RouteAction.newBuilder()
                    .setCluster("another_service_cluster")
                    .build())
                .build())
            .build())
        .build();
  }

  private static Listener getListenerBuild(String listener_name, String listener_ip, int listener_port) {
    return Listener.newBuilder()
        .setName(listener_name)
        .setAddress(getAddressBuild(listener_ip, listener_port)
        )
        .addFilterChains(FilterChain.newBuilder()
            .addFilters(Filter.newBuilder()
                .setName("envoy.http_connection_manager")
                .setConfig(messageAsStruct(getHttpConnectionManager()))
                .build()
            ).build()
        ).build();
  }

  private static HttpConnectionManager getHttpConnectionManager() {
    return
        HttpConnectionManager.newBuilder()
            .setStatPrefixBytes(ByteString.copyFrom("ingress_http".getBytes()))
            .setCodecType(HttpConnectionManager.CodecType.AUTO)
            .setRds(Rds.newBuilder()
                .setRouteConfigName("local_route")
                .setConfigSource(ConfigSource.newBuilder()
                    .setApiConfigSource(ApiConfigSource.newBuilder()
                        .setApiType(ApiConfigSource.ApiType.GRPC)
                        .addGrpcServices(GrpcService.newBuilder()
                            .setEnvoyGrpc(GrpcService.EnvoyGrpc.newBuilder()
                                .setClusterName("ads_cluster")
                                .build()
                            ).build()
                        ).build()
                    ).build()
                ).build()
            )
            .addHttpFilters(HttpFilter.newBuilder()
                .setName("envoy.router")
                .build()
            )
            .build();
  }

  private static ClusterLoadAssignment getClusterLoadAssignment(String clusterName, String ip, int port1, int port2) {

    LocalityLbEndpoints.Builder builder = LocalityLbEndpoints.newBuilder();
    if (port1 > 0) {
      builder.addLbEndpoints(getLbEndPoints(ip, port1, 75));
    }
    if (port2 > 0) {
      builder.addLbEndpoints(getLbEndPoints(ip, port2, 25));
    }

    return ClusterLoadAssignment.newBuilder()
        .setClusterName(clusterName).addEndpoints(builder.build()).build();
  }

  private static ClusterLoadAssignment getClusterLoadAssignmentForFrontProxy(String clusterName, String ip, int port1, int port2) {

    LocalityLbEndpoints.Builder builder = LocalityLbEndpoints.newBuilder();
    if (port1 > 0) {
      builder.addLbEndpoints(LbEndpoint.newBuilder()
          .setEndpoint(Endpoint.newBuilder()
              .setAddress(Address.newBuilder()
                  .setSocketAddress(SocketAddress.newBuilder()
                      .setAddress(ip)
                      .setPortValue(port1)
                      .build()
                  ).build()
              ).build())
          .build());
    }
    if (port2 > 0) {
      builder.addLbEndpoints(LbEndpoint.newBuilder()
          .setEndpoint(Endpoint.newBuilder()
              .setAddress(Address.newBuilder()
                  .setSocketAddress(SocketAddress.newBuilder()
                      .setAddress(ip)
                      .setPortValue(port2)
                      .build()
                  ).build()
              ).build())
          .build());
    }

    return ClusterLoadAssignment.newBuilder()
        .setClusterName(clusterName).addEndpoints(builder.build()).build();
  }

  private static LbEndpoint getLbEndPoints(String ip, int port, int weight) {
    return LbEndpoint.newBuilder()
        .setEndpoint(Endpoint.newBuilder()
            .setAddress(getAddressBuild(ip, port)
            )
            .setHealthCheckConfig(Endpoint.HealthCheckConfig.newBuilder()
                .setPortValue(port)
                .build())
            .build())
        .setLoadBalancingWeight(UInt32Value.newBuilder().setValue(weight).build())
        .build();
  }

  private static Address getAddressBuild(String ip, int port) {
    return Address.newBuilder()
        .setSocketAddress(
            SocketAddress.newBuilder()
                .setAddress(ip)
                .setPortValue(port)
        ).build();
  }

  private static Cluster getClusterBuild(String clusterName) {
    return Cluster.newBuilder()
        .setName(clusterName)
        .setConnectTimeout(Duration.newBuilder().setSeconds(5))
        .setLbPolicy(Cluster.LbPolicy.ROUND_ROBIN)
        .setType(DiscoveryType.EDS)
        .setEdsClusterConfig(Cluster.EdsClusterConfig.newBuilder()
            .setEdsConfig(ConfigSource.newBuilder()
                .setAds(AggregatedConfigSource.newBuilder().build())
            ).build())
        .build();
  }

  public static Struct messageAsStruct(MessageOrBuilder message) {
    try {

      String json = JsonFormat.printer()
          .preservingProtoFieldNames()
          .print(message);

      Struct.Builder structBuilder = Struct.newBuilder();

      JsonFormat.parser().merge(json, structBuilder);

      return structBuilder.build();

    } catch (Exception e) {
      throw new RuntimeException("Failed to convert protobuf message to struct", e);
    }
  }
}
