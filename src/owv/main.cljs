(ns owv.main
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

(defonce todos
  (r/atom nil))

(defn todo-item [{:keys [state headline created scheduled deadline] :as item}]
  [:li.todo-item {:class (str "state-" (str/lower-case state))}
   headline ])

(defn todo-list [items]
  [:ul.todo-list
   (for [item items]
     [todo-item ^{:key (:headline item)} item])])

(defn root []
  (let [groups (group-by #(first (:tags %)) @todos)]
    [:dl#root
     (for [[tag items] groups]
       [:<>
        [:dt.group-header ^{:key (str tag "-header")} tag]
        [:dl.group-body ^{:key (str tag "-body")}
         [todo-list items]]])]))

(defn ^:dev/after-load mount-root []
  (let [root-el (.getElementById js/document "root")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [root] root-el)))

(defn init [todo-data]
  (reset! todos (js->clj todo-data :keywordize-keys true))
  (mount-root))
