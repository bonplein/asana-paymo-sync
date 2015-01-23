(ns asana-paymo-sync.paymo
  (:require [clj-http.client :as client]
            [cheshire.core :refer :all]
            [asana-paymo-sync.config :as config]))

(def authentication-map
  {:basic-auth [(:username config/paymo) (:password config/paymo)]
   :follow-redirects true
   :accept :json
   :content-type :json})

(defn paymo-get
  "Wrapper to include the authentication header for GET calls"
  [url]
  (-> (client/get (str (:api-url config/paymo)
                       url)
                  authentication-map)
      :body
      (parse-string true)))

(defn paymo-post
  "Wrapper to include the authentication header for POST calls"
  [url data]
  (-> (client/post (str (:api-url config/paymo)
                        url)
                   (merge
                    authentication-map
                    {:form-params data}))
      :body
      (parse-string true)))

(defn paymo-put
  "Wrapper to include the authentication header for PUT calls"
  [url data]
  (-> (client/put (str (:api-url config/paymo)
                       url)
                  (merge
                   authentication-map
                   {:form-params data}))
      :body
      (parse-string true)))

(defn clients
  "Retrieve clients from paymo"
  []
  (paymo-get "clients"))

(defn users
  "Retrieve users from paymo"
  []
  (paymo-get "users"))

(defn projects
  "Retrieve projects from paymo"
  []
  (paymo-get "projects"))

(defn project
  "Retrieves a project with its tasklists and tasks from paymo"
  [project-id]
  (->> (str "projects/"
            project-id
            "?include=tasklists,tasks")
       paymo-get
       :projects
       first))

(defn create-tasklist
  [name project-id]
  (paymo-post "tasklists"
              {:name name
               :project_id project-id}))

(defn rename-tasklist
  [name tasklist-id]
  (paymo-put (str
              "tasklists/"
              tasklist-id)
             {:id tasklist-id
              :name name}))

(defn create-task
  [name tasklist-id project-id]
  (paymo-post "tasks"
              {:name name
               :tasklist_id tasklist-id
               :project_id project-id}))

(defn update-task
  [task]
  (paymo-put (str
              "tasks/"
              (:id task))
             (select-keys task
                          [:id :name :users :complete])))
