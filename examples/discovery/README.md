# Discovery Examples

This directory contains reference examples for discovering ONVIF devices with
the library.

These examples are provided for local verification and integration experiments.
They are **not** part of the published Maven API surface and should not be
treated as stable contracts.

## Files

- `SimpleDiscovery.java`: lightweight WS-Discovery example using UDP probe
  messages directly
- `DiscoveryTest.java`: example that uses the library's discovery API

## Relationship to the library

The production library is published from:

- `onvif-ws-client`
- `onvif-java`

Everything under `examples/` is sample code only.

## Build prerequisites

Build the repository first so module classes are available:

```bash
mvn -q package
```

## Run the library-backed example

From the repository root:

```bash
javac -cp "onvif-java/target/classes;onvif-ws-client/target/classes" examples/discovery/DiscoveryTest.java
java -cp ".;examples/discovery;onvif-java/target/classes;onvif-ws-client/target/classes" DiscoveryTest
```

This example does not require a device IP. It broadcasts discovery requests on
the local network and prints any discovered ONVIF service URLs.

## Run the lightweight UDP example

From the repository root:

```bash
javac examples/discovery/SimpleDiscovery.java
java -cp ".;examples/discovery" SimpleDiscovery
```

This example also runs without a device IP. It sends WS-Discovery probe packets
to the standard multicast address and prints any discovered XAddr endpoints.

## Stability note

Example source files may change when repository structure or verification needs
change. If you need a stable integration surface, depend on `onvif-java` rather
than copying assumptions from these examples.
