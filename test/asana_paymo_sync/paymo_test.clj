(ns asana-paymo-sync.paymo-test
  (:require [clojure.test :refer :all]
            [asana-paymo-sync.paymo :refer :all]
            [clj-http.fake :refer :all]
            [cheshire.core :refer :all]))

(defn load-fixture
  "Load and parse the fixture from disk"
  [name]
  (-> (str "fixtures/" name ".edn")
      slurp
      read-string
      generate-string))

(deftest fetch-projects
  (is (= (with-fake-routes
           {"https://app.paymoapp.com/api/projects"
            (fn [request]
              {:status 200
               :headers {:accept :json}
               :body (load-fixture "paymo_projects")})}
           (-> (projects)
               :projects
               count))
         2)))

(deftest fetch-clients
  (is (= (with-fake-routes
           {"https://app.paymoapp.com/api/clients"
            (fn [request]
              {:status 200
               :body (load-fixture "paymo_clients")})}
           (-> (clients)
               :clients
               count))
         2)))

