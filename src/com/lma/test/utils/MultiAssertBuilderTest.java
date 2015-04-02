package com.lma.test.utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lma.utils.MultiAssertBuilder;

public class MultiAssertBuilderTest {
	
	@Rule
	public ExpectedException expe = ExpectedException.none();
	
	private Human bob1;
	private Human bob2;
	private Dog dog1;
	
	@Before
	public void init(){
		dog1 = new Dog("youpi", 5, null);
		bob1 = new Human(null, "bobby", "address11", 26, Human.HumanType.BIG, Human.HumanType.THIN, null, dog1);
		bob2 = new Human("bob", "bobby", "address22", 25, Human.HumanType.BIG, Human.HumanType.THIN, bob1, dog1);
		bob1.setFriend(bob2);
	}
	
	@Test
	public void testSetAssertNotEqualFields(){
		new MultiAssertBuilder(bob1, bob2)
			.setAssertNotEqualFields("name", "address","age","friend.age")
			.runAssertions();
	}
	
	@Test
	public void testSetAssertEqualFields(){
		final String falseSubField = "qfqf";
		expe.expect(IllegalArgumentException.class);
		expe.expectMessage("The sub-field '"+falseSubField+"' does not exist in the field 'dog'.");
		
		new MultiAssertBuilder(bob1, bob2)
			.setAssertEqualFields("surname","humanType","dog","dog.name","dog.age")
			.runAssertions();
		new MultiAssertBuilder(bob1, bob2)
			.setAssertEqualFields("dog."+falseSubField)
			.runAssertions();
	}
	
	@Test
	public void testSetAssertNotNullFields(){
		new MultiAssertBuilder(bob1, bob2)
			.setAssertNotNullFields("surname", "dog.name", "humanType", "dog.age", "age")
			.runAssertions();
	}
	
	@Test
	public void testSetAssertNullFields(){
		new MultiAssertBuilder(bob1, bob2)
			.setAssertNullFields("name", "dog.toy")
			.runAssertions();
	}
	
	@Test
	public void testInvalidSubFieldName1(){
		expe.expect(IllegalArgumentException.class);
		expe.expectMessage("The field 'na' does not exist in the type 'com.lma.test.utils.Human'. Check your String parameters.");
		new MultiAssertBuilder(bob1, bob2)
			.setAssertNullFields("na.me")
			.runAssertions();
	}
	
	@Test
	public void testInvalidSubFieldName2(){
		expe.expect(IllegalArgumentException.class);
		expe.expectMessage("The sub-field 'toey' does not exist in the field 'dog'.");
		new MultiAssertBuilder(bob1, bob2)
			.setAssertNullFields("dog.toey")
			.runAssertions();
	}
	
	@Test
	public void testThisOneShouldGoOK(){
		new MultiAssertBuilder(bob1, null)
		.runAssertions();
	}
	
	@Test
	public void testInvalidConstructorParameter1(){
		expe.expect(IllegalArgumentException.class);
		expe.expectMessage("'actual' parameter is null in constructor.");
		new MultiAssertBuilder(null, bob2)
			.setAssertValue("age", 26, true)
			.runAssertions();
	}
	
	@Test
	public void testInvalidConstructorParameter2(){
		expe.expect(IllegalArgumentException.class);
		expe.expectMessage("'expected' parameter is null in constructor.");
		new MultiAssertBuilder(bob1, null)
			.setAssertNullFields("dog.toey")
			.runAssertions();
	}
	
	@Test
	public void testAssertValues(){
		new MultiAssertBuilder(bob1, bob2, true)
			.setAssertValue("age", 26, true)
			.setAssertValue("surname", "bobby", true)
			.setAssertValue("name", null, true)
			.setAssertValue("wannabe", Human.HumanType.BIG, false)
			.setAssertValue("dog.age", 5, true)
			.setAssertValue("dog.name", "popo", false)
			.runAssertions();
	}
	
	@Test
	public void testAssertValuesWithWrongSubField(){
		final String falseSubField = "qfqf";
		expe.expect(IllegalArgumentException.class);
		expe.expectMessage("The sub-field '"+falseSubField+"' does not exist in the field 'dog'.");
		new MultiAssertBuilder(bob1, null)
			.setAssertValue("dog." + falseSubField, 26, true)
			.runAssertions();
	}
	
	@Test
	public void testAssertValuesWithWrongField(){
		final String falseField = "qfqf";
		expe.expect(IllegalArgumentException.class);
		expe.expectMessage("The field '"+falseField+"' does not exist in the type 'com.lma.test.utils.Human'. Check your String parameters.");
		new MultiAssertBuilder(bob1, null)
			.setAssertValue(falseField, 26, true)
			.runAssertions();
	}

	
	@Test
	public void testGlobal1(){
		new MultiAssertBuilder(bob1, bob2, true)
			.setAssertNotEqualFields("address","age","friend.age")
			.setAssertEqualFields("dog.name", "humanType")
			.setAssertNotNullFields("friend")
			.setAssertNullFields("name")
			.setAssertValue("dog.age", 5, true)
			.runAssertions();
	}
	
	@Test
	public void testGlobal2(){
		try{
		new MultiAssertBuilder(bob1, bob2, true)
			.setAssertNotEqualFields("dog")
			.setAssertEqualFields("name")
			.setAssertNotNullFields("name")
			.setAssertNullFields("dog.name")
			.setAssertValue("wannabe", Human.HumanType.BIG, true)
			.setAssertValue("humanType", Human.HumanType.BIG, true)
			.setAssertValue("dog.age", 10, true)
			.setAssertValue("dog.name", "doddo", false)
			.setAssertValue("name", null, true)
			.runAssertions();
		}catch(final AssertionError ae){
			Assert.assertTrue(ae.getMessage().contains("6 error(s)"));
		}
	}
}


class Dog {

	private String name;
	private int age;
	private String toy;
	
	public Dog(String name, int age, String toy){
		this.name = name;
		this.age = age;
		this.toy = toy;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getToy() {
		return toy;
	}

	public void setToy(String toy) {
		this.toy = toy;
	}
}

class Human {
	
	public enum HumanType{
		TALL, THIN, BIG;
	}

	private String name;
	private String surname;
	private String address;
	private int age;
	private HumanType humanType;
	private HumanType wannabe;
	private Human friend;
	private Dog dog;
	
	public Human(String name, String surname, String address, int age, HumanType humanType, HumanType wannabe, Human friend, Dog dog){
		this.name = name;
		this.surname = surname;
		this.address = address;
		this.age = age;
		this.humanType = humanType;
		this.wannabe = wannabe;
		this.friend = friend;
		this.setDog(dog);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public HumanType getHumanType() {
		return humanType;
	}

	public void setHumanType(HumanType humanType) {
		this.humanType = humanType;
	}

	public HumanType getWannabe() {
		return wannabe;
	}

	public void setWannabe(HumanType wannabe) {
		this.wannabe = wannabe;
	}

	public Human getFriend() {
		return friend;
	}

	public void setFriend(Human friend) {
		this.friend = friend;
	}

	public Dog getDog() {
		return dog;
	}

	public void setDog(Dog dog) {
		this.dog = dog;
	}
}

