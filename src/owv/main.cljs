(ns owv.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<!]]
            [cljs.core.async.interop :refer [<p!]]
            [cljs.reader :refer [read-string]]
            [clojure.string :as str]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

(defonce state
  (r/atom {}))

(defn parse-date [s]
  ;; TODO: it's strongly recommended not to do this, but it seems to work fine
  ;; (https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/Date)
  (if s (js/Date. s)))

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

(defn todo-item [{:keys [state headline created scheduled deadline]}]
  [:div.todo-item {:data-state (str/lower-case state)}
   (if created (str "Created: " created))
   (if scheduled (str "Scheduled: " scheduled))
   (if deadline (str "Deadline: " deadline))
   headline])

(defn todo-group [{:keys [tag items]}]
  [:details
   [:summary tag]
   (for [item items]
     ^{:key (:headline item)} [todo-item item])])

(defn todo-list []
  (let [groups (group-by #(first (:tags %)) (:todos @state))
        expanded-groups (:expanded-groups @state)]
    [:<>
     (for [[tag items] groups]
       ^{:key tag} [todo-group {:tag tag :items items}])
     [:div.footer
      (str "Last updated " (:dumped @state))]]))

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
