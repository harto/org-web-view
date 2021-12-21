(ns owv.todo-api
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async.interop :refer [<p!]]
            [re-frame.core :refer [dispatch reg-fx]]))

(defn init []
  (if-let [data-url (.getItem js/localStorage "owv.data-url")]
    (dispatch [:load-todos data-url])
    (dispatch [:open-config-panel])))

(defn save-url [url]
  (.setItem js/localStorage "owv.data-url" url))

(defn parse-date [s]
  (if s (js/Date. s)))

(defn parse-todo [item]
  (-> item
      (update :created parse-date)
      (update :scheduled parse-date)
      (update :deadline parse-date)))

(defn fetch [url]
  (go
    (let [response (<p! (js/fetch url))
          {:keys [todos dumped]} (-> (js->clj (<p! (.json response)) :keywordize-keys true)
                                     (update :todos #(map parse-todo %))
                                     (update :dumped parse-date))]
      (dispatch [:todos-loaded {:todos todos :updated-at dumped}])
      ;; (.setItem js/localStorage "owv.data-url" url)
      )))

(reg-fx ::init init)
(reg-fx ::save-url save-url)
(reg-fx ::fetch fetch)
