# Eclipse / Tycho example: JAXB with the native Equinox mediator (preview)

A copy of the [eclipse-example-spifly](../eclipse-example-spifly) that uses the Service Loader Mediator implementation built into Equinox ([eclipse-equinox/equinox#295](https://github.com/eclipse-equinox/equinox/pull/295)), **Option 2** of the [main README](../README.md).
It is expected to be part of the Eclipse 2026-12 release and is already available in the milestone repository.

Differences to the `eclipse-example-spifly`:

- The target definition uses `https://download.eclipse.org/releases/milestone/` and adds the unit `org.eclipse.equinox.spi`, there is no mediator in the Maven target location.
- `org.eclipse.equinox.spi` is a framework extension fragment of `org.eclipse.osgi`, so it needs no start level.
- `jaxb-impl` does not need to be started explicitly, the example just works.
- The Eclipse project names are prefixed with `equinox-spi-`.

As the milestone repository is a moving target, treat this as a preview.
