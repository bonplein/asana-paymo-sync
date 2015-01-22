(ns asana-paymo-sync.fixtures
  (:require [asana-paymo-sync.asana :as asana]
            [asana-paymo-sync.paymo :as paymo]
            [clojure.pprint :as pprint]))

(defn write-to-disk
  "Format and write the fixture data to disk"
  [data name]
  (->> data
       (pprint/pprint)
       (with-out-str)
       (spit (str "fixtures/" name ".edn"))))

;; Fetch clients fixture
#_ (write-to-disk (paymo/clients)
                  "paymo_clients")

;; Fetch projects fixture
#_ (write-to-disk (paymo/projects)
                  "paymo_projects")
