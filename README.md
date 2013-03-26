WebScriptUtils for Oracle UCM
=============================

Javascript and CSS utilities for Site Studio websites

- Provides a set of idoc functions which allow rendering of multiple scripts into a single combined and compressed file.
- Environment variables can determine whether scripts are combined or not, meaning you can see the files without compression on a dev environment.

Example
-------
Anywhere in your template you can define a number of scripts.

```html
<!--$ addJs('/cs/groups/webassets/documents/js_main.js', 'main', 5) -->
<!--$ addJs(wcmUrl('resource', 'JS_OTHER'), 'main', 6) -->
```

These can be rendered exactly where you need them (e.g. at the bottom of the body tag)

```html
<!--$ renderJs('main') -->
```

Outputs:

```html
<!-- if WSUCombineScripts and WSUCompressScripts are set to true -->
<script type="text/javascript" src="/cs/resources/wsu/main_EDD6137E_c.js" id="wsu-combined-0-group-main"></script>

<!-- otherwise -->
<script type="text/javascript" src="/cs/groups/webassets/documents/webasset/js_main.js"></script>
<script type="text/javascript" src="/cs/groups/webassets/documents/webasset/js_other.js"></script>
```

Depending on your environment config, these scripts will be rendered as separate script tags or combined into a single, compressed, script.

Idoc Functions
--------------

### addJs(path,[ group,[ priority]])

Adds a script to the queue for rendering in the render scripts clause
- path: the absolute web path (use wcmURL() to return this)
- group: a group name for the scripts for later rendering, can be left blank
- priority: rendering sort order

### renderJs([group])

Renders the scripts added with addScript(), if the server is set to combine, then the scripts will be rendered with
one script tag and will use the combiner service. If the server environment is not configured to
combine scripts each script tag will be rendered individually
- group: The group to render, leave blank to render ungrouped scripts

### addCss(path,[ group,[ priority]])

Similar to the javascript command

### renderCss([group])

Similar to the javascript command

### combineScripts(type, items [, compress])

Render a single tag which uses the combiner to combine scripts (and compress them if the server is set to compress)
- type: content type "text/javascript" or "text/css"
- items: comma separated list of paths e.g. path1,path2
- compress: compress result? (if supplied, overrides environment config)

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
- items: The comma separated list of paths e.g. path1,path2
- compress: true or false (overrides environment variable)
- cache: true or false (default true)

License (MIT)
-------------

Copyright (c) 2012 Tim Stirrat

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.