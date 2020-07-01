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
import org.neo4j.cypherdsl.core.Conditions;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.PropertyContainer;

import com.querydsl.core.types.*;

/**
 * This class takes QueryDSL expressions and transform them into expressions of the Cypher-DSL.
 * It is used for example to build the {@literal where} clause.
 *
 * @author Michael J. Simons
 */
final class QueryToCypherDSLTransformer implements Visitor<Expression, Object> {

	@Override
	public Expression visit(Constant<?> expr, Object context) {

		return literalOf(expr.getConstant());
	}

	@Override
	public Expression visit(FactoryExpression<?> expr, Object context) {

		return Cypher.listOf(expr.getArgs().stream().map(e -> e.accept(this, context)).toArray(Expression[]::new));
	}

	boolean ignoreCase(Operator operator) {

		return EnumSet.of(EQ_IGNORE_CASE, STRING_CONTAINS_IC).contains(operator);
	}

	@Override
	public Expression visit(Operation<?> expr, Object context) {
		System.err.println("Operation " + expr);

		Operator op = expr.getOperator();

		boolean ignoreCase = ignoreCase(op);

		if (op instanceof Ops) {

			switch (((Ops) op)) {
				case LT:
					return toCypherExpression(expr.getArg(0), context, ignoreCase)
						.lt(toCypherExpression(expr.getArg(1), context, ignoreCase)).not();

				case LOE:
					return toCypherExpression(expr.getArg(0), context, ignoreCase)
						.lte(toCypherExpression(expr.getArg(1), context, ignoreCase)).not();

				case EQ:
				case EQ_IGNORE_CASE:
					return toCypherExpression(expr.getArg(0), context, ignoreCase)
						.isEqualTo(toCypherExpression(expr.getArg(1), context, ignoreCase));

				case NE:
					return toCypherExpression(expr.getArg(0), context, ignoreCase)
						.isNotEqualTo(toCypherExpression(expr.getArg(1), context, ignoreCase));

				case GT:
					return toCypherExpression(expr.getArg(0), context, ignoreCase)
						.gt(toCypherExpression(expr.getArg(1), context, ignoreCase)).not();

				case GOE:
					return toCypherExpression(expr.getArg(0), context, ignoreCase)
						.gte(toCypherExpression(expr.getArg(1), context, ignoreCase)).not();

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

				case XOR:
					return ((Condition) expr.getArg(0).accept(this, context))
						.xor((Condition) expr.getArg(1).accept(this, context));

				case IN:
					return toCypherExpression(expr.getArg(0), context, ignoreCase)
						.in(toCypherExpression(expr.getArg(1), context, ignoreCase));

				case NOT_IN:
					return toCypherExpression(expr.getArg(0), context, ignoreCase)
						.in(toCypherExpression(expr.getArg(1), context, ignoreCase)).not();

				case NOT:
					return Conditions.not((Condition) toCypherExpression(expr.getArg(0), context, ignoreCase));

				case ADD:
					return toCypherExpression(expr.getArg(0), context, ignoreCase)
						.add(toCypherExpression(expr.getArg(1), context, ignoreCase));

				case DIV:
					return toCypherExpression(expr.getArg(0), context, ignoreCase)
						.divide(toCypherExpression(expr.getArg(1), context, ignoreCase));

				case MULT:
					return toCypherExpression(expr.getArg(0), context, ignoreCase)
						.multiply(toCypherExpression(expr.getArg(1), context, ignoreCase));

				case SUB:
					return toCypherExpression(expr.getArg(0), context, ignoreCase)
						.subtract(toCypherExpression(expr.getArg(1), context, ignoreCase));

				case MOD:
					return toCypherExpression(expr.getArg(0), context, ignoreCase)
						.remainder(toCypherExpression(expr.getArg(1), context, ignoreCase));

				default:
					throw new UnsupportedOperationException("Unsupported operator " + op);
			}
		}

		throw new UnsupportedOperationException("Unsupported operator " + op);
	}

	Expression toCypherExpression(
		com.querydsl.core.types.Expression<?> queryDslExpress, Object context, boolean addToLower
	) {
		Expression expression = queryDslExpress.accept(this, context);
		if (addToLower) {
			expression = Functions.toLower(expression);
		}
		return expression;
	}

	@Override
	public Expression visit(ParamExpression<?> expr, Object context) {

		System.err.println("ParamExpression " + expr);
		return null;
	}

	@Override
	public Expression visit(Path<?> expr, Object context) {

		if (!(context instanceof PropertyContainer)) {
			throw new IllegalStateException();
		}

		PropertyContainer container = (PropertyContainer) context;
		return container.property(expr.getMetadata().getName());
	}

	@Override
	public Expression visit(SubQueryExpression<?> expr, Object context) {

		System.err.println("SubQueryExpression " + expr);
		return null;
	}

	@Override
	public Expression visit(TemplateExpression<?> expr, Object context) {

		System.err.println("TemplateExpression " + expr);
		return null;
	}
}
