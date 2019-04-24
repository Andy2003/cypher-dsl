/*
 * Copyright (c) 2019 "Neo4j,"
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
package org.springframework.data.neo4j.core.cypher;

import java.util.Optional;

import org.springframework.data.neo4j.core.cypher.support.Visitor;
import org.springframework.util.Assert;

/**
 * An aliased expression, that deals with named expressions when accepting visitors.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
public class AliasedExpression implements Aliased, Expression {

	private final Expression delegate;

	private final String alias;

	AliasedExpression(Expression delegate, String alias) {

		this.delegate = delegate;
		this.alias = alias;
	}

	public String getAlias() {
		return alias;
	}

	/**
	 * This takes the originally aliased expression and re-aliases it. Aliases are not nested.
	 *
	 * @param newAlias The new alias to use
	 * @return A new aliased, expression.
	 */
	@Override
	public AliasedExpression as(String newAlias) {

		Assert.hasText(newAlias, "The alias may not be null or empty.");
		return new AliasedExpression(this.delegate, newAlias);
	}

	@Override
	public void accept(Visitor visitor) {

		visitor.enter(this);
		// Make sure we deal nicely with symbolic names.
		Optional.of(delegate).filter(Named.class::isInstance).map(Named.class::cast).flatMap(Named::getSymbolicName)
			.map(Expression.class::cast)
			.orElse(delegate).accept(visitor);
		visitor.leave(this);
	}
}
