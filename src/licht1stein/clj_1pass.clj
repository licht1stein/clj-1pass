(ns licht1stein.clj-1pass
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [clojure.string :as str]))

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

(defn op-request
  "Makes a request to 1Password Connect API.
  "
  [{:keys [method connect-url token endpoint]}]
  {:pre [(not-empty token)
         (not-empty connect-url)]}
  (-> (client/request (-> {:url (str connect-url endpoint)
                           :method method}
                          (merge (auth-header token))))
      body->edn))

(defn op-get
  "Makes a GET request to the 1Password Connect API
  opts must be a map that include's all the fields from Config record."
  [opts endpoint]
  (op-request (merge opts {:endpoint endpoint :method :get})))

(defn op-get-body
  "Uses get to make a request and returns only the value of :body"
  [opts endpoint]
  (:body (op-get opts endpoint)))

(defn op-post
  "Makes a POST request to the 1Password Connect API"
  [endpoint opts body]
  (op-request (merge opts {:endpoint endpoint :method :post :body body})))

(defn- deref-config []
  (when (empty? @*config)
    (throw (ex-info "To use the function without passing config argument you must first store it using store-config" {})))
  @*config)

(defn op-get-activity
  "List API activity. Returns a list of maps with all API activity.

  GET /v1/activity"
  ([]
   (op-get-activity (deref-config)))
  ([config]
   (op-get-body config "/activity")))

(defn op-list-vaults
  "List all vaults GET /v1/vaults"
  ([]
   (op-list-vaults (deref-config)))
  ([config]
   (op-get-body config "/vaults")))

(defn  get-vault-details
  "GET /v1/vaults/{vaultUUID}"
  ([vault-id]
   (get-vault-details (deref-config) vault-id))
  ([config vault-id]
   (op-get-body config (str "/vaults/" vault-id))))

(defn list-vault-items
  "GET /v1/vaults/{vaultUUID}/items"
  ([vault-id]
   (list-vault-items (deref-config) vault-id))
  ([config vault-id]
   (op-get-body config (str "/vaults/" vault-id "/items"))))

(defn get-item-details
  "GET /v1/vaults/{vaultUUID}/items/{itemUUID}"
  ([item]
   (get-item-details (deref-config) item))
  ([config {:keys [vault-id item-id]}]
   (op-get-body config (str "/vaults/" vault-id "/items/" item-id))))


(defn shorten-item [i]
  {:title (:title i)
   :vault-id (get-in i [:vault :id])
   :item-id (:id i)})

(defn short-items
  "Takes list of items and returns only the essentials"
  [items]
  (map shorten-item items))

(defn all-items
  "Gets all items from all available vaults"
  ([]
   (all-items (deref-config)))
  ([config]
   (let [vaults (map :id (op-list-vaults config))]
     (-> (map #(list-vault-items config %) vaults)
         flatten))))

(defn find-by-title
  "Searches all items for occurrence of item-title in title. Ignores case."
  ([item-title]
   (find-by-title (deref-config) item-title))
  ([config item-title]
   (->> (all-items config)
        (filter #(str/includes? (str/lower-case (:title %)) (str/lower-case item-title))))))
