(ns owv.main
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

(defonce state
  (r/atom {:todos []}))

(defn todo-item [{:keys [state headline created scheduled deadline]}]
  [:div.todo-item {:data-state (str/lower-case state)}
   headline])

(defn todo-group [{:keys [tag items]}]
  [:details
   [:summary tag]
   (for [item items]
     ^{:key (:headline item)} [todo-item item])])

(defn root []
  (let [groups (group-by #(first (:tags %)) (:todos @state))
        expanded-groups (:expanded-groups @state)]
    [:<>
     (for [[tag items] groups]
       ^{:key tag} [todo-group {:tag tag :items items}])]))

(defn ^:dev/after-load mount-root []
  (let [root-el (.getElementById js/document "root")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [root] root-el)))

(defn init [todo-data]
  (swap! state assoc :todos (js->clj todo-data :keywordize-keys true))
  (.log js/console @state)
  (mount-root))
