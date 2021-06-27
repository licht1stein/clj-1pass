(ns licht1stein.clj-1pass
  (:require [clj-http.client :as client]
            [cheshire.core :as json]))

(defonce ^:private *config (atom nil))

(defrecord Config [connect-url token])

(defn store-config
  "Stores an instance of Config in an atom to allow usage of request functions without
  passing the config arg (shorter arities)."
  [config]
  (reset! *config config))

(defn make-config
  "Creates a new instance of Config map"
  [connect-url token]
  (->Config connect-url token))

(defn make-and-store-config
  "Creates a new Config and stores it in an atom."
  [connect-url token]
  (store-config (make-config connect-url token))
  true)

(defn- auth-header
  "Prepares an authorization header map for 1Password Connect REST API."
  [token]
  {:headers {"Authorization" (str "Bearer " token)}})

(defn- body->edn
  "Converts response body to Clojure map"
  [resp]
  (assoc resp :body (json/parse-string (:body resp) true)))

(defn request
  "Makes a request to 1Password Connect API.
  "
  [{:keys [method connect-url token endpoint]}]
  {:pre [(not-empty token)
         (not-empty connect-url)]}
  (-> (client/request (-> {:url (str connect-url endpoint)
                           :method method}
                          (merge (auth-header token))))
      body->edn))

(defn get
  "Makes a GET request to the 1Password Connect API
  opts must be a map that include's all the fields from Config record."
  [opts endpoint]
  (request (merge opts {:endpoint endpoint :method :get})))

(defn get-body
  "Uses get to make a request and returns only the value of :body"
  [opts endpoint]
  (:body (get opts endpoint)))

(defn post
  "Makes a POST request to the 1Password Connect API"
  [endpoint opts body]
  (request (merge opts {:endpoint endpoint :method :post :body body})))

(defn- deref-config []
  (when (empty? @*config)
    (throw (ex-info "To use the function without passing config argument you must first store it using store-config" {})))
  @*config)

(defn get-activity
  "List API activity. Returns a list of maps with all API activity.

  GET /v1/activity"
  ([]
   (get-activity (deref-config)))
  ([config]
   (get-body config "/activity")))

(defn list-vaults
  "List vaults GET /v1/vaults"
  ([]
   (list-vaults (deref-config)))
  ([config]
   (get-body config "/vaults")))

(defn  get-vault-details
  "GET /v1/vaults/{vaultUUID}"
  ([vault-id]
   (get-vault-details (deref-config) vault-id))
  ([config vault-id]
   (get-body config (str "/vaults/" vault-id))))

(defn list-vault-items
  "GET /v1/vaults/{vaultUUID}/items"
  ([vault-id]
   (list-vault-items (deref-config) vault-id))
  ([config vault-id]
   (get-body config (str "/vaults/" vault-id "/items"))))

(defn get-item-details
  "GET /v1/vaults/{vaultUUID}/items/{itemUUID}"
  ([vault-id item-id]
   (get-item-details (deref-config) vault-id item-id))
  ([config vault-id item-id]
   (get-body config (str "/vaults/" vault-id "/items/" item-id))))
