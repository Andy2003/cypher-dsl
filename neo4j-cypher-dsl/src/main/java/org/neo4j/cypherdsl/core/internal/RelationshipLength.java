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
package org.neo4j.cypherdsl.core.internal;

import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.neo4j.cypherdsl.core.ast.Visitable;

/**
 * Expresses the length of a relationship.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = INTERNAL, since = "1.0")
public final class RelationshipLength implements Visitable {

	private final @Nullable Integer minimum;

	private final @Nullable Integer maximum;

	private final boolean unbounded;

	/**
	 * Creates an unbounded {@link RelationshipLength}.
	 *
	 * @return The new length definition
	 */
	public static @NotNull RelationshipLength unbounded() {
		return new RelationshipLength(null, null);
	}

	/**
	 * Creates a {@link RelationshipLength} with given minimum and maximum values.
	 *
	 * @param minimum Minimum length
	 * @param maximum Maximum length
	 * @return The new length definition
	 */
	public static @NotNull RelationshipLength of(Integer minimum, Integer maximum) {
		return new RelationshipLength(minimum, maximum);
	}

	private RelationshipLength(@Nullable Integer minimum, @Nullable Integer maximum) {
		this.minimum = minimum;
		this.maximum = maximum;
		this.unbounded = minimum == null && maximum == null;
	}

	/**
	 * @return Minimum number of hops to match.
	 */
	@API(status = INTERNAL)
	public @Nullable Integer getMinimum() {
		return minimum;
	}

	/**
	 * @return Maximum number of hops to match.
	 */
	@API(status = INTERNAL)
	public @Nullable Integer getMaximum() {
		return maximum;
	}

	/**
	 * @return True if neither minimum nor maximum number of hops are set.
	 */
	@API(status = INTERNAL)
	public boolean isUnbounded() {
		return unbounded;
	}
}
