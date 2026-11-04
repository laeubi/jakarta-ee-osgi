package example;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "person")
public class Person {
	public String name;
	public int age;
}
