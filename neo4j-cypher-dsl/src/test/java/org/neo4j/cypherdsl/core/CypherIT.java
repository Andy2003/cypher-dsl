/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.cypherdsl.core;

import static org.assertj.core.api.Assertions.*;

import java.util.function.Function;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.neo4j.cypherdsl.core.renderer.Renderer;

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 */
class CypherIT {

	private static final Renderer cypherRenderer = Renderer.getDefaultRenderer();
	private final Node bikeNode = Cypher.node("Bike").named("b");
	private final Node userNode = Cypher.node("User").named("u");

	@Nested
	class SingleQuerySinglePart {

		@Nested
		class ReadingAndReturn {

			@Test
			void unrelatedNodes() {
				Statement statement = Cypher.match(bikeNode, userNode, Cypher.node("U").named("o"))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (b:`Bike`), (u:`User`), (o:`U`) RETURN b, u");
			}

			@Test
			void asteriskShouldWork() {
				Statement statement = Cypher.match(bikeNode, userNode, Cypher.node("U").named("o"))
					.returning(Cypher.asterisk())
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (b:`Bike`), (u:`User`), (o:`U`) RETURN *");
			}

			@Test
			void aliasedExpressionsInReturn() {
				Node unnamedNode = Cypher.node("ANode");
				Node namedNode = Cypher.node("AnotherNode").named("o");
				Statement statement = Cypher.match(unnamedNode, namedNode)
					.returning(namedNode.as("theOtherNode"))
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (:`ANode`), (o:`AnotherNode`) RETURN o AS theOtherNode");
			}

			@Test
			void simpleRelationship() {
				Statement statement = Cypher
					.match(userNode.relationshipTo(bikeNode, "OWNS"))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (u:`User`)-[:`OWNS`]->(b:`Bike`) RETURN b, u");
			}

			@Test // GH-169
			void multipleRelationshipTypes() {
				Statement statement = Cypher
					.match(userNode.relationshipTo(bikeNode, "OWNS", "RIDES"))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (u:`User`)-[:`OWNS`|`RIDES`]->(b:`Bike`) RETURN b, u");
			}

			@Test // GH-170
			void relationshipWithProperties() {
				Statement statement = Cypher
					.match(userNode.relationshipTo(bikeNode, "OWNS")
						.withProperties(Cypher.mapOf("boughtOn", Cypher.literalOf("2019-04-16"))))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (u:`User`)-[:`OWNS` {boughtOn: '2019-04-16'}]->(b:`Bike`) RETURN b, u");
			}

			@Test // GH-168
			void relationshipWithMinimumLength() {
				Statement statement = Cypher
					.match(userNode.relationshipTo(bikeNode, "OWNS").min(3))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (u:`User`)-[:`OWNS`*3..]->(b:`Bike`) RETURN b, u");
			}

			@Test // GH-168
			void relationshipWithMaximumLength() {
				Statement statement = Cypher
					.match(userNode.relationshipTo(bikeNode, "OWNS").max(5))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (u:`User`)-[:`OWNS`*..5]->(b:`Bike`) RETURN b, u");
			}

			@Test // GH-168
			void relationshipWithLength() {
				Statement statement = Cypher
					.match(userNode.relationshipTo(bikeNode, "OWNS").length(3, 5))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (u:`User`)-[:`OWNS`*3..5]->(b:`Bike`) RETURN b, u");
			}

			@Test // GH-168
			void relationshipWithLengthAndProperties() {
				Statement statement = Cypher
					.match(userNode.relationshipTo(bikeNode, "OWNS").length(3, 5)
						.withProperties(Cypher.mapOf("boughtOn", Cypher.literalOf("2019-04-16"))))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (u:`User`)-[:`OWNS`*3..5 {boughtOn: '2019-04-16'}]->(b:`Bike`) RETURN b, u");
			}

			@Test
			void simpleRelationshipWithReturn() {
				Relationship owns = userNode
					.relationshipTo(bikeNode, "OWNS").named("o");

				Statement statement = Cypher
					.match(owns)
					.returning(bikeNode, userNode, owns)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (u:`User`)-[o:`OWNS`]->(b:`Bike`) RETURN b, u, o");
			}

			@Test
			void chainedRelations() {
				Node tripNode = Cypher.node("Trip").named("t");
				Statement statement = Cypher
					.match(userNode
						.relationshipTo(bikeNode, "OWNS").named("r1")
						.relationshipTo(tripNode, "USED_ON").named("r2")
					)
					.where(userNode.property("name").matches(".*aName"))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`)-[r1:`OWNS`]->(b:`Bike`)-[r2:`USED_ON`]->(t:`Trip`) WHERE u.name =~ '.*aName' RETURN b, u");

				statement = Cypher
					.match(userNode
						.relationshipTo(bikeNode, "OWNS")
						.relationshipTo(tripNode, "USED_ON").named("r2")
					)
					.where(userNode.property("name").matches(".*aName"))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`)-[:`OWNS`]->(b:`Bike`)-[r2:`USED_ON`]->(t:`Trip`) WHERE u.name =~ '.*aName' RETURN b, u");

				statement = Cypher
					.match(userNode
						.relationshipTo(bikeNode, "OWNS")
						.relationshipTo(tripNode, "USED_ON").named("r2")
						.relationshipFrom(userNode, "WAS_ON").named("x")
						.relationshipBetween(Cypher.node("SOMETHING")).named("y")
					)
					.where(userNode.property("name").matches(".*aName"))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`)-[:`OWNS`]->(b:`Bike`)-[r2:`USED_ON`]->(t:`Trip`)<-[x:`WAS_ON`]-(u)-[y]-(:`SOMETHING`) WHERE u.name =~ '.*aName' RETURN b, u");
			}

			@Test // GH-177
			void chainedRelationshipsWithPropertiesAndLength() {
				Node tripNode = Cypher.node("Trip").named("t");
				Statement statement = Cypher
					.match(userNode
						.relationshipTo(bikeNode, "OWNS")
						.relationshipTo(tripNode, "USED_ON").named("r2").min(1)
						.properties(Cypher.mapOf("when", Cypher.literalOf("2019-04-16")))
						.relationshipFrom(userNode, "WAS_ON").named("x").max(2)
						.properties("whatever", Cypher.literalOf("2020-04-16"))
						.relationshipBetween(Cypher.node("SOMETHING")).named("y").length(2, 3)
						.properties(Cypher.mapOf("idk", Cypher.literalOf("2021-04-16")))
					)
					.where(userNode.property("name").matches(".*aName"))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`)-[:`OWNS`]->(b:`Bike`)-[r2:`USED_ON`*1.. {when: '2019-04-16'}]->(t:`Trip`)<-[x:`WAS_ON`*..2 {whatever: '2020-04-16'}]-(u)-[y*2..3 {idk: '2021-04-16'}]-(:`SOMETHING`) WHERE u.name =~ '.*aName' RETURN b, u");
			}

			@Test // GH-182
			void sizeOfRelationship() {

				Statement statement = Cypher
					.match(Cypher.anyNode("a"))
					.where(Cypher.property("a", "name").isEqualTo(Cypher.literalOf("Alice")))
					.returning(Functions.size(Cypher.anyNode("a").relationshipTo(Cypher.anyNode())).as("fof"))
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (a) WHERE a.name = 'Alice' RETURN size((a)-->()) AS fof");
			}

			@Test // GH-182
			void sizeOfRelationshipChain() {

				Statement statement = Cypher
					.match(Cypher.anyNode("a"))
					.where(Cypher.property("a", "name").isEqualTo(Cypher.literalOf("Alice")))
					.returning(
						Functions.size(
							Cypher.anyNode("a").relationshipTo(Cypher.anyNode()).relationshipTo(Cypher.anyNode())).as("fof"))
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (a) WHERE a.name = 'Alice' RETURN size((a)-->()-->()) AS fof");
			}

			@Test
			void sortOrderDefault() {
				Statement statement = Cypher.match(userNode).returning(userNode)
					.orderBy(Cypher.sort(userNode.property("name"))).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u ORDER BY u.name");
			}

			@Test
			void sortOrderAscending() {
				Statement statement = Cypher.match(userNode).returning(userNode)
					.orderBy(Cypher.sort(userNode.property("name")).ascending()).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u ORDER BY u.name ASC");
			}

			@Test
			void sortOrderDescending() {
				Statement statement = Cypher.match(userNode).returning(userNode)
					.orderBy(Cypher.sort(userNode.property("name")).descending()).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u ORDER BY u.name DESC");
			}

			@Test
			void sortOrderConcatenation() {
				Statement statement = Cypher.match(userNode).returning(userNode)
					.orderBy(
						Cypher.sort(userNode.property("name")).descending(),
						Cypher.sort(userNode.property("age")).ascending()
					)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u ORDER BY u.name DESC, u.age ASC");
			}

			@Test
			void sortOrderDefaultExpression() {
				Statement statement = Cypher.match(userNode).returning(userNode)
					.orderBy(userNode.property("name").ascending()).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u ORDER BY u.name ASC");
			}

			@Test
			void sortOrderAscendingExpression() {
				Statement statement = Cypher.match(userNode).returning(userNode)
					.orderBy(userNode.property("name").ascending()).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u ORDER BY u.name ASC");
			}

			@Test
			void sortOrderDescendingExpression() {
				Statement statement = Cypher.match(userNode).returning(userNode)
					.orderBy(userNode.property("name").descending()).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u ORDER BY u.name DESC");
			}

			@Test
			void sortOrderConcatenationExpression() {
				Statement statement = Cypher.match(userNode).returning(userNode)
					.orderBy(userNode.property("name")).descending()
					.and(userNode.property("age")).ascending()
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u ORDER BY u.name DESC, u.age ASC");
			}

			@Test
			void skip() {
				Statement statement = Cypher.match(userNode).returning(userNode).skip(1).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u SKIP 1");
			}

			@Test
			void nullSkip() {
				Statement statement = Cypher.match(userNode).returning(userNode).skip(null).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u");
			}

			@Test
			void limit() {
				Statement statement = Cypher.match(userNode).returning(userNode).limit(1).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u LIMIT 1");
			}

			@Test
			void nullLimit() {
				Statement statement = Cypher.match(userNode).returning(userNode).limit(null).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u");
			}

			@Test
			void skipAndLimit() {
				Statement statement = Cypher.match(userNode).returning(userNode).skip(1).limit(1).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u SKIP 1 LIMIT 1");
			}

			@Test
			void nullSkipAndLimit() {
				Statement statement = Cypher.match(userNode).returning(userNode).skip(null).limit(null).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u");
			}

			@Test
			void distinct() {
				Statement statement = Cypher.match(userNode).returningDistinct(userNode).skip(1).limit(1).build();
				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN DISTINCT u SKIP 1 LIMIT 1");
			}
		}
	}

	@Nested
	class SingleQueryMultiPart {
		@Test
		void simpleWith() {
			Statement statement = Cypher
				.match(userNode.relationshipTo(bikeNode, "OWNS"))
				.where(userNode.property("a").isNull())
				.with(bikeNode, userNode)
				.returning(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`)-[:`OWNS`]->(b:`Bike`) WHERE u.a IS NULL WITH b, u RETURN b");
		}

		@Test
		void shouldRenderLeadingWith() {
			Statement statement = Cypher
				.with(Cypher.parameter("listOfPropertyMaps").as("p"))
				.unwind("p").as("item")
				.returning("item")
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("WITH $listOfPropertyMaps AS p UNWIND p AS item RETURN item");
		}

		@Test
		void simpleWithChained() {

			Node tripNode = Cypher.node("Trip").named("t");
			Statement statement = Cypher
				.match(userNode.relationshipTo(bikeNode, "OWNS"))
				.where(userNode.property("a").isNull())
				.with(bikeNode, userNode)
				.match(tripNode)
				.where(tripNode.property("name").isEqualTo(Cypher.literalOf("Festive500")))
				.with(tripNode)
				.returning(bikeNode, userNode, tripNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`)-[:`OWNS`]->(b:`Bike`) WHERE u.a IS NULL WITH b, u MATCH (t:`Trip`) WHERE t.name = 'Festive500' WITH t RETURN b, u, t");
		}

		@Test
		void deletingSimpleWith() {
			Statement statement = Cypher
				.match(userNode.relationshipTo(bikeNode, "OWNS"))
				.where(userNode.property("a").isNull())
				.delete(userNode)
				.with(bikeNode, userNode)
				.returning(bikeNode, userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`)-[:`OWNS`]->(b:`Bike`) WHERE u.a IS NULL DELETE u WITH b, u RETURN b, u");
		}

		@Test
		void deletingSimpleWithReverse() {
			Statement statement = Cypher
				.match(userNode.relationshipTo(bikeNode, "OWNS"))
				.where(userNode.property("a").isNull())
				.with(bikeNode, userNode)
				.delete(userNode)
				.returning(bikeNode, userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`)-[:`OWNS`]->(b:`Bike`) WHERE u.a IS NULL WITH b, u DELETE u RETURN b, u");
		}

		@Test
		void mixedClausesWithWith() {

			Node tripNode = Cypher.node("Trip").named("t");
			Statement statement = Cypher
				.match(userNode.relationshipTo(bikeNode, "OWNS"))
				.match(tripNode)
				.delete(tripNode)
				.with(bikeNode, tripNode)
				.match(userNode)
				.with(bikeNode, userNode)
				.returning(bikeNode, userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`)-[:`OWNS`]->(b:`Bike`) MATCH (t:`Trip`) DELETE t WITH b, t MATCH (u) WITH b, u RETURN b, u");
		}
	}

	@Nested
	class MultipleMatches {
		@Test
		void simple() {
			Statement statement = Cypher
				.match(bikeNode)
				.match(userNode, Cypher.node("U").named("o"))
				.returning(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (b:`Bike`) MATCH (u:`User`), (o:`U`) RETURN b");
		}

		@Test
		void simpleWhere() {
			Statement statement = Cypher
				.match(bikeNode)
				.match(userNode, Cypher.node("U").named("o"))
				.where(userNode.property("a").isNull())
				.returning(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (b:`Bike`) MATCH (u:`User`), (o:`U`) WHERE u.a IS NULL RETURN b");
		}

		@Test
		void multiWhere() {
			Statement statement = Cypher
				.match(bikeNode)
				.where(bikeNode.property("a").isNotNull())
				.match(userNode, Cypher.node("U").named("o"))
				.where(userNode.property("a").isNull())
				.returning(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (b:`Bike`) WHERE b.a IS NOT NULL MATCH (u:`User`), (o:`U`) WHERE u.a IS NULL RETURN b");
		}

		@Test
		void multiWhereMultiConditions() {
			Statement statement = Cypher
				.match(bikeNode)
				.where(bikeNode.property("a").isNotNull())
				.and(bikeNode.property("b").isNull())
				.match(userNode, Cypher.node("U").named("o"))
				.where(userNode.property("a").isNull().or(userNode.internalId().isEqualTo(Cypher.literalOf(4711))))
				.returning(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (b:`Bike`) WHERE (b.a IS NOT NULL AND b.b IS NULL) MATCH (u:`User`), (o:`U`) WHERE (u.a IS NULL OR id(u) = 4711) RETURN b");
		}

		@Test
		void optional() {
			Statement statement = Cypher
				.optionalMatch(bikeNode)
				.match(userNode, Cypher.node("U").named("o"))
				.where(userNode.property("a").isNull())
				.returning(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("OPTIONAL MATCH (b:`Bike`) MATCH (u:`User`), (o:`U`) WHERE u.a IS NULL RETURN b");
		}

		@Test
		void usingSameWithStepWithoutReassign() {
			StatementBuilder.OrderableOngoingReadingAndWith firstStep = Cypher.match(bikeNode).with(bikeNode);

			firstStep.optionalMatch(userNode);
			firstStep.optionalMatch(Cypher.node("Trip"));

			Statement statement = firstStep.returning(Cypher.asterisk()).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (b:`Bike`) WITH b OPTIONAL MATCH (u:`User`) OPTIONAL MATCH (:`Trip`) RETURN *");
		}

		@Test
		void usingSameWithStepWithoutReassignThenUpdate() {
			StatementBuilder.OrderableOngoingReadingAndWith firstStep = Cypher.match(bikeNode).with(bikeNode);

			firstStep.optionalMatch(userNode);
			firstStep.optionalMatch(Cypher.node("Trip"));
			firstStep.delete("u");

			Statement statement = firstStep.returning(Cypher.asterisk()).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (b:`Bike`) WITH b OPTIONAL MATCH (u:`User`) OPTIONAL MATCH (:`Trip`) DELETE u RETURN *");
		}

		@Test
		void usingSameWithStepWithReassign() {
			ExposesMatch firstStep = Cypher.match(bikeNode).with(bikeNode);

			firstStep = firstStep.optionalMatch(userNode);
			firstStep = firstStep.optionalMatch(Cypher.node("Trip"));

			Statement statement = ((ExposesReturning) firstStep).returning(Cypher.asterisk()).build();
			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (b:`Bike`) WITH b OPTIONAL MATCH (u:`User`) OPTIONAL MATCH (:`Trip`) RETURN *");
		}

		@Test
		void queryPartsShouldBeExtractableInQueries() {

			// THose can be a couple of queries ending in a WITH statement so the
			// pipeline they present in the full query is also present in Java.
			Function<ExposesMatch, ExposesMatch> step1Supplier =
				previous -> previous.match(Cypher.node("S1").named("n")).where(
					Cypher.property("n", "a").isEqualTo(Cypher.literalOf("A")))
					.with("n");
			Function<ExposesMatch, ExposesReturning> step2Supplier =
				previous -> previous.match(Cypher.anyNode("n").relationshipTo(Cypher.node("S2").named("m"), "SOMEHOW_RELATED"))
					.with("n", "m");

			Statement statement = step1Supplier.andThen(step2Supplier).apply(Statement.builder()).returning("n", "m")
				.build();
			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n:`S1`) WHERE n.a = 'A' WITH n MATCH (n)-[:`SOMEHOW_RELATED`]->(m:`S2`) WITH n, m RETURN n, m");
		}

		@Test
		void optionalNext() {
			Statement statement = Cypher
				.match(bikeNode)
				.optionalMatch(userNode, Cypher.node("U").named("o"))
				.where(userNode.property("a").isNull())
				.returning(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (b:`Bike`) OPTIONAL MATCH (u:`User`), (o:`U`) WHERE u.a IS NULL RETURN b");
		}

		@Test
		void optionalMatchThenDelete() {
			Statement statement = Cypher
				.match(bikeNode)
				.optionalMatch(userNode, Cypher.node("U").named("o"))
				.delete(userNode, bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (b:`Bike`) OPTIONAL MATCH (u:`User`), (o:`U`) DELETE u, b");
		}
	}

	@Nested
	class FunctionRendering {
		@Test
		void inWhereClause() {
			Statement statement = Cypher.match(userNode).where(userNode.internalId().isEqualTo(Cypher.literalOf(1L)))
				.returning(userNode).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE id(u) = 1 RETURN u");
		}

		@Test
		void inReturnClause() {
			Statement statement = Cypher.match(userNode).returning(Functions.count(userNode)).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) RETURN count(u)");
		}

		@Test
		void aliasedInReturnClause() {
			Statement statement = Cypher.match(userNode).returning(Functions.count(userNode).as("cnt")).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) RETURN count(u) AS cnt");
		}

		@Test
		void shouldSupportMoreThanOneArgument() {
			Statement statement = Cypher.match(userNode)
				.returning(Functions.coalesce(userNode.property("a"), userNode.property("b"), Cypher.literalOf("¯\\_(ツ)_/¯")))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) RETURN coalesce(u.a, u.b, '¯\\\\_(ツ)_/¯')");
		}

		@Test
		void literalsShouldDealWithNull() {
			Statement statement = Cypher.match(userNode)
				.returning(Functions.coalesce(Cypher.literalOf(null), userNode.property("field")).as("p"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) RETURN coalesce(NULL, u.field) AS p");
		}

		@Test // GH-257
		void functionsBasedOnRelationships() {
			Relationship relationship = Cypher.node("Person").named("bacon").withProperties("name", Cypher.literalOf("Kevin Bacon"))
				.relationshipBetween(Cypher.node("Person").named("meg").withProperties("name", Cypher.literalOf("Meg Ryan"))).unbounded();
			Statement statement = Cypher.match(Cypher.shortestPath("p").definedBy(relationship)).returning("p").build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH p = shortestPath((bacon:`Person` {name: 'Kevin Bacon'})-[*]-(meg:`Person` {name: 'Meg Ryan'})) RETURN p");
		}
	}

	@Nested
	class ComparisonRendering {

		@Test
		void equalsWithStringLiteral() {
			Statement statement = Cypher.match(userNode)
				.where(userNode.property("name").isEqualTo(Cypher.literalOf("Test")))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE u.name = 'Test' RETURN u");
		}

		@Test
		void equalsWithNumberLiteral() {
			Statement statement = Cypher.match(userNode)
				.where(userNode.property("age").isEqualTo(Cypher.literalOf(21)))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE u.age = 21 RETURN u");
		}
	}

	@Nested
	class Conditions {
		@Test
		void conditionsChainingAnd() {
			Statement statement = Cypher.match(userNode)
				.where(
					userNode.property("name").isEqualTo(Cypher.literalOf("Test"))
						.and(userNode.property("age").isEqualTo(Cypher.literalOf(21))))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (u.name = 'Test' AND u.age = 21) RETURN u");
		}

		@Test
		void conditionsChainingOr() {
			Statement statement = Cypher.match(userNode)
				.where(
					userNode.property("name").isEqualTo(Cypher.literalOf("Test"))
						.or(userNode.property("age").isEqualTo(Cypher.literalOf(21))))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (u.name = 'Test' OR u.age = 21) RETURN u");
		}

		@Test
		void nestedConditions() {
			Statement statement;

			statement = Cypher.match(userNode)
				.where(org.neo4j.cypherdsl.core.Conditions.isTrue().or(org.neo4j.cypherdsl.core.Conditions.isFalse()).and(
					org.neo4j.cypherdsl.core.Conditions.isTrue()))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`) WHERE ((true OR false) AND true) RETURN u");

			statement = Cypher.match(userNode)
				.where(org.neo4j.cypherdsl.core.Conditions.isTrue().or(org.neo4j.cypherdsl.core.Conditions.isFalse()).and(
					org.neo4j.cypherdsl.core.Conditions.isTrue()))
				.or(org.neo4j.cypherdsl.core.Conditions.isFalse())
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (((true OR false) AND true) OR false) RETURN u");

			statement = Cypher.match(userNode)
				.where(org.neo4j.cypherdsl.core.Conditions.isTrue().or(org.neo4j.cypherdsl.core.Conditions.isFalse()).and(
					org.neo4j.cypherdsl.core.Conditions.isTrue()))
				.or(org.neo4j.cypherdsl.core.Conditions.isFalse())
				.and(org.neo4j.cypherdsl.core.Conditions.isFalse())
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`) WHERE ((((true OR false) AND true) OR false) AND false) RETURN u");

			statement = Cypher.match(userNode)
				.where(org.neo4j.cypherdsl.core.Conditions.isTrue().or(org.neo4j.cypherdsl.core.Conditions.isFalse()).and(
					org.neo4j.cypherdsl.core.Conditions.isTrue()))
				.or(org.neo4j.cypherdsl.core.Conditions.isFalse().and(org.neo4j.cypherdsl.core.Conditions.isTrue()))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`) WHERE ((true OR false) AND true OR (false AND true)) RETURN u");

			statement = Cypher.match(userNode)
				.where(org.neo4j.cypherdsl.core.Conditions.isTrue().or(org.neo4j.cypherdsl.core.Conditions.isFalse()).and(
					org.neo4j.cypherdsl.core.Conditions.isTrue()))
				.or(org.neo4j.cypherdsl.core.Conditions.isFalse().and(org.neo4j.cypherdsl.core.Conditions.isTrue()))
				.and(org.neo4j.cypherdsl.core.Conditions.isTrue())
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`) WHERE ((true OR false) AND true OR (false AND true) AND true) RETURN u");

			statement = Cypher.match(userNode)
				.where(org.neo4j.cypherdsl.core.Conditions.isTrue().or(org.neo4j.cypherdsl.core.Conditions.isFalse()).and(
					org.neo4j.cypherdsl.core.Conditions.isTrue()))
				.or(org.neo4j.cypherdsl.core.Conditions.isFalse().or(org.neo4j.cypherdsl.core.Conditions.isTrue()))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`) WHERE ((true OR false) AND true OR (false OR true)) RETURN u");

			statement = Cypher.match(userNode)
				.where(org.neo4j.cypherdsl.core.Conditions.isTrue().or(org.neo4j.cypherdsl.core.Conditions.isFalse()).and(
					org.neo4j.cypherdsl.core.Conditions.isTrue()).or(
					org.neo4j.cypherdsl.core.Conditions.isFalse().or(org.neo4j.cypherdsl.core.Conditions.isTrue())))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`) WHERE ((true OR false) AND true OR (false OR true)) RETURN u");
		}

		@Test
		void conditionsChainingXor() {
			Statement statement = Cypher.match(userNode)
				.where(
					userNode.property("name").isEqualTo(Cypher.literalOf("Test"))
						.xor(userNode.property("age").isEqualTo(Cypher.literalOf(21))))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (u.name = 'Test' XOR u.age = 21) RETURN u");
		}

		@Test
		void chainingOnWhere() {
			Statement statement;

			Literal test = Cypher.literalOf("Test");
			Literal foobar = Cypher.literalOf("foobar");

			statement = Cypher.match(userNode)
				.where(userNode.property("name").isEqualTo(test))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`) WHERE u.name = 'Test' RETURN u");

			statement = Cypher.match(userNode)
				.where(userNode.property("name").isEqualTo(test))
				.and(userNode.property("name").isEqualTo(test))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`) WHERE (u.name = 'Test' AND u.name = 'Test') RETURN u");

			statement = Cypher.match(userNode)
				.where(userNode.property("name").isEqualTo(test))
				.and(userNode.property("name").isEqualTo(test))
				.and(userNode.property("name").isEqualTo(test))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`) WHERE (u.name = 'Test' AND u.name = 'Test' AND u.name = 'Test') RETURN u");

			statement = Cypher.match(userNode)
				.where(userNode.property("name").isEqualTo(test))
				.or(userNode.property("name").isEqualTo(test))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`) WHERE (u.name = 'Test' OR u.name = 'Test') RETURN u");

			statement = Cypher.match(userNode)
				.where(userNode.property("name").isEqualTo(test))
				.or(userNode.property("name").isEqualTo(test))
				.or(userNode.property("name").isEqualTo(test))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`) WHERE (u.name = 'Test' OR u.name = 'Test' OR u.name = 'Test') RETURN u");

			statement = Cypher.match(userNode)
				.where(userNode.property("name").isEqualTo(test))
				.and(userNode.property("name").isEqualTo(test))
				.or(userNode.property("name").isEqualTo(foobar))
				.and(userNode.property("name").isEqualTo(test))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (((u.name = 'Test' AND u.name = 'Test') OR u.name = 'foobar') AND u.name = 'Test') RETURN u");

			statement = Cypher.match(userNode)
				.where(userNode.property("name").isEqualTo(test))
				.or(userNode.property("name").isEqualTo(foobar))
				.and(userNode.property("name").isEqualTo(test))
				.and(userNode.property("name").isEqualTo(test))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE ((u.name = 'Test' OR u.name = 'foobar') AND u.name = 'Test' AND u.name = 'Test') RETURN u");

			statement = Cypher.match(userNode)
				.where(userNode.property("name").isEqualTo(test))
				.or(userNode.property("name").isEqualTo(foobar))
				.and(userNode.property("name").isEqualTo(test))
				.or(userNode.property("name").isEqualTo(foobar))
				.and(userNode.property("name").isEqualTo(test))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE ((((u.name = 'Test' OR u.name = 'foobar') AND u.name = 'Test') OR u.name = 'foobar') AND u.name = 'Test') RETURN u");

			statement = Cypher.match(userNode)
				.where(userNode.property("name").isNotNull())
				.and(userNode.property("name").isEqualTo(test))
				.or(userNode.property("age").isEqualTo(Cypher.literalOf(21)))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE ((u.name IS NOT NULL AND u.name = 'Test') OR u.age = 21) RETURN u");
		}

		@Test
		void chainingOnConditions() {
			Statement statement;

			Literal test = Cypher.literalOf("Test");
			Literal foobar = Cypher.literalOf("foobar");
			Literal bazbar = Cypher.literalOf("bazbar");

			statement = Cypher.match(userNode)
				.where(
					userNode.property("name").isEqualTo(test)
						.or(userNode.property("name").isEqualTo(foobar))
						.or(userNode.property("name").isEqualTo(foobar))
				)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (u.name = 'Test' OR u.name = 'foobar' OR u.name = 'foobar') RETURN u");

			statement = Cypher.match(userNode)
				.where(
					userNode.property("name").isEqualTo(test)
						.and(userNode.property("name").isEqualTo(bazbar))
						.or(userNode.property("name").isEqualTo(foobar))
						.or(userNode.property("name").isEqualTo(foobar))
				)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE ((u.name = 'Test' AND u.name = 'bazbar') OR u.name = 'foobar' OR u.name = 'foobar') RETURN u");

			statement = Cypher.match(userNode)
				.where(
					userNode.property("name").isEqualTo(test))
				.and(
					userNode.property("name").isEqualTo(bazbar)
						.and(userNode.property("name").isEqualTo(foobar))
				)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (u.name = 'Test' AND u.name = 'bazbar' AND u.name = 'foobar') RETURN u");

			statement = Cypher.match(userNode)
				.where(
					userNode.property("name").isEqualTo(test)
						.and(userNode.property("name").isEqualTo(bazbar))
						.or(userNode.property("name").isEqualTo(foobar))
						.or(userNode.property("name").isEqualTo(foobar))
				)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE ((u.name = 'Test' AND u.name = 'bazbar') OR u.name = 'foobar' OR u.name = 'foobar') RETURN u");

			statement = Cypher.match(userNode)
				.where(
					userNode.property("name").isEqualTo(test)
						.and(userNode.property("name").isEqualTo(bazbar))
						.or(userNode.property("name").isEqualTo(foobar))
						.or(userNode.property("name").isEqualTo(foobar))
						.and(userNode.property("name").isEqualTo(bazbar))
				)
				.returning(userNode)
				.build();
			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (((u.name = 'Test' AND u.name = 'bazbar') OR u.name = 'foobar' OR u.name = 'foobar') AND u.name = 'bazbar') RETURN u");
		}

		@Test
		void chainingCombined() {
			Statement statement;

			Literal test = Cypher.literalOf("Test");
			Literal foobar = Cypher.literalOf("foobar");
			Literal bazbar = Cypher.literalOf("bazbar");

			statement = Cypher.match(userNode)
				.where(
					userNode.property("name").isEqualTo(test)
						.and(userNode.property("name").isEqualTo(bazbar))
						.or(userNode.property("name").isEqualTo(foobar))
						.or(userNode.property("name").isEqualTo(foobar))
				)
				.and(
					userNode.property("name").isEqualTo(bazbar)
						.and(userNode.property("name").isEqualTo(foobar))
						.or(userNode.property("name").isEqualTo(test))
						.not()
				)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (((u.name = 'Test' AND u.name = 'bazbar') OR u.name = 'foobar' OR u.name = 'foobar') AND NOT (((u.name = 'bazbar' AND u.name = 'foobar') OR u.name = 'Test'))) RETURN u");

		}

		@Test
		void negatedConditions() {
			Statement statement = Cypher.match(userNode)
				.where(userNode.property("name").isNotNull().not())
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE NOT (u.name IS NOT NULL) RETURN u");
		}

		@Test
		void noConditionShouldNotBeRendered() {
			Statement statement;
			statement = Cypher.match(userNode)
				.where(org.neo4j.cypherdsl.core.Conditions.noCondition())
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) RETURN u");

			statement = Cypher.match(userNode)
				.where(userNode.property("name").isEqualTo(Cypher.literalOf("test")))
				.and(org.neo4j.cypherdsl.core.Conditions.noCondition()).or(
					org.neo4j.cypherdsl.core.Conditions.noCondition())
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE u.name = 'test' RETURN u");
		}

		@Nested // GH-206, 3.6.5. Using path patterns in WHERE
		class PathPatternConditions {

			@Test
			void doc3651And() {
				Node timothy = Cypher.node("Person").named("timothy").withProperties("name", Cypher.literalOf("Timothy"));
				Node other = Cypher.node("Person").named("other");

				Statement statement;

				String expected = "MATCH (timothy:`Person` {name: 'Timothy'}), (other:`Person`) WHERE (other.name IN ['Andy', 'Peter'] AND (timothy)<--(other)) RETURN other.name, other.age";
				statement = Cypher.match(timothy, other)
					.where(other.property("name").in(Cypher.listOf(Cypher.literalOf("Andy"), Cypher.literalOf("Peter"))))
					.and(timothy.relationshipFrom(other))
					.returning(other.property("name"), other.property("age"))
					.build();
				assertThat(cypherRenderer.render(statement)).isEqualTo(expected);

				statement = Cypher.match(timothy, other)
					.where(other.property("name").in(Cypher.listOf(Cypher.literalOf("Andy"), Cypher.literalOf("Peter")))
						.and(timothy.relationshipFrom(other)))
					.returning(other.property("name"), other.property("age"))
					.build();

				assertThat(cypherRenderer.render(statement)).isEqualTo(expected);
			}

			@Test
			void doc3651Or() {
				Node timothy = Cypher.node("Person").named("timothy").withProperties("name", Cypher.literalOf("Timothy"));
				Node other = Cypher.node("Person").named("other");

				Statement statement;

				String expected = "MATCH (timothy:`Person` {name: 'Timothy'}), (other:`Person`) WHERE (other.name IN ['Andy', 'Peter'] OR (timothy)<--(other)) RETURN other.name, other.age";
				statement = Cypher.match(timothy, other)
					.where(other.property("name").in(Cypher.listOf(Cypher.literalOf("Andy"), Cypher.literalOf("Peter"))))
					.or(timothy.relationshipFrom(other))
					.returning(other.property("name"), other.property("age"))
					.build();
				assertThat(cypherRenderer.render(statement)).isEqualTo(expected);

				statement = Cypher.match(timothy, other)
					.where(other.property("name").in(Cypher.listOf(Cypher.literalOf("Andy"), Cypher.literalOf("Peter")))
						.or(timothy.relationshipFrom(other)))
					.returning(other.property("name"), other.property("age"))
					.build();

				assertThat(cypherRenderer.render(statement)).isEqualTo(expected);
			}

			@Test
			void doc3651XOr() {
				Node timothy = Cypher.node("Person").named("timothy").withProperties("name", Cypher.literalOf("Timothy"));
				Node other = Cypher.node("Person").named("other");

				Statement statement;

				String expected = "MATCH (timothy:`Person` {name: 'Timothy'}), (other:`Person`) WHERE (other.name IN ['Andy', 'Peter'] XOR (timothy)<--(other)) RETURN other.name, other.age";
				statement = Cypher.match(timothy, other)
					.where(other.property("name").in(Cypher.listOf(Cypher.literalOf("Andy"), Cypher.literalOf("Peter")))
						.xor(timothy.relationshipFrom(other)))
					.returning(other.property("name"), other.property("age"))
					.build();

				assertThat(cypherRenderer.render(statement)).isEqualTo(expected);
			}

			@Test
			void doc3652() {

				Node person = Cypher.node("Person").named("person");
				Node peter = Cypher.node("Person").named("peter").withProperties("name", Cypher.literalOf("Peter"));

				Statement statement;

				statement = Cypher.match(person, peter)
					.where(org.neo4j.cypherdsl.core.Conditions.not(person.relationshipTo(peter)))
					.returning(person.property("name"), person.property("age"))
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (person:`Person`), (peter:`Person` {name: 'Peter'}) WHERE NOT (person)-->(peter) RETURN person.name, person.age");
			}

			@Test
			void doc3653() {

				Node person = Cypher.node("Person").named("n");
				Statement statement;

				statement = Cypher.match(person)
					.where(person.relationshipBetween(Cypher.anyNode().withProperties("name", Cypher.literalOf("Timothy")), "KNOWS"))
					.returning(person.property("name"), person.property("age"))
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (n:`Person`) WHERE (n)-[:`KNOWS`]-( {name: 'Timothy'}) RETURN n.name, n.age");
			}

			@Test
			void doc3654() {

				Node person = Cypher.node("Person").named("n");
				Statement statement;

				Relationship pathPattern = person.relationshipTo(Cypher.anyNode()).named("r");
				statement = Cypher.match(pathPattern)
					.where(person.property("name").isEqualTo(Cypher.literalOf("Andy")))
					.and(Functions.type(pathPattern).matches("K.*"))
					.returning(Functions.type(pathPattern), pathPattern.property("since"))
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (n:`Person`)-[r]->() WHERE (n.name = 'Andy' AND type(r) =~ 'K.*') RETURN type(r), r.since");
			}

			@Test
			void afterWith() {

				Node timothy = Cypher.node("Person").named("timothy").withProperties("name", Cypher.literalOf("Timothy"));
				Node other = Cypher.node("Person").named("other");

				Statement statement;

				String expected = "MATCH (timothy:`Person` {name: 'Timothy'}), (other:`Person`) WITH timothy, other WHERE (other.name IN ['Andy', 'Peter'] AND (timothy)<--(other)) RETURN other.name, other.age";
				statement = Cypher.match(timothy, other)
					.with(timothy, other)
					.where(other.property("name").in(Cypher.listOf(Cypher.literalOf("Andy"), Cypher.literalOf("Peter"))))
					.and(timothy.relationshipFrom(other))
					.returning(other.property("name"), other.property("age"))
					.build();
				assertThat(cypherRenderer.render(statement)).isEqualTo(expected);

				statement = Cypher.match(timothy, other)
					.with(timothy, other)
					.where(other.property("name").in(Cypher.listOf(Cypher.literalOf("Andy"), Cypher.literalOf("Peter")))
						.and(timothy.relationshipFrom(other)))
					.returning(other.property("name"), other.property("age"))
					.build();

				assertThat(cypherRenderer.render(statement)).isEqualTo(expected);
			}
		}

		@Test // GH-244
		void inPatternComprehensions() {

			Statement statement;
			Node a = Cypher.node("Person").withProperties("name", Cypher.literalOf("Keanu Reeves")).named("a");
			Node b = Cypher.anyNode("b");

			statement = Cypher.match(a)
				.returning(
					Cypher.listBasedOn(a.relationshipBetween(b))
						.where(b.hasLabels("Movie").and(b.property("released").isNotNull()))
						.returning(b.property("released"))
						.as("years"))
				.build();
			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (a:`Person` {name: 'Keanu Reeves'}) RETURN [(a)--(b) WHERE (b:`Movie` AND b.released IS NOT NULL) | b.released] AS years");

			statement = Cypher.match(a)
				.returning(
					Cypher.listBasedOn(a.relationshipBetween(b))
						.where(
							b.hasLabels("Movie")
								.and(b.property("released").isNotNull())
								.or(b.property("title").isEqualTo(Cypher.literalOf("The Matrix")))
								.or(b.property("title").isEqualTo(Cypher.literalOf("The Matrix 2"))))
						.returning(b.property("released"))
						.as("years"))
				.build();
			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (a:`Person` {name: 'Keanu Reeves'}) RETURN [(a)--(b) WHERE ((b:`Movie` AND b.released IS NOT NULL) OR b.title = 'The Matrix' OR b.title = 'The Matrix 2') | b.released] AS years");

			statement = Cypher.match(a)
				.returning(
					Cypher.listBasedOn(a.relationshipBetween(b))
						.where(b.hasLabels("Movie"))
						.and(b.property("released").isNotNull())
						.or(b.property("title").isEqualTo(Cypher.literalOf("The Matrix")))
						.or(b.property("title").isEqualTo(Cypher.literalOf("The Matrix 2")))
						.returning(b.property("released"))
						.as("years"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (a:`Person` {name: 'Keanu Reeves'}) RETURN [(a)--(b) WHERE ((b:`Movie` AND b.released IS NOT NULL) OR b.title = 'The Matrix' OR b.title = 'The Matrix 2') | b.released] AS years");

			statement = Cypher.match(a)
				.returning(
					Cypher.listBasedOn(a.relationshipBetween(b))
						.where(b.hasLabels("Movie"))
						.returning(b.property("released"))
						.as("years"))
				.build();
			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (a:`Person` {name: 'Keanu Reeves'}) RETURN [(a)--(b) WHERE b:`Movie` | b.released] AS years");
		}
	}

	@Nested
	class RemoveClause {
		@Test
		void shouldRenderRemoveOnNodes() {
			Statement statement;

			statement = Cypher.match(userNode)
				.remove(userNode, "A", "B")
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) REMOVE u:`A`:`B` RETURN u");

			statement = Cypher.match(userNode)
				.with(userNode)
				.set(userNode, "A", "B")
				.remove(userNode, "C", "D")
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WITH u SET u:`A`:`B` REMOVE u:`C`:`D` RETURN u");
		}

		@Test
		void shouldRenderRemoveOfProperties() {
			Statement statement;

			statement = Cypher.match(userNode)
				.remove(userNode.property("a"), userNode.property("b"))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) REMOVE u.a, u.b RETURN u");

			statement = Cypher.match(userNode)
				.with(userNode)
				.remove(userNode.property("a"))
				.remove(userNode.property("b"))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WITH u REMOVE u.a REMOVE u.b RETURN u");
		}
	}

	@Nested
	class SetClause {

		@Test
		void shouldRenderSetAfterCreate() {
			Statement statement;
			statement = Cypher.create(userNode)
				.set(userNode.property("p").to(Cypher.literalOf("Hallo, Welt")))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`) SET u.p = 'Hallo, Welt'");
		}

		@Test
		void shouldRenderSetAfterMerge() {
			Statement statement;
			statement = Cypher.merge(userNode)
				.set(userNode.property("p").to(Cypher.literalOf("Hallo, Welt")))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`) SET u.p = 'Hallo, Welt'");
		}

		@Test
		void shouldRenderSetAfterCreateAndWith() {
			Statement statement;
			statement = Cypher.create(userNode)
				.with(userNode)
				.set(userNode.property("p").to(Cypher.literalOf("Hallo, Welt")))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`) WITH u SET u.p = 'Hallo, Welt'");
		}

		@Test
		void shouldRenderSetAfterMergeAndWith() {
			Statement statement;
			statement = Cypher.merge(userNode)
				.with(userNode)
				.set(userNode.property("p").to(Cypher.literalOf("Hallo, Welt")))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`) WITH u SET u.p = 'Hallo, Welt'");
		}

		@Test
		void shouldRenderSet() {

			Statement statement;

			statement = Cypher.match(userNode)
				.set(userNode.property("p").to(Cypher.literalOf("Hallo, Welt")))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) SET u.p = 'Hallo, Welt'");

			statement = Cypher.match(userNode)
				.set(userNode.property("p").to(Cypher.literalOf("Hallo, Welt")))
				.set(userNode.property("a").to(Cypher.literalOf("Selber hallo.")))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) SET u.p = 'Hallo, Welt' SET u.a = 'Selber hallo.'");

			statement = Cypher.match(userNode)
				.set(
					userNode.property("p").to(Cypher.literalOf("Hallo")),
					userNode.property("g").to(Cypher.literalOf("Welt"))
				)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) SET u.p = 'Hallo', u.g = 'Welt'");

		}

		@Test
		void shouldRenderSetOnNodes() {
			Statement statement;

			statement = Cypher.match(userNode)
				.set(userNode, "A", "B")
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) SET u:`A`:`B` RETURN u");

			statement = Cypher.match(userNode)
				.with(userNode)
				.set(userNode, "A", "B")
				.set(userNode, "C", "D")
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WITH u SET u:`A`:`B` SET u:`C`:`D` RETURN u");
		}

		@Test
		void shouldRenderSetFromAListOfExpression() {
			Statement statement;

			statement = Cypher.match(userNode)
				.set(userNode.property("p"), Cypher.literalOf("Hallo, Welt"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) SET u.p = 'Hallo, Welt'");

			statement = Cypher.match(userNode)
				.set(userNode.property("p"), Cypher.literalOf("Hallo"),
					userNode.property("g"), Cypher.literalOf("Welt"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) SET u.p = 'Hallo', u.g = 'Welt'");

			statement = Cypher.match(userNode)
				.set(userNode.property("p"), Cypher.literalOf("Hallo, Welt"))
				.set(userNode.property("p"), Cypher.literalOf("Hallo"),
					userNode.property("g"), Cypher.literalOf("Welt"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) SET u.p = 'Hallo, Welt' SET u.p = 'Hallo', u.g = 'Welt'");

			assertThatIllegalArgumentException().isThrownBy(() -> {
				Cypher.match(userNode).set(userNode.property("g"));
			}).withMessage("The list of expression to set must be even.");
		}

		@Test
		void shouldRenderMixedSet() {
			Statement statement;

			statement = Cypher.match(userNode)
				.set(userNode.property("p1"), Cypher.literalOf("Two expressions"))
				.set(userNode.property("p2").to(Cypher.literalOf("A set expression")))
				.set(
					userNode.property("p3").to(Cypher.literalOf("One of two set expression")),
					userNode.property("p4").to(Cypher.literalOf("Two of two set expression"))
				)
				.set(
					userNode.property("p5"), Cypher.literalOf("Pair one of 2 expressions"),
					userNode.property("p6"), Cypher.literalOf("Pair two of 4 expressions")
				)
				.returning(Cypher.asterisk())
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) SET u.p1 = 'Two expressions' SET u.p2 = 'A set expression' SET u.p3 = 'One of two set expression', u.p4 = 'Two of two set expression' SET u.p5 = 'Pair one of 2 expressions', u.p6 = 'Pair two of 4 expressions' RETURN *");
		}
	}

	@Nested
	class MergeClause {

		@Test
		void shouldRenderMergeWithoutReturn() {
			Statement statement;
			statement = Cypher.merge(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`)");

			statement = Cypher.merge(userNode.relationshipTo(bikeNode, "OWNS").named("o"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`)-[o:`OWNS`]->(b:`Bike`)");
		}

		@Test
		void shouldRenderMultipleMergesWithoutReturn() {
			Statement statement;
			statement = Cypher.merge(userNode)
				.merge(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`) MERGE (b:`Bike`)");

			statement = Cypher
				.merge(userNode.relationshipTo(bikeNode, "OWNS").named("o"))
				.merge(Cypher.node("Other"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`)-[o:`OWNS`]->(b:`Bike`) MERGE (:`Other`)");
		}

		@Test
		void shouldRenderMergeReturn() {
			Statement statement;
			statement = Cypher.merge(userNode)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`) RETURN u");

			Relationship r = userNode.relationshipTo(bikeNode, "OWNS").named("o");
			statement = Cypher.merge(r)
				.returning(userNode, r)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`)-[o:`OWNS`]->(b:`Bike`) RETURN u, o");

			statement = Cypher.merge(userNode)
				.returning(userNode)
				.orderBy(userNode.property("name"))
				.skip(23)
				.limit(42)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`) RETURN u ORDER BY u.name SKIP 23 LIMIT 42");
		}

		@Test
		void shouldRenderMultipleMergesReturn() {
			Statement statement;
			statement = Cypher.merge(userNode)
				.merge(bikeNode)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`) MERGE (b:`Bike`) RETURN u");

			Relationship r = userNode.relationshipTo(bikeNode, "OWNS").named("o");
			statement = Cypher
				.merge(userNode.relationshipTo(bikeNode, "OWNS").named("o"))
				.merge(Cypher.node("Other"))
				.returning(userNode, r)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`)-[o:`OWNS`]->(b:`Bike`) MERGE (:`Other`) RETURN u, o");
		}

		@Test
		void shouldRenderMergeWithWith() {
			Statement statement;
			statement = Cypher.merge(userNode)
				.with(userNode)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`) WITH u RETURN u");

			Relationship r = userNode.relationshipTo(bikeNode, "OWNS").named("o");
			statement = Cypher.merge(userNode)
				.with(userNode)
				.set(userNode.property("x").to(Cypher.literalOf("y")))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`) WITH u SET u.x = 'y'");
		}

		@Test
		void matchShouldExposeMerge() {
			Statement statement;
			statement = Cypher.match(userNode)
				.merge(userNode.relationshipTo(bikeNode, "OWNS").named("o"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) MERGE (u)-[o:`OWNS`]->(b:`Bike`)");
		}

		@Test
		void withShouldExposeMerge() {
			Statement statement;
			statement = Cypher.match(userNode)
				.withDistinct(userNode)
				.merge(userNode.relationshipTo(bikeNode, "OWNS").named("o"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WITH DISTINCT u MERGE (u)-[o:`OWNS`]->(b:`Bike`)");
		}

		@Test
		void mixedCreateAndMerge() {
			Statement statement;

			Node tripNode = Cypher.node("Trip").named("t");

			statement = Cypher.create(userNode)
				.merge(userNode.relationshipTo(bikeNode, "OWNS").named("o"))
				.withDistinct(bikeNode)
				.merge(tripNode.relationshipFrom(bikeNode, "USED_ON"))
				.returning(Cypher.asterisk())
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`) MERGE (u)-[o:`OWNS`]->(b:`Bike`) WITH DISTINCT b MERGE (t:`Trip`)<-[:`USED_ON`]-(b) RETURN *");
		}
	}

	@Nested
	class CreateClause {

		@Test
		void shouldRenderCreateWithoutReturn() {
			Statement statement;
			statement = Cypher.create(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`)");

			statement = Cypher.create(userNode.relationshipTo(bikeNode, "OWNS").named("o"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`)-[o:`OWNS`]->(b:`Bike`)");
		}

		@Test
		void shouldRenderMultipleCreatesWithoutReturn() {
			Statement statement;
			statement = Cypher.create(userNode)
				.create(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`) CREATE (b:`Bike`)");

			statement = Cypher
				.create(userNode.relationshipTo(bikeNode, "OWNS").named("o"))
				.create(Cypher.node("Other"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`)-[o:`OWNS`]->(b:`Bike`) CREATE (:`Other`)");
		}

		@Test
		void shouldRenderCreateReturn() {
			Statement statement;
			statement = Cypher.create(userNode)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`) RETURN u");

			Relationship r = userNode.relationshipTo(bikeNode, "OWNS").named("o");
			statement = Cypher.create(r)
				.returning(userNode, r)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`)-[o:`OWNS`]->(b:`Bike`) RETURN u, o");

			statement = Cypher.create(userNode)
				.returning(userNode)
				.orderBy(userNode.property("name"))
				.skip(23)
				.limit(42)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`) RETURN u ORDER BY u.name SKIP 23 LIMIT 42");
		}

		@Test
		void shouldRenderMultipleCreatesReturn() {
			Statement statement;
			statement = Cypher.create(userNode)
				.create(bikeNode)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`) CREATE (b:`Bike`) RETURN u");

			Relationship r = userNode.relationshipTo(bikeNode, "OWNS").named("o");
			statement = Cypher
				.create(userNode.relationshipTo(bikeNode, "OWNS").named("o"))
				.create(Cypher.node("Other"))
				.returning(userNode, r)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`)-[o:`OWNS`]->(b:`Bike`) CREATE (:`Other`) RETURN u, o");
		}

		@Test
		void shouldRenderCreateWithWith() {
			Statement statement;
			statement = Cypher.create(userNode)
				.with(userNode)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`) WITH u RETURN u");

			Relationship r = userNode.relationshipTo(bikeNode, "OWNS").named("o");
			statement = Cypher.create(userNode)
				.with(userNode)
				.set(userNode.property("x").to(Cypher.literalOf("y")))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`) WITH u SET u.x = 'y'");
		}

		@Test
		void matchShouldExposeCreate() {
			Statement statement;
			statement = Cypher.match(userNode)
				.create(userNode.relationshipTo(bikeNode, "OWNS").named("o"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) CREATE (u)-[o:`OWNS`]->(b:`Bike`)");
		}

		@Test
		void withShouldExposeCreate() {
			Statement statement;
			statement = Cypher.match(userNode)
				.withDistinct(userNode)
				.create(userNode.relationshipTo(bikeNode, "OWNS").named("o"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WITH DISTINCT u CREATE (u)-[o:`OWNS`]->(b:`Bike`)");
		}
	}

	@Nested
	class DeleteClause {

		@Test
		void shouldRenderDeleteWithoutReturn() {

			Statement statement;
			statement = Cypher.match(userNode)
				.detachDelete(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) DETACH DELETE u");

			statement = Cypher.match(userNode)
				.with(userNode)
				.detachDelete(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WITH u DETACH DELETE u");

			statement = Cypher.match(userNode)
				.where(userNode.property("a").isNotNull()).and(userNode.property("b").isNull())
				.delete(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (u.a IS NOT NULL AND u.b IS NULL) DELETE u");

			statement = Cypher.match(userNode, bikeNode)
				.delete(userNode, bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`), (b:`Bike`) DELETE u, b");
		}

		@Test
		void shouldRenderDeleteWithReturn() {

			Statement statement;
			statement = Cypher.match(userNode)
				.detachDelete(userNode)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) DETACH DELETE u RETURN u");

			statement = Cypher.match(userNode)
				.where(userNode.property("a").isNotNull()).and(userNode.property("b").isNull())
				.detachDelete(userNode)
				.returning(userNode).orderBy(userNode.property("a").ascending()).skip(2).limit(1)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (u.a IS NOT NULL AND u.b IS NULL) DETACH DELETE u RETURN u ORDER BY u.a ASC SKIP 2 LIMIT 1");

			statement = Cypher.match(userNode)
				.where(userNode.property("a").isNotNull()).and(userNode.property("b").isNull())
				.detachDelete(userNode)
				.returningDistinct(userNode).orderBy(userNode.property("a").ascending()).skip(2).limit(1)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (u.a IS NOT NULL AND u.b IS NULL) DETACH DELETE u RETURN DISTINCT u ORDER BY u.a ASC SKIP 2 LIMIT 1");
		}

		@Test
		void shouldRenderNodeDelete() {
			Node n = Cypher.anyNode("n");
			Relationship r = n.relationshipBetween(Cypher.anyNode()).named("r0");
			Statement statement = Cypher
				.match(n).where(n.internalId().isEqualTo(Cypher.literalOf(4711)))
				.optionalMatch(r)
				.delete(r, n)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n) WHERE id(n) = 4711 OPTIONAL MATCH (n)-[r0]-() DELETE r0, n");
		}

		@Test
		void shouldRenderChainedDeletes() {
			Node n = Cypher.anyNode("n");
			Relationship r = n.relationshipBetween(Cypher.anyNode()).named("r0");
			Statement statement = Cypher
				.match(n).where(n.internalId().isEqualTo(Cypher.literalOf(4711)))
				.optionalMatch(r)
				.delete(r, n)
				.delete(bikeNode)
				.detachDelete(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n) WHERE id(n) = 4711 OPTIONAL MATCH (n)-[r0]-() DELETE r0, n DELETE b DETACH DELETE u");
		}
	}

	@Nested
	class Expressions {
		@Test
		void shouldRenderParameters() {
			Statement statement;
			statement = Cypher.match(userNode)
				.where(userNode.property("a").isEqualTo(Cypher.parameter("aParameter")))
				.detachDelete(userNode)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE u.a = $aParameter DETACH DELETE u RETURN u");
		}
	}

	@Nested
	class OperationsAndComparisons {

		@Test
		void shouldRenderOperations() {
			Statement statement;
			statement = Cypher.match(Cypher.anyNode("n"))
				.returning(Cypher.literalOf(1).add(Cypher.literalOf(2)))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n) RETURN (1 + 2)");
		}

		@Test
		void shouldRenderComparision() {
			Statement statement;
			statement = Cypher.match(Cypher.anyNode("n"))
				.returning(Cypher.literalOf(1).gt(Cypher.literalOf(2)))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n) RETURN 1 > 2");

			statement = Cypher.match(Cypher.anyNode("n"))
				.returning(Cypher.literalOf(1).gt(Cypher.literalOf(2)).isTrue())
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n) RETURN (1 > 2) = true");

			statement = Cypher.match(Cypher.anyNode("n"))
				.returning(Cypher.literalOf(1).gt(Cypher.literalOf(2)).isTrue().isFalse())
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n) RETURN ((1 > 2) = true) = false");
		}
	}

	@Nested
	class ExpressionsRendering {
		@Test
		void shouldRenderMap() {
			Statement statement;
			statement = Cypher.match(Cypher.anyNode("n"))
				.returning(
					Functions.point(
						Cypher.mapOf(
							"latitude", Cypher.parameter("latitude"),
							"longitude", Cypher.parameter("longitude"),
							"crs", Cypher.literalOf(4326)
						)
					)
				)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n) RETURN point({latitude: $latitude, longitude: $longitude, crs: 4326})");
		}
	}

	@Nested
	class PropertyRendering {
		@Test
		void shouldRenderNodeProperties() {

			for (Node nodeWithProperties : new Node[] {
				Cypher.node("Test", Cypher.mapOf("a", Cypher.literalOf("b"))),
				Cypher.node("Test").withProperties(Cypher.mapOf("a", Cypher.literalOf("b"))),
				Cypher.node("Test").withProperties("a", Cypher.literalOf("b"))
			}) {

				Statement statement;
				statement = Cypher.match(nodeWithProperties)
					.returning(Cypher.asterisk())
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (:`Test` {a: 'b'}) RETURN *");

				statement = Cypher.merge(nodeWithProperties)
					.returning(Cypher.asterisk())
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MERGE (:`Test` {a: 'b'}) RETURN *");
			}
		}

		@Test
		void nestedProperties() {

			Node nodeWithProperties = Cypher.node("Test").withProperties("outer", Cypher.mapOf("a", Cypher.literalOf("b")));

			Statement statement;
			statement = Cypher.match(nodeWithProperties)
				.returning(Cypher.asterisk())
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (:`Test` {outer: {a: 'b'}}) RETURN *");
		}

		@Test
		void shouldNotRenderPropertiesInReturn() {

			Node nodeWithProperties = bikeNode.withProperties("a", Cypher.literalOf("b"));

			Statement statement;
			statement = Cypher.match(nodeWithProperties, nodeWithProperties.relationshipFrom(userNode, "OWNS"))
				.returning(nodeWithProperties)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (b:`Bike` {a: 'b'}), (b)<-[:`OWNS`]-(u:`User`) RETURN b");
		}
	}

	@Nested
	class UnwindRendering {

		@Test
		void unwindWithoutWith() {

			final Node rootNode = Cypher.anyNode("n");
			final SymbolicName label = Cypher.name("label");
			final Statement statement = Cypher.match(rootNode).where(rootNode.internalId().isEqualTo(Cypher.literalOf(1)))
				.unwind(rootNode.labels()).as("label")
				.with(label).where(label.in(Cypher.parameter("fixedLabels")).not())
				.returning(Functions.collect(label).as("labels")).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n) WHERE id(n) = 1 UNWIND labels(n) AS label WITH label WHERE NOT (label IN $fixedLabels) RETURN collect(label) AS labels");
		}

		@Test
		void shouldRenderLeadingUnwind() {

			Statement statement;
			statement = Cypher.unwind(Cypher.literalOf(1), Cypher.literalTrue(), Cypher.literalFalse())
				.as("n").returning(Cypher.name("n"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"UNWIND [1, true, false] AS n RETURN n");
		}

		@Test
		void shouldRenderLeadingUnwindWithUpdate() {

			Statement statement;
			statement = Cypher.unwind(Cypher.literalOf(1), Cypher.literalTrue(), Cypher.literalFalse())
				.as("n")
				.merge(bikeNode.withProperties("b", Cypher.name("n")))
				.returning(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"UNWIND [1, true, false] AS n MERGE (b:`Bike` {b: n}) RETURN b");
		}

		@Test
		void shouldRenderLeadingUnwindWithCreate() {

			Statement statement;
			statement = Cypher.unwind(Cypher.literalOf(1), Cypher.literalTrue(), Cypher.literalFalse())
				.as("n")
				.create(bikeNode.withProperties("b", Cypher.name("n")))
				.returning(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"UNWIND [1, true, false] AS n CREATE (b:`Bike` {b: n}) RETURN b");
		}

		@Test
		void shouldRenderUnwind() {

			Statement statement;

			AliasedExpression collected = Functions.collect(bikeNode).as("collected");
			statement = Cypher.match(bikeNode)
				.with(collected)
				.unwind(collected).as("x")
				.with("x")
				.delete(Cypher.name("x"))
				.returning("x")
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (b:`Bike`) WITH collect(b) AS collected UNWIND collected AS x WITH x DELETE x RETURN x");
		}
	}

	@Nested
	class Unions {

		@Test
		void shouldRenderUnions() {

			Statement statement1 = Cypher.match(bikeNode)
				.where(bikeNode.property("a").isEqualTo(Cypher.literalOf("A")))
				.returning(bikeNode)
				.build();

			Statement statement2 = Cypher.match(bikeNode)
				.where(bikeNode.property("b").isEqualTo(Cypher.literalOf("B")))
				.returning(bikeNode)
				.build();

			Statement statement3 = Cypher.match(bikeNode)
				.where(bikeNode.property("c").isEqualTo(Cypher.literalOf("C")))
				.returning(bikeNode)
				.build();
			Statement statement;
			statement = Cypher.union(statement1, statement2, statement3);

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (b:`Bike`) WHERE b.a = 'A' RETURN b UNION MATCH (b) WHERE b.b = 'B' RETURN b UNION MATCH (b) WHERE b.c = 'C' RETURN b");
		}

		@Test
		void shouldRenderAllUnions() {

			Statement statement1 = Cypher.match(bikeNode)
				.where(bikeNode.property("a").isEqualTo(Cypher.literalOf("A")))
				.returning(bikeNode)
				.build();

			Statement statement2 = Cypher.match(bikeNode)
				.where(bikeNode.property("b").isEqualTo(Cypher.literalOf("B")))
				.returning(bikeNode)
				.build();

			Statement statement;
			statement = Cypher.unionAll(statement1, statement2);

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (b:`Bike`) WHERE b.a = 'A' RETURN b UNION ALL MATCH (b) WHERE b.b = 'B' RETURN b");
		}

		@Test
		void shouldAppendToExistingUnions() {

			Statement statement1 = Cypher.match(bikeNode)
				.where(bikeNode.property("a").isEqualTo(Cypher.literalOf("A")))
				.returning(bikeNode)
				.build();

			Statement statement2 = Cypher.match(bikeNode)
				.where(bikeNode.property("b").isEqualTo(Cypher.literalOf("B")))
				.returning(bikeNode)
				.build();

			Statement statement;
			statement = Cypher.unionAll(statement1, statement2);

			Statement statement3 = Cypher.match(bikeNode)
				.where(bikeNode.property("c").isEqualTo(Cypher.literalOf("C")))
				.returning(bikeNode)
				.build();

			Statement statement4 = Cypher.match(bikeNode)
				.where(bikeNode.property("d").isEqualTo(Cypher.literalOf("D")))
				.returning(bikeNode)
				.build();

			statement = Cypher.unionAll(statement, statement3, statement4);

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (b:`Bike`) WHERE b.a = 'A' RETURN b UNION ALL MATCH (b) WHERE b.b = 'B' RETURN b UNION ALL MATCH (b) WHERE b.c = 'C' RETURN b UNION ALL MATCH (b) WHERE b.d = 'D' RETURN b");
		}

		@Test
		void shouldNotMix() {

			Statement statement1 = Cypher.match(bikeNode)
				.where(bikeNode.property("a").isEqualTo(Cypher.literalOf("A")))
				.returning(bikeNode)
				.build();

			Statement statement2 = Cypher.match(bikeNode)
				.where(bikeNode.property("b").isEqualTo(Cypher.literalOf("B")))
				.returning(bikeNode)
				.build();

			Statement statement;
			statement = Cypher.unionAll(statement1, statement2);

			Statement statement3 = Cypher.match(bikeNode)
				.where(bikeNode.property("c").isEqualTo(Cypher.literalOf("C")))
				.returning(bikeNode)
				.build();

			assertThatIllegalArgumentException().isThrownBy(() ->
				Cypher.union(statement, statement3)).withMessage("Cannot mix union and union all!");

		}
	}

	@Nested
	class MapProjections {

		@Nested
		class OnNodes {

			@Test
			void simple() {

				Statement statement;
				Node n = Cypher.anyNode("n");

				statement = Cypher.match(n)
					.returning(n.project("__internalNeo4jId__", Functions.id(n), "name"))
					.build();
				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (n) RETURN n{__internalNeo4jId__: id(n), .name}");

				statement = Cypher.match(n)
					.returning(n.project("name", "__internalNeo4jId__", Functions.id(n)))
					.build();
				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (n) RETURN n{.name, __internalNeo4jId__: id(n)}");
			}

			@Test
			void doc21221() {

				String expected = "MATCH (actor:`Person` {name: 'Tom Hanks'})-[:`ACTED_IN`]->(movie:`Movie`) RETURN actor{.name, .realName, movies: collect(movie{.title, .released})}";

				Statement statement;
				Node actor = Cypher.node("Person").named("actor");
				Node movie = Cypher.node("Movie").named("movie");

				statement = Cypher
					.match(actor.withProperties("name", Cypher.literalOf("Tom Hanks")).relationshipTo(movie, "ACTED_IN"))
					.returning(actor
						.project("name", "realName", "movies", Functions.collect(movie.project("title", "released"))))
					.build();
				assertThat(cypherRenderer.render(statement)).isEqualTo(expected);

				statement = Cypher
					.match(actor.withProperties("name", Cypher.literalOf("Tom Hanks")).relationshipTo(movie, "ACTED_IN"))
					.returning(actor.project("name", "realName", "movies",
						Functions.collect(movie.project(movie.property("title"), movie.property("released")))))
					.build();
				assertThat(cypherRenderer.render(statement)).isEqualTo(expected);

				statement = Cypher
					.match(actor.withProperties("name", Cypher.literalOf("Tom Hanks")).relationshipTo(movie, "ACTED_IN"))
					.returning(actor.project("name", "realName", "movies",
						Functions.collect(movie.project("title", "year", movie.property("released")))))
					.build();
				assertThat(cypherRenderer.render(statement)).isEqualTo(
					"MATCH (actor:`Person` {name: 'Tom Hanks'})-[:`ACTED_IN`]->(movie:`Movie`) RETURN actor{.name, .realName, movies: collect(movie{.title, year: movie.released})}");
			}

			@Test
			void nested() {

				Statement statement;
				Node n = Cypher.node("Person").named("p");
				Node m = Cypher.node("Movie").named("m");

				statement = Cypher.match(n.relationshipTo(m, "ACTED_IN"))
					.returning(
						n.project(
							"__internalNeo4jId__", Functions.id(n), "name", "nested",
							m.project("title", "__internalNeo4jId__", Functions.id(m))
						))
					.build();
				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (p:`Person`)-[:`ACTED_IN`]->(m:`Movie`) RETURN p{__internalNeo4jId__: id(p), .name, nested: m{.title, __internalNeo4jId__: id(m)}}");
			}

			@Test
			void requiresSymbolicName() {
				assertThatIllegalStateException().isThrownBy(() -> {
					Node n = Cypher.node("Person");
					n.project("something");
				}).withMessage("No name present.");
			}
		}

		@Nested
		class OnRelationShips {

			@Test
			void simple() {

				Statement statement;
				Node n = Cypher.node("Person").named("p");
				Node m = Cypher.node("Movie").named("m");
				Relationship rel = n.relationshipTo(m, "ACTED_IN").named("r");

				statement = Cypher.match(rel)
					.returning(
						rel.project(
							"__internalNeo4jId__", Functions.id(rel), "roles"
						))
					.build();
				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (p:`Person`)-[r:`ACTED_IN`]->(m:`Movie`) RETURN r{__internalNeo4jId__: id(r), .roles}");
			}

			@Test
			void nested() {

				Statement statement;
				Node n = Cypher.node("Person").named("p");
				Node m = Cypher.node("Movie").named("m");
				Relationship rel = n.relationshipTo(m, "ACTED_IN").named("r");

				statement = Cypher.match(rel)
					.returning(
						m.project(
							"title", "roles",
							rel.project(
								"__internalNeo4jId__", Functions.id(rel), "roles"
							)
						)
					)
					.build();
				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (p:`Person`)-[r:`ACTED_IN`]->(m:`Movie`) RETURN m{.title, roles: r{__internalNeo4jId__: id(r), .roles}}");
			}

			@Test
			void requiresSymbolicName() {
				assertThatIllegalStateException().isThrownBy(() -> {
					Node n = Cypher.node("Person").named("p");
					Node m = Cypher.node("Movie").named("m");
					Relationship rel = n.relationshipTo(m, "ACTED_IN");
					rel.project("something");
				}).withMessage("No name present.");
			}
		}

		@Test
		void asterisk() {

			Statement statement;
			Node n = Cypher.anyNode("n");

			statement = Cypher.match(n)
				.returning(n.project(Cypher.asterisk()))
				.build();
			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n) RETURN n{.*}");
		}

		@Test
		void invalid() {

			String expectedMessage = "FunctionInvocation{functionName='id'} of type class org.neo4j.cypherdsl.core.FunctionInvocation cannot be used with an implicit name as map entry.";
			assertThatIllegalArgumentException().isThrownBy(() -> {
				Node n = Cypher.anyNode("n");
				n.project(Functions.id(n));
			}).withMessage(expectedMessage);

			assertThatIllegalArgumentException().isThrownBy(() -> {
				Node n = Cypher.anyNode("n");
				n.project("a", Cypher.mapOf("a", Cypher.literalOf("b")), Functions.id(n));
			}).withMessage(expectedMessage);
		}
	}

	@Nested
	class WithAndOrder {

		@Test
		void orderOnWithShouldWork() {
			Statement statement = Cypher
				.match(
					Cypher.node("Movie").named("m").relationshipFrom(Cypher.node("Person").named("p"), "ACTED_IN").named("r")
				)
				.with(Cypher.name("m"), Cypher.name("p"))
				.orderBy(
					Cypher.sort(Cypher.property("m", "title")),
					Cypher.sort(Cypher.property("p", "name"))
				).returning(Cypher.property("m", "title").as("movie"),
					Functions.collect(Cypher.property("p", "name")).as("actors")).build();

			String expected = "MATCH (m:`Movie`)<-[r:`ACTED_IN`]-(p:`Person`) WITH m, p ORDER BY m.title, p.name RETURN m.title AS movie, collect(p.name) AS actors";
			assertThat(cypherRenderer.render(statement)).isEqualTo(expected);
		}

		@Test
		void concatenatedOrdering() {
			Statement statement;
			statement = Cypher.match(
				Cypher.node("Movie").named("m").relationshipFrom(Cypher.node("Person").named("p"), "ACTED_IN").named("r"))
				.with(Cypher.name("m"), Cypher.name("p")).orderBy(Cypher.property("m", "title")).ascending()
				.and(Cypher.property("p", "name")).ascending()
				.returning(Cypher.property("m", "title").as("movie"),
					Functions.collect(Cypher.property("p", "name")).as("actors")).build();

			String expected = "MATCH (m:`Movie`)<-[r:`ACTED_IN`]-(p:`Person`) WITH m, p ORDER BY m.title ASC, p.name ASC RETURN m.title AS movie, collect(p.name) AS actors";
			assertThat(cypherRenderer.render(statement)).isEqualTo(expected);
		}
	}

	@Nested
	class ListComprehensions {

		@Test
		void simple() {

			SymbolicName name = Cypher.name("a");
			Statement statement = Cypher.returning(
				Cypher.listWith(name)
					.in(Cypher.listOf(Cypher.literalOf(1), Cypher.literalOf(2), Cypher.literalOf(3), Cypher.literalOf(4)))
					.returning()).build();
			assertThat(cypherRenderer.render(statement))
				.isEqualTo("RETURN [a IN [1, 2, 3, 4]]");
		}

		@Test
		void withReturning() {

			SymbolicName name = Cypher.name("a");
			Statement statement = Cypher.returning(
				Cypher.listWith(name)
					.in(Cypher.listOf(Cypher.literalOf(1), Cypher.literalOf(2), Cypher.literalOf(3), Cypher.literalOf(4)))
					.returning(name.remainder(Cypher.literalOf(2)))).build();
			assertThat(cypherRenderer.render(statement))
				.isEqualTo("RETURN [a IN [1, 2, 3, 4] | (a % 2)]");
		}

		@Test
		void withWhere() {

			SymbolicName name = Cypher.name("a");
			Statement statement = Cypher.returning(
				Cypher.listWith(name)
					.in(Cypher.listOf(Cypher.literalOf(1), Cypher.literalOf(2), Cypher.literalOf(3), Cypher.literalOf(4)))
					.where(name.gt(Cypher.literalOf(2)))
					.returning()).build();
			assertThat(cypherRenderer.render(statement))
				.isEqualTo("RETURN [a IN [1, 2, 3, 4] WHERE a > 2]");
		}

		@Test
		void withWhereAndReturning() {

			SymbolicName name = Cypher.name("a");
			Statement statement = Cypher.returning(
				Cypher.listWith(name)
					.in(Cypher.listOf(Cypher.literalOf(1), Cypher.literalOf(2), Cypher.literalOf(3), Cypher.literalOf(4)))
					.where(name.gt(Cypher.literalOf(2)))
					.returning(name.remainder(Cypher.literalOf(2)))).build();
			assertThat(cypherRenderer.render(statement))
				.isEqualTo("RETURN [a IN [1, 2, 3, 4] WHERE a > 2 | (a % 2)]");
		}

		@Test
		void docsExample() {

			SymbolicName name = Cypher.name("x");
			Statement statement;

			statement = Cypher.returning(
				Cypher.listWith(name)
					.in(Functions.range(Cypher.literalOf(0), Cypher.literalOf(10)))
					.where(name.remainder(Cypher.literalOf(2)).isEqualTo(Cypher.literalOf(0)))
					.returning(name.pow(Cypher.literalOf(3))).as("result")).build();
			assertThat(cypherRenderer.render(statement))
				.isEqualTo("RETURN [x IN range(0, 10) WHERE (x % 2) = 0 | x^3] AS result");

			statement = Cypher.returning(
				Cypher.listWith(name)
					.in(Functions.range(Cypher.literalOf(0), Cypher.literalOf(10)))
					.where(name.remainder(Cypher.literalOf(2)).isEqualTo(Cypher.literalOf(0)))
					.returning().as("result")).build();
			assertThat(cypherRenderer.render(statement))
				.isEqualTo("RETURN [x IN range(0, 10) WHERE (x % 2) = 0] AS result");

			statement = Cypher.returning(
				Cypher.listWith(name)
					.in(Functions.range(Cypher.literalOf(0), Cypher.literalOf(10)))
					.returning(name.pow(Cypher.literalOf(3))).as("result")).build();
			assertThat(cypherRenderer.render(statement))
				.isEqualTo("RETURN [x IN range(0, 10) | x^3] AS result");
		}
	}

	@Nested
	class PatternComprehensions {

		@Test
		void simple() {

			Statement statement;
			Node a = Cypher.node("Person").withProperties("name", Cypher.literalOf("Keanu Reeves")).named("a");
			Node b = Cypher.anyNode("b");

			statement = Cypher.match(a)
				.returning(Cypher.listBasedOn(a.relationshipBetween(b)).returning(b.property("released")).as("years"))
				.build();
			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (a:`Person` {name: 'Keanu Reeves'}) RETURN [(a)--(b) | b.released] AS years");
		}

		@Test
		void simpleWithWhere() {

			Statement statement;
			Node a = Cypher.node("Person").withProperties("name", Cypher.literalOf("Keanu Reeves")).named("a");
			Node b = Cypher.anyNode("b");

			statement = Cypher.match(a)
				.returning(
					Cypher.listBasedOn(a.relationshipBetween(b)).where(b.hasLabels("Movie")).returning(b.property("released"))
						.as("years"))
				.build();
			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (a:`Person` {name: 'Keanu Reeves'}) RETURN [(a)--(b) WHERE b:`Movie` | b.released] AS years");
		}

		@Test
		void nested() {

			Statement statement;

			Node n = Cypher.node("Person").named("n");
			Node o1 = Cypher.node("Organisation").named("o1");
			Node l1 = Cypher.node("Location").named("l1");
			Node p2 = Cypher.node("Person").named("p2");

			Relationship r_f1 = n.relationshipTo(o1, "FOUNDED").named("r_f1");
			Relationship r_e1 = n.relationshipTo(o1, "EMPLOYED_BY").named("r_e1");
			Relationship r_l1 = n.relationshipTo(l1, "LIVES_AT").named("r_l1");
			Relationship r_l2 = l1.relationshipFrom(p2, "LIVES_AT").named("r_l2");

			statement = Cypher.match(n)
				.returning(n.getRequiredSymbolicName(),
					Cypher.listOf(
						Cypher.listBasedOn(r_f1).returning(r_f1, o1),
						Cypher.listBasedOn(r_e1).returning(r_e1, o1),
						Cypher.listBasedOn(r_l1).returning(
							r_l1.getRequiredSymbolicName(), l1.getRequiredSymbolicName(),
							// The building of the statement works with and without the outer list,
							// I'm not sure if it would be necessary for the result, but as I took the query from
							// Neo4j-OGM, I'd like to keep it
							Cypher.listOf(Cypher.listBasedOn(r_l2).returning(r_l2, p2))
						)
					)
				)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n:`Person`) RETURN n, [[(n)-[r_f1:`FOUNDED`]->(o1:`Organisation`) | [r_f1, o1]], [(n)-[r_e1:`EMPLOYED_BY`]->(o1) | [r_e1, o1]], [(n)-[r_l1:`LIVES_AT`]->(l1:`Location`) | [r_l1, l1, [[(l1)<-[r_l2:`LIVES_AT`]-(p2:`Person`) | [r_l2, p2]]]]]]");
		}
	}

	@Nested
	class MultipleLabels {

		@Test
		void matchWithMultipleLabels() {
			Node node = Cypher.node("a", "b", "c").named("n");
			Statement statement = Cypher.match(node).returning(node).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (n:`a`:`b`:`c`) RETURN n");
		}

		@Test
		void createWithMultipleLabels() {
			Node node = Cypher.node("a", "b", "c").named("n");
			Statement statement = Cypher.create(node).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("CREATE (n:`a`:`b`:`c`)");
		}
	}

	@Nested
	class Case {

		@Test
		void simpleCase() {
			Node node = Cypher.node("a").named("n");
			Statement statement = Cypher.match(node).where(
				Cypher.caseExpression(node.property("value"))
					.when(Cypher.literalOf("blubb"))
					.then(Cypher.literalTrue())
			).returning(node).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (n:`a`) WHERE CASE n.value WHEN 'blubb' THEN true END RETURN n");
		}

		@Test
		void simpleCaseWithElse() {
			Node node = Cypher.node("a").named("n");
			Statement statement = Cypher.match(node).where(
				Cypher.caseExpression(node.property("value"))
					.when(Cypher.literalOf("blubb"))
					.then(Cypher.literalTrue())
					.elseDefault(Cypher.literalFalse())
			).returning(node).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (n:`a`) WHERE CASE n.value WHEN 'blubb' THEN true ELSE false END RETURN n");
		}

		@Test
		void simpleCaseWithMultipleWhenThen() {
			Node node = Cypher.node("a").named("n");
			Statement statement = Cypher.match(node).where(
				Cypher.caseExpression(node.property("value"))
					.when(Cypher.literalOf("blubb"))
					.then(Cypher.literalTrue())
					.when(Cypher.literalOf("bla"))
					.then(Cypher.literalFalse())
			).returning(node).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n:`a`) WHERE CASE n.value WHEN 'blubb' THEN true WHEN 'bla' THEN false END RETURN n");
		}

		@Test
		void simpleCaseWithMultipleWhenThenAndElse() {
			Node node = Cypher.node("a").named("n");
			Statement statement = Cypher.match(node).where(
				Cypher.caseExpression(node.property("value"))
					.when(Cypher.literalOf("blubb"))
					.then(Cypher.literalTrue())
					.when(Cypher.literalOf("bla"))
					.then(Cypher.literalFalse())
					.elseDefault(Cypher.literalOf(1))
			).returning(node).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n:`a`) WHERE CASE n.value WHEN 'blubb' THEN true WHEN 'bla' THEN false ELSE 1 END RETURN n");
		}

		@Test
		void genericCase() {
			Node node = Cypher.node("a").named("n");
			Statement statement = Cypher.match(node).where(
				Cypher.caseExpression()
					.when(node.property("value").isEqualTo(Cypher.literalOf("blubb")))
					.then(Cypher.literalTrue())
			).returning(node).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (n:`a`) WHERE CASE WHEN n.value = 'blubb' THEN true END RETURN n");
		}

		@Test
		void genericCaseWithElse() {
			Node node = Cypher.node("a").named("n");
			Statement statement = Cypher.match(node).where(
				Cypher.caseExpression()
					.when(node.property("value").isEqualTo(Cypher.literalOf("blubb")))
					.then(Cypher.literalTrue())
					.elseDefault(Cypher.literalFalse())
			).returning(node).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (n:`a`) WHERE CASE WHEN n.value = 'blubb' THEN true ELSE false END RETURN n");
		}

		@Test
		void genericCaseWithMultipleWhenThen() {
			Node node = Cypher.node("a").named("n");
			Statement statement = Cypher.match(node).where(
				Cypher.caseExpression()
					.when(node.property("value").isEqualTo(Cypher.literalOf("blubb")))
					.then(Cypher.literalTrue())
					.when(node.property("value").isEqualTo(Cypher.literalOf("bla")))
					.then(Cypher.literalFalse())
			).returning(node).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n:`a`) WHERE CASE WHEN n.value = 'blubb' THEN true WHEN n.value = 'bla' THEN false END RETURN n");
		}

		@Test
		void genericCaseWithMultipleWhenThenAndElse() {
			Node node = Cypher.node("a").named("n");
			Statement statement = Cypher.match(node).where(
				Cypher.caseExpression()
					.when(node.property("value").isEqualTo(Cypher.literalOf("blubb")))
					.then(Cypher.literalTrue())
					.when(node.property("value").isEqualTo(Cypher.literalOf("bla")))
					.then(Cypher.literalFalse())
					.elseDefault(Cypher.literalOf(1))
			).returning(node).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n:`a`) WHERE CASE WHEN n.value = 'blubb' THEN true WHEN n.value = 'bla' THEN false ELSE 1 END RETURN n");
		}

		// from https://neo4j.com/docs/cypher-manual/current/syntax/expressions/#syntax-simple-case
		@Test
		void canGetAliasedInReturn() {
			Node node = Cypher.anyNode("n");
			Statement statement = Cypher.match(node)
				.returning(
					Cypher.caseExpression(node.property("eyes"))
						.when(Cypher.literalOf("blue"))
						.then(Cypher.literalOf(1))
						.when(Cypher.literalOf("brown"))
						.then(Cypher.literalOf(2))
						.elseDefault(Cypher.literalOf(3))
						.as("result")
				).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (n) RETURN CASE n.eyes WHEN 'blue' THEN 1 WHEN 'brown' THEN 2 ELSE 3 END AS result");
		}
	}

	@Nested
	class Issues {

		@Test
		void gh167() {
			final Node app = Cypher.node("Location").named("app").withProperties("uuid", Cypher.parameter("app_uuid"));
			final Node locStart = Cypher.node("Location").named("loc_start");
			final Node resume = Cypher.node("Resume").named("r");
			final Node offer = Cypher.node("Offer").named("o");
			final Node startN = Cypher.node("ResumeNode").named("start_n");

			final Relationship aFl = app.relationshipFrom(locStart, "PART_OF").length(0, 3);
			final Relationship lFr = locStart.relationshipFrom(resume, "IN", "IN_ANALYTICS");

			Statement statement = Cypher.match(aFl, lFr)
				.withDistinct(resume, locStart, app)
				.match(resume
					.relationshipTo(offer.withProperties("is_valid", Cypher.literalTrue()), "IN_COHORT_OF")
					.relationshipTo(Cypher.anyNode("app"), "IN")
				)
				.withDistinct(resume, locStart, app, offer)
				.match(offer.relationshipTo(startN, "FOR"))
				.where(Functions.id(startN).in(Cypher.parameter("start_ids")))
				.returningDistinct(resume, locStart, app, offer, startN).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (app:`Location` {uuid: $app_uuid})<-[:`PART_OF`*0..3]-(loc_start:`Location`), (loc_start)<-[:`IN`|`IN_ANALYTICS`]-(r:`Resume`) WITH DISTINCT r, loc_start, app MATCH (r)-[:`IN_COHORT_OF`]->(o:`Offer` {is_valid: true})-[:`IN`]->(app) WITH DISTINCT r, loc_start, app, o MATCH (o:`Offer`)-[:`FOR`]->(start_n:`ResumeNode`) WHERE id(start_n) IN $start_ids RETURN DISTINCT r, loc_start, app, o, start_n");
		}

		@Test
		void gh174() {
			final Node r = Cypher.node("Resume").named("r");
			final Node o = Cypher.node("Offer").named("o");

			Statement s = Cypher.match(r.relationshipTo(o, "FOR"))
				.where(r.hasLabels("LastResume").not())
				.and(Functions.coalesce(o.property("valid_only"), Cypher.literalFalse()).isEqualTo(Cypher.literalFalse())
					.and(r.hasLabels("InvalidStatus").not())
					.or(o.property("valid_only").isTrue()
						.and(r.hasLabels("InvalidStatus"))))
				.returningDistinct(r, o)
				.build();

			assertThat(cypherRenderer.render(s))
				.isEqualTo(
					"MATCH (r:`Resume`)-[:`FOR`]->(o:`Offer`) WHERE (NOT (r:`LastResume`) AND (coalesce(o.valid_only, false) = false AND NOT (r:`InvalidStatus`) OR (o.valid_only = true AND r:`InvalidStatus`))) RETURN DISTINCT r, o");
		}

		@Test
		void gh184() {
			final Node r = Cypher.node("Resume").named("r");
			final Node u = Cypher.node("UserSearchable").named("u");
			final Node o = Cypher.node("Offer").named("o");

			Statement s = Cypher.match(r.relationshipFrom(u, "HAS"))
				.where(r.hasLabels("LastResume").not())
				.and(Functions.coalesce(o.property("valid_only"), Cypher.literalFalse()).isEqualTo(Cypher.literalFalse())
					.and(r.hasLabels("InvalidStatus").not())
					.or(o.property("valid_only").isTrue()
						.and(r.hasLabels("ValidStatus")))
				)
				.and(r.property("is_internship").isTrue()
					.and(Functions.size(r.relationshipTo(Cypher.anyNode(), "PART_OF")).isEmpty())
					.not())
				.and(r.property("is_sandwich_training").isTrue()
					.and(Functions.size(r.relationshipTo(Cypher.anyNode(), "PART_OF")).isEmpty())
					.not())
				.returningDistinct(r, o)
				.build();

			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (r:`Resume`)<-[:`HAS`]-(u:`UserSearchable`) "
					+ "WHERE (NOT (r:`LastResume`) "
					+ "AND (coalesce(o.valid_only, false) = false "
					+ "AND NOT (r:`InvalidStatus`) "
					+ "OR (o.valid_only = true "
					+ "AND r:`ValidStatus`)) "
					+ "AND NOT ("
					+ "(r.is_internship = true AND size(size((r)-[:`PART_OF`]->())) = 0)"
					+ ") "
					+ "AND NOT ("
					+ "(r.is_sandwich_training = true AND size(size((r)-[:`PART_OF`]->())) = 0)"
					+ ")"
					+ ") RETURN DISTINCT r, o");
		}

		@Test
		void gh185() {
			final Node r = Cypher.node("Resume").named("r");
			final Node u = Cypher.node("UserSearchable").named("u");

			Statement s = Cypher.match(r.relationshipFrom(u, "HAS"))
				.where(org.neo4j.cypherdsl.core.Conditions.not(Predicates.exists(r.relationshipTo(u, "EXCLUDES"))))
				.returningDistinct(r)
				.build();

			assertThat(cypherRenderer.render(s))
				.isEqualTo(
					"MATCH (r:`Resume`)<-[:`HAS`]-(u:`UserSearchable`) WHERE NOT (exists((r)-[:`EXCLUDES`]->(u))) RETURN DISTINCT r");
		}

		@Test
		void gh187() {
			final Node r = Cypher.node("Resume").named("r");
			final Node u = Cypher.node("User").named("u");

			Statement s = Cypher.match(r.relationshipFrom(u, "HAS"))
				.with(Functions.head(Functions.collect(r.getRequiredSymbolicName())).as("r"))
				.returning(r)
				.build();

			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (r:`Resume`)<-[:`HAS`]-(u:`User`) WITH head(collect(r)) AS r RETURN r");
		}

		@Test
		void gh188() {
			final Node r = Cypher.node("Resume").named("r");
			final Node u = Cypher.node("User").named("u");

			Statement s = Cypher.match(r.relationshipFrom(u, "HAS"))
				.returning(Functions.countDistinct(r.getRequiredSymbolicName()).as("r"))
				.build();

			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (r:`Resume`)<-[:`HAS`]-(u:`User`) RETURN count(DISTINCT r) AS r");
		}

		@Test
		void gh197() {
			final Node n = Cypher.node("Person").named("n");

			// avg
			Statement s = Cypher.match(n)
				.returning(Functions.avg(n.property("age")))
				.build();
			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (n:`Person`) RETURN avg(n.age)");

			// max/min
			final ListExpression list = Cypher.listOf(
				Cypher.literalOf(1),
				Cypher.literalOf("a"),
				Cypher.literalOf(null),
				Cypher.literalOf(0.2),
				Cypher.literalOf("b"),
				Cypher.literalOf("1"),
				Cypher.literalOf("99"));
			s = Cypher.unwind(list).as("val")
				.returning(Functions.max(Cypher.name("val"))).build();
			assertThat(cypherRenderer.render(s))
				.isEqualTo("UNWIND [1, 'a', NULL, 0.2, 'b', '1', '99'] AS val RETURN max(val)");
			s = Cypher.unwind(list).as("val")
				.returning(Functions.min(Cypher.name("val"))).build();
			assertThat(cypherRenderer.render(s))
				.isEqualTo("UNWIND [1, 'a', NULL, 0.2, 'b', '1', '99'] AS val RETURN min(val)");

			// percentileCont/percentileDisc
			s = Cypher.match(n)
				.returning(Functions.percentileCont(n.property("age"), 0.4))
				.build();
			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (n:`Person`) RETURN percentileCont(n.age, 0.4)");
			s = Cypher.match(n)
				.returning(Functions.percentileDisc(n.property("age"), 0.5))
				.build();
			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (n:`Person`) RETURN percentileDisc(n.age, 0.5)");

			// stDev/stDevP
			s = Cypher.match(n)
				.where(n.property("name").in(
					Cypher.listOf(Cypher.literalOf("A"), Cypher.literalOf("B"), Cypher.literalOf("C"))))
				.returning(Functions.stDev(n.property("age")))
				.build();
			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (n:`Person`) WHERE n.name IN ['A', 'B', 'C'] RETURN stDev(n.age)");
			s = Cypher.match(n)
				.where(n.property("name").in(
					Cypher.listOf(Cypher.literalOf("A"), Cypher.literalOf("B"), Cypher.literalOf("C"))))
				.returning(Functions.stDevP(n.property("age")))
				.build();
			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (n:`Person`) WHERE n.name IN ['A', 'B', 'C'] RETURN stDevP(n.age)");

			// sum
			s = Cypher.match(n)
				.with(Cypher.listOf(Cypher.mapOf(
					"type", n.getRequiredSymbolicName(),
					"nb", Functions.sum(n.getRequiredSymbolicName())))
					.as("counts"))
				.returning(Functions.sum(n.property("age")))
				.build();
			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (n:`Person`) WITH [{type: n, nb: sum(n)}] AS counts RETURN sum(n.age)");
		}

		@Test
		void gh200() {
			final Node r = Cypher.node("Resume").named("r");

			Statement s = Cypher.match(r)
				.with(r.getRequiredSymbolicName())
				.returningDistinct(r.getRequiredSymbolicName())
				.build();

			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (r:`Resume`) WITH r RETURN DISTINCT r");
		}

		@Test
		void gh204() {
			final Node a = Cypher.node("A").named("a");
			final Node b = Cypher.node("B").named("b");
			final Node c = Cypher.node("C").named("c");

			Statement s = Cypher.match(a.relationshipTo(b).relationshipTo(c).max(2))
				.returning(a)
				.build();

			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (a:`A`)-->(b:`B`)-[*..2]->(c:`C`) RETURN a");
		}

		@Test
		void gh245() {
			String expected = "MATCH (p:`Person`) RETURN p{alias: p.name}";

			Node n = Cypher.node("Person").named("p");
			Statement statement;
			statement = Cypher.match(n)
				.returning(n.project("alias", n.property("name")))
				.build();
			assertThat(cypherRenderer.render(statement)).isEqualTo(expected);

			statement = Cypher.match(n)
				.returning(n.project(n.property("name").as("alias")))
				.build();
			assertThat(cypherRenderer.render(statement)).isEqualTo(expected);
		}
	}

	@Nested
	class NamedPaths {

		@Test
		void doc3148() {

			// See docs
			NamedPath p = Cypher.path("p").definedBy(
					Cypher.anyNode("michael").withProperties("name", Cypher.literalOf("Michael Douglas"))
						.relationshipTo(Cypher.anyNode()));
			Statement statement = Cypher.match(p).returning(p.getRequiredSymbolicName()).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH p = (michael {name: 'Michael Douglas'})-->() RETURN p");
		}
	}

	@Nested
	class Predicatez {

		@Test
		void allShouldWork() {

			NamedPath p = Cypher.path("p").definedBy(Cypher.anyNode("a").relationshipTo(Cypher.anyNode("b")).min(1).max(3));
			Statement statement = Cypher.match(p)
				.where(Cypher.property("a", "name").isEqualTo(Cypher.literalOf("Alice")))
				.and(Cypher.property("b", "name").isEqualTo(Cypher.literalOf("Daniel")))
				.and(Predicates.all("x").in(Functions.nodes(p)).where(Cypher.property("x", "age").gt(Cypher.literalOf(30))))
				.returning(p).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH p = (a)-[*1..3]->(b) WHERE (a.name = 'Alice' AND b.name = 'Daniel' AND all(x IN nodes(p) WHERE x.age > 30)) RETURN p");
		}

		@Test
		void anyShouldWork() {

			Node a = Cypher.anyNode("a");
			Statement statement = Cypher.match(a)
				.where(Cypher.property("a", "name").isEqualTo(Cypher.literalOf("Eskil")))
				.and(Predicates.any("x").in(a.property("array")).where(Cypher.name("x").isEqualTo(Cypher.literalOf("one"))))
				.returning(a.property("name"), a.property("array")).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (a) WHERE (a.name = 'Eskil' AND any(x IN a.array WHERE x = 'one')) RETURN a.name, a.array");
		}

		@Test
		void noneShouldWork() {

			NamedPath p = Cypher.path("p").definedBy(Cypher.anyNode("a").relationshipTo(Cypher.anyNode("b")).min(1).max(3));
			Statement statement = Cypher.match(p)
				.where(Cypher.property("a", "name").isEqualTo(Cypher.literalOf("Alice")))
				.and(Predicates
					.none("x").in(Functions.nodes(p)).where(Cypher.property("x", "age").isEqualTo(Cypher.literalOf(25))))
				.returning(p).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH p = (a)-[*1..3]->(b) WHERE (a.name = 'Alice' AND none(x IN nodes(p) WHERE x.age = 25)) RETURN p");
		}

		@Test
		void singleShouldWork() {

			NamedPath p = Cypher.path("p").definedBy(Cypher.anyNode("n").relationshipTo(Cypher.anyNode("b")));
			Statement statement = Cypher.match(p)
				.where(Cypher.property("n", "name").isEqualTo(Cypher.literalOf("Alice")))
				.and(Predicates.single("var").in(Functions.nodes(p)).where(
					Cypher.property("var", "eyes").isEqualTo(Cypher.literalOf("blue"))))
				.returning(p).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH p = (n)-->(b) WHERE (n.name = 'Alice' AND single(var IN nodes(p) WHERE var.eyes = 'blue')) RETURN p");
		}
	}
}
