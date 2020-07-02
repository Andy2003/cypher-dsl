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
import org.neo4j.cypherdsl.querydsl.QueryDSLAdapter;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Param;

/**
 * @author Michael J. Simons
 */
class QueryMoviesTest {

	public static final Renderer RENDERER = Renderer.getDefaultRenderer();

	@Test
	void x() {
		System.out.println("Ohne");
		QueryDSLAdapter.toCypherDSL(QPerson.person.name);
		System.out.println("--");
		System.out.println("mit");
		QueryDSLAdapter.toCypherDSL(QPerson.person.bornOn);
	}

	@Test
	void simpleMatchShouldWork() {

		CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(QMovie.movie);

		Statement statement = movieQuery.buildStatement();
		assertThat(RENDERER.render(statement)).isEqualTo("MATCH (movie:`Movie`) RETURN movie");
	}

	@Nested
	class Parameters {

		@Test
		void simpleParameters() {

			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(QMovie.movie)
				.where(QMovie.movie.title.eq(new Param<>(String.class, "title")));

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement))
				.isEqualTo("MATCH (movie:`Movie`) WHERE movie.title = $title RETURN movie");
		}
	}

	@Nested
	class Projections {

		@Test
		void properties() {

			CypherDSLQuery<Tuple> movieQuery = CypherDSLQuery.match(QMovie.movie)
				.where(QMovie.movie.title.eq("The Matrix"))
				.returning(QMovie.movie.title, QMovie.movie.released);

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement))
				.isEqualTo("MATCH (movie:`Movie`) WHERE movie.title = 'The Matrix' RETURN movie.title, movie.released");
		}
	}

	@Nested
	class Operations {

		@Test
		void eq() {

			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(QMovie.movie)
				.where(QMovie.movie.title.eq("The Matrix"));

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement))
				.isEqualTo("MATCH (movie:`Movie`) WHERE movie.title = 'The Matrix' RETURN movie");
		}

		@Test
		void not() {

			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(QMovie.movie)
				.where(QMovie.movie.title.eq("The Matrix").not());

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement))
				.isEqualTo("MATCH (movie:`Movie`) WHERE NOT (movie.title = 'The Matrix') RETURN movie");
		}

		@Test
		void ne() {

			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(QMovie.movie)
				.where(QMovie.movie.title.ne("The Matrix"));

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement))
				.isEqualTo("MATCH (movie:`Movie`) WHERE movie.title <> 'The Matrix' RETURN movie");
		}

		@Test
		void isFalse() {

			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(QMovie.movie)
				.where(QMovie.movie.title.ne("The Matrix").isFalse());

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement))
				.isEqualTo("MATCH (movie:`Movie`) WHERE (movie.title <> 'The Matrix') = false RETURN movie");
		}

		@Test
		void isTrue() {

			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(QMovie.movie)
				.where(QMovie.movie.title.ne("The Matrix").isTrue());

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement))
				.isEqualTo("MATCH (movie:`Movie`) WHERE (movie.title <> 'The Matrix') = true RETURN movie");
		}

		@Test
		void contains() {

			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(QMovie.movie)
				.where(QMovie.movie.title.contains("Matrix"));

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement))
				.isEqualTo("MATCH (movie:`Movie`) WHERE movie.title CONTAINS 'Matrix' RETURN movie");
		}

		@Test
		void containsIC() {

			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(QMovie.movie)
				.where(QMovie.movie.title.containsIgnoreCase("Matrix"));

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement))
				.isEqualTo("MATCH (movie:`Movie`) WHERE toLower(movie.title) CONTAINS toLower('Matrix') RETURN movie");
		}
	}

	@Nested
	class Booleans {

		@Test
		void and() {

			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(QMovie.movie)
				.where(QMovie.movie.title.eq("The Matrix").and(QMovie.movie.released.eq(1999L)));

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement))
				.isEqualTo(
					"MATCH (movie:`Movie`) WHERE (movie.title = 'The Matrix' AND movie.released = 1999) RETURN movie");
		}

		@Test
		void or() {

			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(QMovie.movie)
				.where(QMovie.movie.title.eq("The Matrix").or(QMovie.movie.released.eq(2003L)));

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement))
				.isEqualTo(
					"MATCH (movie:`Movie`) WHERE (movie.title = 'The Matrix' OR movie.released = 2003) RETURN movie");
		}

		@Test
		void multiple() {

			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(QMovie.movie)
				.where(
					QMovie.movie.title.contains("The Matrix")
						.and(QMovie.movie.released.eq(1999L).or(QMovie.movie.released.eq(2003L))));

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement))
				.isEqualTo(
					"MATCH (movie:`Movie`) WHERE (movie.title CONTAINS 'The Matrix' AND (movie.released = 1999 OR movie.released = 2003)) RETURN movie");
		}
	}

	@Nested
	class Limitations {

		@Test
		void limit() {

			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(QMovie.movie).limit(23);

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement)).isEqualTo("MATCH (movie:`Movie`) RETURN movie LIMIT 23");
		}

		@Test
		void offset() {

			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(QMovie.movie).offset(23);

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement)).isEqualTo("MATCH (movie:`Movie`) RETURN movie SKIP 23");
		}

		@Test
		void offsetAndLimit() {

			CypherDSLQuery<Movie> movieQuery = CypherDSLQuery.match(QMovie.movie).limit(42).offset(23);

			Statement statement = movieQuery.buildStatement();
			assertThat(RENDERER.render(statement)).isEqualTo("MATCH (movie:`Movie`) RETURN movie SKIP 23 LIMIT 42");
		}
	}
}
