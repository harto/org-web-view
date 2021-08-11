(ns owv.date
  (:require
   [goog.string :as gs]
   goog.string.format))

(defn format-date [d]
  (if d
    (gs/format "%s %02d"
               (get ["Jan" "Feb" "Mar" "Apr" "May" "Jun"
                     "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"]
                    (.getMonth d))
               (.getDate d))))

(defn minutes-since [d]
  (if d
    (Math/floor (/ (- (js/Date.) d) 60000))
    js/NaN))

(defn min->hour [m] (/ m 60))
(defn hour->day [h] (/ h 24))

(defn days-since [d]
  (-> (minutes-since d) (min->hour) (hour->day) (Math/round)))

(defn days-until [d]
  (- (days-since d)))
