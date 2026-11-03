# jakarta-ee-osgi
Showcase for using JakartaEE inside OSGi

## Pitfall: Jakarta EE bundles can not find each other

The [maven-example](maven-example) consists of two modules:

- `app-bundle` is a small bundle (built with the `bnd-maven-plugin`) with an activator that unmarshals the embedded `person.xml` with JAXB, modifies it and prints the marshalled result.
- `runner` resolves and runs the bundle together with the JAXB implementation (`com.sun.xml.bind:jaxb-impl` and `jaxb-core`, they embed `txw2` and `istack` and therefore need fewer dependencies than `org.glassfish.jaxb:jaxb-runtime`) on Apache Felix using a [bndrun](maven-example/runner/example.bndrun).

Run it with `mvn clean verify` inside `maven-example`.
All bundles resolve and start without any complaint, but the activator fails with:

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
