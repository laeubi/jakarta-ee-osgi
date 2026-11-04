# jakarta-ee-osgi
Showcase for using JakartaEE inside OSGi

## Pitfall: Jakarta EE bundles can not find each other

The [maven-example](maven-example) consists of two modules:

- `app-bundle` is a small bundle (built with the `bnd-maven-plugin`) with an activator that unmarshals the embedded `person.xml` with JAXB, modifies it and prints the marshalled result.
- `runner` resolves and runs the bundle together with the JAXB implementation (`com.sun.xml.bind:jaxb-impl` and `jaxb-core`, they embed `txw2` and `istack` and therefore need fewer dependencies than `org.glassfish.jaxb:jaxb-runtime`) on Apache Felix using a [bndrun](maven-example/runner/example.bndrun).

Run it with `mvn clean verify` inside `maven-example`.
Without the `org.glassfish.hk2.osgi-resource-locator` bundle in the [bndrun](maven-example/runner/example.bndrun) (the naive setup) all bundles resolve and start without any complaint, but the activator fails with:

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

## The fix: org.glassfish.hk2.osgi-resource-locator

Adding `org.glassfish.hk2:osgi-resource-locator` (a bundle without further dependencies) to the runtime makes the example print the modified XML.
Its `Activator` tracks all installed bundles and indexes their `META-INF/services` entries.
`ServiceLoaderUtil` in the API finds the class `org.glassfish.hk2.osgiresourcelocator.ServiceLoader` through its `DynamicImport-Package` header and asks it for the `JAXBContextFactory` providers, which the locator loads through the class loader of the implementation bundle.
That is the missing link: it lets the API reach the implementation although it neither imports nor sees it.

Things to watch out for:

- The locator must be active **before** anything calls `JAXBContext`.
  Until then `ServiceLoader.lookupProviderClasses` returns `null` and you get a `NullPointerException` inside the API instead of the error above.
  In the example the application calls JAXB directly in its activator, so `app-bundle` imports the package `org.glassfish.hk2.osgiresourcelocator` (see its [bnd.bnd](maven-example/app-bundle/bnd.bnd)).
  The code never uses it, but it makes the resolver add the locator to the runtime and start it before the application (`-runstartlevel: order=leastdependenciesfirst`).
- The implementation (`jaxb-impl`) still has to be part of the runtime, the locator only finds what is installed.

## Eclipse / Tycho example

The [eclipse-example](eclipse-example) does the same with Eclipse Tycho 5.0.4 (requires Java 21, as the Eclipse 2026-09 release does):

- `target-platform` is a target definition with the Eclipse 2026-09 release repository (only the OSGi framework, launcher and simpleconfigurator) and the JAXB bundles and the resource locator as Maven target location.
- `app` is the same application as above, as a manifest-first Tycho plug-in.
- `product` is an `eclipse-repository` that materializes a runnable product.

Build with `mvn clean package` inside `eclipse-example` and start the result or directly run from the IDE after setting the target platform.
```

Also here the start order matters: Equinox does not start bundles in dependency order, so the [product](eclipse-example/product/example.product) starts `org.glassfish.hk2.osgi-resource-locator` explicitly at start level 2, before the application at level 4.
