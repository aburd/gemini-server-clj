(ns gs-clj.request
  (:require
   [clojure.string :as s]
   [taoensso.timbre :as log]
   [clojure.java.io :as io]
   [gs-clj.gemini :refer [clrf max-request-bytes]]
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
    ; Do not allow path traversal in fs
    (s/replace (str public-path path) ".." "")))

; TODO: Not implemented
; only handle resources for now, input can happen later
(defn- handler-type
  "get the general class of response handler type"
  [^String path]
  (cond
    false :input
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
      nil)))

(def mime-types {:gemini "text/gemini; charset=utf-8"
                 :text "text/plain; charset=utf-8"
                 :png "media/png"
                 :jpeg "media/jpeg"})

(defn text-response
  [file-path]
  (try
    {:header (headers/success (:gemini mime-types))
     :body {:utf8 (slurp file-path)}}
    (catch
     Exception
     e
      (log/error "could not handle gemini response:" e)
      (log/error "FILE PATH" file-path)
      {:header (headers/not-found)
       :body {}})))

(defn file-response
  [file-path ext]
  (try
    (cond
      (or (nil? ext) (not (.exists (io/file file-path))))
      {:header (headers/not-found)
       :body {}}
      (not (nil? ext))
      {:header (headers/success ext)
       :body {:bytes (slurp-bytes file-path)}})
    (catch
     Exception
     e
      (log/error "could not handle gemini response:" e)
      (log/error "FILE PATH" file-path "EXT" ext)
      {:header (headers/not-found)
       :body {}})))

(defn- handle-resource-req [req {:keys [public-path] :as _opts}]
  (let [file-path (resolve-file-path (:path req) public-path)
        ext (file-ext file-path)]
    (condp = ext
      :gemini (text-response file-path)
      :text (text-response file-path)
      (file-response file-path ext))))

; TODO: Not-implemented
(defn- handle-input-req [_req & _opts]
  {:header (headers/input)
   :body nil})

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
  (let [handler-fn (get req-handlers (handler-type (:path req)))]
    (handler-fn req opts)))
