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

import static com.querydsl.core.types.Ops.*;
import static org.neo4j.cypherdsl.core.Cypher.*;

import java.util.EnumSet;

import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.PropertyContainer;

import com.querydsl.core.types.*;

final class QueryToCypherDSLTransformer implements Visitor<Object, Object> {

	@Override public Object visit(Constant<?> expr, Object context) {

		return literalOf(expr.getConstant());
	}

	@Override public Object visit(FactoryExpression<?> expr, Object context) {
		System.err.println("FactoryExpression " + expr);
		return null;
	}

	boolean ignoreCase(Operator operator) {

		return EnumSet.of(EQ_IGNORE_CASE, STRING_CONTAINS_IC).contains(operator);
	}

	@Override public Object visit(Operation<?> expr, Object context) {
		System.err.println("Operation " + expr);

		Operator op = expr.getOperator();

		boolean ignoreCase = ignoreCase(op);

		if (op instanceof Ops) {

			switch (((Ops) op)) {
				case EQ:
				case EQ_IGNORE_CASE:
					return toCypherExpression(expr.getArg(0), context, ignoreCase)
						.isEqualTo(toCypherExpression(expr.getArg(1), context, ignoreCase));

				case STRING_CONTAINS:
				case STRING_CONTAINS_IC:
					return toCypherExpression(expr.getArg(0), context, ignoreCase)
						.contains(toCypherExpression(expr.getArg(1), context, ignoreCase));

				case AND:
					return ((Condition) expr.getArg(0).accept(this, context))
						.and((Condition) expr.getArg(1).accept(this, context));

				case OR:
					return ((Condition) expr.getArg(0).accept(this, context))
						.or((Condition) expr.getArg(1).accept(this, context));

				default:
					throw new UnsupportedOperationException("Unsupported operator " + op);
			}
		}
		throw new UnsupportedOperationException("Unsupported operator " + op);
	}

	Expression toCypherExpression(
		com.querydsl.core.types.Expression<?> queryDslExpress, Object context, boolean addToLower
	) {
		Expression expression = (Expression) queryDslExpress.accept(this, context);
		if (addToLower) {
			expression = Functions.toLower(expression);
		}
		return expression;
	}

	@Override public Object visit(ParamExpression<?> expr, Object context) {

		System.err.println("ParamExpression " + expr);
		return null;
	}

	@Override public Object visit(Path<?> expr, Object context) {

		if (!(context instanceof PropertyContainer)) {
			throw new IllegalStateException();
		}

		PropertyContainer container = (PropertyContainer) context;
		return container.property(expr.getMetadata().getName());
	}

	@Override public Object visit(SubQueryExpression<?> expr, Object context) {

		System.err.println("SubQueryExpression " + expr);
		return null;
	}

	@Override public Object visit(TemplateExpression<?> expr, Object context) {

		System.err.println("TemplateExpression " + expr);
		return null;
	}
}
