(ns licht1stein.clj-1pass
  (:require [clj-http.client :as client]
            [cheshire.core :as json]))

(defonce *op (atom {:init false}))

(defn initialize
  "Initializes the *op atom. Specify the Connect API url and token.
  This will allow you to use the helper functions arities without providing
  connect-url and token args.

  You can use the library without initializing the token, using the longer
  arities. This allows you to decide how to store and provide the token
  to the library."
  [connect-url token]
  (swap! *op merge {:token token :connect-url connect-url :init true})
  true)

(defn auth-header
  "Prepares an authorization header map for 1Password Connect REST API."
  [token]
  {:headers {"Authorization" (str "Bearer " token)}})

(defn get
  "Makes a get request to 1Password Connect endpoint"
  ([endpoint]
   (when-not (:init @*op)
     (throw (ex-info "clj-1pass not initialized. Run clj-1pass/initialize first" {})))
   (get (:connect-url @*op) (:token @*op) endpoint))
  ([endpoint connect-url token]
   (let [resp (client/get (str connect-url endpoint) (auth-header token) {:as :clojure})
         edn-body (json/parse-string (:body resp) true)]
     (assoc resp :body edn-body)
     )))

