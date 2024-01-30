(ns gs-clj.headers
  (:require [gs-clj.gemini :as gemini]))

(defn byte-len [s]
  (alength (.getBytes s "UTF-8")))

(defn header
  "creates a response header as per description of '3.1 Response headers'"
  [^Integer status & {:keys [meta] :or {meta ""}}]
  (if (> (byte-len meta) gemini/max-response-meta-bytes)
    (throw (new Exception (str "size of meta in gemini response must be " gemini/max-response-meta-bytes " bytes or less")))
    (str status " " meta gemini/clrf)))

; Request - Input
(defn input
  "As per definition of single-digit code 1 in 3.2. "
  []
  (header 10))

(defn sensitive-input
  "As per status code 10, but for use with sensitive input such as passwords. Clients should present the prompt as per status code 10, but the user's input should not be echoed to the screen to prevent it being read by \"shoulder surfers\"."
  []
  (header 11))

; Request - Success
(defn success
  "As per definition of single-digit code 2 in 3.2."
  [^String mime-type]
  (header 20 {:meta mime-type}))

; Request - Redirect
(defn redirect-temporary
  "As per definition of single-digit code 3 in 3.2."
  [^String uri]
  (header 30 {:meta uri}))

(defn redirect-permanent
  "The requested resource should be consistently requested from the new URL provided in future. Tools like search engine indexers or content aggregators should update their configurations to avoid requesting the old URL, and end-user clients may automatically update bookmarks, etc. Note that clients which only pay attention to the initial digit of status codes will treat this as a temporary redirect. They will still end up at the right place, they just won't be able to make use of the knowledge that this redirect is permanent, so they'll pay a small performance penalty by having to follow the redirect each time."
  [^String uri]
  (header 31 {:meta uri}))

; Request - Permanent Failure
(defn temporary-failure
  "As per definition of single-digit code 4 in 3.2."
  []
  (header 40))

(defn server-unavailable
  "The server is unavailable due to overload or maintenance. (cf HTTP 503)"
  []
  (header 41))

(defn cgi-error
  "A CGI process, or similar system for generating dynamic content, died unexpectedly or timed out."
  []
  (header 42))

(defn proxy-error
  "A proxy request failed because the server was unable to successfully complete a transaction with the remote host. (cf HTTP 502, 504)"
  []
  (header 43))

(defn slow-down
  "Rate limiting is in effect. <META> is an integer number of seconds which the client must wait before another request is made to this server. (cf HTTP 429) "
  []
  (header 44))

(defn permanent-failure
  ([msg]
   (header 50 {:meta msg}))
  ([]
   (header 50)))
