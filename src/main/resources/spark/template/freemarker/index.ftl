<#ftl strip_whitespace = true>

<#import "layout.ftl" as layout>

<@layout.page>
<h1>${title}</h1>

<p>Here are a list of currently loaded trees:</p>

<ul>
    <#list phylogenies as phylogeny>
        <li><a href="file/${phylogeny.shortName}">${phylogeny.name}</a></li>
    <#else>
        <em>No phylogenies loaded.</em>
    </#list>
</ul>

<p>For more details, please see <a href="https://github.com/gaurav/phyloderef">the Git repo</a>
or <a href="http://www.phyloref.org/">Phyloreferencing Project</a>.
</p>

</@layout.page>

