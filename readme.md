WebScriptUtils for Oracle UCM
=============================

Javascript and CSS utilities for Site Studio websites

- Provides a set of idoc functions which allow rendering of multiple scripts into a single combined and compressed file.
- Environment variables can determine whether scripts are combined or not, meaning you can see the files without compression on a dev environment.

Example
-------
Anywhere in your template you can define a number of scripts.

```html
<!--$ addScript('JS_MAIN', 'footer', 5) -->
<!--$ addScript('JS_OTHER', 'footer', 6) -->
```

These can be rendered exactly where you need them (e.g. at the bottom of the body tag)

```html
<!--$ renderJavascripts('footer') -->
```

Depending on your environment config, these scripts will be rendered as separate script tags or combined into a single, compressed, script.

Idoc Functions
--------------

### addJavascript(content_id,[ group,[ priority]])

Adds a script to the queue for rendering in the render scripts clause
- group: a group name for the scripts for later rendering, can be left blank
- priority: rendering sort order

### renderJavascripts([group])

Renders the scripts added with addScript(), if the server is set to combine, then the scripts will be rendered with
one script tag and will use the combiner service. If the server environment is not configured to
combine scripts each script tag will be rendered individually
- group: The group to render, leave blank to render ungrouped scripts

### addStyleSheet(content_id,[ group,[ priority]])

Similar to the javascript command

### renderStylesheets([group])

Similar to the javascript command

### combineScripts(type, items [, compress])
- type: content type "text/javascript" or "text/css"
- items: comma separated list of items e.g. ITEM_1,ITEM_A
- compress: compress result? (if supplied, overrides environment config)

Render a single tag which uses the combiner to combine scripts (and compress them if the server is set to compress)

Environment Variables
---------------------
Using these environment variables, you can configure a development environment to leave scripts separated or uncompressed on some environments without changing the template code.

### WSUCombineScripts
This will force the renderStylesheets() and renderJavascripts() functions to combine scripts into a single tag. 
Usually you would set this to true on a production server. Defaults to **false**

`WSUCombineScripts=true`

### WSUCompressScripts

This will force the renderStylesheets/renderJavascripts functions to additionally compress scripts. Defaults to **false**

`WSUCompressScripts=true`

Services
--------
The workhorse service which combines, caches and compresses files:

### WSU_COMBINE_SCRIPTS
##### parameters
- type: text/javascript or text/css
- items: The comma separated list of content items e.g. CONTENT_ID1,CONTENT_ID2
- compress: true or false (overrides environment variable)
- cache: true or false (default true)