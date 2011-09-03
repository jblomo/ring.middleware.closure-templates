(ns ring.middleware.closure-templates
  "Google Closure Template compiling and rendering"
  (:require [clj-soy :as soy]))

(defn- render [tofu data]
  (if (map? data)
    (if (string? (:template data))
      [(soy/render tofu (:template data) data)]
      (map #(soy/render tofu % data) (:template data)))
    (mapcat #(render tofu %) data)))

(defn wrap-templates
  "Wrap an app such that the :body of the response (a map) is rendered as a Closure Template.
  
  templates is the path to the templates, as anything that can be coerced to a File
  opts is a map of options:
    :dynamic - detect and recompile on changes
    :globals - map of global variables to be interpolated at template compile time
  "
  [app templates & [opts]]

  (if (:dynamic opts)
    ; compile tofu inside handler per request
    (fn [req]
      (let [{body :body :as result} (app req)
            tofu (soy/from-files templates (:globals opts))]
        (assoc result :body (render tofu body))))

    ; else compile tofu once at setup
    (let [tofu (soy/from-files templates (:globals opts))]
      (fn [req]
        (let [{body :body :as result} (app req)]
          (assoc result :body (render tofu body)))))))
