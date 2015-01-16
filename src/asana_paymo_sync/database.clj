(ns asana-paymo-sync.database
  (:require [clojure.java.jdbc :as jdbc]
            [yesql.core :refer :all]
            [asana-paymo-sync.config :as config]))

;; insert mapping (:asana :paymo)
(defquery insert-mapping! "asana_paymo_sync/sql/insert_mapping.sql")

;; get :paymo by :asana
(defquery paymo-by-asana "asana_paymo_sync/sql/paymo_by_asana.sql")

(defn get-tuple
  "Get the correlating Paymo key"
  [asana-key]
  (-> (paymo-by-asana config/database asana-key)
      first
      :paymo))

(defn set-tuple
  "Set the Asana and correlating Paymo key"
  [asana-key paymo-key]
  (insert-mapping! config/database asana-key paymo-key))
