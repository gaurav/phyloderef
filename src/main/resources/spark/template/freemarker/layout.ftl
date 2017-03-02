<#ftl strip_whitespace = true>

<#macro page title="PhyloDeRef">
<!DOCTYPE html>
<html>
    <head>
        <title>${title}</title>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="stylesheet" type="text/css" href="/static/main.css">
    </head>
    <body>
        <#nested>
    </body>
</html>
</#macro>