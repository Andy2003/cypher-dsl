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

import com.querydsl.core.QueryMetadata;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;

/**
 * Proposed entry point to the QueryDSL integration.
 *
 * @author Michael J. Simons
 * @param <T> Type of the domain object being fetched.
 */
public final class CypherDSLQuery<T> extends AbstractCypherDSLQuery<T, CypherDSLQuery<T>> {

	public static <T> CypherDSLQuery<T> match(EntityPath<T> rootEntity) {
		return new CypherDSLQuery<>(rootEntity);
	}

	private CypherDSLQuery(EntityPath<T> rootEntity) {
		super(rootEntity);
	}

	private CypherDSLQuery(EntityPath<?> rootEntity, QueryMetadata queryMetadata) {
		super(rootEntity, queryMetadata);
	}

	public CypherDSLQuery<Tuple> returning(Expression<?>... o) {

		QueryMetadata queryMetadata = super.queryMixin.getMetadata().clone();
		queryMetadata.setProjection(Projections.tuple(o));

		return new CypherDSLQuery<>(super.rootEntity, queryMetadata);
	}
}
