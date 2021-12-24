(ns owv.api
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async.interop :refer [<p!]]
            [re-frame.core :refer [dispatch reg-fx]]))

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
          payload (js->clj (<p! (.json response)) :keywordize-keys true)
          todos (map parse-todo (:todos payload))
          updated-at (parse-date (:dumped payload))]
      (dispatch [:todos-loaded {:todos todos :updated-at updated-at}]))))

(reg-fx ::fetch fetch)
