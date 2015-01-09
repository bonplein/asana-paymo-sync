(ns asana-paymo-sync.asana
  (:require [clj-http.client :as client]
            [cheshire.core :refer :all]
            [asana-paymo-sync.config :as config]))

(def workspace (:workspace config/asana))

(defn section?
  [task]
  (= \:
     (last (:name task))))

(defn asana-get
  "Wrapper to include the basic authentication header"
  [url]
  (-> (client/get (str (:api-url config/asana)
                       url)
                  {:basic-auth [(:api-key config/asana) ""]})
      (:body)
      (parse-string true)
      (:data)))

(defn projects
  "Retrieve all projects"
  []
  (asana-get (str "workspaces/"
                  workspace
                  "/projects")))

(defn tasks-by-project
  "Retrieve all tasks by project"
  [project-id]
  (asana-get (str "projects/"
                      project-id
                      "/tasks")))


(defn sections-and-tasks
  "Split the sections and tasks"
  [tasks-from-asana]
  (let [tasks (if (section? (first tasks-from-asana))
                tasks-from-asana
                (cons {:id 0 :name "Default"}
                      tasks-from-asana))]
    (->> tasks
         (partition-by section?)
         (partition-all 2))))

