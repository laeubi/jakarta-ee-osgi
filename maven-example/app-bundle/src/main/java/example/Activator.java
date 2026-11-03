package example;

import java.io.StringWriter;
import java.net.URL;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

/**
 * Reads person.xml from this bundle, modifies it and prints it again.
 */
public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		int exitCode = 0;
		try {
			URL url = context.getBundle().getEntry("person.xml");
			JAXBContext jaxb = JAXBContext.newInstance(Person.class);
			Person person = (Person) jaxb.createUnmarshaller().unmarshal(url);
			System.out.println("Read: " + person.name + ", " + person.age);
			person.age++;
			Marshaller marshaller = jaxb.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			StringWriter writer = new StringWriter();
			marshaller.marshal(person, writer);
			System.out.println("SUCCESS, modified XML:\n" + writer);
		} catch (Throwable t) {
			System.out.println("FAILURE:");
			t.printStackTrace(System.out);
			exitCode = 1;
		}
		// end the run, this is only a demo
		System.exit(exitCode);
	}

	@Override
	public void stop(BundleContext context) {
	}
}
