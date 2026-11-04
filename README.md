# jakarta-ee-osgi
Showcase for using JakartaEE inside OSGi

## Examples

Every example prints a modified XML document that it reads and writes with JAXB from inside an OSGi framework.
See the README in each folder for details.

| Example                                                           | Build        | Framework | Solution                              |
|-------------------------------------------------------------------|--------------|-----------|---------------------------------------|
| [maven-example](maven-example)                                    | Maven + bnd  | Felix     | Option 1: resource locator            |
| [maven-example-spifly](maven-example-spifly)                      | Maven + bnd  | Felix     | Option 2: mediator (Aries SPI Fly)    |
| [eclipse-example](eclipse-example)                                | Tycho        | Equinox   | Option 1: resource locator            |
| [eclipse-example-spifly](eclipse-example-spifly)                  | Tycho        | Equinox   | Option 2: mediator (Aries SPI Fly)    |
| [eclipse-example-equinox-spi](eclipse-example-equinox-spi)        | Tycho        | Equinox   | Option 2: mediator (native, preview)  |

## Pitfall: Jakarta EE bundles can not find each other

If you install the Jakarta XML Binding API and the JAXB reference implementation (`jaxb-impl`) into an OSGi framework, all bundles resolve and start without any complaint, but the first call to `JAXBContext.newInstance(...)` fails with:

```
jakarta.xml.bind.JAXBException: Implementation of Jakarta XML Binding-API has not been found on module path or classpath.
 - with linked exception:
[java.lang.ClassNotFoundException: org.glassfish.jaxb.runtime.v2.ContextFactory]
```

The message points to the module path or classpath, which is misleading in OSGi: the implementation is installed and resolved, it is just not discoverable.

### Why does it happen

Reading the sources of `jakarta.xml.bind-api` (`ContextFinder` and `ServiceLoaderUtil`) shows how the implementation is looked up:

1. `java.util.ServiceLoader.load(JAXBContextFactory.class)` is used, this only sees what the thread context class loader offers.
   In OSGi the `META-INF/services` entry of the implementation bundle is not visible to the class loader of the API bundle.
2. As a fallback `ServiceLoaderUtil.lookupUsingOSGiServiceLoader` reflectively loads the class `org.glassfish.hk2.osgiresourcelocator.ServiceLoader` and calls `lookupProviderClasses`.
   The class is not there, the `ClassNotFoundException` is swallowed (logged on `FINE` only).
3. As a last resort the hard coded default `org.glassfish.jaxb.runtime.v2.ContextFactory` is loaded with the class loader of the API bundle.
   The API bundle does not import `org.glassfish.jaxb.runtime.v2` (it can't, it has no dependency on the implementation), so this fails with the error above.

As the API only uses reflection and the API bundle has no way to declare a dependency on its provider, the OSGi resolver can not help here and every step fails silently.

There are two ways to fix this.

## Option 1: org.glassfish.hk2.osgi-resource-locator

Adding `org.glassfish.hk2:osgi-resource-locator` (a bundle without further dependencies) to the runtime makes the examples work.
Its `Activator` tracks all installed bundles and indexes their `META-INF/services` entries.
`ServiceLoaderUtil` in the API finds the class `org.glassfish.hk2.osgiresourcelocator.ServiceLoader` through its `DynamicImport-Package` header and asks it for the `JAXBContextFactory` providers, which the locator loads through the class loader of the implementation bundle.
That is the missing link: it lets the API reach the implementation although it neither imports nor sees it.

Things to watch out for:

- The locator must be active **before** anything calls `JAXBContext`.
  Until then `ServiceLoader.lookupProviderClasses` returns `null` and you get a `NullPointerException` inside the API instead of the error above.
- The implementation (`jaxb-impl`) still has to be part of the runtime, the locator only finds what is installed.

Examples: [maven-example](maven-example) (see there how to make sure the locator starts first) and [eclipse-example](eclipse-example) (Tycho, explicit start level in the product).

## Option 2: Service Loader Mediator

OSGi already offers a standard solution for this problem: the [Service Loader Mediator Specification](https://docs.osgi.org/specification/osgi.cmpn/8.1.0/service.loader.html).
Bundles declare the services they provide and consume with the `osgi.serviceloader` capability and the `osgi.extender` requirement, and a mediator makes `java.util.ServiceLoader` work across bundles.
The application needs no knowledge of the mediator, it only has to be installed in the runtime.

This works because `jakarta.xml.bind-api` declares an optional `Require-Capability` for `osgi.extender=osgi.serviceloader.processor` and for `osgi.serviceloader` of `jakarta.xml.bind.JAXBContextFactory`, and `jaxb-impl` declares the matching `Provide-Capability`.
As with the locator, the mediator must be active before the API is used.

**Check before relying on it:** this only works if the Jakarta EE API and the implementation in the versions you use actually contain these headers.
For Jakarta XML Binding they were added only recently (checked against the manifests on Maven Central):

| Artifact                                                                   | First version with the headers |
|----------------------------------------------------------------------------|--------------------------------|
| `jakarta.xml.bind:jakarta.xml.bind-api`                                    | 4.0.1                          |
| `com.sun.xml.bind:jaxb-impl` and `org.glassfish.jaxb:jaxb-runtime`         | 4.0.4                          |

Older versions (including all 2.3.x and 3.0.x) have none, so the mediator can not help there and the resource locator is needed.
Do not assume that other specifications behave the same way: inspect the `MANIFEST.MF` of the API and the implementation for `osgi.serviceloader` and `osgi.extender`.

Examples with [Apache Aries SPI Fly](https://aries.apache.org/documentation/modules/spi-fly.html), which is just one implementation of a mediator:

- [maven-example-spifly](maven-example-spifly)
- [eclipse-example-spifly](eclipse-example-spifly), where SPI Fly only registers providers of **started** bundles, so `jaxb-impl` has to be started explicitly.

Equinox gets its own implementation ([eclipse-equinox/equinox#295](https://github.com/eclipse-equinox/equinox/pull/295)), which will be available from the Eclipse 2026-12 release.
A preview is already available in the Eclipse milestone repository: [eclipse-example-equinox-spi](eclipse-example-equinox-spi) needs neither an additional bundle nor an explicit start of `jaxb-impl`.

## Background and outlook

The Service Loader Mediator is not generally adopted, and it requires the cooperation of every API and provider: each one has to ship the correct headers, and many Jakarta EE APIs still contain their own hand-rolled lookup (for example the reflective call into `org.glassfish.hk2.osgiresourcelocator.ServiceLoader` shown above).
As a result users often have to know about workarounds like the resource locator.

To improve the situation there is an initiative to bring SPI support into the OSGi core framework, so that it works without modifying the providers and consumers:

- [osgi/osgi#924](https://github.com/osgi/osgi/pull/924) is the pull request with the specification proposal.
- [osgi/osgi#372](https://github.com/osgi/osgi/issues/372) is the issue it originates from.

It is currently in the specification phase, so feedback is very welcome.
