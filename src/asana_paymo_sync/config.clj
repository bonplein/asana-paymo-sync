(ns asana-paymo-sync.config
  (:require [environ.core :as environ-env]
            [clojure.string :as str]))

(defn env
  "Override the environment extract with default value"
  [env-variable default]
  (let [ev (environ-env/env env-variable)]
    (if (nil? ev)
      default
      ev)))

(defn parse-mappings
  [env-variable]
  (->> (-> (env env-variable "0,0")
           (str/split #"\,|\;"))
       (partition-all 2)
       (map (fn [m]
              {(Long/parseLong (first m))
               (Long/parseLong (last m))}))
       (apply merge)))

(def asana
  {:api-key (env :asana-api-key "")
   :workspace (env :asana-workspace "")
   :api-url "https://app.asana.com/api/1.0/"})

(def paymo
  {:username (env :paymo-username "")
   :password (env :paymo-password "")
   :api-url "https://app.paymoapp.com/api/"})

;; use the entry ids on both sides
;; PROJECTS_MAPPING=1234123412,1234134;123423415,452452345
(def projects-mapping
  (parse-mappings :projects-mapping))

;; USERS_MAPPING=565465,56454;563257,56861
(def users-mapping
  (parse-mappings :users-mapping))

(def database
  (env :database-url "mysql://root:@localhost/asana_paymo_sync"))

(def dynamodb
  (merge {:access-key (env :aws-access-key "1234")
          :secret-key (env :aws-secret-key "1234")
          :table      (keyword (env :dynamo-table-name "mappings"))}
         (if-let [endpoint (env :dynamo-endpoint nil)]
           {:endpoint endpoint}
           {})))

