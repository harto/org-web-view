(ns owv.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<!]]
            [cljs.core.async.interop :refer [<p!]]
            [cljs.reader :refer [read-string]]
            [clojure.string :as str]
            [goog.string :as gs]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

(defonce state
  (r/atom {}))

(defn parse-date [s]
  ;; TODO: it's strongly recommended not to do this, but it seems to work fine
  ;; (https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/Date)
  (if s (js/Date. s)))

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

(defn fetch-todos [url]
  (go
    (let [response (<p! (js/fetch url))]
      (-> (js->clj (<p! (.json response)) :keywordize-keys true)
          (update :todos (fn [todos]
                           (map (fn [item]
                                  (-> item
                                      (update :created parse-date)
                                      (update :scheduled parse-date)
                                      (update :deadline parse-date)))
                                todos)))
          (update :dumped parse-date)))))

(defn load-todos! [url]
  (.log js/console "loading" url)
  (go
    (swap! state assoc :loading? true)
    (let [{:keys [todos dumped]} (<! (fetch-todos url))]
      (swap! state assoc :data-url url
                         :todos todos
                         :dumped dumped
                         :loading? false)
      (.setItem js/localStorage "owv.data-url" url))))

;; Configuration

(defn config-panel []
  (let [url (r/atom (:data-url @state))
        close! #(swap! state dissoc :configuring?)
        submit! #(do
                   (close!)
                   (load-todos! @url))]
    (fn []
      [:form {:on-submit #(do
                            (.preventDefault %)
                            (submit!))}
       [:div
        [:label {:for "todo-json-url"} "Data URL: "]
        [:input#todo-json-url {:type "text"
                               :value @url
                               :on-change #(reset! url (.-value (.-target %)))}]]
       [:div
        [:input {:type "submit"
                 :disabled (nil? @url)
                 :value "OK"}]
        [:button {:disabled (nil? (:data-url @state))
                  :on-click #(do
                               (.preventDefault %)
                               (close!))}
         "Cancel"]]])))

(defn loading-indicator []
  "Loading ...")

;; To-do list

(defn format-date [d]
  (if d
    (gs/format "%s %02d"
               (get ["Jan" "Feb" "Mar" "Apr" "May" "Jun"
                     "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"]
                    (.getMonth d))
               (.getDate d))))

(defn todo-group [{:keys [tag items]}]
  [:details.todo-group
   [:summary.todo-tag tag]
   (for [{:keys [state headline created scheduled deadline]} items]
     [:details {:key headline
                ;; TODO: figure out which things we want to see here
                ;; (e.g. waiting, deferred?)
                :data-todo-state (str/lower-case state)
                :data-stale (> (days-since created) 30)
                :data-scheduled (let [days-since-scheduled (days-since scheduled)]
                                  (cond (> days-since-scheduled 0) :overdue
                                        (= days-since-scheduled 0) :now))
                :data-deadline (let [days-until-deadline (days-until deadline)]
                                 (cond (< days-until-deadline 0) :overdue
                                       (< days-until-deadline 3) :now
                                       (<= days-until-deadline 7) :soon))}
      [:summary headline]
      [:div.todo-meta
       [:div "Created: " (if created (format-date created) "?")]
       (if scheduled [:div "Scheduled: " (format-date scheduled)])
       (if deadline [:div "Deadline: " (format-date deadline)])]])])

(defn time-ago [d]
  (str (minutes-since (:dumped @state)) " minutes ago"))

(defn todo-list []
  (let [groups (group-by #(first (:tags %)) (:todos @state))
        expanded-groups (:expanded-groups @state)]
    [:<>
     (for [[tag items] groups]
       ^{:key tag} [todo-group {:tag tag :items items}])
     (let [dumped-at (:dumped @state)]
       [:div.updated-at
        "Updated " [:span {:title dumped-at} (time-ago dumped-at)]])]))

;; Initialization

(defn root []
  (let [{:keys [loading? configuring?]} @state]
    [:<>
     [todo-list]
     (if loading? [loading-indicator])
     (if configuring? [config-panel])]))

(defn ^:dev/after-load mount-root []
  (let [root-el (.getElementById js/document "root")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [root] root-el)))

(defn init []
  (if-let [todos (.getItem js/localStorage "owv.todos")]
    (swap! state assoc :todos (read-string todos)))
  (if-let [url (.getItem js/localStorage "owv.data-url")]
    (load-todos! url)
    (swap! state assoc :configuring? true))
  (mount-root))
