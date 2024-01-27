/*
 * Copyright (c) 2019-2024 "Neo4j,"
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

import static org.apiguardian.api.API.Status.STABLE;

import java.util.List;

import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.neo4j.cypherdsl.core.ast.Visitable;
import org.neo4j.cypherdsl.core.ast.Visitor;

/**
 * An existential sub-query  can only be used in  a where clause. The  sub-query must consist only of  a match statement
 * which may have a {@code WHERE} clause on its own but is not allowed to return anything.
 *
 * @author Michael J. Simons
 * @soundtrack Die Ärzte - Seitenhirsch
 * @neo4j.version 4.0.0
 * @since 2020.1.2
 */
@API(status = STABLE, since = "2020.1.2")
@Neo4jVersion(minimum = "4.0.0")
public final class ExistentialSubquery implements SubqueryExpression, Condition {

	static @NotNull ExistentialSubquery exists(@NotNull Match fragment) {

		return new ExistentialSubquery(fragment);
	}

	static @NotNull Condition exists(@NotNull Statement statement, IdentifiableElement... imports) {
		return new ExistentialSubquery(statement, imports);
	}

	static @NotNull Condition exists(@NotNull List<PatternElement> patternElements, @Nullable Where innerWhere) {
		return new ExistentialSubquery(patternElements, innerWhere);
	}

	private final @Nullable ImportingWith importingWith;
	private final List<Visitable> fragments;
	@Nullable
	private  final Where innerWhere;

	ExistentialSubquery(@NotNull List<PatternElement> fragments, @Nullable Where innerWhere) {
		this.fragments = List.copyOf(fragments);
		this.importingWith = new ImportingWith();
		this.innerWhere = innerWhere;
	}

	ExistentialSubquery(@NotNull Match fragment) {
		this.fragments = List.of(fragment);
		this.importingWith = new ImportingWith();
		this.innerWhere = null;
	}

	ExistentialSubquery(@NotNull Statement statement, IdentifiableElement... imports) {
		this.fragments = List.of(statement);
		this.importingWith = ImportingWith.of(imports);
		this.innerWhere = null;
	}

	@Override
	public void accept(@NotNull Visitor visitor) {

		visitor.enter(this);
		importingWith.accept(visitor);
		fragments.forEach(v -> v.accept(visitor));
		Visitable.visitIfNotNull(innerWhere, visitor);
		visitor.leave(this);
	}
}
