(ns gs-clj.request
  (:require
   [clojure.string :as s]
   [clojure.java.io :as io]
   [taoensso.timbre :as log]
   [gs-clj.gemini :refer [clrf max-request-bytes]]
   [gs-clj.headers :as headers]
   [gs-clj.utils :refer [byte-len-within pick]]
   [lambdaisland.uri :as uri]))

(defn- is-ip-address? [s]
  (not (nil? (re-matches #"\d+\.\d+\.\d+\.\d+" s))))

; REQUEST
(defn from-str [s]
  (when (s/ends-with? s clrf)
    (let [u (uri/parse (s/trim s))
          uri (when (and
                     (byte-len-within s max-request-bytes)
                     (= (:scheme u) "gemini")
                     (every? nil? (vec (pick u :user :password :fragment)))
                     ((comp not is-ip-address?) (:host u)))
                (dissoc u :user :password :fragment))]
      (when (not (nil? uri))
        {:scheme (:scheme uri)
         :host (:host uri)
         :port (:port uri)
         :path (if (nil? (:path uri)) "/" (:path uri))
         :query (:query uri)}))))

; RESPONSE
(defn- resolve-file-path
  [^String uri-path]
  (condp = uri-path
    "/" "/index.gmi"
    uri-path))

(defn- get-file-body [file-path]
  (log/debug "Getting file at:", file-path)
  (slurp (io/resource (str "public" file-path))))

; TODO: Not implemented
(defn- handler-type
  "get the general class of response handler type"
  [^String path]
  (cond
    false :input
    ; TODO; not sure
    false :client-certifiates
    :else :resource))

; TODO: Not-implemented
(defn- handle-input-req [_req]
  {:header (headers/input)
   :body nil})

; TODO: Not-implemented
(defn- handle-resource-req [{:keys [path]}]
  {:header (headers/success "text/gemini")
   :body (get-file-body (resolve-file-path path))})

; TODO: Not-implemented
(defn- handle-client-certificates-req [_req]
  {:header (headers/certificate-not-valid)
   :body nil})

(def req-handlers
  {:input handle-input-req
   :client-certifiates handle-client-certificates-req
   :resource handle-resource-req})

(defn handle!
  "Handles a gemini request, returns a gemini response"
  [req]
  (let [handler-fn (get req-handlers (handler-type (:path req)))
        res (handler-fn req)]
    {:header (headers/to-str (:header res))
     :body (:body res)}))
