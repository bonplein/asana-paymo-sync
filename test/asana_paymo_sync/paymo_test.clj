(ns asana-paymo-sync.paymo-test
  (:require [clojure.test :refer :all]
            [asana-paymo-sync.paymo :refer :all]
            [clj-http.fake :refer :all]
            [cheshire.core :refer :all]))

(deftest fetch-projects
  (is (= (with-fake-routes
           {"https://app.paymoapp.com/api/projects" (fn [request]
                                                      {:status 200
                                                       :headers {:accept :json}
                                                       :body (generate-string
                                                              {:projects [{:name "Projekt 1"}
                                                                          {:name "Projekt 2"}]})})}
           (-> (projects)
               :projects
               count))
         2)))

