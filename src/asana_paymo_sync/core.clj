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

(defn vget-user
  "Get the Paymo user-id by the Asana user-id"
  [asana-id]
  (get config/users-mapping asana-id))

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

(defn get-paymo-task
  "Extract a single task entry from the paymo project map"
  [project task-id]
  (->> project
       :tasks
       (filter #(= task-id (:id %)))
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
                           (:name section)))))
         ;; get tasklist for which the tasks need to be created
         (let [paymo-tasklist-id (->> section
                                      :id
                                      database/get-tuple)]
           ;; go through the tasks sequentially, either creating or updating them
           (doseq [asana-task tasks]
             (if-let [paymo-task-id (->> asana-task
                                         :id
                                         database/get-tuple)]
               ;; check if the task attributes need to be changed
               (let [original-paymo-task (get-paymo-task paymo-project paymo-task-id)
                     paymo-task (atom original-paymo-task)]
                 (do
                   ;; update the name if it's changed
                   (if (not
                        (= (:name asana-task)
                           (:name @paymo-task)))
                     (do
                       (swap! paymo-task assoc :name (:name asana-task))
                       (println (str "Change the name of paymo-task: " paymo-task-id)))
                     nil)
                   ;; update the assigned user if it's changed
                   (if-let [paymo-user-id (->> asana-task
                                               :assignee
                                               :id
                                               vget-user)]
                     ;; since in paymo multiple users can be assigned to a task we only check for
                     ;; contain? so that manual assignments on paymo's side remain
                     (if (not
                          (contains? (set (:users @paymo-task)) paymo-user-id))
                       (do
                         (swap! paymo-task assoc :users (vector paymo-user-id))
                         (println "Add user to paymo-task: " paymo-task-id))
                       nil))
                   ;; if no assignee is defined make sure that's the case too in paymo
                   (if (and (nil? (:assignee asana-task))
                            (not (empty? (:users @paymo-task))))
                     (do
                       (swap! paymo-task assoc :users nil)
                       (println "Cleared user for paymo-task: " paymo-task-id))
                     nil)
                   ;; if task is completed, then mark it as such in paymo
                   (if (and (= true (:completed asana-task))
                            (not (= true (:complete @paymo-task))))
                     (do
                       (swap! paymo-task assoc :complete true)
                       (println (str "Marked paymo-task: " paymo-task-id " as done")))
                     nil)
                   ;; if task has been reopened, then reopen the task in paymo too
                   (if (and (= false (:completed asana-task))
                            (not (= false (:complete @paymo-task))))
                     (do
                       (swap! paymo-task assoc :complete false)
                       (println (str "Marked paymo-task: " paymo-task-id " as open")))
                     nil)
                   ;; make the update call to paymo if any of the previous checks changed the atom
                   (if (not (= original-paymo-task @paymo-task))
                     (do
                       (paymo/update-task @paymo-task)
                       (println (str "Updated Task: " (:id @paymo-task))))
                     (println (str "Leave the paymo task " paymo-task-id " unchanged")))))
               ;; task does not exist yet, therefore create it.                                                               
               (do
                 (->> 
                  ;; create the task in Paymo
                  (-> (paymo/create-task (:name asana-task)
                                         paymo-tasklist-id
                                         (:id paymo-project))
                      :tasks
                      first
                      :id)
                  ;; save the returned task-id from Paymo together with the Asana task-id
                  (database/set-tuple (:id asana-task)))
                 (println (str "Created Task: "
                               (:name asana-task))))))))))))

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
  [& args-coll]
  (let [args     (vec args-coll)
        provider (get args 0)
        resource (get args 1)]
    (-> (case provider
          "asana" (-> (case resource
                        "projects" (asana/projects)
                        "users"    (asana/users)
                        "Please provide a resource.")
                      (sort-hashmaps))
          "paymo" (-> (case resource
                        "projects" (extract-and-normalize-paymo (paymo/projects)
                                                                [:id :name])
                        "users"    (extract-and-normalize-paymo (paymo/users)
                                                                [:id :email])
                        "Please provide a resource.")
                      (sort-hashmaps))
          "sync" (synchronize-all)
          "sync_asana" (map asana-extend-project (asana-projects-to-sync))
          "sync_paymo" (map #(paymo/project (:id %)) (paymo/projects)))
        (pprint/pprint)
        (println))))

