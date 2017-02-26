package org.phyloref.phyloderef;

import java.io.*;
import java.util.*;
import org.semanticweb.owlapi.model.*;

import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;

import static spark.Spark.*;

public class PhyloDeRef {
	private final List<Phylogeny> phylogenies;

	public String getName() { return "PhyloDeRef/" + VERSION; }	
	public static final String VERSION = "0.1";

	public PhyloDeRef() throws OWLException, IOException {
		phylogenies = Arrays.asList(
			new Phylogeny(
				"Crowl et al., 2014: Plasmid + PPR tree",
				new File("examples/journal.pone.0094199.s022.owl"),
				new File("examples/journal.pone.0094199.s022.phylorefs.omn")
			)
		);
	}
	public List<Phylogeny> getPhylogenies() { return phylogenies; }
	
	private static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 4567; //return default port if heroku-port isn't set (i.e. on localhost)
    }
	
    public static void main(String[] args) {
		port(getHerokuAssignedPort());
		
		// Set up the system.
		PhyloDeRef pr;
		
		try {
			pr = new PhyloDeRef();
		} catch(IOException e) {
			throw new RuntimeException("Could not load phyloreferences: " + e);
		} catch(OWLException e) {
			throw new RuntimeException("Could not parse OWL while loading phyloreferences: " + e);
		}
		
		// Set up the index page.
		get("/", (req, res) -> new ModelAndView(pr, "index.ftl"), new FreeMarkerEngine());
		
		pr.getPhylogenies().forEach((owlFile) -> {
			get("/file/" + owlFile.getShortName(), (req, res) -> new ModelAndView(owlFile, "phylogeny.ftl"), new FreeMarkerEngine());
		});
	}
}
