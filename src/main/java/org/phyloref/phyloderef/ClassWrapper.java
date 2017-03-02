package org.phyloref.phyloderef;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.ShortFormProvider;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxObjectRenderer;

/**
 * A ClassWrapper wraps a single OWL class within an OWLFile.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class ClassWrapper implements Comparable {
	private OWLOntology ontology;
	private Reasoner reasoner;
	private OWLClass clazz;

	public ClassWrapper(OWLOntology ont, Reasoner r, OWLClass c) {
		ontology = ont;
		reasoner = r;
		clazz = c;
	}

	public Set<OWLClassExpression> getEquivalentClasses() {
		return clazz.getEquivalentClasses(ontology);
	}

	public Set<String> getEquivalentClassesAsManchester() {
		ShortFormProvider entityShortFormProvider = new ShortFormProvider() {
			@Override
			public String getShortForm(OWLEntity entity) {
				// Since we can't get to the annotations in CDAO yet,
				// we'll hard-code some of them here.
				Map<IRI, String> shortForms = new HashMap<>();
				shortForms.put(IRI.create("http://purl.obolibrary.org/obo/CDAO_0000140"), "Node");
				shortForms.put(IRI.create("http://purl.obolibrary.org/obo/CDAO_0000144"), "has_Ancestor");
				shortForms.put(IRI.create("http://purl.obolibrary.org/obo/CDAO_0000149"), "has_Child");
				shortForms.put(IRI.create("http://purl.obolibrary.org/obo/CDAO_0000174"), "has_Descendant");
				String name = null;
				// System.err.println("Trying to find a short form for " + entity.toString());
				// Try to find an rdfs:label for this entity.
				OWLOntology ontology_containing_entity = ontology.getOWLOntologyManager().getOntology(entity.getIRI());
				if (ontology_containing_entity != null) {
					for (OWLAnnotation annot : entity.getAnnotations(ontology_containing_entity)) {
						// System.err.println(" - Annotation: " + annot.toString());
						if (annot.getProperty().getIRI().equals(IRI.create("http://www.w3.org/2000/01/rdf-schema#label"))) {
							name = annot.getValue().toString();
						}
					}
				}
				if (name == null && shortForms.containsKey(entity.getIRI())) {
					name = shortForms.get(entity.getIRI());
				}
				if (name == null) {
					name = entity.getIRI().getFragment();
				}
				
				return name.trim();
			}

			@Override
			public void dispose() {
			}
		};
		return getEquivalentClasses().stream().map((OWLClassExpression ce) -> {
			StringWriter writer = new StringWriter();
			ManchesterOWLSyntaxObjectRenderer renderer = new ManchesterOWLSyntaxObjectRenderer(writer, entityShortFormProvider);
			ce.accept(renderer);
			return writer.toString();
		}).collect(Collectors.toSet());
	}

	public OWLClass getOWLClass() {
		return clazz;
	}
	
	public String getShortName() {
		return clazz.getIRI().getFragment();
	}

	public IRI getIRI() {
		return clazz.getIRI();
	}

	@Override
	public int compareTo(Object o) {
		return getShortName().compareTo(((ClassWrapper)o).getShortName());
	}
	
}
