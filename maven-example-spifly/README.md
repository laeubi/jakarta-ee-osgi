# Maven example: JAXB with Aries SPI Fly

A copy of the [maven-example](../maven-example) that uses the Service Loader Mediator (**Option 2** of the [main README](../README.md)) instead of the resource locator.
The mediator is [Apache Aries SPI Fly](https://aries.apache.org/documentation/modules/spi-fly.html) (`org.apache.aries.spifly.dynamic.bundle`), used here only as an example of an implementation.

Differences to the `maven-example`:

- The `runner` depends on SPI Fly instead of the locator, bnd adds the required ASM bundles when resolving.
- `app-bundle` has no extra import, it does not know anything about the mediator.

Run it with `mvn clean verify`, the activator prints the modified XML.

It works because `jakarta.xml.bind-api` and `jaxb-impl` (in the versions used here) declare the `osgi.serviceloader` and `osgi.extender` headers, see the version table in the main README.
The mediator must be active before the API bundle is used, here this is ensured by `-runstartlevel: order=leastdependenciesfirst`.
