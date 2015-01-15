(ns asana-paymo-sync.core
  (:require [asana-paymo-sync.asana :as asana]
            [asana-paymo-sync.paymo :as paymo]
            [asana-paymo-sync.config :as config]
            [clojure.pprint :as pprint])
  (:gen-class))

;;
;; sync from asana to paymo
;;

(defn mapped?
  [project]
  (not
   (nil?
    (get config/projects-mapping (:id project)))))

(defn asana-projects-to-sync
  "Fetches projects from asana and only returns ones that are mapped to paymo projects"
  []
  (->> (asana/projects)
       (seq)
       (filter mapped?)))

(defn asana-extend-project
  "Fetches the sections and tasks for each project"
  [project]
  (->> project
       (:id)
       (asana/tasks-by-project)
       (asana/sections-and-tasks)))

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

