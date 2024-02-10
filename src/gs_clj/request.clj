(ns gs-clj.request
  (:require
   [clojure.string :as s]
   [gs-clj.gemini :refer [clrf]]
   [clojure.java.io :as io]
   [taoensso.timbre :as log]
   [gs-clj.headers :as headers]))

; REQUEST
(defn gemini-uri? [s]
  (and
   (s/starts-with? s "gemini://")
   (s/ends-with? s clrf)))

(defn from-str
  "Takes a string as defined in [gemini network spec](https://geminiprotocol.net/docs/protocol-specification.gmi) and returns back an request map"
  [s]
  (when (gemini-uri? s)
    {:uri s}))

; RESPONSE
(defn- get-path
  [uri]
  (s/trim (s/replace uri "gemini://localhost", "")))

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
    false :failure
    ; TODO; not sure
    false :client-certifiates
    :else :resource))

; TODO: Not-implemented
(defn- handle-input-req [{:keys [uri]}]
  {:header (headers/input)
   :body nil
   :success? true})

; TODO: Not-implemented
(defn- handle-failure-req [{:keys [uri]}]
  {:header (headers/permanent-failure (str "bad request: " uri))
   :body nil
   :success? true})

; TODO: Not-implemented
(defn- handle-client-certificates-req [{:keys [uri]}]
  {:header (headers/success "text/gemini")
   :body nil
   :success? true})

; TODO: Not-implemented
(defn- handle-resource-req [{:keys [uri]}]
  {:header (headers/success "text/gemini")
   :body (get-file-body (resolve-file-path uri))
   :success? true})

(def req-handlers
  {:input handle-input-req
   :failure handle-failure-req
   :client-certifiates handle-client-certificates-req
   :resource handle-resource-req})

(defn handle!
  "Handles a gemini request, returns a gemini response"
  [^String uri]
  (log/debug "Got a URI! " uri)
  (let [path (get-path uri)
        handler-fn (get req-handlers (handler-type path))]
    (log/debug "Handling path:" path)
    (log/debug "Handler Type:" handler-type)
    (handler-fn path)))
