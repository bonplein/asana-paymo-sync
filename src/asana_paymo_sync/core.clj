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

(defn get-paymo-tasklist
  "Extracts a single tasklist entry from the paymo project map"
  [project tasklist-id]
  (->> project
       :tasklists
       (filter #(= tasklist-id (:id %)))
       first))

(defn synchronize
  "Master task which is responsible for syncing Asana with Paymo"
  [project]
  (doall
   (let [paymo-project (->> project
                            :id
                            vget-project
                            paymo/project)]
     (doseq [section-with-tasks (asana-extend-project project)
             :let [section (first (first section-with-tasks))
                   tasks   (last section-with-tasks)]]
       (do
         (if-let [tasklist-id (->> section
                                   :id
                                   database/get-tuple)]
           ;; check if the name needs to be changed
           (let [tasklist (get-paymo-tasklist paymo-project tasklist-id)]
             (if (not
                  (= (:name tasklist)
                     (:name section)))
               (do
                 (println (pr-str (paymo/rename-tasklist (:name section) tasklist-id)))
                 (println "Tasklist/Section names were different and are now the same again."))
               (println "Tasklist/Section names are the same, everything ok.")))
           ;; tasklist does not exist yet, therefore create it.
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
                           (apply str (drop-last (:name section))))))))))))

(defn synchronize-all
  "Synchronize all projects"
  []
  (doseq [project (asana-projects-to-sync)]
    (do
      (println (str "Sync project: " (:name project)))
      (synchronize project)
      (println (str "Synced project: " (:name project))))))
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

