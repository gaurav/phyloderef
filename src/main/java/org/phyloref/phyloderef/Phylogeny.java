package org.phyloref.phyloderef;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxParserFactory;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLParser;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * A Phylogeny wraps a single ontology (in an OWL file) and the pre-built
 phyloreferences that go with it.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class Phylogeny {
	
	private File fileOntology;
	private String phyloreference = "";
	private OWLOntologyManager manager;
	private OWLOntology ontology;
	private Reasoner reasoner;
	private Set<IndividualWrapper> rootNodes;
	private Map<ClassWrapper, Set<IndividualWrapper>> individualsByClass;
	private Map<ClassWrapper, Set<OWLNamedIndividual>> phylorefs = new HashMap<>();
	private String shortName = "";

	public String getShortName() {
		return shortName;
	}
	private String name = "";

	public String getName() {
		return name;
	}

	public Phylogeny(String name, File fOnt, File fPhyloreference) throws OWLException, IOException {
		this.name = name;
		this.shortName = fOnt.getName();
		fileOntology = fOnt;
		manager = OWLManager.createOWLOntologyManager();
		// Since our Phyloreferencing ontology hasn't been published,
		// we redirect its IRI to the real URI here. We can fix this
		// once we publish that ontology.
		manager.addIRIMapper((IRI ontologyIRI) -> {
			if (ontologyIRI.equals(IRI.create("http://phyloinformatics.net/phyloref.owl"))) {
				return IRI.create("https://raw.githubusercontent.com/hlapp/phyloref/master/phyloref.owl");
			}
			return null;
		});
		BufferedReader reader = new BufferedReader(new FileReader(fPhyloreference));
		String line;
		while ((line = reader.readLine()) != null) {
			phyloreference = phyloreference + "\n" + line;
		}
		phyloreference = phyloreference.trim();
		recalculate();
	}

	public void recalculate() throws OWLException, IOException {
		// Load fileOntology.
		if (ontology != null) {
			manager.removeOntology(ontology);
		}
		ontology = manager.loadOntologyFromOntologyDocument(fileOntology);
		// Load phyloreference.
		OWLParser parser = new ManchesterOWLSyntaxParserFactory().createParser(manager);
		parser.parse(new StringDocumentSource(phyloreference), ontology);
		reason();
	}

	public void reason() {
		reasoner = new Reasoner(ontology);
		individualsByClass = new HashMap<>();
		for (OWLClass c : ontology.getClassesInSignature()) {
			// Ignore owl:Thing
			if (c.isTopEntity()) {
				continue;
			}
			individualsByClass.put(
				new ClassWrapper(ontology, reasoner, c), 
				reasoner.getInstances(c, false).getFlattened().stream().map((OWLNamedIndividual indiv) -> new IndividualWrapper(ontology, reasoner, indiv)).collect(Collectors.toSet())
			);
		}
		rootNodes = new HashSet<>();
		rootNodes.addAll(
			ontology.getEntitiesInSignature(IRI.create("http://phyloinformatics.net/phylo/" + getShortName().replaceAll("\\.owl$", "") + "#Node_1")).stream()
				.map((OWLEntity e) -> new IndividualWrapper(ontology, reasoner, e.asOWLNamedIndividual()))
				.collect(Collectors.toSet())
		);
		
		// Phyloreferences are classes that are subclasses of phyloref:Phyloreferences.
		IRI iri_phyloreferences = IRI.create("http://phyloinformatics.net/phyloref.owl#Phyloreference");
		Set<OWLEntity> classes_phyloreferences = ontology.getEntitiesInSignature(iri_phyloreferences);
		phylorefs = new HashMap<>();
		//System.err.println("classes_phyloreferences: " + classes_phyloreferences + ".");
		for (OWLEntity class_phyloreferences : classes_phyloreferences) {
			Set<OWLClass> subClasses = reasoner.getSubClasses(class_phyloreferences.asOWLClass(), false).getFlattened();
			//System.err.println("subClasses: " + subClasses + ".");
			for (OWLClass c : subClasses) {
				if (c.isBottomEntity()) {
					continue;
				}
				Set<OWLNamedIndividual> individuals = reasoner.getInstances(c, false).getFlattened();
				//System.err.println("individuals: " + individuals + ".");
				phylorefs.put(new ClassWrapper(ontology, reasoner, c), individuals);
			}
		}
	}

	public OWLOntology getOntology() {
		return ontology;
	}

	public Reasoner getReasoner() {
		return reasoner;
	}

	public File getFile() {
		return fileOntology;
	}

	public Map<ClassWrapper, Set<IndividualWrapper>> getIndividualsByClass() {
		return individualsByClass;
	}

	public Set<IndividualWrapper> getRootNodes() {
		return rootNodes;
	}

	public String getPhyloreferences() {
		return phyloreference;
	}

	public void setPhyloreferences(String p) {
		if (p == null) {
			addError("p is null");
			return;
		}
		String oldPhyloreference = phyloreference;
		phyloreference = p;
		try {
			recalculate();
		} catch (OWLException e) {
			addError("Could not parse phyloreference: " + e);
			phyloreference = oldPhyloreference;
		} catch (IOException e) {
			addError("Could not read phyloreference: " + e);
			phyloreference = oldPhyloreference;
		} catch (Exception e) {
			// Why isn't this being caught as an OWLException?
			addError("Unknown exception: " + e);
			phyloreference = oldPhyloreference;
		}
	}

	public Map<ClassWrapper, Set<IndividualWrapper>> getPhylorefs() {
		Map<ClassWrapper, Set<IndividualWrapper>> results = new HashMap<>();
		for (ClassWrapper c : phylorefs.keySet()) {
			results.put(c, phylorefs.get(c).stream()
				// .filter((OWLNamedIndividual i) -> !i.getIRI().getFragment().endsWith("_expected"))
				.map((OWLNamedIndividual i) -> new IndividualWrapper(ontology, reasoner, i))
				.collect(Collectors.toSet()));
		}
		return results;
	}
	
	public List<ClassWrapper> getPhylorefsSorted() {
		return phylorefs.keySet().stream().sorted().collect(Collectors.toList());
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
