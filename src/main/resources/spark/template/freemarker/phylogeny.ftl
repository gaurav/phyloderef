<#ftl strip_whitespace=true>

<#import "layout.ftl" as layout>

<#macro drawTreeStartingWith node>
    <li>${node}</li>
    <ul>
        <#list node.children as child>
            <@drawTreeStartingWith child />
        </#list>
    </ul>
</#macro>

<@layout.page title="Phylogeny: ${name}">
<h1>Phylogeny: ${name}</h1>

<p>Return to the <a href="/">main menu</a>, or jump to: <a href="#tree">tree</a>, <a href="#class_membership">class membership</a>, <a href="#properties">properties</a>.</p>

        <h2>Query phyloreference</h2>

        <div>
            <form method="POST" action="./post">
                <textarea name="phyloreferences" rows="30" cols="150">${phyloreferences}</textarea><br>
                <button type="submit">Update</button>
            </form>
        </div>

        <#if errors??>
        <p class="errors">
            ${errors}
        </p>
        </#if>
        
        <h3 id="phylorefs">Phyloreferences</h3>
        
        <p>Jump to phyloref: <#list phylorefs?keys?sort as phyloref><a href="#phyloref_${phyloref?index}">${phyloref.IRI.fragment}</a><#sep>, </#list>.</p>
        
        <ul>
        <#list phylorefs?keys?sort as phyloref>
            <li id="phyloref_${phyloref?index}"><strong>${phyloref.IRI.fragment}</strong></li>
                <ul>
                <#list phyloref.equivalentClassesAsManchester as equivClass>
                    <li><strong>Equivalent to:</strong> ${equivClass}</li>
                </#list>
                    <li>Containing nodes:</li>
                    <ul class="tree">
                        <#list phylorefs?api.get(phyloref) as rootNode>    
                            <@drawTreeStartingWith rootNode />
                        </#list>
                    </ul>
                </ul>
                <br>
        </#list>
        </ul>
            
        <h2>Other visualizations</h2>

        <h3 id="tree">Tree</h3>
        
        <p>Jump to: <#list rootNodes?sort as node><a href="#tree_root_${node?index}">${node}</a><#sep>, </#list>.</p>
        
        <ul>
            <#list rootNodes?sort as node>
                <li id="#tree_root_${node?index}">${node}</a>
                <ul class="tree">
                    <#list node.children as child>
                        <@drawTreeStartingWith child />
                    </#list>
                </ul>
            </#list>
        </ul>
        
        <h3 id="class_membership">Class membership</h3>

        <p>Jump to class: <#list individualsByClass?keys?sort as clazz><a href="#membership_of_class_${clazz?index}">${clazz.shortName}</a><#sep>, </#list>.</p>

        <ul>
            <#list individualsByClass?keys?sort as clazz>
            <li id="membership_of_class_${clazz?index}">${clazz.IRI.fragment}:
            <ul>
                <#list individualsByClass?api.get(clazz)?sort as indiv>
                <li>${indiv}</li>
                </#list>
            </ul>
            </#list>
            </li>
        </ul>

        <h3 id="properties">Properties</h3>

        <ul>
            <li>OWLFile: <tt>${file}</tt>.</li>
            <li>Ontology: <tt>${ontology}</tt>.</li>
            <li>Reasoner: <tt>${reasoner}</tt>.</li>
        </ul>
        
</@layout.page>