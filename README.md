# AMQ 7 Reference Architecture

This reference architecture demonstrates basic messaging patterns with single-broker, clustered, and fault-tolerant Red Hat JBoss AMQ 7 topologies, as well as direct, closest, balanced and 
multicast Interconnect routing configurations.

## Overview
Based on the upstream Apache ActiveMQ and Apache Qpid community projects, Red Hat JBoss AMQ 7 is a lightweight, standards-based open source messaging platform designed to enable real-time 
communication between different applications, services, devices, and Internet of Things (IoT) devices. It also serves as the messaging foundation for Red Hat JBoss Fuse, Red Hat’s lightweight, 
flexible integration platform, and is designed to provide the real-time, distributed messaging capabilities needed to support an agile integration approach for modern application development.

AMQ 7 introduces technology enhancements across three core components: the broker, clients, and Interconnect router.

###  Broker
The :amq: broker, based on Apache ActiveMQ Artemis, manages connections, queues, topics, and subscriptions. The new :amq: broker has a asynchronous internal architecture, which can increase 
performance and scalability 
and enable it to handle more concurrent connections and achieve greater message throughput. AMQ Broker is a full-featured, message-oriented middleware broker. It offers specialized queueing 
behaviors, message persistence, and manageability. Core messaging is provided with support for different messaging patterns such as publish-subscribe, point-to-point, and store-and-forward. AMQ 7 
supports multiple protocols and client languages, allowing integration of many, if not all, application assets.

###  Clients
AMQ 7 expands its support of popular messaging APIs and protocols by adding new client libraries, including Java Message Service (JMS) 2.0, JavaScript, C++, .Net, and Python. With existing 
support for the popular open protocols MQTT and AMQP, :amq: now offers broad interoperability across the IT landscape that can open up data in embedded devices to inspection, analysis, and control.

### Interconnect
The new Interconnect router in :amq: enables users to create an internet-scale network of uniformly-addressed messaging paths spanning data centers, cloud services, and geographic zones. The 
interconnect component serves as the backbone for distributed messaging, providing redundant network pathing for fault handling, traffic optimization, and more secure and reliable connectivity.

## S2I Image ##

The S2I-Base-Image directory supplies a base build image & accompanying template set for establishing an environment on OpenShift Container Platform featuring single-broker, symmetric, 
fault-tolerant, and brokerless Interconnect topologies.

## Test Suite ##  

The Test-Suite directory supplies a maven-based project housing a test suite that will showcase the various topologies available after building an OpenShift environment from the provided S2I base 
image and templates.