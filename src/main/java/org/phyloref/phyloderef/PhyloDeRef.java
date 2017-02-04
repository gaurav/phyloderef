package org.phyloref.phyloderef;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.*;
import org.semanticweb.owlapi.apibinding.*;
import org.semanticweb.owlapi.model.*;

import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;

import static spark.Spark.*;

class NodeWrapper {
	private OWLOntology ontology;
	private Reasoner reasoner;
	private OWLIndividual node;
	public IRI iri_CDAO_has_Child = IRI.create("http://purl.obolibrary.org/obo/CDAO_0000149");
	
	public NodeWrapper(OWLOntology ont, Reasoner r, OWLIndividual n) {
		ontology = ont;
		reasoner = r;
		node = n;
	}
	
	public Set<NodeWrapper> getChildren() {
		Set<NodeWrapper> children = new HashSet<>();
		Set<OWLObjectPropertyAssertionAxiom> objectPropertyAssertionAxioms = ontology.getObjectPropertyAssertionAxioms(node);

		for(OWLObjectPropertyAssertionAxiom axiom: objectPropertyAssertionAxioms) {
			if(axiom.getProperty().asOWLObjectProperty().getIRI().equals(iri_CDAO_has_Child))
				children.add(new NodeWrapper(ontology, reasoner, axiom.getObject()));
		}
		
		return children;
	}
	
	@Override
	public String toString() {
		return node.asOWLNamedIndividual().getIRI().getFragment();
	}
}

class OWLFile {
    private File file;
    private OWLOntology ontology;
	private Reasoner reasoner;
	private Set<NodeWrapper> rootNodes;
	private Map<OWLClass, Set<NodeWrapper>> individualsByClass;

    public OWLFile(File f) throws OWLException { 
        file = f;

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        // Since our Phyloreferencing ontology hasn't been published,
        // we redirect its IRI to the real URI here. We can fix this
        // once we publish that ontology.
        manager.addIRIMapper((IRI ontologyIRI) -> {
			if(ontologyIRI.equals(IRI.create("http://phyloinformatics.net/phyloref.owl"))) {
				return IRI.create("https://raw.githubusercontent.com/hlapp/phyloref/master/phyloref.owl");
			}
			
			return null;
		});

        ontology = manager.loadOntologyFromOntologyDocument(f);
		reasoner = new Reasoner(ontology);
		
		individualsByClass = new HashMap<>();
		for(OWLClass c: ontology.getClassesInSignature()) {
            individualsByClass.put(c, reasoner.getInstances(c, false).getFlattened().stream().map(indiv -> new NodeWrapper(ontology, reasoner, indiv)).collect(Collectors.toSet()));
        }
		
		rootNodes = new HashSet<>();
		rootNodes.addAll(
			ontology.getEntitiesInSignature(IRI.create("http://phyloinformatics.net/phylo/journal.pone.0094199.s022#Node_1"))
				.stream().map(e -> new NodeWrapper(ontology, reasoner, e.asOWLNamedIndividual()))
				.collect(Collectors.toSet())
		);
    }

    public OWLOntology getOntology() { return ontology; }
	public Reasoner getReasoner() { return reasoner; }
    public File getFile() { return file; }
	public Map<OWLClass, Set<NodeWrapper>> getIndividualsByClass() { return individualsByClass; }
	public Set<NodeWrapper> getRootNodes() { return rootNodes; }
}

public class PhyloDeRef {
    public static void main(String[] args) {
        OWLFile crowl_et_al;
        try {
            crowl_et_al = new OWLFile(new File("examples/journal.pone.0094199.s022.owl"));
        } catch(OWLException e) {
            throw new RuntimeException("Could not load source files: " + e);
        }

        get("/file/crowl_et_al", (req, res) -> new ModelAndView(crowl_et_al, "owlfile.mustache"), new HandlebarsTemplateEngine());
    }
}
