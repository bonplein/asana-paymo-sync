(ns asana-paymo-sync.core
  (:require [asana-paymo-sync.asana :as asana]
            [asana-paymo-sync.paymo :as paymo]
            [asana-paymo-sync.config :as config]
            [clojure.pprint :as pprint]))

(defn sort-hashmaps
  "Makes sure the map items from both services are sorted the same way."
  [maps]
  (map #(into (sorted-map) %) maps))

(defn extract-and-normalize-paymo
  "Returns the data in the same format as is being returned from Asana"
  [response required-keys]
  (->> response
       (first)
       (last)
       (map #(select-keys % required-keys))))

(defn -main
  "Command-line entry point."
  [& args]
  (let [provider (first args)
        resource (nth args 1)]
    (-> (case provider
          "asana" (case resource
                    "projects" (asana/projects)
                    "users"    (asana/users)
                    "Please provide a resource.")
          "paymo" (case resource
                    "projects" (extract-and-normalize-paymo (paymo/projects)
                                                            [:id :name])
                    "users"    (extract-and-normalize-paymo (paymo/users)
                                                            [:id :email])
                    "Please provide a resource."))
        (sort-hashmaps)
        (pprint/pprint)
        (println))))

