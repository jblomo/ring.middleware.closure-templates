(ns ring.middleware.closure-templates.test
  (:use ring.middleware.closure-templates
        clojure.test)
  (:require [clojure.java.io :as jio]))

; note: globals must be primitive types, so can't use java.lang.Long boxed
(def globals {:GLOBAL_STR "global string" :GLOBAL_INT "42" :GLOBAL_BOOL true})

(defn hw [req]
  {:status 200 :body {:template "soy.examples.simple.helloWorld"}})

(defn hn [req]
  {:status 200 :body {:name "Jim" :template "soy.examples.simple.helloName"}})

; chain together templates using the same data map
(defn hw-hn [req]
  {:status 200 :body {:name "Jim" :template ["soy.examples.simple.helloWorld" "soy.examples.simple.helloName"]}})

; chain together templates using different data maps
(defn hn-hw [req]
  {:status 200 :body [{:name "Jim" :template "soy.examples.simple.helloName"} {:template "soy.examples.simple.helloWorld"}]})

; turtles
(defn hn-hw-hn-hw [req]
  {:status 200 :body [{:name "Jim" :template ["soy.examples.simple.helloName" "soy.examples.simple.helloWorld"]}
                      {:name "Jim" :template ["soy.examples.simple.helloName" "soy.examples.simple.helloWorld"]}]})

(defn newfile [req]
  {:status 200 :body {:name "New" :template "soy.examples.newfile.helloName"}})

(def static-hw (wrap-templates hw (jio/resource "examples") {:globals globals}))
(def static-hn (wrap-templates hn (jio/resource "examples") {:globals globals}))
(def static-hw-hn (wrap-templates hw-hn (jio/resource "examples") {:globals globals}))
(def static-hn-hw (wrap-templates hn-hw (jio/resource "examples") {:globals globals}))
(def static-hn-hw-hn-hw (wrap-templates hn-hw-hn-hw (jio/resource "examples") {:globals globals}))

(def dynamic-hw (wrap-templates hw (jio/resource "examples") {:globals globals :dynamic true}))
(def dynamic-hn (wrap-templates hn (jio/resource "examples") {:globals globals :dynamic true}))
(def dynamic-newfile (wrap-templates newfile (jio/resource "examples") {:globals globals :dynamic true}))


(deftest test-static-hw
  (let [{:keys [status body]}
         (static-hw {:request-method :get :uri "/" :headers {}})]
    (is (= 200 status))
    (is (= ["Hello world!"] body))))

(deftest test-static-hn
  (let [{:keys [status body]}
         (static-hn {:request-method :get :uri "/" :headers {}})]
    (is (= 200 status))
    (is (= ["Hello Jim!"] body))))

(deftest test-static-hn-hw
  (let [{:keys [status body]}
         (static-hn-hw {:request-method :get :uri "/" :headers {}})]
    (is (= 200 status))
    (is (= ["Hello Jim!" "Hello world!"] body))))

(deftest test-static-hw-hn
  (let [{:keys [status body]}
         (static-hw-hn {:request-method :get :uri "/" :headers {}})]
    (is (= 200 status))
    (is (= ["Hello world!" "Hello Jim!"] body))))

(deftest test-static-hn-hw-hn-hw
  (let [{:keys [status body]}
         (static-hn-hw-hn-hw {:request-method :get :uri "/" :headers {}})]
    (is (= 200 status))
    (is (= ["Hello Jim!" "Hello world!" "Hello Jim!" "Hello world!"] body))))

(deftest test-dynamic-edit-hw
  (let [{:keys [status body]}
         (dynamic-hw {:request-method :get :uri "/" :headers {}})]
    (is (= 200 status))
    (is (= ["Hello world!"] body)))

  (let [simple-soy (jio/resource "examples/simple.soy")
        contents (slurp simple-soy)
        hola (.replaceAll contents "Hello" "Hola")]
    (try
      (spit simple-soy hola)
      (let [{:keys [status body]}
            (dynamic-hw {:request-method :get :uri "/" :headers {}})]
        (is (= 200 status))
        (is (= ["Hola world!"] body)))
      (finally (spit simple-soy contents)))))

(deftest test-dynamic-newfile-hn
  (let [newfile-soy (java.io.File. (.getPath (jio/resource "examples")) "newfile.soy")]
    (.delete newfile-soy)

    (is (thrown? com.google.template.soy.tofu.SoyTofuException
                 (dynamic-newfile {:request-method :get :uri "/" :headers {}})))

    (try
      (spit newfile-soy "
{namespace soy.examples.newfile}
/**
 * Says hello to a person (or to the world if no person is given).
 * @param? name The name of the person to say hello to.
 */
{template .helloName}
  {if hasData() and $name}
    {msg desc=\"Says hello to a person.\"}
      Hello {$name}!
    {/msg}
  {else}
    {call .helloWorld /}
  {/if}
{/template}
                        ")

      (let [{:keys [status body]}
            (dynamic-newfile {:request-method :get :uri "/" :headers {}})]
        (is (= 200 status))
        (is (= ["Hello New!"] body)))
      (finally (.delete newfile-soy)))))

