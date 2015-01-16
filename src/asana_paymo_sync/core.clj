(ns asana-paymo-sync.core
  (:require [asana-paymo-sync.asana :as asana]
            [asana-paymo-sync.paymo :as paymo]
            [asana-paymo-sync.config :as config]
            [asana-paymo-sync.database :as database]
            [clojure.pprint :as pprint])
  (:gen-class))

;;
;; sync from asana to paymo
;;

(defn vget-project
  "Get the Paymo project-id by the Asana project-id"
  [asana-id]
  (get config/projects-mapping asana-id))

(defn mapped?
  "Returns true if the Asana project has been mapped to a Paymo project."
  [project]
  (->> project
       :id
       vget-project
       nil?
       not))

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

(defn synchronize
  "Master task which is responsible for syncing Asana with Paymo"
  [project]
  (doall
   (for [section-with-tasks (asana-extend-project project)
         :let [section (first (first section-with-tasks))
               tasks   (last section-with-tasks)]]
     (do
       (if-let [paymo-id (->> section
                              :id
                              database/get-tuple)]
         (println "Tasklist is already there, make a deep equality check")
         (do
           (->>
            ;; create the tasklist in Paymo
            (-> (paymo/create-tasklist (:name section) (vget-project (:id project)))
                (extract-and-normalize-paymo [:id])
                first
                :id)
            ;; save the returned tasklist-id from Paymo together with the section-id
            (database/set-tuple (:id section)))
           (println (str "Created Tasklist: "
                         (apply str (drop-last (:name section)))))))))))
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

