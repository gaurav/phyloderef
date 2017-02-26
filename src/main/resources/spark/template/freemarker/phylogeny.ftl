<#ftl strip_whitespace = true>

<#import "layout.ftl" as layout>

<@layout.page title="Phylogeny: ${name}">
<h1>Phylogeny: ${name}</h1>

<p>Jump to: <a href="#tree">tree</a>, <a href="#class_membership">class membership</a>, <a href="#properties">properties</a>.</p>

        <h2>Query phyloreference</h2>

        <form method="POST" action="./post">
            <textarea name="phyloreferences" rows="40" cols="160">${phyloreferences}</textarea>
            <button type="submit">Update</button>
        </form>

        <#if errors??>
        <p class="errors">
            ${errors}
        </p>
        </#if>
            
        <h2>Other visualizations</h2>

        <h3 id="tree">Tree</h3>
        
        <p>Jump to: {{#each rootNodes}}<a href="#tree_root_{{@index}}">{{this}}</a> {{/each}}.</p>

        
        <h3 id="class_membership">Class membership</h3>

        <p>Jump to class: {{#each individualsByClass}}<a href="#membership_of_class_{{@key.IRI.Fragment}}">{{@key.IRI.Fragment}}</a> {{/each}}.</p>

        <ul>
            {{#each individualsByClass}}
            <li id="membership_of_class_{{@key.IRI.Fragment}}">{{@key}}</li>
            <ul>
                {{#each this}}
                <li>{{this}}</li>      
                {{/each}}
            </ul>
            {{/each}}
        </ul>

        <h3 id="properties">Properties</h3>

        <ul>
            <li>OWLFile: <tt>${file}</tt>.</li>
            <li>Ontology: <tt>${ontology}</tt>.</li>
            <li>Reasoner: <tt>${reasoner}</tt>.</li>
        </ul>
        
</@layout.page>

