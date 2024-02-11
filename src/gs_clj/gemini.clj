(ns gs-clj.gemini)

(def default-port 1965)

(def max-request-bytes 1024)

(def max-response-meta-bytes 1024)

(def clrf "\r\n")

(def statuses {; input
               :input 10
               :sensitive-input 11
               ; success})
               :success 20
               ; redirection})
               :temporary-redirection 30
               :permanent-redirection 31
               ; temporary-failure})
               :temporary-failure 40
               :server-unavailable 41
               :cgi-error 42
               :proxy-error 43
               :slow-down 44
               ; permanent-failure})
               :permanent-failure 50
               :not-found 51
               :gone 52
               :proxy-request-refused 53
               :bad-request 59
               ; client certificates               
               :client-certificate-required 60
               :certificate-not-authorized 61
               :certificate-not-valid 62})

(def mime-types {:gemini "text/gemini; charset=utf-8"
                 :text "text/plain; charset=utf-8"})
