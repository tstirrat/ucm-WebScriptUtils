WebScriptUtils UCM Component
============================

Provides a set of site studio helpers to allow combining and compressing javascript and 
CSS files for high performance web applications.

Site Studio Extensions
----------------------

### addJavascript(content_id, group, priority)
Adds a script to the queue for rendering in the render scripts clause
> content_id: the content id of the script
> group: group some scripts, for rendering in a chunk
> priority: rendering sort order for scripts that have dependencies

### renderJavascripts(group)
Renders the scripts added with addScript(), if the server is set to combine, then the 
scripts will be rendered with one script tag and will use the combiner service. If the 
server environment is not configured to combine scripts each script tag will be rendered individually
> group: renders a specific group only, leave blank to render ungrouped scripts

### addStyleSheet(content_id, group, priority)
Similar to the javascripts commands

### renderStylesheets(group)
Similar to the javascripts commands

### combineScripts(type, items, compress)
Render a single tag which uses the combiner to combine scripts (and compress them if the server is set to compress)
> type: the type of scripts (determines which compressor to use)
> items: the comma separated list of content items
> compress: override the environment compression parameter

Example usage:

	<!-- anywhere in a SS template -->
	<!--$ addScript('JS_MAIN', 'footer', 5) -->
	
	<!-- in footer -->
	<!--$ renderJavascripts('footer') -->

Services
--------

The workhorse service which combines, caches and compresses files:

### WSU\_COMBINE_SCRIPTS
  parameters:
> type: text/javascript or text/css
> items: The comma separated list of content ids
> compress: (optional) overrides environment variable
> cache: (optional) overrides environment variable