(ns asana-paymo-sync.asana
  (:require [clj-http.client :as client]
            [cheshire.core :refer :all]
            [asana-paymo-sync.config :as config]))

(def workspace (:workspace config/asana))

(defn asana-get
  "Wrapper to include the basic authentication header"
  [url]
  (-> (client/get (str (:api-url config/asana)
                       url)
                  {:basic-auth [(:api-key config/asana) ""]})
      (:body)
      (parse-string true)))

(defn projects
  "Retrieve all projects"
  []
  (asana-get (str "workspaces/"
                  workspace
                  "/projects")))

