/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.phyloref.phyloderef;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class NodeWrapper {
	private OWLOntology ontology;
	private Reasoner reasoner;
	private OWLIndividual node;
	public IRI iri_CDAO_has_Child = IRI.create("http://purl.obolibrary.org/obo/CDAO_0000149");

	public NodeWrapper(OWLOntology ont, Reasoner r, OWLIndividual n) {
		ontology = ont;
		reasoner = r;
		node = n;
	}

	public List<NodeWrapper> getChildren() {
		List<NodeWrapper> children = new LinkedList<>();
		Set<OWLObjectPropertyAssertionAxiom> objectPropertyAssertionAxioms = ontology.getObjectPropertyAssertionAxioms(node);
		for (OWLObjectPropertyAssertionAxiom axiom : objectPropertyAssertionAxioms) {
			if (axiom.getProperty().asOWLObjectProperty().getIRI().equals(iri_CDAO_has_Child)) {
				children.add(new NodeWrapper(ontology, reasoner, axiom.getObject()));
			}
		}
		return children;
	}
	
	public String getShortName() {
		return node.toStringID().replaceAll("[^a-zA-Z_]", "_");
	}

	@Override
	public String toString() {
		return node.asOWLNamedIndividual().getIRI().getFragment();
	}
	
}
