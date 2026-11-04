# Eclipse / Tycho example: JAXB with the OSGi resource locator

Does the same as the [maven-example](../maven-example) (**Option 1** of the [main README](../README.md)) with Eclipse Tycho 5.0.4 and Equinox.
It requires Java 21, as the Eclipse 2026-09 release does.

- `target-platform` is a target definition with the Eclipse 2026-09 release repository (only the OSGi framework, launcher and simpleconfigurator) and the JAXB bundles and the resource locator as Maven target location.
- `app` is the same application as in the Maven example, as a manifest-first plug-in.
- `product` is an `eclipse-repository` that materializes a runnable product.

Build it with `mvn clean package` and start the result from `product/target/products`, or run directly from the IDE after setting the target platform.

Equinox does not start bundles in dependency order, so the [product](product/example.product) starts `org.glassfish.hk2.osgi-resource-locator` explicitly at start level 2, before the application at level 4.
Note that `eclipse.ini` is ignored when starting with `java -jar`, pass `-Declipse.ignoreApp=true -Dosgi.noShutdown=true` yourself in that case.
