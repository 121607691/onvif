# Java ONVIF

This repository provides a Java ONVIF foundation library intended to be packaged
and consumed as a dependency by other projects.

The project is organized as a Maven multi-module build:

- `onvif-ws-client`: generated and maintained SOAP client bindings
- `onvif-java`: higher-level Java wrapper APIs built on top of the bindings

The main goal is to keep ONVIF web service interaction behind Apache CXF and a
small Java-facing API surface so downstream projects can focus on application
logic instead of maintaining SOAP integration code.

## Repository layout

```text
onvif/
|- onvif-ws-client/
|- onvif-java/
|- examples/
|  \- discovery/
|- docs/
```

`examples/` contains reference sample code only. Example sources are not part of
the published library API surface and should not be treated as stable contracts.

## Build and test

Run the full test suite:

```bash
mvn -q test
```

Build the modules:

```bash
mvn -q package
```

The root `distributionManagement` is configured to publish artifacts into the
repository-local directory below:

```text
dist/mvn-repo
```

To produce the local Maven repository layout:

```bash
mvn -q deploy
```

## Published artifacts

The project is intended to be consumed through these module artifacts:

- `org.onvif:onvif-ws-client`
- `org.onvif:onvif-java`

`onvif-java` also attaches a `with-dependencies` shaded jar for command-line or
standalone integration scenarios, while the main artifact remains the thin jar
for regular Maven dependency usage.

## Examples

Discovery examples live in [examples/discovery/README.md](examples/discovery/README.md).

They are useful for integration checks and local experimentation, but they are
kept separate from the core library on purpose:

- they are not published as Maven modules
- they may evolve with repository structure
- they should not be imported as production dependencies

## Regenerating WSDL bindings

If you need to refresh generated bindings after updating managed WSDL files:

1. Download the required ONVIF WSDLs into the resource directory used by
   `onvif-ws-client`.
2. Update any related WSDL location constants if the service catalog changes.
3. Adjust catalog rewriting rules if new remote imports are introduced.
4. Regenerate bindings through the configured CXF build flow.
5. Re-run tests before committing regenerated output.

## Project direction

The repository remains focused on:

- maintainable ONVIF Java integration
- generated client bindings separated from wrapper logic
- dependency-friendly packaging for downstream projects
- a small set of reference examples kept outside the published modules
