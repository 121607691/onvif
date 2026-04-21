# Releases

This file summarizes repository-level release and packaging notes for the Java
ONVIF library.

## Current packaging model

- Root project: `org.onvif:onvif:1.0-SNAPSHOT` (aggregator POM)
- Published modules:
  - `org.onvif:onvif-ws-client`
  - `org.onvif:onvif-java`
- Local distribution target:
  - `dist/mvn-repo`

## Scope of published artifacts

Only the Maven modules under `onvif-ws-client/` and `onvif-java/` are treated as
published deliverables.

Repository examples under `examples/` are intentionally excluded from the
published API surface:

- they are reference code
- they are not stable compatibility contracts
- they are not distributed as dedicated Maven artifacts

## Release checklist

Before cutting a release, verify:

1. `mvn -q test`
2. `mvn -q package`
3. generated client bindings are intentional
4. no `.class` files or temporary local scripts are tracked
5. `README.md` and example docs reflect the current repository layout

## Notes

- `onvif-java` attaches a shaded `with-dependencies` jar in addition to the
  standard thin jar.
- The repository keeps example code separate so library consumers can depend on
  the modules without pulling in local debugging assets.
