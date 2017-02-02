package org.phyloref.phyloderef;

import java.io.*;
import java.util.*;
import org.semanticweb.owlapi.*;
import org.semanticweb.owlapi.apibinding.*;
import org.semanticweb.owlapi.model.*;

import spark.ModelAndView;
import spark.template.mustache.MustacheTemplateEngine;

import static spark.Spark.*;

class OWLFile {
    private File file;
    private OWLOntology ontology;

    public OWLFile(File f) throws OWLException { 
        file = f;

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        // Since our Phyloreferencing ontology hasn't been published,
        // we redirect its IRI to the real URI here. We can fix this
        // once we publish that ontology.
        manager.addIRIMapper(new OWLOntologyIRIMapper() {
            public IRI getDocumentIRI(IRI ontologyIRI) {
                if(ontologyIRI.equals(IRI.create("http://phyloinformatics.net/phyloref.owl"))) {
                    return IRI.create("https://raw.githubusercontent.com/hlapp/phyloref/master/phyloref.owl");
                }

                return null;
            }
        });

        ontology = manager.loadOntologyFromOntologyDocument(f);
    }

    public OWLOntology getOntology() { return ontology; }
    public File getFile() { return file; }
}

public class PhyloDeRef {
    public static void main(String[] args) {
        OWLFile crowl_et_al;
        try {
            crowl_et_al = new OWLFile(new File("examples/journal.pone.0094199.s022.owl"));
        } catch(OWLException e) {
            throw new RuntimeException("Could not load source files: " + e);
        }

        get("/file/crowl_et_al", (req, res) -> new ModelAndView(crowl_et_al, "owlfile.mustache"), new MustacheTemplateEngine());
    }
}
