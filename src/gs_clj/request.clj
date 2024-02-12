(ns gs-clj.request
  (:require
   [clojure.string :as s]
   [taoensso.timbre :as log]
   [gs-clj.gemini :refer [clrf max-request-bytes mime-types]]
   [gs-clj.headers :as headers]
   [gs-clj.utils :refer [byte-len-within pick slurp-bytes]]
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
  [^String uri-path public-path]
  (let [path (condp = uri-path
               "/" "/index.gmi"
               uri-path)]
    (str public-path path)))

; TODO: Not implemented
(defn- handler-type
  "get the general class of response handler type"
  [^String path]
  (cond
    false :input
    ; TODO; not sure
    false :client-certifiates
    :else :resource))

(defn file-ext
  [path]
  (let [resource-name (s/lower-case (last (s/split path #"/")))]
    (condp (fn [ext v] (s/ends-with? v ext)) resource-name
      "gmi" :gemini
      "png" :png
      "jpg" :jpeg
      "jpeg" :jpeg
      "txt" :text
      :gemini)))

(defn get-mime-type
  [path]
  (get mime-types (file-ext path)))

(defn get-file-body [file-path]
  (condp = (file-ext file-path)
    :png (slurp-bytes file-path)
    :jpeg (slurp-bytes file-path)
    (slurp file-path)))

; TODO: Not-implemented
(defn- handle-input-req [_req & _opts]
  {:header (headers/input)
   :body nil})

; TODO: Not-implemented
(defn- handle-resource-req [{:keys [path] :as _req} {:keys [public-path] :as _opts}]
  (try
    (let [headers (headers/success (get-mime-type path))
          body (get-file-body (resolve-file-path path public-path))]
      {:header headers
       :body body})
    (catch
     Exception
     e
      (println "Error handling request:" e)
      {:header (headers/not-found)
       :body nil})))

; TODO: Not-implemented
(defn- handle-client-certificates-req [_req & _opts]
  {:header (headers/certificate-not-valid)
   :body nil})

(def req-handlers
  {:input handle-input-req
   :client-certifiates handle-client-certificates-req
   :resource handle-resource-req})

(defn handle!
  "Handles a gemini request, returns a gemini response"
  [req opts]
  (let [handler-fn (get req-handlers (handler-type (:path req)))
        res (handler-fn req opts)]
    {:header (headers/to-str (:header res))
     :body (:body res)}))
