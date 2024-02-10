(ns gs-clj.request
  (:require
   [clojure.string :as s]
   [clojure.java.io :as io]
   [taoensso.timbre :as log]
   [gs-clj.gemini :refer [clrf max-request-bytes]]
   [gs-clj.headers :as headers]
   [gs-clj.utils :refer [byte-len-within]]))

; REQUEST
(defn gemini-uri? [s]
  (and
   (byte-len-within s max-request-bytes)
   (s/starts-with? s "gemini://")
   (s/ends-with? s clrf)))

; TODO: parse URI properly
(defn- parse-uri-path
  [uri]
  (s/trim (s/replace uri "gemini://localhost", "")))

(defn from-str
  "Takes a string as defined in [gemini network spec](https://geminiprotocol.net/docs/protocol-specification.gmi) and returns back an request map"
  [s]
  (when (gemini-uri? s)
    {:uri s
     :path (parse-uri-path s)}))

; RESPONSE

(defn- resolve-file-path
  [^String uri-path]
  (condp = uri-path
    "/" "/index.gmi"
    "" "/index.gmi"
    nil "/index.gmi"
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
