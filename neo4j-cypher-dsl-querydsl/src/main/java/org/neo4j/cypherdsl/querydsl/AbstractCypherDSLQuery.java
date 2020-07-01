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
package org.neo4j.cypherdsl.querydsl;

import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Conditions;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.StatementBuilder.OngoingReadingAndReturn;
import org.neo4j.cypherdsl.core.StatementBuilder.OngoingReadingWithoutWhere;

import com.querydsl.core.DefaultQueryMetadata;
import com.querydsl.core.QueryMetadata;
import com.querydsl.core.QueryModifiers;
import com.querydsl.core.SimpleQuery;
import com.querydsl.core.support.QueryMixin;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.ParamExpression;
import com.querydsl.core.types.Predicate;

abstract class AbstractCypherDSLQuery<T, Q extends AbstractCypherDSLQuery<T, Q>> implements SimpleQuery<Q> {

	private final EntityPath<T> rootEntity;
	private final QueryMixin<Q> queryMixin;

	AbstractCypherDSLQuery(EntityPath<T> rootEntity) {

		this.rootEntity = rootEntity;
		this.queryMixin = new QueryMixin<Q>((Q) this, new DefaultQueryMetadata(), false);
	}

	@Override public Q limit(long limit) {
		return queryMixin.limit(limit);
	}

	@Override public Q offset(long offset) {
		return queryMixin.offset(offset);
	}

	@Override public Q restrict(QueryModifiers modifiers) {
		return null;
	}

	@Override public Q orderBy(OrderSpecifier<?>... o) {
		return null;
	}

	@Override public <T> Q set(ParamExpression<T> param, T value) {
		return null;
	}

	@Override public Q distinct() {
		return null;
	}

	@Override public Q where(Predicate... o) {
		return queryMixin.where(o);
	}

	public Statement buildStatement() {

		Node rootNode = Cypher.node(rootEntity.getMetadata().getName()).named("r");
		QueryMetadata metadata = this.queryMixin.getMetadata();

		OngoingReadingWithoutWhere match = Cypher.match(rootNode);

		Condition condition = Conditions.noCondition();
		Predicate where = metadata.getWhere();
		if (where != null) {
			condition = (Condition) where.accept(new QueryToCypherDSLTransformer(), rootNode);
		}

		OngoingReadingAndReturn returning = match
			.where(condition)
			.returning(rootNode);

		if (metadata.getModifiers().isRestricting()) {
			returning = (OngoingReadingAndReturn) returning.skip(metadata.getModifiers().getOffset())
				.limit(metadata.getModifiers().getLimit());
		}

		return returning
			.build();
	}
}
