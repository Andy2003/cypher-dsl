package org.neo4j.cypherdsl.querydsl;

import java.util.List;
import java.util.function.Function;

import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;

import com.querydsl.core.types.EntityPath;

/**
 * Adapter utils for converting QueryDSL constructs to CypherDSL.
 *
 * @author Michael J. Simons
 */
public final class QueryDSLAdapter {

	public static Node toNode(EntityPath<?> entityPath, Function<EntityPath<?>, List<String>> labelProvider) {

		List<String> allLabels = labelProvider.apply(entityPath);

		if (allLabels == null || allLabels.isEmpty()) {
			throw new IllegalStateException("No labels provided");
		}

		String primaryLabel = allLabels.get(0);
		String[] additionalLabels = allLabels.stream().skip(1).toArray(String[]::new);

		return Cypher.node(primaryLabel, additionalLabels).named(entityPath.getMetadata().getName());
	}
}
