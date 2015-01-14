(ns asana-paymo-sync.core
  (:require [asana-paymo-sync.asana :as asana]
            [asana-paymo-sync.paymo :as paymo]
            [asana-paymo-sync.config :as config]
            [clojure.pprint :as pprint]))

(defn -main
  "Command-line entry point."
  [& args]
  (let [provider (first args)
        resource (nth args 1)]
    (-> (case provider
          "asana" (case resource
                    "projects" (asana/projects)
                    "Please provide a resource.")
          "paymo" (case resource
                    "projects" (->> (paymo/projects)
                                   (:projects)
                                   (map #(into (sorted-map)
                                               (select-keys % [:id :name]))))
                    "Please provide a resource."))
        (pprint/pprint)
        (println))))

