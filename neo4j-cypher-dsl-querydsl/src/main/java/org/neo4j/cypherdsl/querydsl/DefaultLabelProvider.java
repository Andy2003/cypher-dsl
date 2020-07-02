package org.neo4j.cypherdsl.querydsl;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.querydsl.core.types.EntityPath;

/**
 * The default label provider uses the simple class name as the nodes label to match.
 *
 * @author Michael J. Simons
 */
final class DefaultLabelProvider implements Function<EntityPath<?>, List<String>> {

	@Override
	public List<String> apply(EntityPath<?> entityPath) {

		return Collections.singletonList(entityPath.getRoot().getType().getSimpleName());
	}
}
