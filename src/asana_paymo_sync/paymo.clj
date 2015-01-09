(ns asana-paymo-sync.paymo
  (:require [clj-http.client :as client]
            [cheshire.core :refer :all]
            [asana-paymo-sync.config :as config]))

(defn paymo-get
  "Wrapper to include the authentication header for GET calls"
  [url]
  (-> (client/get (str (:api-url config/paymo)
                       url)
                  {:basic-auth [(:username config/paymo) (:password config/paymo)]
                   :follow-redirects true
                   :accept :json})
      (:body)
      (parse-string true)))

(defn clients
  "Retrieve clients from paymo"
  []
  (paymo-get "projects"))

