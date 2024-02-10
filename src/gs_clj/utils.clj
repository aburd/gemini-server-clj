(ns gs-clj.utils
  (:require [clojure.string :as string]))

(defn emphasize
  [s & {:keys [len c] :or {len 20 c '*}}]
  (let [s-len (count s)
        star-count (/ (- len s-len) 2)]
    (if (> s-len len)
      s
      (let [stars (apply str (take star-count (repeat c)))]
        (str stars (string/upper-case s) stars)))))

(defn- byte-len [s]
  (alength (.getBytes s "UTF-8")))

(defn byte-len-within [s len]
  (< (byte-len s) len))
