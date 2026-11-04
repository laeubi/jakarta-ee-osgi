# Eclipse / Tycho example: JAXB with Aries SPI Fly

A copy of the [eclipse-example](../eclipse-example) that uses the Service Loader Mediator (**Option 2** of the [main README](../README.md)) with [Apache Aries SPI Fly](https://aries.apache.org/documentation/modules/spi-fly.html) instead of the resource locator.

Differences to the `eclipse-example`:

- The Maven target location contains `org.apache.aries.spifly.dynamic.bundle` (and, transitively, the ASM bundles) instead of the locator.
- The [product](product/example.product) starts SPI Fly at start level 2.
- SPI Fly only registers providers of bundles that are **started**, and Equinox does not start bundles that are merely resolved.
  Therefore the product starts `jaxb-impl` explicitly (start level 3), before the application (level 4).
  Without it the activator fails with the original `ClassNotFoundException`.
- The Eclipse project names are prefixed with `spifly-` so they can be imported into the same workspace as the other examples.
