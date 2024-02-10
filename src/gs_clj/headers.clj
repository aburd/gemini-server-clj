(ns gs-clj.headers
  (:require [gs-clj.gemini :refer [max-response-meta-bytes
                                   statuses
                                   clrf]]
            [clojure.spec.alpha :as spec]))

(defn byte-len [s]
  (alength (.getBytes s "UTF-8")))

(defn valid-status? [status]
  (boolean (get statuses status)))

(defn valid-code? [code]
  (boolean (get (set (vals statuses)) code)))

(defn to-str
  "creates a response header as per description of '3.1 Response headers'
  code is a gemini-status code
  status is a key defined in gemini module"
  [{:keys [status code meta] :or {meta ""}}]
  {:pre [(or (spec/valid? valid-status? status)
             (spec/valid? valid-code? code))]}
  (if (> (byte-len meta) max-response-meta-bytes)
    (throw (new Exception (str "size of meta in gemini response must be " max-response-meta-bytes " bytes or less")))
    (str
     (or (statuses status) code)
     " "
     meta
     clrf)))

; Request - Input
(defn input
  "As per definition of single-digit code 1 in 3.2. "
  []
  {:status :input})

(defn sensitive-input
  "As per status code 10, but for use with sensitive input such as passwords. Clients should present the prompt as per status code 10, but the user's input should not be echoed to the screen to prevent it being read by \"shoulder surfers\"."
  []
  {:status :sensitive-input})

; Request - Success
(defn success
  "As per definition of single-digit code 2 in 3.2."
  [^String mime-type]
  {:status :success
   :meta mime-type})

; Request - Redirect
(defn redirect-temporary
  "As per definition of single-digit code 3 in 3.2."
  [^String uri]
  {:status :redirect-temporary
   :meta uri})

(defn redirect-permanent
  "The requested resource should be consistently requested from the new URL provided in future. Tools like search engine indexers or content aggregators should update their configurations to avoid requesting the old URL, and end-user clients may automatically update bookmarks, etc. Note that clients which only pay attention to the initial digit of status codes will treat this as a temporary redirect. They will still end up at the right place, they just won't be able to make use of the knowledge that this redirect is permanent, so they'll pay a small performance penalty by having to follow the redirect each time."
  [^String uri]
  {:status :redirect-permanent
   :meta uri})

; Request - Permanent Failure
(defn temporary-failure
  "As per definition of single-digit code 4 in 3.2."
  []
  {:status :temporary-failure})

(defn server-unavailable
  "The server is unavailable due to overload or maintenance. (cf HTTP 503)"
  []
  {:status :server-unavailable})

(defn cgi-error
  "A CGI process, or similar system for generating dynamic content, died unexpectedly or timed out."
  []
  {:status :cgi-error})

(defn proxy-error
  "A proxy request failed because the server was unable to successfully complete a transaction with the remote host. (cf HTTP 502, 504)"
  []
  {:status :proxy-error})

(defn slow-down
  "Rate limiting is in effect. <META> is an integer number of seconds which the client must wait before another request is made to this server. (cf HTTP 429) "
  []
  {:status :slow-down})

(defn permanent-failure
  ([]
   (permanent-failure ""))
  ([msg]
   {:status :permanent-failure
    :meta msg}))

(defn not-found []
  {:status :not-found})

(defn gone []
  {:status :gone})

(defn proxy-request-refused []
  {:status :proxy-request-refused})

(defn bad-request []
  {:status :bad-request})

(defn client-certificate-required
  ([]
   (client-certificate-required "Client certificate required"))
  ([msg]
   {:status :client-certificate-required
    :meta msg}))

(defn certificate-not-authorized
  ([]
   (certificate-not-authorized "Client certificate not authorized"))
  ([msg]
   {:status :certificate-not-authorized
    :meta msg}))

(defn certificate-not-valid
  ([]
   (certificate-not-valid "Client certificate not valid"))
  ([msg]
   {:status :certificate-not-valid
    :meta msg}))

