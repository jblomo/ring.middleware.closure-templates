# ring.middleware.closure-templates

ALPHA

Wrap responses by rendering Google Closure Templates with the data provided in :body.

This middleware takes a path to your Closure Templates and compiles them.  When
a response is returned, wrap-templates will use the :body value to render the
template specified by :template.

## Usage

    (use 'ring.middleware.closure-templates)
    (defn app [req] {:body {:template "ns.templateName" :templateVar "value"}})
    (def templ-app (wrap-templates app "test-resources"))

See tests for details.

## License

Copyright (C) 2011 Jim Blomo

Distributed under the Eclipse Public License, the same as Clojure.
