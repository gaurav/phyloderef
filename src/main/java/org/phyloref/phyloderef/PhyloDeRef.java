package org.phyloref.phyloderef;

import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxParserFactory;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.*;
import org.semanticweb.owlapi.io.OWLParser;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.NodeSet;

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
    private File fileOntology;
    private String phyloreference = "";
	private OWLOntologyManager manager;
    private OWLOntology ontology;
	private Reasoner reasoner;
	private Set<NodeWrapper> rootNodes;
	private Map<OWLClass, Set<NodeWrapper>> individualsByClass;
	private Map<OWLClass, Set<OWLNamedIndividual>> phylorefs = new HashMap<>();

    public OWLFile(File fOnt, File fPhyloreference) throws OWLException, IOException { 
        fileOntology = fOnt;

        manager = OWLManager.createOWLOntologyManager();

        // Since our Phyloreferencing ontology hasn't been published,
        // we redirect its IRI to the real URI here. We can fix this
        // once we publish that ontology.
        manager.addIRIMapper((IRI ontologyIRI) -> {
			if(ontologyIRI.equals(IRI.create("http://phyloinformatics.net/phyloref.owl"))) {
				return IRI.create("https://raw.githubusercontent.com/hlapp/phyloref/master/phyloref.owl");
			}
			
			return null;
		});
		
		BufferedReader reader = new BufferedReader(new FileReader(fPhyloreference));
		String line;
		while((line = reader.readLine()) != null) {
			phyloreference = phyloreference + "\n" + line;
		}
		phyloreference = phyloreference.trim();
		
		recalculate();
    }
	
	public void recalculate() throws OWLException, IOException {
		// Load fileOntology.
		if(ontology != null)
			manager.removeOntology(ontology);
		
        ontology = manager.loadOntologyFromOntologyDocument(fileOntology);		
		
		// Load phyloreference.
		OWLParser parser = new ManchesterOWLSyntaxParserFactory().createParser(manager);
        parser.parse(new StringDocumentSource(phyloreference), ontology);
	
		reason();
	}
	
	public void reason() {
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
		
		// Phyloreferences are classes that are subclasses of phyloref:Phyloreferences.
		IRI iri_phyloreferences = IRI.create("http://phyloinformatics.net/phyloref.owl#Phyloreference");
		Set<OWLEntity> classes_phyloreferences = ontology.getEntitiesInSignature(iri_phyloreferences);
		
		phylorefs = new HashMap<>();
		
		System.err.println("classes_phyloreferences: " + classes_phyloreferences + ".");
		for(OWLEntity class_phyloreferences: classes_phyloreferences) {
			Set<OWLClass> subClasses = reasoner.getSubClasses(class_phyloreferences.asOWLClass(), false).getFlattened();
			System.err.println("subClasses: " + subClasses + ".");
			for(OWLClass c: subClasses) {
				if(c.isBottomEntity())
					continue;
				
				Set<OWLNamedIndividual> individuals = reasoner.getInstances(c, false).getFlattened();
				System.err.println("individuals: " + individuals + ".");
				
				phylorefs.put(c, individuals);
			}
		}
	}

    public OWLOntology getOntology() { return ontology; }
	public Reasoner getReasoner() { return reasoner; }
    public File getFile() { return fileOntology; }
	public Map<OWLClass, Set<NodeWrapper>> getIndividualsByClass() { return individualsByClass; }
	public Set<NodeWrapper> getRootNodes() { return rootNodes; }
	
	public String getPhyloreferences() { return phyloreference; }
	public void setPhyloreferences(String p) {
		if(p == null) {
			addError("p is null");
			return;
		}
		String oldPhyloreference = phyloreference;
		phyloreference = p;
		
		try {
			recalculate();	
		} catch(OWLException e) {		
			addError("Could not parse phyloreference: " + e);
			phyloreference = oldPhyloreference;
		} catch(IOException e) {
			addError("Could not read phyloreference: " + e);
			phyloreference = oldPhyloreference;
		} catch(Exception e) { // Why isn't this being caught as an OWLException?
			addError("Unknown exception: " + e);
			phyloreference = oldPhyloreference;
		}
	}
	
	public Map<OWLClass, Set<NodeWrapper>> getPhylorefs() {
		Map<OWLClass, Set<NodeWrapper>> results = new HashMap<>();
		for(OWLClass c: phylorefs.keySet()) {
			results.put(c, phylorefs.get(c).stream().map(i -> new NodeWrapper(ontology, reasoner, i)).collect(Collectors.toSet()));
		}
		return results;
	}
	
	StringBuilder errors = new StringBuilder();
	public void addError(String err) {
		errors.append("\n").append(Instant.now()).append(": ").append(err);
	}
	public String getErrors() { 
		String errString = errors.toString(); 
		errors = new StringBuilder();
		return errString;
	}
}

public class PhyloDeRef {
    public static void main(String[] args) {
        OWLFile crowl_et_al;
        try {
            crowl_et_al = new OWLFile(
				new File("examples/journal.pone.0094199.s022.owl"),
				new File("examples/journal.pone.0094199.s022.phylorefs.omn")
			);
        } catch(OWLException e) {
            throw new RuntimeException("Could not parse source files: " + e);
        } catch(IOException e) {
			throw new RuntimeException("Could not load source files: " + e);
		}

		get("/file/crowl_et_al", (req, res) -> { res.redirect("/file/crowl_et_al/"); return res; });
        get("/file/crowl_et_al/", (req, res) -> new ModelAndView(crowl_et_al, "owlfile.mustache"), new HandlebarsTemplateEngine());
		post("/file/crowl_et_al/post", (req, res) -> {
			crowl_et_al.setPhyloreferences(req.queryParams("phyloreferences"));
			res.redirect("/file/crowl_et_al/");
			return res;
		});
	}
}
