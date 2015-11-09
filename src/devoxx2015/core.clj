(ns devoxx2015.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.pprint :refer [pprint]]))

[1 2 3]
(get [1 2 3] 2)

(def talk
  {:title "BeClojure School: A gentle introduction to Clojure"})

(assoc talk
  :lang "en"
  :talkType "BOF (Bird of a Feather)"
  :summary "During this BeClojure School session, we will introduce the audience to the Clojure programming language. Concepts being explained range from the lisp syntax over immutable data structures and lazy sequences to concurrency constructs. This will be an interactive session where participants can code along, ask questions and trigger discussions.\r\n\r\n[BeClojure](http://www.meetup.com/BeClojure/) is the User Group of Belgian Clojure enthusiasts. It tries to bring together companies, universities, students and hobbyists to share their experiences and expand their knowledge. It was founded in 2012 and has steadily been attracting more attendees.")

talk                                                        ;; still the same

(def updated-talk
  (assoc talk
    :lang "en"
    :talkType "BOF (Bird of a Feather)"
    :lang "en"
    :summary "During this BeClojure School session, we will introduce the audience to the Clojure programming language. Concepts being explained range from the lisp syntax over immutable data structures and lazy sequences to concurrency constructs. This will be an interactive session where participants can code along, ask questions and trigger discussions.\r\n\r\n[BeClojure](http://www.meetup.com/BeClojure/) is the User Group of Belgian Clojure enthusiasts. It tries to bring together companies, universities, students and hobbyists to share their experiences and expand their knowledge. It was founded in 2012 and has steadily been attracting more attendees."))


;; work towards threading macro
(pprint (http/get "http://cfp.devoxx.be/api/conferences/DV15/talks/BSR-7365"))

(:body (http/get "http://cfp.devoxx.be/api/conferences/DV15/talks/BSR-7365"))

(pprint (json/parse-string (:body (http/get "http://cfp.devoxx.be/api/conferences/DV15/talks/BSR-7365")) true))


(def beclojure-talk
  (-> (http/get "http://cfp.devoxx.be/api/conferences/DV15/talks/BSR-7365")
      (:body)
      (json/parse-string true)))

(comment
  (def beclojure-talk
    (-> (http/get "http://cfp.devoxx.be/api/conferences/DV15/talks/BSR-7365")
        (:body _)
        (json/parse-string _ true)))
  )

(keys beclojure-talk)

(:id beclojure-talk)

(select-keys beclojure-talk [:lang :title :talkType])


(def talks (-> (http/get "http://cfp.devoxx.be/api/conferences/DV15/talks")
               :body
               (json/parse-string true)
               :talks
               :accepted))

(count talks)

(first talks)

(filter (fn [talk] (= (-> talk :talkType :id) "bof")) talks)

(count (filter #(= (-> % :talkType :id) "bof") talks))

(map :mainSpeaker talks)

(defn speakers
  [talk]
  (-> talk
      (select-keys [:mainSpeaker :secondarySpeaker])
      vals))

(map speakers talks)

(mapcat speakers talks)

(count (mapcat speakers talks))

(count (distinct (mapcat speakers talks)))

(->> talks
     (mapcat speakers)
     (distinct)
     (count))

(comment
  (->> talks
       (mapcat speakers _)
       (distinct _)
       (count _))
  )

;; who's performing the most talks at Devoxx?

(->> talks
     (group-by :mainSpeaker)
     seq)


(->> talks
     (group-by :mainSpeaker)
     (map #(vector (first %) (-> % second count)))
     (sort-by second)
     (reverse))

;; let's find out the number of hours of talks at devoxx

(defn get-schedule
  [day]
  (-> (http/get (str "http://cfp.devoxx.be/api/conferences/DV15/schedules/" day))
      :body
      (json/parse-string true)
      :slots))

(->> (get-schedule "monday")
     (map (fn [{:keys [fromTimeMillis toTimeMillis]}]
            (/ (- toTimeMillis fromTimeMillis)
               (* 1000 60 60))))
     (reduce +)
     (double))

(def days ["monday" "tuesday" "wednesday" "thursday" "friday"])

(->> days
     (mapcat get-schedule)
     (map (fn [{:keys [fromTimeMillis toTimeMillis]}]
            (/ (- toTimeMillis fromTimeMillis)
               (* 1000 60 60))))
     (reduce +)
     (int))

;; but this takes quite some time...
;; let's cache some things for a second run or other calculation

;; CACHE = STATE! :)

(defn cached
  [f]
  (let [cache (atom {})]
    (fn [request]
      (if-let [cached (get @cache request)]
        cached
        (let [result (f request)]
          (swap! cache assoc request result)
          result)))))

(defn get-schedule-new
  [request-fn day]
  (-> (request-fn (str "http://cfp.devoxx.be/api/conferences/DV15/schedules/" day))
      :body
      (json/parse-string true)
      :slots))

(def cached-get (cached http/get))

(get-schedule-new cached-get "tuesday")

(time (->> days
           (mapcat (partial get-schedule-new cached-get))
           (map (fn [{:keys [fromTimeMillis toTimeMillis]}]
                  (/ (- toTimeMillis fromTimeMillis)
                     (* 1000 60 60))))
           (reduce +)
           (int)))

;; do some java interrop

(new java.util.Date)

(-> (new java.util.Date)
    (.getTime)
    (+ 10))

;; state

(def a (ref 42))
(def b (ref 3))

(defn swap [x y]
  (dosync
    (let [c @x]
      (ref-set x @y)
      (ref-set y c))))

(swap a b)
(println @a " - " @b)
