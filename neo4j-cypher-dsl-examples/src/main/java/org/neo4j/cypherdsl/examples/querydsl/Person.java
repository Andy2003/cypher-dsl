package org.neo4j.cypherdsl.examples.querydsl;

import com.querydsl.core.annotations.QueryEntity;

@QueryEntity
public class Person {

	private int bornOn;

	private String name;
}
