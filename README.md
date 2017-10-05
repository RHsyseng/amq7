# AMQ 7 Reference Architecture Test Suite

AMQ7 RefArch project using JMS to showcase various broker uses, clustering, high-availability & interconnect routing features.

## Prerequisites

* JDK 1.8+
* Maven
* OpenShift 3.X environment running broker/router topologies from [correlating project](https://github.com/jeremyary/amq7-image).

## Test Suite

The included tests are, in essence, runner scripts that are best consume individually to understand the aspect showcased within each. 

### Standalone AMQ7 Broker

Demonstrates AMQ7 queue & topic interactivity and various basic EIP patterns via JMS.

```
mvn -Dtest=SingleBrokerTest test
```

### 3-Node Symmetric AMQ7 Broker Cluster

Demonstrates AMQ7 queue & topic interactivity across a 3-broker symmetric cluster via JMS.

![Symmetric Broker Cluster Topology](images/symmetric.png?raw=true "Symmetric Broker Cluster Topology")

```
mvn -Dtest=SymmetricClusterTest test
```

### 3-Pair Master/Slave AMQ7 Broker Failover Cluster

Demonstrates producing/consuming to/from queues before and after a master broker failover scenario via JMS.

![Replication Cluster Topology](images/replication.png?raw=true "Replication Cluster Topology")

```
mvn -Dtest=ReplicatedFailoverTest test
```

### 7-Node Interconnect Router Topology

Demonstrates various Interconnect routing mechanisms across a topology featuring several inter-router connections and multiple endpoint listeners for client 
connectivity via JMS.

![Interconnect Topology](images/interconnect.png?raw=true "Interconnect Topology")

* Direct produce/consume:
```
mvn -Dtest=InterconnectTest#testSendDirect test
```

* Multicast from a single producer to 4 consumers:
```
mvn -Dtest=InterconnectTest#testSendMulticast test
```

* Balanced multi-consumer distribution with equal route weight:
```
mvn -Dtest=testSendBalancedSameHops test
```

* Balanced multi-consumer distribution with differentiating route weight:
```
mvn -Dtest=testSendBalancedDifferentHops test
```

* Single-consumer routing to closest consumer based on origin point:
```
mvn -Dtest=testSendClosest test
```