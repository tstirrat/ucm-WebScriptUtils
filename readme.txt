Provides a set of site studio helpers to allow combining and compressing javascript and CSS files
for high performance web applications.

- addJavascript(content_id,[ group,[ priority]])
Adds a script to the queue for rendering in the render scripts clause
+ group (group some scripts, for rendering in a chunk)
+ priority (rendering sort order)

- renderJavascripts([group])
Renders the scripts added with addScript(), if the server is set to combine, then the scripts will be rendered with
one script tag and will use the combiner service. If the server environment is not configured to
combine scripts each script tag will be rendered individually
+ group - renders a specific group only, leave blank to render ungrouped scripts

- addStyleSheet(content_id,[ group,[ priority]])
- renderStylesheets([group])
Similar to the javascripts commands

- combineScripts(type = "text/javascript", items = "a,b,etc" [, compress = false])
Render a single tag which uses the combiner to combine scripts (and compress them if the server is set to compress)

Example usage:
<!-- anywhere in a SS template -->
<!--$ addScript('JS_MAIN', 'footer', 5) -->

<!-- in footer -->
<!--$ renderJavascripts('footer') -->

The workhorse service which combines, caches and compresses files:

service:
WSU_COMBINE_SCRIPTS
  parameters:
	type=text/javascript|text/css
	items=CONTENT_ID1,CONTENT_ID2
	compress=true|false (overrides environment variable)
	cache=true|false (overrides environment variable)