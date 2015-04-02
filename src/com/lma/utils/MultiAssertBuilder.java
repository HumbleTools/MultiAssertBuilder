package com.lma.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Assert;

/**
 * MultiAssertBuilder is a class that will enable fast assertions for all the fields of an object,
 * without the need to write an assertion instruction for each field. It should be used in a JUnit test class.
 * The developer now only needs to specify which field names to assert as not equal, equal, null or not null.
 * You can also choose to assert equal not not equal a specific field against a specific value.
 * It is possible to assert sub-fields (1 level down) with the dot notation if you have nested objects.
 * All of the results will be displayed in the logs with the verbose mode on.
 * With the verbose mode off, only failed assertions will be displayed.<br /><br />
 * 
 * How to use :<br /><br />
 * <pre>
 * {@code new MultiAssertBuilder(objectToTest, referenceObject)
 * 	.setAssertNotEqualFields("fieldToAssertNotEqual1", "fieldToAssertNotEqual2", "field1.subfield1", ...)
 * 	.setAssertEqualFields("fieldToAssertEqual1", "fieldToAssertEqual2", "field2.subfield2", ...)
 * 	.setAssertNotNullFields("fieldToAssertNotNull1", "fieldToAssertNotNull2", ...)
 * 	.setAssertNullFields("fieldToAssertNull1", "fieldToAssertNull2", ...)
 * 	.setAssertValue("field12", valueToTestAgainst12, true)
 * 	.setAssertValue("field18", valueToTestAgainst18, false)
 * 	.setAssertValue("field22.subfield1", valueToTestAgainst22, false)
 * 	.setAssertValue("field28", null, true)
 * 	.runAssertions();
 * }</pre>
 * @author Louis Madeuf
 * @version 2.1
 */
public class MultiAssertBuilder{

	/**
	 * AssertionType enumerates all the assertion operations covered by MultiAssertBuilder.
	 * Each enumerated value holds two specific customizable log messages : one for assertion errors and another for assertion success.
	 */
	private enum AssertionType{

		NOT_EQUALS("OK : '%s' fields are not equal. Actual : '%s' / Expected : '%s'", "KO : the fields named '%s' are equal but they should not be.\n	Actual : '%s' / Expected : '%s'"),
		EQUALS("OK : '%s' fields are equal. Actual : '%s' / Expected : '%s'", "KO : the fields named '%s' are not equal but they should be.\n	Actual : '%s' / Expected : '%s'"),
		NULL("OK : '%s' field is null. Actual : '%s' / Expected : '%s'", "KO : the field named '%s' is not null but it should be.\n	Actual : '%s' / Expected : '%s'"),
		NOT_NULL("OK : '%s' field is not null. Actual : '%s' / Expected : '%s'", "KO : the field named '%s' is null but it should not be.\n	Actual : '%s' / Expected : '%s'");

		private String successMessage;
		private String errorMessage;

		private AssertionType(final String successMessage, final String errorMessage){
			this.errorMessage = errorMessage;
			this.successMessage = successMessage;
		}

		/**
		 * Retrieve the message for a successful assertion.
		 */
		public String getSuccessMessage(){
			return successMessage;
		}

		/**
		 * Retrieve the message for an assertion error.
		 */
		public String getErrorMessage(){
			return errorMessage;
		}
	}

	/**
	 * Field storing the time at which the tests started, in nanoseconds.
	 */
	private long startTime;
	
	/**
	 * Field storing the time at which the tests ended, in nanoseconds.
	 */
	private long endTime;
	
	/**
	 * Message constants.
	 */
	private static final String METHOD_SET_ASSERT_VALUE_FIELD_NAME_PARAMETER_IS_NULL_OR_EMPTY = "Method setAssertValue() - fieldName parameter is null or empty.";
	private static final String THE_FIELD_DOES_NOT_EXIST_IN_THE_TYPE = "The field '%s' does not exist in the type '%s'. Check your String parameters.";
	private static final String UNKNOWN_ERROR = "The field named '%s' should have been made accessible. This error is not supposed to happen !";
	private static final String FIELD_VALUE_CANNOT_BE_NULL_TO_FETCH_SUB_FIELD = "The field value '%s' cannot be null to fetch sub-field '%s'.";
	private static final String EXECUTE_OUT_SUCCESS = "=> MultiAssertBuilder tests for two '%s' ended successfully with no errors and lasted %.3f milliseconds.";
	private static final String EXECUTE_OUT_MSG_ERROR_MAIN = "=> MultiAssertBuilder tests for '%s' ended with %d error(s) and lasted %.3f milliseconds.";
	private static final String THE_SUB_FIELD_DOES_NOT_EXIST = "The sub-field '%s' does not exist in the field '%s'.";
	private static final String INCORRECT_FORMAT = "The sub-field '%s' is incorrectly named. 1 dot '.' is required.";
	private static final String EXECUTE_IN_MSG = "=> MultiAssertBuilder is testing two objects of the type '%s'.";
	private static final String MAIN_ASSERTION_ERROR_MESSAGE = "See the logs for the details on the %d error(s).";
	private static final String EXPECTED_IS_NULL_PARAMETER = "'expected' parameter is null in constructor.";
	private static final String ACTUAL_IS_NULL_PARAMETER = "'actual' parameter is null in constructor.";
	private static final String PARAMETERS_NOT_SAME_TYPE = "Both parameters must be of the same type.";
	private static final String ESCAPED_DOT = "\\.";
	private static final String DOT = ".";

	/**
	 * The Object to test.
	 */
	private final Object actual;

	/**
	 * The Object containing the target values.
	 */
	private final Object expected;

	/**
	 * Collection containing the names of the fields to assert as not equal.
	 */
	private final List<String> assertNotEqualFields;

	/**
	 * Collection containing the names of the fields to assert as equal.
	 */
	private final List<String> assertEqualFields;

	/**
	 * Collection containing the names of the fields to assert as not null.
	 */
	private final List<String> assertNotNullFields;

	/**
	 * Collection containing the names of the fields to assert as null.
	 */
	private final List<String> assertNullFields;

	/**
	 * Collection containing the names of the sub-fields to assert as equal.
	 * The key of the map is the name of the attribute of the tested object.
	 * The value is the list of names of the sub-attributes for the above-level key attribute.
	 */
	private final Map<String, List<String>> assertEqualSubFields;

	/**
	 * Collection containing the names of the sub-fields to assert as not equal.
	 * The key of the map is the name of the attribute of the tested object.
	 * The value is the list of names of the sub-attributes for the above-level key attribute.
	 */
	private final Map<String, List<String>> assertNotEqualSubFields;

	/**
	 * Collection containing the names of the sub-fields to assert as null.
	 * The key of the map is the name of the attribute of the tested object.
	 * The value is the list of names of the sub-attributes for the above-level key attribute.
	 */
	private final Map<String, List<String>> assertNullSubFields;

	/**
	 * Collection containing the names of the sub-fields to assert as not null.
	 * The key of the map is the name of the attribute of the tested object.
	 * The value is the list of names of the sub-attributes for the above-level key attribute.
	 */
	private final Map<String, List<String>> assertNotNullSubFields;
	
	/**
	 * Collection containing the list of fields to assert equal against the associated value.
	 * The key is the name of the field.
	 * The value is the value to use for the assertion.
	 */
	private final Map<String, Object> assertEqualsValueFields;
	
	/**
	 * Collection containing the list of fields to assert not equal against the associated value.
	 * The key is the name of the field.
	 * The value is the value to use for the assertion.
	 */
	private final Map<String, Object> assertNotEqualsValueFields;

	/**
	 * Activates the verbose mode.
	 * True : all the logs will be displayed.
	 * False : only errors will be displayed in the logs, if any.
	 */
	private final boolean verbose;

	/**
	 * The Class of the object being examined.
	 */
	private Class<? extends Object> examinedClass;
	
	/**
	 * Map used to store all the necessary fields for each time, so the fields can be fetched only once per type.
	 */
	private Map<String, List<Field>> typeToFieldsListMap;

	/**
	 * Collection of logs for assertions that went well.
	 */
	private final List<String> OKMessages = new ArrayList<String>();

	/**
	 * Collection of logs for assertions that went wrong.
	 */
	private final List<String> KOMessages = new ArrayList<String>();

	/**
	 * Constructor for a MultiAssertBuilder. Mode verbose is off by default.
	 * 
	 * @param actual (Object) the Object to test
	 * @param expected (Object) the Object containing the target values
	 */
	public MultiAssertBuilder(final Object actual, final Object expected){
		this(actual, expected, false);
	}

	/**
	 * Constructor for a MultiAssertBuilder. Verbose mode can be set manually.
	 * 
	 * @param actual (Object) the Object to test
	 * @param expected (Object) the Object containing the target values
	 * @param verbose (boolean) activates the verbose mode (more logs, not only errors)
	 */
	public MultiAssertBuilder(final Object actual, final Object expected, final boolean verbose){
		startTime = System.nanoTime();
		if(actual == null){
			throw new IllegalArgumentException(ACTUAL_IS_NULL_PARAMETER);
		}
		examinedClass = actual.getClass();
		if(expected != null){
			if(!examinedClass.equals(expected.getClass())){
				throw new IllegalArgumentException(PARAMETERS_NOT_SAME_TYPE);
			}
		}
		this.actual = actual;
		this.expected = expected;
		this.verbose = verbose;
		assertNotEqualFields = new ArrayList<String>();
		assertEqualFields = new ArrayList<String>();
		assertNullFields = new ArrayList<String>();
		assertNotNullFields = new ArrayList<String>();
		assertEqualSubFields = new HashMap<String, List<String>>();
		assertNotEqualSubFields = new HashMap<String, List<String>>();
		assertNotNullSubFields = new HashMap<String, List<String>>();
		assertNullSubFields = new HashMap<String, List<String>>();
		assertEqualsValueFields = new HashMap<String, Object>();
		assertNotEqualsValueFields = new HashMap<String, Object>();
		typeToFieldsListMap = new HashMap<String, List<Field>>();
	}

	/**
	 * Set all the field names to assert as not equal. It is made for fields and sub-fields and can be called multiple times 
	 * on the same instance, it will not overwrite previous set values.
	 * 
	 * @param assertNotEqualFields (String...) the names of the fields to add
	 * @return this instance of MultiAssertBuilder
	 */
	public MultiAssertBuilder setAssertNotEqualFields(final String... assertNotEqualFields){
		return genericFieldSetter(assertNotEqualFields, assertNotEqualSubFields, this.assertNotEqualFields);
	}

	/**
	 * Set all the field names to assert as equal. It is made for fields and sub-fields and can be called multiple times 
	 * on the same instance, it will not overwrite previous set values.
	 * 
	 * @param assertNotEqualFields (String...) the names of the fields to add
	 * @return this instance of MultiAssertBuilder
	 */
	public MultiAssertBuilder setAssertEqualFields(final String... assertEqualFields){
		return genericFieldSetter(assertEqualFields, assertEqualSubFields, this.assertEqualFields);
	}

	/**
	 * Sets all the field names to assert as null. It is made for fields and sub-fields and can be called multiple times 
	 * on the same instance, as it will not overwrite previous set values.
	 * 
	 * @param assertNullFields (String...) the names of the fields to add
	 * @return this instance of MultiAssertBuilder
	 */
	public MultiAssertBuilder setAssertNullFields(final String... assertNullFields){
		return genericFieldSetter(assertNullFields, assertNullSubFields, this.assertNullFields);
	}

	/**
	 * Sets all the fields to assert as not null. It is made for fields and sub-fields and can be called multiple times 
	 * on the same instance, as it will not overwrite previous set values.
	 * 
	 * @param assertNotNullFields (String...) the names of the fields to add
	 * @return this instance of MultiAssertBuilder
	 */
	public MultiAssertBuilder setAssertNotNullFields(final String... assertNotNullFields){
		return genericFieldSetter(assertNotNullFields, assertNotNullSubFields, this.assertNotNullFields);
	}
	
	/**
	 * Sets a fieldName to be asserted as equal to a  value if assertEquals is true, or as different if assertEquals is false.
	 * It is made for fields and sub-fields and can be called multiple times on the same instance, as it will not overwrite previous set values.
	 * 
	 * @param fieldName (String) the name of the field to assert against a value.
	 * @param value (Object) the value to assert the field against
	 * @param assertEquals (boolean) determines if the assertion should be equal or not equal.
	 * @return this instance of MultiAssertBuilder
	 */
	public MultiAssertBuilder setAssertValue(final String fieldName, final Object value, final boolean assertEquals){
		if(fieldName==null || fieldName.length()==0){
			throw new IllegalArgumentException(METHOD_SET_ASSERT_VALUE_FIELD_NAME_PARAMETER_IS_NULL_OR_EMPTY);
		}
		if(assertEquals){
			assertEqualsValueFields.put(fieldName, value);
		}else{
			assertNotEqualsValueFields.put(fieldName, value);
		}
		return this;
	}

	/**
	 * Helps filling all the fields of this class, separating dot notation parameters from classic ones.
	 * 
	 * @param parameters raw user parameters.
	 * @param subFieldCollection the collection of sub-fields to fill.
	 * @param fieldCollection the collection of fields to fill.
	 * @return this instance of MultiAssertBuilder.
	 * @throws IllegalArgumentException : if expected field is null because not set in the constructor call.
	 */
	private MultiAssertBuilder genericFieldSetter(final String[] parameters, final Map<String, List<String>> subFieldCollection, final List<String> fieldCollection) {
		if(expected == null){
			throw new IllegalArgumentException(EXPECTED_IS_NULL_PARAMETER);
		}
		if((parameters!=null) && (parameters.length>0)){
			final List<String> usingDot = new ArrayList<String>();
			final List<String> noDot = new ArrayList<String>();
			for(final String parameter : Arrays.asList(parameters)){
				if(parameter.contains(DOT)){
					usingDot.add(parameter);
				}else{
					noDot.add(parameter);
				}
			}
			feedSubFieldMap(usingDot, subFieldCollection);
			fieldCollection.addAll(noDot);
		}
		return this;
	}

	/**
	 * Executes all the assertions as asked by the user and prints all the necessary logs in the output. Constants will be ignored.
	 */
	public void runAssertions(){
		printStartLogs();
		OKMessages.clear();
		KOMessages.clear();
		final List<Field> actualFields = retrieveFieldList(examinedClass);
		checkSpecifiedFields(actualFields);
		for (final Field field : actualFields) {
			final String fieldName = field.getName();
			if(assertEqualSubFields.containsKey(fieldName)){
				assertSubFields(field, assertEqualSubFields.get(fieldName), AssertionType.EQUALS);
			}
			if(assertNotEqualSubFields.containsKey(fieldName)){
				assertSubFields(field, assertNotEqualSubFields.get(fieldName), AssertionType.NOT_EQUALS);
			}
			if(assertNullSubFields.containsKey(fieldName)){
				assertSubFields(field, assertNullSubFields.get(fieldName), AssertionType.NULL);
			}
			if(assertNotNullSubFields.containsKey(fieldName)){
				assertSubFields(field, assertNotNullSubFields.get(fieldName), AssertionType.NOT_NULL);
			}
			if(assertNullFields.contains(fieldName)){
				assertField(field, AssertionType.NULL);
			}
			if(assertNotNullFields.contains(fieldName)){
				assertField(field, AssertionType.NOT_NULL);
			}
			if(assertNotEqualFields.contains(fieldName)){
				assertField(field, AssertionType.NOT_EQUALS);
			}
			if(assertEqualFields.contains(fieldName)){
				assertField(field, AssertionType.EQUALS);
			}
		}
		for (final Entry<String, Object> assertPair : assertEqualsValueFields.entrySet()) {
			assertFieldToValue(actualFields, assertPair.getKey(), assertPair.getValue(), AssertionType.EQUALS);
		}
		for (final Entry<String, Object> assertPair : assertNotEqualsValueFields.entrySet()) {
			assertFieldToValue(actualFields, assertPair.getKey(), assertPair.getValue(), AssertionType.NOT_EQUALS);
		}
		printEndLog();
	}

	/**
	 * Asserts a field against a value for a certain type of assertion.
	 * 
	 * @param actualFields (List<Field>) the list of fields in the type being tested.
	 * @param fieldName (String) the name of the field being tested.
	 * @param expectedValue (Object) the value for the field to be tested against.
	 * @param assertionType (AssertionType) The type of assertion to use.
	 */
	private void assertFieldToValue(final List<Field> actualFields, final String fieldName, final Object expectedValue, final AssertionType assertionType) {
		Object actualValue = null;
		Field field = null;
		final String[] splitResult = fieldName.split(ESCAPED_DOT);
		try{
			if(splitResult.length==2){
				field = findField(splitResult[0], actualFields);
				actualValue = getSubFieldValue(splitResult[1], field, actual);
			}else if(splitResult.length==1){
				field = findField(splitResult[0], actualFields);
				actualValue = field.get(actual);
			}else{
				throw new IllegalArgumentException(String.format(INCORRECT_FORMAT, fieldName));
			}
			doCoreAssertion(assertionType, actualValue, expectedValue);
			logOKMessage(String.format(assertionType.getSuccessMessage(), fieldName, actualValue, expectedValue));
		} catch (final AssertionError ae){
			KOMessages.add(String.format(assertionType.getErrorMessage(), fieldName, getStringValue(actualValue), getStringValue(expectedValue)));
		} catch (final IllegalAccessException e) {
			throw new RuntimeException(String.format(UNKNOWN_ERROR, fieldName));
		}
	}

	/**
	 * Asserts a single field for a certain assertion type.
	 * 
	 * @param field (Field) : the field to assert.
	 * @param assertionType (AssertionType) : the assertion type to use.
	 */
	private void assertField(final Field field, final AssertionType assertionType) {
		Object actualValue = null;
		Object expectedValue = null;
		String fieldName = null;
		try{
			fieldName = field.getName();
			actualValue = field.get(actual);
			expectedValue = field.get(expected);
			doCoreAssertion(assertionType, actualValue, expectedValue);
			logOKMessage(String.format(assertionType.getSuccessMessage(), fieldName, actualValue, expectedValue));
		} catch (final AssertionError ae){
			KOMessages.add(String.format(assertionType.getErrorMessage(), fieldName, getStringValue(actualValue), getStringValue(expectedValue)));
		} catch (final IllegalAccessException iae){
			throw new RuntimeException(String.format(UNKNOWN_ERROR, fieldName));
		}
	}
	
	/**
	 * Performs a simple asertion between two objects.
	 * 
	 * @param assertionType (AssertionType) The type of assertion to perform.
	 * @param actualValue (Object) The object which contains the value to test.
	 * @param expectedValue (Object) The object which contains the value to be tested against.
	 */
	private void doCoreAssertion(final AssertionType assertionType, final Object actualValue, final Object expectedValue) {
		switch(assertionType){
			case EQUALS :
				Assert.assertEquals(actualValue, expectedValue);
				break;
			case NOT_EQUALS:
				Assert.assertNotEquals(actualValue, expectedValue);
				break;
			case NOT_NULL:
				Assert.assertNotNull(actualValue);
				break;
			case NULL:
				Assert.assertNull(actualValue);
				break;
		}
	}

	/**
	 * Asserts a list of sub-field for a certain assertion type, under a main field.
	 * 
	 * @param field (Field) : the field to be asserted.
	 * @param subFieldsToAssertEquals (List<String>) : the list of fields to assert.
	 * @param assertionType (List<String>) : the type of assertion to use.
	 */
	private void assertSubFields(final Field field, final List<String> subFieldsToAssertEquals, final AssertionType assertionType) {
		for (final String subFieldName : subFieldsToAssertEquals) {
			final String fieldName = field.getName();
			final String composedFieldName = buildComposedFieldName(subFieldName, fieldName);
			Object actualValue = null;
			Object expectedValue = null;
			try{
				actualValue = getSubFieldValue(subFieldName, field, actual);
				expectedValue = getSubFieldValue(subFieldName, field, expected);
				doCoreAssertion(assertionType, actualValue, expectedValue);
				logOKMessage(String.format(assertionType.getSuccessMessage(), composedFieldName, actualValue, expectedValue));
			} catch (final AssertionError ae){
				KOMessages.add(String.format(assertionType.getErrorMessage(), composedFieldName, getStringValue(actualValue), getStringValue(expectedValue)));
			} catch (final IllegalAccessException e) {
				throw new RuntimeException(String.format(UNKNOWN_ERROR, fieldName));
			}
		}
	}

	/**
	 * Builds a name like so : subFieldName + '.' + fieldName.
	 * 
	 * @param subFieldName (String) 
	 * @param fieldName (String) 
	 * @return (String) subFieldName + '.' + fieldName
	 */
	private String buildComposedFieldName(final String subFieldName, final String fieldName) {
		final StringBuilder builder = new StringBuilder();
		builder.append(fieldName);
		builder.append(DOT);
		builder.append(subFieldName);
		return builder.toString();
	}

	/**
	 * Determines if the developer has named only existing fields.
	 * 
	 * @param actualFields (List<Field>) : the fields of the type being tested.
	 * @throws IllegalArgumentException : if one of the parameters entered by the developer does not exist in the type being checked for assertions.
	 */
	private void checkSpecifiedFields(final List<Field> actualFields) {
		final List<String> actualFieldNames = new ArrayList<String>();
		final List<String> parametersList = new ArrayList<String>();
		if(actualFields!=null){
			for (final Field field : actualFields) {
				actualFieldNames.add(field.getName());
			}
		}
		parametersList.addAll(assertNotEqualFields);
		parametersList.addAll(assertEqualFields);
		parametersList.addAll(assertNotNullFields);
		parametersList.addAll(assertNullFields);
		parametersList.addAll(assertNotEqualSubFields.keySet());
		parametersList.addAll(assertEqualSubFields.keySet());
		parametersList.addAll(assertNotNullSubFields.keySet());
		parametersList.addAll(assertNullSubFields.keySet());
		parametersList.addAll(getOnlyTopFieldNames(assertEqualsValueFields.keySet()));
		parametersList.addAll(getOnlyTopFieldNames(assertNotEqualsValueFields.keySet()));
		for (final String parameter : parametersList) {
			if(!actualFieldNames.contains(parameter)){
				throw new IllegalArgumentException(String.format(THE_FIELD_DOES_NOT_EXIST_IN_THE_TYPE, parameter, examinedClass.getName()));
			}
		}
	}

	/**
	 * Retrieves all the field names but discards the sub-field parts.
	 * 
	 * @param (Set<String>) a collection of field names that can contain also dot notation.
	 * @return (List<String>) the top field names.
	 * @throws IllegalArgumentException : if a field name in the keySet has more than one dot.
	 */
	private List<String> getOnlyTopFieldNames(final Set<String> keySet) {
		final List<String> resultList = new ArrayList<String>();
		if(keySet!=null){
			for (String fieldName : keySet) {
				final String[] splitResult = fieldName.split(ESCAPED_DOT);
				if(splitResult.length<=2) {
					resultList.add(splitResult[0]);
				}else{
					throw new IllegalArgumentException(String.format(INCORRECT_FORMAT, fieldName));
				}
			}
		}
		return resultList;
	}

	/**
	 * Fetches the sub-field value.
	 * 
	 * @param subFieldName (String) : the name of the sub-field.
	 * @param field (Field) : the name of the field containing the sub-field.
	 * @param source (Object) : the source object.
	 * @return (Object) : the value of the sub-field.
	 * @throws IllegalAccessException see the {@link java.lang.reflect.Field#get(Object) Field.get()} method.
	 * @throws IllegalArgumentException see the {@link java.lang.reflect.Field#get(Object) Field.get()} method.
	 */
	private Object getSubFieldValue(final String subFieldName, final Field field, final Object source) throws IllegalAccessException {
		Object returnValue = null;
		if((subFieldName!=null) && (subFieldName.length()>0) && (field!=null) && (source!=null)){
			final Object subObject = field.get(source);
			if(subObject==null){
				throw new NullPointerException(String.format(FIELD_VALUE_CANNOT_BE_NULL_TO_FETCH_SUB_FIELD, field.getName(), subFieldName));
			}
			final List<Field> subFieldtempList = retrieveFieldList(subObject.getClass());
			final Field subField = findField(subFieldName, subFieldtempList);
			if(subField==null){
				throw new IllegalArgumentException(String.format(THE_SUB_FIELD_DOES_NOT_EXIST, subFieldName, field.getName()));
			}
			returnValue = subField.get(subObject);
		}
		return returnValue;
	}

	/**
	 * Finds a Field in a List<Field> by the fieldName.
	 * 
	 * @param fieldName (String) the name of the field to search.
	 * @param fieldList (List<Field>) the list of Fields to search in.
	 * @return (Field) the Field, if found.
	 */
	private Field findField(final String fieldName, final List<Field> fieldList){
		Field returnValue = null;
		if((fieldName!=null) && (fieldList!=null)){
			for (Field field : fieldList) {
				if(field.getName().equals(fieldName)){
					returnValue = field;
					break;
				}
			}
		}
		return returnValue;
	}

	/**
	 * Logs a message. To be used when an assertion is successful.
	 */
	private void logOKMessage(final String message) {
		if(verbose){
			System.out.println(message);
		}
		OKMessages.add(message);
	}

	/**
	 * This method tries to get the best readable value for the object source.
	 */
	private String getStringValue(final Object source) {
		String returnValue = null;
		if(source!=null){
			if (source instanceof Enum<?>){
				returnValue = ((Enum<?>)source).name();
			} else {
				returnValue = source.toString();
			}
		}
		return returnValue;
	}

	/**
	 * Prints the logs at the start of the assertions.
	 */
	private void printStartLogs() {
		if(verbose){
			System.out.println(String.format(EXECUTE_IN_MSG, examinedClass.getName()));
		}
	}

	/**
	 * Prints the logs at the end of the assertions.
	 */
	private void printEndLog() {
		endTime = System.nanoTime();
		long elapsedTime = endTime - startTime;
		double durationInSeconds = (double) elapsedTime / 1000000.0f;
		final int KOSize = KOMessages.size();
		if(KOSize>0){
			for (final String message : KOMessages) {
				System.out.println(message);
			}
			if(verbose){
				System.out.println(String.format(EXECUTE_OUT_MSG_ERROR_MAIN, examinedClass.getName(), KOSize, durationInSeconds));
				System.out.println();
			}
			throw new AssertionError(String.format(MAIN_ASSERTION_ERROR_MESSAGE, KOSize));
		} else if(verbose){
			System.out.println(String.format(EXECUTE_OUT_SUCCESS, examinedClass.getName(), durationInSeconds));
			System.out.println();
		}
	}


	/**
	 * Builds the map of all the sub-attributes.
	 * 
	 * @param subAttributes (List<String>) : contains the String in dot notation of the sub-attributes.
	 * @param mapToFeed (Map<String, String>) : the map of sub-attributes to use.
	 */
	private void feedSubFieldMap(final List<String> subAttributes, final Map<String, List<String>> mapToFeed) {
		if((subAttributes!=null) && (subAttributes.size()>0)){
			for (final String attribute : subAttributes) {
				final String[] splitResult = attribute.split(ESCAPED_DOT);
				if(splitResult.length!=2){
					throw new IllegalArgumentException(String.format(INCORRECT_FORMAT, attribute));
				}
				if(mapToFeed.containsKey(splitResult[0])){
					mapToFeed.get(splitResult[0]).add(splitResult[1]);
				}else{
					final List<String> list = new ArrayList<String>();
					list.add(splitResult[1]);
					mapToFeed.put(splitResult[0], list);
				}
			}
		}
	}
	
	/**
	 * Retrieves the list of Fields available for a certain type. 
	 * If the fields have been already fetched, they will be retrieved from the map typeToFieldsListMap.
	 * Otherwise, they will be retrieved with the method {@link #getAllFields(List, Class) getAllFields()}.
	 * 
	 * @param (Class<?>) the type to retrieve the fields from.
	 * @return (List<Field>) the list of fields of the specified type.
	 */
	private List<Field> retrieveFieldList(final Class<?> type){
		List<Field> fieldList = null;
		final String typeName = type.getName();
		if(typeToFieldsListMap.containsKey(typeName)){
			fieldList = typeToFieldsListMap.get(type.getName());
		}else{
			fieldList = getAllFields(new ArrayList<Field>(), type);
			typeToFieldsListMap.put(typeName, fieldList);
		}
		return fieldList;
	}

	/**
	 * This method retrieves all the Fields of the type and puts them in the fields collection.
	 * All the super class fields will be recursively fetched. Only constants will be ignored (private final static).
	 * If the fields are private, they will be made accessible with {@link java.lang.reflect.AccessibleObject.setAccessible()}
	 * 
	 * @param fields (List<Field>) : the instance of List to fill with the results.
	 * @param type (Class<?>) : the class type to retrieve fields from.
	 * @return (List<Field>) : the 'fields' parameter that was passed but now containing the results, if any.
	 */
	private List<Field> getAllFields(List<Field> fields, final Class<?> type) {
		final Field[] declaredFields = type.getDeclaredFields();
		for (int i = 0; i < declaredFields.length; i++) {
			final Field field = declaredFields[i];
			final int mod = field.getModifiers();
			if(!(Modifier.isPrivate(mod) && Modifier.isStatic(mod) && Modifier.isFinal(mod))){
				field.setAccessible(true);
				fields.add(field);
			}
		}
		if (type.getSuperclass() != null) {
			fields = getAllFields(fields, type.getSuperclass());
		}
		return fields;
	}
}
