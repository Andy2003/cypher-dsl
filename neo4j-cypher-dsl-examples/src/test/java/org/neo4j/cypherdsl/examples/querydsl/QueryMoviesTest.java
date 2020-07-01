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
package org.neo4j.cypherdsl.examples.querydsl;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.neo4j.cypherdsl.querydsl.CypherDSLQuery;

import com.querydsl.core.Tuple;

/**
 * @author Michael J. Simons
 */
class QueryMoviesTest {

	public static final Renderer RENDERER = Renderer.getDefaultRenderer();

	@Test
	void simpleMatchShouldWork() {

		QMovie qMovie = new QMovie("Movie");
		CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(qMovie);

		Statement statement = movieQuery.buildStatement();
		assertThat(RENDERER.render(statement)).isEqualTo("MATCH (r:`Movie`) RETURN r");
	}

	@Nested
	class Projections {

		@Test
		void properties() {

			QMovie qMovie = new QMovie("Movie");
			CypherDSLQuery<Tuple> movieQuery = CypherDSLQuery.match(qMovie)
				.where(qMovie.title.eq("The Matrix"))
				.returning(qMovie.title, qMovie.released);

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement))
				.isEqualTo("MATCH (r:`Movie`) WHERE r.title = 'The Matrix' RETURN r.title, r.released");
		}
	}

	@Nested
	class Operations {

		@Test
		void eq() {

			QMovie qMovie = new QMovie("Movie");
			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(qMovie)
				.where(qMovie.title.eq("The Matrix"));

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement))
				.isEqualTo("MATCH (r:`Movie`) WHERE r.title = 'The Matrix' RETURN r");
		}

		@Test
		void not() {

			QMovie qMovie = new QMovie("Movie");
			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(qMovie)
				.where(qMovie.title.eq("The Matrix").not());

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement))
				.isEqualTo("MATCH (r:`Movie`) WHERE NOT (r.title = 'The Matrix') RETURN r");
		}

		@Test
		void ne() {

			QMovie qMovie = new QMovie("Movie");
			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(qMovie)
				.where(qMovie.title.ne("The Matrix"));

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement))
				.isEqualTo("MATCH (r:`Movie`) WHERE r.title <> 'The Matrix' RETURN r");
		}

		@Test
		void isFalse() {

			QMovie qMovie = new QMovie("Movie");
			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(qMovie)
				.where(qMovie.title.ne("The Matrix").isFalse());

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement))
				.isEqualTo("MATCH (r:`Movie`) WHERE (r.title <> 'The Matrix') = false RETURN r");
		}

		@Test
		void isTrue() {

			QMovie qMovie = new QMovie("Movie");
			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(qMovie)
				.where(qMovie.title.ne("The Matrix").isTrue());

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement))
				.isEqualTo("MATCH (r:`Movie`) WHERE (r.title <> 'The Matrix') = true RETURN r");
		}

		@Test
		void contains() {

			QMovie qMovie = new QMovie("Movie");
			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(qMovie)
				.where(qMovie.title.contains("Matrix"));

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement))
				.isEqualTo("MATCH (r:`Movie`) WHERE r.title CONTAINS 'Matrix' RETURN r");
		}

		@Test
		void containsIC() {

			QMovie qMovie = new QMovie("Movie");
			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(qMovie)
				.where(qMovie.title.containsIgnoreCase("Matrix"));

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement))
				.isEqualTo("MATCH (r:`Movie`) WHERE toLower(r.title) CONTAINS toLower('Matrix') RETURN r");
		}
	}

	@Nested
	class Booleans {

		@Test
		void and() {

			QMovie qMovie = new QMovie("Movie");
			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(qMovie)
				.where(qMovie.title.eq("The Matrix").and(qMovie.released.eq(1999L)));

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement))
				.isEqualTo("MATCH (r:`Movie`) WHERE (r.title = 'The Matrix' AND r.released = 1999) RETURN r");
		}

		@Test
		void or() {

			QMovie qMovie = new QMovie("Movie");
			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(qMovie)
				.where(qMovie.title.eq("The Matrix").or(qMovie.released.eq(2003L)));

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement))
				.isEqualTo("MATCH (r:`Movie`) WHERE (r.title = 'The Matrix' OR r.released = 2003) RETURN r");
		}

		@Test
		void multiple() {

			QMovie qMovie = new QMovie("Movie");
			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(qMovie)
				.where(
					qMovie.title.contains("The Matrix").and(qMovie.released.eq(1999L).or(qMovie.released.eq(2003L))));

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement))
				.isEqualTo(
					"MATCH (r:`Movie`) WHERE (r.title CONTAINS 'The Matrix' AND (r.released = 1999 OR r.released = 2003)) RETURN r");
		}
	}

	@Nested
	class Limitations {

		@Test
		void limit() {

			QMovie qMovie = new QMovie("Movie");
			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(qMovie).limit(23);

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement)).isEqualTo("MATCH (r:`Movie`) RETURN r LIMIT 23");
		}

		@Test
		void offset() {

			QMovie qMovie = new QMovie("Movie");
			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(qMovie).offset(23);

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement)).isEqualTo("MATCH (r:`Movie`) RETURN r SKIP 23");
		}

		@Test
		void offsetAndLimit() {

			QMovie qMovie = new QMovie("Movie");
			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(qMovie).limit(42).offset(23);

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement)).isEqualTo("MATCH (r:`Movie`) RETURN r SKIP 23 LIMIT 42");
		}
	}
}
