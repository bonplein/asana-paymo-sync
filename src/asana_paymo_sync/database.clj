(ns asana-paymo-sync.database
  (:require [taoensso.faraday :as far]
            [clojure.java.jdbc :as jdbc]
            [yesql.core :refer :all]
            [asana-paymo-sync.config :as config]))

(defn get-tuple
  "Get the correlating Paymo key"
  [asana-key]
  (-> (far/get-item config/dynamodb
                    :mappings
                    {:asana asana-key})
      :paymo))

(defn set-tuple
  "Set the Asana and correlating Paymo key"
  [asana-key paymo-key]
  (far/put-item config/dynamodb
                :mappings
                {:asana asana-key
                 :paymo paymo-key}))

;; migrate all the mappings from mysql to dynamodb
(defquery select-all "asana_paymo_sync/sql/select_all.sql")
#_(doseq [mapping (select-all config/database)]
    (far/put-item config/dynamodb
                  :mappings
                  mapping))

