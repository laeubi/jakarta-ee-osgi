# Maven example: JAXB with the OSGi resource locator

This example shows the pitfall and **Option 1** of the [main README](../README.md) on Apache Felix, built and run with Maven and bnd.

- `app-bundle` is a small bundle (built with the `bnd-maven-plugin`) with an activator that unmarshals the embedded `person.xml` with JAXB, modifies it and prints the marshalled result.
- `runner` resolves and runs the bundle with the `bnd-resolver-maven-plugin` and `bnd-run-maven-plugin` using the [bndrun](runner/example.bndrun).
  It runs `com.sun.xml.bind:jaxb-impl` and `jaxb-core`, they embed `txw2` and `istack` and therefore need fewer dependencies than `org.glassfish.jaxb:jaxb-runtime`.

Run it with `mvn clean verify`, the activator prints the modified XML.

## Reproducing the failure

Remove the `Import-Package: org.glassfish.hk2.osgiresourcelocator, *` line from [bnd.bnd](app-bundle/bnd.bnd) (and the `osgi-resource-locator` dependency in the [runner pom](runner/pom.xml)).
All bundles still resolve and start, but the activator fails with the `ClassNotFoundException: org.glassfish.jaxb.runtime.v2.ContextFactory` described in the main README.

## How the locator is added

- The `runner` pom has `org.glassfish.hk2:osgi-resource-locator` as dependency, so bnd can resolve against it.
- `app-bundle` imports the package `org.glassfish.hk2.osgiresourcelocator`.
  The code never uses it, but it makes the resolver add the locator to the runtime and start it before the application (`-runstartlevel: order=leastdependenciesfirst`).
  Without it the locator is not active when the activator calls JAXB and you get a `NullPointerException` instead of the original error.
