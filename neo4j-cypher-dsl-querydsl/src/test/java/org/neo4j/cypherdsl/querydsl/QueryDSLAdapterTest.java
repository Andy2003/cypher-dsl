package org.neo4j.cypherdsl.querydsl;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.NodeLabel;
import org.neo4j.cypherdsl.core.SymbolicName;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.dsl.EntityPathBase;

class QueryDSLAdapterTest {

	@Test
	public void toNodeShouldWorkWithOneLabel() {

		EntityPath<?> s = new EntityPathBase<>(String.class, "s");
		Node n = QueryDSLAdapter.toNode(s, new DefaultLabelProvider());

		n.accept(segment -> {
			if (segment instanceof Node) {
				return;
			} else if (segment instanceof SymbolicName) {
				assertThat(segment).extracting("value").isEqualTo("s");
			} else if (segment instanceof NodeLabel) {
				assertThat(segment).extracting("value").isEqualTo("String");
			} else {
				Assertions.fail("Unexpected segment");
			}
		});
	}

	@Test
	public void toNodeShouldWorkWithMultipleLabels() {

		EntityPath<?> s = new EntityPathBase<>(String.class, "s");
		Node n = QueryDSLAdapter.toNode(s, e -> Arrays.asList("L", "A2"));

		List<String> labels = new ArrayList<>();

		n.accept(segment -> {
			if (segment instanceof Node) {
				return;
			} else if (segment instanceof SymbolicName) {
				assertThat(segment).extracting("value").isEqualTo("s");
			} else if (segment instanceof NodeLabel) {
				labels.add(((NodeLabel) segment).getValue());
			} else {
				Assertions.fail("Unexpected segment");
			}
		});
		assertThat(labels).containsExactly("L", "A2");
	}

	@Test
	public void toNodeShouldRequireLabel() {

		EntityPath<?> s = new EntityPathBase<>(String.class, "s");
		assertThatIllegalStateException().isThrownBy(() -> QueryDSLAdapter.toNode(s, e -> Collections.emptyList()));
		assertThatIllegalStateException().isThrownBy(() -> QueryDSLAdapter.toNode(s, e -> null));
	}
}